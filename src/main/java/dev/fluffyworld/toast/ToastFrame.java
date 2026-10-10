package dev.fluffyworld.toast;

import net.minecraft.advancements.AdvancementType;

import java.util.Locale;

public enum ToastFrame {
    TASK(AdvancementType.TASK),
    GOAL(AdvancementType.GOAL),
    CHALLENGE(AdvancementType.CHALLENGE);

    private final AdvancementType type;

    ToastFrame(AdvancementType type) {
        this.type = type;
    }

    public AdvancementType type() {
        return type;
    }

    public static ToastFrame parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
