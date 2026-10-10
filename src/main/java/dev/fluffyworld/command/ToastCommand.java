package dev.fluffyworld.command;

import dev.fluffyworld.toast.ToastFrame;
import dev.fluffyworld.toast.ToastService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ToastCommand implements CommandExecutor, TabCompleter {

    private static final List<String> FRAMES = List.of("task", "goal", "challenge");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("fluffycustomtoast.use")) {
            sender.sendMessage("§cคุณไม่มีสิทธิ์ใช้คำสั่งนี้");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage("§cใช้: /ftoast <player> <task|goal|challenge> <item> <message>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cไม่พบผู้เล่นนี้");
            return true;
        }

        ToastFrame frame = ToastFrame.parse(args[1]);
        if (frame == null) {
            sender.sendMessage("§cกรอบไม่ถูกต้อง ใช้ task, goal หรือ challenge");
            return true;
        }

        ItemStack icon = ToastService.parseIcon(args[2]);
        if (icon == null) {
            sender.sendMessage("§cไอเทมไม่ถูกต้อง ตัวอย่าง PAPER{CMD:11001} หรือ PAPER:11105");
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        if (message.isBlank()) {
            sender.sendMessage("§cกรุณาใส่ข้อความ");
            return true;
        }

        ToastService.send(target, frame, icon, message);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("fluffycustomtoast.use")) {
            return List.of();
        }
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> startsWith(name, args[0]))
                    .sorted()
                    .toList();
        }
        if (args.length == 2) {
            return FRAMES.stream().filter(frame -> startsWith(frame, args[1])).toList();
        }
        return List.of();
    }

    private static boolean startsWith(String value, String prefix) {
        return value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }
}
