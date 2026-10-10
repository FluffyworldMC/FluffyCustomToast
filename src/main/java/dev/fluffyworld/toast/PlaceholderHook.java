package dev.fluffyworld.toast;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

final class PlaceholderHook {

    private PlaceholderHook() {
    }

    static String apply(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
