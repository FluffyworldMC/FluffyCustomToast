package dev.fluffyworld.toast;

import dev.fluffyworld.FluffyCustomToast;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStackTemplate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ToastService {

    private static final Pattern ICON = Pattern.compile("(?i)^(?:minecraft:)?([a-z0-9_]+)(?:\\{cmd:(\\d+)\\}|:(\\d+))?$");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private ToastService() {
    }

    public static ItemStack parseIcon(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher matcher = ICON.matcher(raw);
        if (!matcher.matches()) {
            return null;
        }
        Material material = Material.matchMaterial(matcher.group(1));
        if (material == null || material.isAir() || !material.isItem()) {
            return null;
        }
        ItemStack item = new ItemStack(material);
        String modelData = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
        if (modelData == null) {
            return item;
        }
        int value;
        try {
            value = Integer.parseInt(modelData);
        } catch (NumberFormatException ignored) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        component.setFloats(List.of((float) value));
        meta.setCustomModelDataComponent(component);
        item.setItemMeta(meta);
        return item;
    }

    public static void send(Player player, ToastFrame frame, ItemStack icon, String rawText) {
        ItemStack snapshot = icon.clone();
        FluffyCustomToast.getScheduler().runTask(player, () -> deliver(player, frame, snapshot, rawText));
    }

    private static void deliver(Player player, ToastFrame frame, ItemStack icon, String rawText) {
        if (!player.isOnline()) {
            return;
        }
        try {
            String resolved = rawText;
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                resolved = PlaceholderHook.apply(player, resolved);
            }
            Component title = LEGACY.deserialize(resolved);
            show(player, frame, icon, title);
        } catch (RuntimeException exception) {
            FluffyCustomToast.getInstance().getLogger().warning("ส่ง toast ไม่สำเร็จ: " + exception.getMessage());
        }
    }

    private static void show(Player player, ToastFrame frame, ItemStack icon, Component title) {
        Identifier id = Identifier.fromNamespaceAndPath(
                "fluffycustomtoast",
                "t/" + UUID.randomUUID().toString().replace("-", "")
        );
        DisplayInfo display = new DisplayInfo(
                ItemStackTemplate.fromNonEmptyStack(CraftItemStack.asNMSCopy(icon)),
                PaperAdventure.asVanilla(title),
                PaperAdventure.asVanilla(Component.empty()),
                Optional.empty(),
                frame.type(),
                true,
                false,
                true
        );
        AdvancementRequirements requirements = AdvancementRequirements.allOf(List.of("impossible"));
        Advancement advancement = new Advancement(
                Optional.of(Identifier.fromNamespaceAndPath("minecraft", "story/root")),
                Optional.of(display),
                AdvancementRewards.EMPTY,
                Map.of("impossible", new Criterion<>(CriteriaTriggers.IMPOSSIBLE, new ImpossibleTrigger.TriggerInstance())),
                requirements,
                false
        );
        AdvancementHolder holder = new AdvancementHolder(id, advancement);
        AdvancementProgress progress = new AdvancementProgress();
        progress.update(requirements);
        progress.grantProgress("impossible");

        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        serverPlayer.connection.send(new ClientboundUpdateAdvancementsPacket(
                false,
                List.of(holder),
                Set.of(),
                Map.of(id, progress),
                true
        ));
        serverPlayer.connection.send(new ClientboundUpdateAdvancementsPacket(
                false,
                List.of(),
                Set.of(id),
                Map.of(),
                false
        ));
    }
}
