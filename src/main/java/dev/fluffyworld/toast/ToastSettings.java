package dev.fluffyworld.toast;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ToastSettings {

    private final JavaPlugin plugin;
    private final Map<String, String> systems = new LinkedHashMap<>();
    private FileConfiguration config;

    public ToastSettings(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.config = plugin.getConfig();
        systems.clear();
        for (String raw : config.getStringList("systems")) {
            if (raw == null) {
                continue;
            }
            String name = raw.trim();
            if (name.isEmpty() || name.chars().anyMatch(Character::isWhitespace)) {
                plugin.getLogger().warning("ข้ามชื่อระบบ toast ที่ไม่ถูกต้องใน config.yml: '" + raw + "'");
                continue;
            }
            systems.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        }
    }

    @Nullable
    public String resolveSystem(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        return systems.get(raw.toLowerCase(Locale.ROOT));
    }

    @NotNull
    public Collection<String> systems() {
        return systems.values();
    }

    @NotNull
    public String message(@NotNull String key) {
        String raw = config.getString("messages." + key);
        return raw == null ? "" : ChatColor.translateAlternateColorCodes('&', raw);
    }

    @NotNull
    public String message(@NotNull String key, @NotNull String placeholder, @NotNull String value) {
        return message(key).replace(placeholder, value);
    }
}
