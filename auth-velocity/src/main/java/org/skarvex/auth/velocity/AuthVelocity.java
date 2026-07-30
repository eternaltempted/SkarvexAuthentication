package org.skarvex.auth.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.skarvex.auth.core.repository.RestrictedIpRepository;
import org.skarvex.auth.core.repository.UserRepository;
import org.skarvex.auth.core.security.BCryptPasswordHasher;
import org.skarvex.auth.core.security.PasswordHasher;
import org.skarvex.auth.core.service.AuthService;
import org.skarvex.auth.core.service.BruteForceService;
import org.skarvex.auth.core.service.LoginTimeoutService;
import org.skarvex.auth.core.service.SessionService;
import org.skarvex.auth.core.util.TimeUtil;
import org.skarvex.auth.velocity.commands.LoginCommand;
import org.skarvex.auth.velocity.commands.LogoutCommand;
import org.skarvex.auth.velocity.commands.RegisterCommand;
import org.skarvex.auth.velocity.commands.ResetPasswordCommand;
import org.skarvex.auth.velocity.database.DatabaseManager;
import org.skarvex.auth.core.repository.jdbc.JdbcRestrictedIpRepository;
import org.skarvex.auth.core.repository.jdbc.JdbcUserRepository;
import org.skarvex.auth.velocity.manager.ConfigurationManager;
import org.skarvex.auth.velocity.scheduler.LoginReminderScheduler;
import org.skarvex.auth.velocity.scheduler.LoginTimeoutScheduler;
import org.skarvex.auth.velocity.utils.Messages;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthVelocity {

    @Inject
    private Logger logger;
    @Inject
    private ProxyServer proxy;

    @Inject
    @DataDirectory
    private Path dataDirectory;

    private ConfigurationManager config;

    private SessionService sessions;
    private PasswordHasher hasher;
    private UserRepository users;
    private RestrictedIpRepository restrictedIps;
    private DatabaseManager database;
    private AuthService authService;

    private LoginTimeoutService loginTimeoutService;
    private BruteForceService bruteForceService;

    private LoginTimeoutScheduler timeoutScheduler;
    private LoginReminderScheduler reminderScheduler;

    private ExecutorService asyncExecutor;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.asyncExecutor = Executors.newCachedThreadPool();
        loadConfiguration();
        setupDatabase();
        setupServices();
        registerCommands();
        registerSchedulers();
    }

    @Subscribe
    public void onConnect(ServerPreConnectEvent event) {

        Player customer = event.getPlayer();

        if (authService.isAuthenticated(customer.getUniqueId())) {
            return;
        }

        String target =
                event.getOriginalServer()
                        .getServerInfo()
                        .getName();

        if (target.equals(config.getString("servers.initial-server"))) {
            return;
        }

        event.setResult(
                ServerPreConnectEvent.ServerResult.denied()
        );
    }

    @Subscribe
    public void onJoin(PlayerChooseInitialServerEvent event, Continuation continuation) {
        Player customer = event.getPlayer();
        UUID uuid = customer.getUniqueId();
        String incomingIp = customer.getRemoteAddress().getAddress().getHostAddress();

        CompletableFuture.supplyAsync(() -> authService.tryAutoLogin(uuid, incomingIp), asyncExecutor)
                .thenCompose(autoLogin -> {
                    if (autoLogin) {
                        proxy.getServer(config.getString("servers.forward-server")).ifPresent(event::setInitialServer);
                        return CompletableFuture.completedFuture(null);
                    }

                    return CompletableFuture.supplyAsync(() -> bruteForceService.getRemainingTime(incomingIp), asyncExecutor)
                            .thenAccept(remainingTimeOpt -> {
                                if (remainingTimeOpt.isPresent()) {
                                    customer.disconnect(
                                            Messages.parse(config.getString("messages.login.kick-screen")
                                                    .replace("<time>", TimeUtil.formatDuration(remainingTimeOpt.get())))
                                    );
                                    return;
                                }

                                loginTimeoutService.start(uuid, Duration.ofSeconds(config.getInt("login.timeout")));
                                timeoutScheduler.startTimeout(uuid);

                                if (!authService.isAuthenticated(uuid)) {
                                    reminderScheduler.start(uuid);
                                    proxy.getServer(config.getString("servers.initial-server"))
                                            .ifPresent(event::setInitialServer);
                                }
                            });
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Error during join process", throwable);
                    }
                    continuation.resume();
                });
    }

    @Subscribe
    public void onProxyShutdown(
            ProxyShutdownEvent event
    ) {
        if (database != null) {
            database.disconnect();
        }
    }

    @Subscribe
    public void onDisconnect(
            DisconnectEvent event
    ) {
        Player customer = event.getPlayer();
        UUID uuid = customer.getUniqueId();

        authService.logout(uuid);

        loginTimeoutService.cancel(uuid);
        timeoutScheduler.removeTask(uuid);
    }

    private void loadConfiguration() {
        this.config = new ConfigurationManager(dataDirectory);
        config.load();
    }

    private void setupDatabase() {
        this.database = new DatabaseManager(
                config,
                logger
        );

        database.connect();
    }

    private void setupServices() {
        this.sessions = new SessionService();
        this.hasher = new BCryptPasswordHasher();
        this.users = new JdbcUserRepository(database.getDataSource());
        this.restrictedIps = new JdbcRestrictedIpRepository(database.getDataSource());
        this.loginTimeoutService = new LoginTimeoutService();
        this.authService = new AuthService(users, hasher, sessions);
        this.bruteForceService = new BruteForceService(restrictedIps);
    }

    private void registerCommands() {
        proxy.getCommandManager().register(
                proxy.getCommandManager()
                        .metaBuilder("register")
                        .aliases("reg")
                        .build(),
                new RegisterCommand(
                        proxy,
                        config,
                        authService
                )
        );

        proxy.getCommandManager().register(
                proxy.getCommandManager()
                        .metaBuilder("login")
                        .aliases("l")
                        .build(),
                new LoginCommand(
                        proxy,
                        config,
                        authService,
                        bruteForceService,
                        loginTimeoutService,
                        asyncExecutor
                )
        );

        proxy.getCommandManager().register(
                proxy.getCommandManager()
                        .metaBuilder("logout")
                        .build(),
                new LogoutCommand(
                        proxy,
                        config,
                        authService
                )
        );

        proxy.getCommandManager().register(
                proxy.getCommandManager()
                        .metaBuilder("changepass")
                        .build(),
                new ResetPasswordCommand(
                        proxy,
                        config,
                        authService
                )
        );
    }

    private void registerSchedulers() {
        this.timeoutScheduler = new LoginTimeoutScheduler(this, proxy, authService, loginTimeoutService, config);
        this.reminderScheduler = new LoginReminderScheduler(this, proxy, authService, config);
    }
}
