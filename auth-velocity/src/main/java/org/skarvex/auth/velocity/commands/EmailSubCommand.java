package org.skarvex.auth.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import org.skarvex.auth.core.model.EmailBindResult;
import org.skarvex.auth.core.model.EmailStatus;
import org.skarvex.auth.core.service.AuthService;
import org.skarvex.auth.velocity.manager.ConfigurationManager;
import org.skarvex.auth.velocity.manager.TemplateManager;
import org.skarvex.auth.velocity.utils.Messages;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

public class EmailSubCommand implements SimpleCommand {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final ConfigurationManager config;
    private final AuthService authService;
    private final TemplateManager templateManager;
    private final ExecutorService executor;

    public EmailSubCommand(ConfigurationManager config,
                           AuthService authService,
                           TemplateManager templateManager,
                           ExecutorService executorService) {
        this.config = config;
        this.authService = authService;
        this.templateManager = templateManager;
        this.executor = executorService;
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
            return;
        }

        if (!authService.isAuthenticated(player.getUniqueId())) {
            player.sendMessage(Messages.parse(
                    config.getString("messages.not-logged")
            ));
            return;
        }

        switch (arguments[0].toLowerCase()) {
            case "link" -> handleLink(player, arguments);
            case "info" -> handleInfo(player, arguments);
            case "confirm" -> handleConfirm(player, arguments);
            case "unlink" -> handleUnlink(player, arguments);
            default -> sendUsage(player);
        }
    }

    private void handleLink(Player player, String[] arguments) {
        if (arguments.length < 2) {
            player.sendMessage(Messages.parse(config.getString("messages.email.link.usage")));
            return;
        }

        String email = arguments[1];
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            player.sendMessage(Messages.parse(config.getString("messages.email.link.wrong-email")));
            return;
        }

        executor.execute(() -> {
            EmailBindResult bindResult = authService.requestEmailBinding(
                    player.getUniqueId(),
                    email,
                    templateManager.getTemplate("binding", email)
            );

            switch (bindResult) {
                case SUCCESS -> player.sendMessage(Messages.parse(
                        config.getString("messages.email.link.success").replace("<email>", email)
                ));
                case AWAITING_CONFIRMATION -> player.sendMessage(Messages.parse(
                        config.getString("messages.email.link.already-sent")
                ));
                case ALREADY_BOUND -> player.sendMessage(Messages.parse(
                        config.getString("messages.email.link.already-bound")
                ));
            }
        });
    }

    private void handleConfirm(Player player, String[] arguments) {
        if (arguments.length < 2) {
            player.sendMessage(Messages.parse(config.getString("messages.email.confirm.usage")));
            return;
        }

        String code = arguments[1];

        executor.execute(() -> {
            EmailBindResult bindResult = authService.confirmEmailBinding(player.getUniqueId(), code);

            switch (bindResult) {
                case SUCCESS -> player.sendMessage(Messages.parse(
                        config.getString("messages.email.confirm.success")
                ));
                case INVALID_CODE -> player.sendMessage(Messages.parse(
                        config.getString("messages.email.confirm.invalid-code")
                ));
                case NO_PENDING_REQUEST -> player.sendMessage(Messages.parse(
                        config.getString("messages.email.confirm.no-pending-request")
                ));
            }
        });
    }

    private void handleUnlink(Player player, String[] arguments) {
        if (arguments.length > 1) {
            player.sendMessage(Messages.parse(config.getString("messages.email.unlink.usage")));
            return;
        }

        executor.execute(() -> {
            EmailBindResult bindResult = authService.unbindEmail(player.getUniqueId());

            switch (bindResult) {
                case SUCCESS -> player.sendMessage(Messages.parse(
                        config.getString("messages.email.unlink.success")
                ));
                case NOT_BOUND -> player.sendMessage(Messages.parse(
                        config.getString("messages.email.unlink.not-bound")
                ));
            }
        });
    }

    private void handleInfo(Player player, String[] arguments) {
        if (arguments.length > 1) {
            player.sendMessage(Messages.parse(config.getString("messages.email.info.usage")));
            return;
        }

        executor.execute(() -> {
            Optional<String> email = authService.getEmail(player.getUniqueId());

            if (email.isPresent()) {
                String message = config.getString("messages.email.info.success").replace("<email>", email.get());
                player.sendMessage(Messages.parse(message));
                return;
            }

            player.sendMessage(Messages.parse(
                    config.getString("messages.email.info.failed")
            ));
        });
    }

    private void sendUsage(Player player) {
        player.sendMessage(Messages.parse(config.getString("messages.email.link.usage")));
        player.sendMessage(Messages.parse(config.getString("messages.email.info.usage")));
        player.sendMessage(Messages.parse(config.getString("messages.email.confirm.usage")));
        player.sendMessage(Messages.parse(config.getString("messages.email.unlink.usage")));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0 || args.length == 1) {
            return List.of("link", "unlink", "confirm", "info");
        }
        return List.of();
    }
}
