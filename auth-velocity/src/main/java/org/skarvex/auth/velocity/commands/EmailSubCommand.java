package org.skarvex.auth.velocity.commands.email;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import org.skarvex.auth.core.service.AuthService;
import org.skarvex.auth.velocity.manager.ConfigurationManager;
import org.skarvex.auth.velocity.manager.TemplateManager;
import org.skarvex.auth.velocity.utils.Messages;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

public class EmailSubCommand implements SimpleCommand {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final ConfigurationManager config;
    private final AuthService authService;
    private final TemplateManager templateManager;
    private final ExecutorService executorService;

    public EmailSubCommand(ConfigurationManager config,
                           AuthService authService,
                           TemplateManager templateManager,
                           ExecutorService executorService) {
        this.config = config;
        this.authService = authService;
        this.templateManager = templateManager;
        this.executorService = executorService;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Messages.parse("messages.not-a-player"));
            return;
        }

        UUID uuid = player.getUniqueId();
        String[] arguments = invocation.arguments();

        if (arguments.length == 0) {
            sendUsage(player);
        }

        if (!authService.isAuthenticated(player.getUniqueId())) {
            player.sendMessage(Messages.parse(
                    config.getString("messages.email.link.must-be-logged")
            ));
            return;
        }

        switch (arguments[0].toLowerCase()) {
            case "add" -> handleAdd(player, arguments);
            case "confirm" -> handleConfirm(player, arguments);
            case "recovery" -> handleRecovery(player, arguments);
            case "code" -> handleCode(player, arguments);
            default -> sendUsage(player);
        }
    }

    private void handleAdd(Player player, String[] arguments) {
        if (arguments.length < 2) {
            player.sendMessage(Messages.parse(config.getString("messages.email.link.usage")));
            return;
        }

        String email = arguments[1];
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            player.sendMessage(Messages.parse(config.getString("messages.email.link.wrong-email")));
            return;
        }

        
    }

    private void sendUsage(Player player) {
        player.sendMessage(Messages.parse(config.getString("messages.email.link.usage")));
        player.sendMessage(Messages.parse(config.getString("messages.email.confirm.usage")));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0 || args.length == 1) {
            return List.of("add", "confirm");
        }
        return List.of();
    }
}
