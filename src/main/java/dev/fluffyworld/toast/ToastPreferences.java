package dev.fluffyworld.toast;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ToastPreferences {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerState> states = new ConcurrentHashMap<>();

    public ToastPreferences(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    public boolean isGlobalEnabled(@NotNull UUID uuid) {
        PlayerState state = states.get(uuid);
        return state == null || state.global;
    }

    public boolean isSystemEnabled(@NotNull UUID uuid, @NotNull String system) {
        PlayerState state = states.get(uuid);
        return state == null || state.systems.getOrDefault(key(system), true);
    }

    public boolean isEnabled(@NotNull UUID uuid, @Nullable String system) {
        PlayerState state = states.get(uuid);
        if (state == null) {
            return true;
        }
        if (!state.global) {
            return false;
        }
        return system == null || state.systems.getOrDefault(key(system), true);
    }

    public void setGlobal(@NotNull UUID uuid, boolean enabled) {
        state(uuid).global = enabled;
        save();
    }

    public void setSystem(@NotNull UUID uuid, @NotNull String system, boolean enabled) {
        state(uuid).systems.put(key(system), enabled);
        save();
    }

    private PlayerState state(@NotNull UUID uuid) {
        return states.computeIfAbsent(uuid, ignored -> new PlayerState());
    }

    private static String key(@NotNull String system) {
        return system.toLowerCase(Locale.ROOT);
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection players = yaml.getConfigurationSection("players");
            if (players == null) {
                return;
            }
            for (String rawUuid : players.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(rawUuid);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("ข้าม UUID ที่ไม่ถูกต้องใน data.yml: " + rawUuid);
                    continue;
                }
                ConfigurationSection section = players.getConfigurationSection(rawUuid);
                if (section == null) {
                    continue;
                }
                PlayerState state = new PlayerState();
                state.global = section.getBoolean("global", true);
                ConfigurationSection systems = section.getConfigurationSection("systems");
                if (systems != null) {
                    for (String system : systems.getKeys(false)) {
                        state.systems.put(key(system), systems.getBoolean(system, true));
                    }
                }
                states.put(uuid, state);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("โหลด data.yml ไม่สำเร็จ: " + exception.getMessage());
        }
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection players = yaml.createSection("players");
        for (Map.Entry<UUID, PlayerState> entry : states.entrySet()) {
            ConfigurationSection section = players.createSection(entry.getKey().toString());
            PlayerState state = entry.getValue();
            section.set("global", state.global);
            if (!state.systems.isEmpty()) {
                ConfigurationSection systems = section.createSection("systems");
                state.systems.forEach(systems::set);
            }
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("ไม่สามารถสร้างโฟลเดอร์ปลั๊กอินได้");
            }
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("บันทึก data.yml ไม่สำเร็จ: " + exception.getMessage());
        }
    }

    private static final class PlayerState {
        private volatile boolean global = true;
        private final Map<String, Boolean> systems = new ConcurrentHashMap<>();
    }
}
