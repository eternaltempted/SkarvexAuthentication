package org.skarvex.auth.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.skarvex.auth.core.service.AuthService;
import org.skarvex.auth.core.service.BruteForceService;
import org.skarvex.auth.core.service.LoginTimeoutService;
import org.skarvex.auth.core.util.TimeUtil;
import org.skarvex.auth.velocity.manager.ConfigurationManager;
import org.skarvex.auth.velocity.utils.Messages;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class LoginCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigurationManager config;
    private final AuthService authService;
    private final BruteForceService bruteForceService;
    private final LoginTimeoutService loginTimeoutService;
    private final ExecutorService asyncExecutor;

    public LoginCommand(ProxyServer server,
                        ConfigurationManager config,
                        AuthService authService,
                        BruteForceService bruteForceService,
                        LoginTimeoutService loginTimeoutService,
                        ExecutorService asyncExecutor) {
        this.server = server;
        this.config = config;
        this.authService = authService;
        this.bruteForceService = bruteForceService;
        this.loginTimeoutService = loginTimeoutService;
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public void execute(Invocation invocation) {

        Player player = (Player) invocation.source();
        UUID uuid = player.getUniqueId();
        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        String[] arguments = invocation.arguments();

        if (arguments.length != 1) {
            player.sendMessage(Messages.parse(config.getString
                    ("messages.login.usage"))
            );
            return;
        }

        if (authService.isAuthenticated(uuid)) {
            player.sendMessage(Messages.parse(
                    config.getString("messages.already-logged"))
            );
            return;
        }

        if (!authService.isRegistered(uuid)) {
            player.sendMessage(Messages.parse(config.getString("messages.not-registered")));
            return;
        }

        String password = arguments[0];

        if (password.length() < config.getInt("register.min-password-chars")) {
            player.sendMessage(Messages.parse(
                    config.getString("messages.insufficient-characters")
            ));
            return;
        }

        if (!authService.authenticate(uuid, password, ip)) {

            int attempts = bruteForceService.increment(ip);

            if (attempts >= bruteForceService.getMaxAttempts()) {
                CompletableFuture.runAsync(() -> bruteForceService.block(ip), asyncExecutor).thenRun(() -> {
                    bruteForceService.getRemainingTime(ip).ifPresent(duration -> {
                        player.disconnect(
                                Messages.parse(config.getString("messages.login.kick-screen")
                                        .replace("<time>", TimeUtil.formatDuration(duration))
                                )
                        );
                    });
                });
            }

            player.sendMessage(Messages.parse(
                    config.getString("messages.wrong-credentials")
            ));

            return;
        }

        authService.authenticate(uuid, password, ip);
        bruteForceService.reset(ip);

        player.sendMessage(Messages.parse(
                config.getString("messages.login.success"))
        );

        server.getServer("lobby").ifPresent(lobby ->
                player.createConnectionRequest(lobby).fireAndForget());
    }
}