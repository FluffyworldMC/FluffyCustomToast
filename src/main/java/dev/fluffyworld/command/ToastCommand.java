package dev.fluffyworld.command;

import dev.fluffyworld.FluffyCustomToast;
import dev.fluffyworld.toast.ToastFrame;
import dev.fluffyworld.toast.ToastService;
import dev.fluffyworld.toast.ToastSettings;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ToastCommand implements CommandExecutor, TabCompleter {

    private static final List<String> FRAMES = List.of("task", "goal", "challenge");
    private static final List<String> TOGGLES = List.of("on", "off");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("noti")) {
            return handleNotification(sender, args);
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            return handleReload(sender);
        }
        return handleSend(sender, args);
    }

    private boolean handleSend(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("fluffycustomtoast.use")) {
            send(sender, "no-permission");
            return true;
        }
        if (args.length < 4) {
            send(sender, "usage-send");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            send(sender, "player-not-found");
            return true;
        }

        ToastFrame frame = ToastFrame.parse(args[1]);
        if (frame == null) {
            send(sender, "invalid-frame");
            return true;
        }

        ItemStack icon = ToastService.parseIcon(args[2]);
        if (icon == null) {
            send(sender, "invalid-item");
            return true;
        }

        ToastSettings settings = FluffyCustomToast.getSettings();
        String[] messageArgs = Arrays.copyOfRange(args, 3, args.length);
        String system = null;
        if (messageArgs.length >= 2) {
            String candidate = settings.resolveSystem(messageArgs[messageArgs.length - 1]);
            if (candidate != null) {
                system = candidate;
                messageArgs = Arrays.copyOfRange(messageArgs, 0, messageArgs.length - 1);
            }
        }

        String message = String.join(" ", messageArgs);
        if (message.isBlank()) {
            send(sender, "empty-message");
            return true;
        }

        if (!ToastService.send(target, frame, icon, message, system)) {
            send(sender, "recipient-disabled");
        }
        return true;
    }

    private boolean handleNotification(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("fluffycustomtoast.noti")) {
            send(sender, "no-permission");
            return true;
        }
        if (!(sender instanceof Player player)) {
            send(sender, "not-player");
            return true;
        }
        if (args.length < 2 || args.length > 3) {
            send(sender, "usage-noti");
            return true;
        }

        Boolean toggle = parseToggle(args[1]);
        if (toggle == null) {
            send(sender, "invalid-toggle");
            return true;
        }

        ToastSettings settings = FluffyCustomToast.getSettings();
        UUID uuid = player.getUniqueId();
        if (args.length == 2) {
            FluffyCustomToast.getPreferences().setGlobal(uuid, toggle);
            player.sendMessage(settings.message(toggle ? "global-on" : "global-off"));
            return true;
        }

        String system = settings.resolveSystem(args[2]);
        if (system == null) {
            send(sender, "unknown-system");
            return true;
        }
        FluffyCustomToast.getPreferences().setSystem(uuid, system, toggle);
        player.sendMessage(settings.message(toggle ? "system-on" : "system-off", "%system%", system));
        if (toggle && !FluffyCustomToast.getPreferences().isGlobalEnabled(uuid)) {
            player.sendMessage(settings.message("global-off-note"));
        }
        return true;
    }

    private boolean handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("fluffycustomtoast.reload")) {
            send(sender, "no-permission");
            return true;
        }
        try {
            FluffyCustomToast.getInstance().reloadConfig();
            ToastSettings settings = FluffyCustomToast.getSettings();
            settings.reload();
            sender.sendMessage(settings.message("reload-success", "%systems%", Integer.toString(settings.systems().size())));
        } catch (Exception exception) {
            String error = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            sender.sendMessage(FluffyCustomToast.getSettings().message("reload-failed", "%error%", error));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            if (startsWith("noti", args[0])) {
                suggestions.add("noti");
            }
            if (sender.hasPermission("fluffycustomtoast.reload") && startsWith("reload", args[0])) {
                suggestions.add("reload");
            }
            if (sender.hasPermission("fluffycustomtoast.use")) {
                Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> startsWith(name, args[0]))
                        .sorted()
                        .forEach(suggestions::add);
            }
            return suggestions;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            return List.of();
        }

        if (args[0].equalsIgnoreCase("noti")) {
            if (!sender.hasPermission("fluffycustomtoast.noti")) {
                return List.of();
            }
            if (args.length == 2) {
                return TOGGLES.stream().filter(toggle -> startsWith(toggle, args[1])).toList();
            }
            if (args.length == 3) {
                return FluffyCustomToast.getSettings().systems().stream()
                        .filter(system -> startsWith(system, args[2]))
                        .toList();
            }
            return List.of();
        }

        if (!sender.hasPermission("fluffycustomtoast.use")) {
            return List.of();
        }
        if (args.length == 2) {
            return FRAMES.stream().filter(frame -> startsWith(frame, args[1])).toList();
        }
        if (args.length >= 6) {
            String prefix = args[args.length - 1];
            return FluffyCustomToast.getSettings().systems().stream()
                    .filter(system -> startsWith(system, prefix))
                    .toList();
        }
        return List.of();
    }

    private static @Nullable Boolean parseToggle(@NotNull String raw) {
        if (raw.equalsIgnoreCase("on")) {
            return true;
        }
        if (raw.equalsIgnoreCase("off")) {
            return false;
        }
        return null;
    }

    private static void send(@NotNull CommandSender sender, @NotNull String key) {
        String message = FluffyCustomToast.getSettings().message(key);
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    private static boolean startsWith(String value, String prefix) {
        return value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }
}
