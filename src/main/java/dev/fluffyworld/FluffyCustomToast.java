package dev.fluffyworld;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import dev.fluffyworld.command.ToastCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class FluffyCustomToast extends JavaPlugin {

    private static TaskScheduler scheduler;
    private static FluffyCustomToast instance;

    @Override
    public void onEnable() {
        scheduler = UniversalScheduler.getScheduler(this);
        instance = this;

        PluginCommand command = getCommand("ftoast");
        if (command == null) {
            getLogger().severe("ไม่พบคำสั่ง ftoast ใน plugin.yml");
            return;
        }
        ToastCommand toastCommand = new ToastCommand();
        command.setExecutor(toastCommand);
        command.setTabCompleter(toastCommand);
    }

    @Override
    public void onDisable() {
        if (scheduler != null) {
            scheduler.cancelTasks();
            scheduler = null;
        }
        instance = null;
    }

    @NotNull
    public static TaskScheduler getScheduler() {
        return scheduler;
    }

    @NotNull
    public static FluffyCustomToast getInstance() {
        return instance;
    }
}
