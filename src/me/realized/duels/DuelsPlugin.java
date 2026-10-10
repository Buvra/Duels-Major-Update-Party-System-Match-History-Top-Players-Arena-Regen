package me.realized.duels;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;
import me.realized.duels.api.Duels;
import me.realized.duels.api.command.SubCommand;
import me.realized.duels.arena.ArenaManagerImpl;
import me.realized.duels.betting.BettingManager;
import me.realized.duels.command.commands.*;
import me.realized.duels.command.commands.duel.DuelCommand;
import me.realized.duels.command.commands.duels.DuelsCommand;
import me.realized.duels.command.commands.queue.QueueCommand;
import me.realized.duels.config.Config;
import me.realized.duels.config.Lang;
import me.realized.duels.data.SqliteUserRepository;
import me.realized.duels.data.UserManagerImpl;
import me.realized.duels.data.UserRepository;
import me.realized.duels.duel.DuelManager;
import me.realized.duels.extension.ExtensionClassLoader;
import me.realized.duels.extension.ExtensionManager;
import me.realized.duels.hook.HookManager;
import me.realized.duels.inventories.InventoryManager;
import me.realized.duels.kit.KitManagerImpl;
import me.realized.duels.listeners.DamageListener;
import me.realized.duels.listeners.EnderpearlListener;
import me.realized.duels.listeners.KitItemListener;
import me.realized.duels.listeners.KitOptionsListener;
import me.realized.duels.listeners.PotionListener;
import me.realized.duels.listeners.ProjectileHitListener;
import me.realized.duels.listeners.TeleportListener;
import me.realized.duels.logging.LogManager;
import me.realized.duels.player.PlayerInfoManager;
import me.realized.duels.queue.QueueManager;
import me.realized.duels.queue.sign.QueueSignManagerImpl;
import me.realized.duels.request.RequestManager;
import me.realized.duels.setting.SettingsManager;
import me.realized.duels.spectate.SpectateManagerImpl;
import me.realized.duels.teleport.Teleport;
import me.realized.duels.util.Loadable;
import me.realized.duels.util.Log;
import me.realized.duels.util.Reloadable;
import me.realized.duels.util.command.AbstractCommand;
import me.realized.duels.util.gui.GuiListener;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class DuelsPlugin extends JavaPlugin implements Duels, Log.LogSource {

    private static DuelsPlugin instance;

    private final List<Loadable> loadables = new ArrayList<Loadable>();
    private int lastLoad;

    private LogManager logManager;
    private MatchHistoryMenu matchHistoryMenu;
    private Config configuration;
    private Lang lang;
    private UserRepository userRepository;
    private UserManagerImpl userManager;
    private GuiListener<DuelsPlugin> guiListener;
    private KitManagerImpl kitManager;
    private ArenaManagerImpl arenaManager;
    private SettingsManager settingManager;
    private PlayerInfoManager playerManager;
    private SpectateManagerImpl spectateManager;
    private BettingManager bettingManager;
    private InventoryManager inventoryManager;
    private DuelManager duelManager;
    private QueueManager queueManager;
    private QueueSignManagerImpl queueSignManager;
    private RequestManager requestManager;
    private HookManager hookManager;
    private TopDuelistsMenu topDuelistsMenu;
    private Teleport teleport;
    private ExtensionManager extensionManager;
    private SpectateMenu spectateMenu;
    private me.realized.duels.party.PartyManager partyManager;
    private me.realized.duels.arena.regen.ArenaRegenManager arenaRegenManager;
    private me.realized.duels.layout.KitLayoutManager kitLayoutManager;

    private final Map<String, AbstractCommand<DuelsPlugin>> commands =
            new HashMap<String, AbstractCommand<DuelsPlugin>>();
    private final List<Listener> registeredListeners = new ArrayList<Listener>();


    @Override
    public void onEnable() {
        instance = this;
        Log.addSource(this);

        try {
            this.logManager = new LogManager(this);
        } catch (IOException ex) {
            Log.error("Could not load LogManager. Please contact the developer.");
            ex.printStackTrace();
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        Log.addSource(this.logManager);
        this.logManager.debug("onEnable start -> " + System.currentTimeMillis() + "\n");


        this.matchHistoryMenu = new MatchHistoryMenu(this);

        Bukkit.getPluginManager().registerEvents(this.matchHistoryMenu, this);

        this.loadables.add(this.configuration = new Config(this));
        this.loadables.add(this.lang = new Lang(this));
        this.topDuelistsMenu = new TopDuelistsMenu(this);
        Bukkit.getPluginManager().registerEvents(this.topDuelistsMenu, this);

        // MySQL repository
        this.userRepository = new SqliteUserRepository(this);
        this.loadables.add(this.userManager = new UserManagerImpl(this, this.userRepository));
        this.loadables.add(this.guiListener = new GuiListener<DuelsPlugin>(this));
        this.loadables.add(this.kitManager = new KitManagerImpl(this));

        this.spectateMenu = new SpectateMenu(this);
        Bukkit.getPluginManager().registerEvents(this.spectateMenu, this);

        this.loadables.add(this.arenaManager = new ArenaManagerImpl(this));
        this.loadables.add(this.settingManager = new SettingsManager(this));
        this.loadables.add(this.playerManager = new PlayerInfoManager(this));
        this.loadables.add(this.spectateManager = new SpectateManagerImpl(this));
        this.loadables.add(this.bettingManager = new BettingManager(this));
        this.loadables.add(this.inventoryManager = new InventoryManager(this));
        this.loadables.add(this.duelManager = new DuelManager(this));
        this.loadables.add(this.queueManager = new QueueManager(this));
        this.loadables.add(this.queueSignManager = new QueueSignManagerImpl(this));
        this.loadables.add(this.requestManager = new RequestManager(this));

        this.hookManager = new HookManager(this);

        this.loadables.add(this.teleport = new Teleport(this));
        this.loadables.add(this.extensionManager = new ExtensionManager(this));

        this.partyManager = new me.realized.duels.party.PartyManager(this);
        this.arenaRegenManager = new me.realized.duels.arena.regen.ArenaRegenManager(this);
        this.kitLayoutManager = new me.realized.duels.layout.KitLayoutManager(this);

        if (!this.load()) {
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        this.arenaRegenManager.loadZones();

        new KitItemListener(this);
        new DamageListener(this);
        new PotionListener(this);
        new TeleportListener(this);
        new ProjectileHitListener(this);
        new EnderpearlListener(this);
        new KitOptionsListener(this);
        Bukkit.getPluginManager().registerEvents(new me.realized.duels.listeners.PartyDamageListener(this), this);
        Bukkit.getPluginManager().registerEvents(new me.realized.duels.party.gui.PartyGuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new me.realized.duels.arena.regen.ArenaRegenListener(this, this.arenaRegenManager), this);
        Bukkit.getPluginManager().registerEvents(new me.realized.duels.gui.confirm.OwnInventoryConfirmListener(this), this);
        Bukkit.getPluginManager().registerEvents(new me.realized.duels.layout.KitLayoutEditorListener(this), this);
        Bukkit.getPluginManager().registerEvents(new me.realized.duels.gui.betting.MoneyBettingListener(this), this);
    }

    @Override
    public void onDisable() {
        long start = System.currentTimeMillis();
        this.logManager.debug("onDisable start -> " + start + "\n");
        if (this.arenaRegenManager != null) {
            this.arenaRegenManager.save();
        }
        if (this.kitLayoutManager != null) {
            this.kitLayoutManager.saveAllSync();
        }
        this.unload();
        long last = System.currentTimeMillis();
        this.logManager.debug("unload done (took " + Math.abs(start - last) + "ms)");
        Log.clearSources();
        this.logManager.debug("Log#clearSources done (took " + Math.abs(last - System.currentTimeMillis()) + "ms)");
        this.logManager.handleDisable();
        instance = null;
        this.log(Level.INFO, "Disable process took " + (System.currentTimeMillis() - start) + "ms.");
    }

    private boolean load() {
        this.registerCommands(
                new DuelCommand(this),
                new QueueCommand(this),
                new SpectateCommand(this),
                new DuelsCommand(this),
                new MatchHistoryCommand(this),
                new TopDuelistsCommand(this),
                new me.realized.duels.command.commands.party.PartyCommand(this),
                new me.realized.duels.arena.regen.ArenaRegenCommand(this),
                new me.realized.duels.layout.KitLayoutCommand(this)
        );

        for (Loadable loadable : this.loadables) {
            String name = loadable.getClass().getSimpleName();

            try {
                long now = System.currentTimeMillis();
                this.logManager.debug("Starting load of " + name + " at " + now);
                loadable.handleLoad();
                this.logManager.debug(name + " has been loaded. (took " + (System.currentTimeMillis() - now) + "ms)");
                this.lastLoad = this.loadables.indexOf(loadable);
            } catch (Exception ex) {
                if (loadable instanceof Log.LogSource) {
                    ex.printStackTrace();
                }

                Log.error("There was an error while loading " + name + "! If you believe this is an issue from the plugin, please contact the developer.", ex);
                return false;
            }
        }

        return true;
    }

    private boolean unload() {
        this.registeredListeners.forEach(HandlerList::unregisterAll);
        this.registeredListeners.clear();

        HandlerList.getRegisteredListeners(this).stream()
                .filter(listener -> listener.getListener().getClass().getClassLoader().getClass()
                        .isAssignableFrom(ExtensionClassLoader.class))
                .forEach(listener -> HandlerList.unregisterAll(listener.getListener()));

        this.commands.clear();

        for (Loadable loadable : Lists.reverse(this.loadables)) {
            String name = loadable.getClass().getSimpleName();

            try {
                if (this.loadables.indexOf(loadable) <= this.lastLoad) {
                    long now = System.currentTimeMillis();
                    this.logManager.debug("Starting unload of " + name + " at " + now);
                    loadable.handleUnload();
                    this.logManager.debug(name + " has been unloaded. (took " + (System.currentTimeMillis() - now) + "ms)");
                }
            } catch (Exception ex) {
                Log.error("There was an error while unloading " + name + "! If you believe this is an issue from the plugin, please contact the developer.", ex);
                return false;
            }
        }

        return true;
    }

    @SafeVarargs
    private final void registerCommands(AbstractCommand<DuelsPlugin>... commands) {
        for (AbstractCommand<DuelsPlugin> command : commands) {
            this.commands.put(command.getName().toLowerCase(), command);
            command.register();
        }
    }

    public boolean registerSubCommand(@NotNull String command, @NotNull final SubCommand subCommand) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(subCommand, "subCommand");

        AbstractCommand<DuelsPlugin> result = this.commands.get(command.toLowerCase());
        if (result != null && !result.isChild(subCommand.getName().toLowerCase())) {
            result.child(new AbstractCommand[] {
                    new AbstractCommand<DuelsPlugin>(this, subCommand) {
                        @Override
                        protected void execute(CommandSender sender, String label, String[] args) {
                            subCommand.execute(sender, label, args);
                        }
                    }
            });
            return true;
        }

        return false;
    }

    public void registerListener(@NotNull Listener listener) {
        Objects.requireNonNull(listener, "listener");
        this.registeredListeners.add(listener);
        Bukkit.getPluginManager().registerEvents(listener, this);
    }

    public boolean reload() {
        if (this.unload() && this.load()) {
            return true;
        }

        this.getPluginLoader().disablePlugin(this);
        return false;
    }

    public String getVersion() {
        return this.getDescription().getVersion();
    }

    public boolean reload(Loadable loadable) {
        boolean unloaded = false;

        try {
            loadable.handleUnload();
            unloaded = true;
            loadable.handleLoad();
            return true;
        } catch (Exception ex) {
            Log.error(
                    "There was an error while " + (unloaded ? "loading " : "unloading ")
                            + loadable.getClass().getSimpleName()
                            + "! If you believe this is an issue from the plugin, please contact the developer.",
                    ex
            );
            return false;
        }
    }

    public BukkitTask doSync(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        return Bukkit.getScheduler().runTask(this, task);
    }

    public BukkitTask doSyncAfter(@NotNull Runnable task, long delay) {
        Objects.requireNonNull(task, "task");
        return Bukkit.getScheduler().runTaskLater(this, task, delay);
    }

    public BukkitTask doSyncRepeat(@NotNull Runnable task, long delay, long period) {
        Objects.requireNonNull(task, "task");
        return Bukkit.getScheduler().runTaskTimer(this, task, delay, period);
    }

    public BukkitTask doAsync(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        return Bukkit.getScheduler().runTaskAsynchronously(this, task);
    }

    public BukkitTask doAsyncAfter(@NotNull Runnable task, long delay) {
        Objects.requireNonNull(task, "task");
        return Bukkit.getScheduler().runTaskLaterAsynchronously(this, task, delay);
    }

    public BukkitTask doAsyncRepeat(@NotNull Runnable task, long delay, long period) {
        Objects.requireNonNull(task, "task");
        return Bukkit.getScheduler().runTaskTimerAsynchronously(this, task, delay, period);
    }

    public void cancelTask(@NotNull BukkitTask task) {
        Objects.requireNonNull(task, "task");
        task.cancel();
    }

    public void cancelTask(int id) {
        Bukkit.getScheduler().cancelTask(id);
    }

    public void info(@NotNull String message) {
        Objects.requireNonNull(message, "message");
        Log.info(message);
    }

    public void warn(@NotNull String message) {
        Objects.requireNonNull(message, "message");
        Log.warn(message);
    }

    public void error(@NotNull String message) {
        Objects.requireNonNull(message, "message");
        Log.error(message);
    }

    public void error(@NotNull String message, @NotNull Throwable thrown) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(thrown, "thrown");
        Log.error(message, thrown);
    }

    public Loadable find(String name) {
        return this.loadables.stream()
                .filter(loadable -> loadable.getClass().getSimpleName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public List<String> getReloadables() {
        return (List<String>) this.loadables.stream()
                .filter(loadable -> loadable instanceof Reloadable)
                .map(loadable -> loadable.getClass().getSimpleName())
                .collect(Collectors.toList());
    }
    public TopDuelistsMenu getTopDuelistsMenu() {
        return this.topDuelistsMenu;
    }

    public void log(Level level, String s) {
        this.getLogger().log(level, s);
    }

    public void log(Level level, String s, Throwable thrown) {
        this.getLogger().log(level, s, thrown);
    }

    public static DuelsPlugin getInstance() {
        return instance;
    }

    public LogManager getLogManager() {
        return this.logManager;
    }

    public Config getConfiguration() {
        return this.configuration;
    }

    public Lang getLang() {
        return this.lang;
    }

    public UserManagerImpl getUserManager() {
        return this.userManager;
    }

    public UserRepository getUserRepository() {
        return this.userRepository;
    }

    public GuiListener<DuelsPlugin> getGuiListener() {
        return this.guiListener;
    }

    public KitManagerImpl getKitManager() {
        return this.kitManager;
    }

    public ArenaManagerImpl getArenaManager() {
        return this.arenaManager;
    }

    public SettingsManager getSettingManager() {
        return this.settingManager;
    }

    public PlayerInfoManager getPlayerManager() {
        return this.playerManager;
    }

    public SpectateManagerImpl getSpectateManager() {
        return this.spectateManager;
    }

    public BettingManager getBettingManager() {
        return this.bettingManager;
    }

    public InventoryManager getInventoryManager() {
        return this.inventoryManager;
    }

    public DuelManager getDuelManager() {
        return this.duelManager;
    }

    public QueueManager getQueueManager() {
        return this.queueManager;
    }

    public QueueSignManagerImpl getQueueSignManager() {
        return this.queueSignManager;
    }

    public RequestManager getRequestManager() {
        return this.requestManager;
    }

    public HookManager getHookManager() {
        return this.hookManager;
    }

    public Teleport getTeleport() {
        return this.teleport;
    }

    public ExtensionManager getExtensionManager() {
        return this.extensionManager;
    }

    public SpectateMenu getSpectateMenu() {
        return this.spectateMenu;
    }


    public MatchHistoryMenu getMatchHistoryMenu() {
        return this.matchHistoryMenu;
    }

    public me.realized.duels.party.PartyManager getPartyManager() {
        return this.partyManager;
    }

    public me.realized.duels.arena.regen.ArenaRegenManager getArenaRegenManager() {
        return this.arenaRegenManager;
    }

    public me.realized.duels.layout.KitLayoutManager getKitLayoutManager() {
        return this.kitLayoutManager;
    }

    public me.realized.duels.hook.hooks.VaultHook getVault() {
        return this.hookManager != null ? (me.realized.duels.hook.hooks.VaultHook) this.hookManager.getHook(me.realized.duels.hook.hooks.VaultHook.class) : null;
    }
}
