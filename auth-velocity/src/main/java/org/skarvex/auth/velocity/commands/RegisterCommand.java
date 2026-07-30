package org.skarvex.auth.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.skarvex.auth.core.service.AuthService;
import org.skarvex.auth.velocity.manager.ConfigurationManager;
import org.skarvex.auth.velocity.utils.Messages;

import java.util.UUID;

public class RegisterCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigurationManager config;
    private final AuthService authService;

    public RegisterCommand(ProxyServer server,
                           ConfigurationManager config,
                           AuthService authService) {
        this.server = server;
        this.config = config;
        this.authService = authService;
    }

    @Override
    public void execute(Invocation invocation) {

        Player player = (Player) invocation.source();
        UUID uuid = player.getUniqueId();
        String playerIp = player.getRemoteAddress().getAddress().getHostAddress();

        String[] arguments = invocation.arguments();

        if (arguments.length != 1) {
            player.sendMessage(Messages.parse(config.getString
                    ("messages.register.usage"))
            );

            return;
        }

        if (authService.isRegistered(uuid)) {
            player.sendMessage(Messages.parse(
                    config.getString("messages.already-registered"))
            );

            return;
        }

        String password = arguments[0];

        if (password.length() < config.getInt("register.min-password-chars")) {
            player.sendMessage(Messages.parse(
                    config.getString("messages.insufficient-characters")
            ));
            return;
        }

        if (authService.register(uuid, password, playerIp, playerIp, true)) {
            player.sendMessage(Messages.parse(
                    config.getString("messages.register.success"))
            );

            server.getServer("lobby").ifPresent(lobby ->
                    player.createConnectionRequest(lobby).fireAndForget());

        }
    }
}
