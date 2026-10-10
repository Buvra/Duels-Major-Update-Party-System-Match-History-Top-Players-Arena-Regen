package me.realized.duels.hook;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.hook.hooks.PlaceholderHook;
import me.realized.duels.hook.hooks.VaultHook;
import me.realized.duels.util.hook.AbstractHookManager;

public class HookManager extends AbstractHookManager<DuelsPlugin> {

    public HookManager(final DuelsPlugin plugin) {
        super(plugin);
        register(VaultHook.NAME, VaultHook.class);
        register(PlaceholderHook.NAME, PlaceholderHook.class);
    }
}
