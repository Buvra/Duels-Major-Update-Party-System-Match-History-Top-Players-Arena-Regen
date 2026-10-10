package me.realized.duels.data;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.event.user.UserCreateEvent;
import me.realized.duels.api.kit.Kit;
import me.realized.duels.api.user.User;
import me.realized.duels.api.user.UserManager;
import me.realized.duels.config.Config;
import me.realized.duels.config.Lang;
import me.realized.duels.util.DateUtil;
import me.realized.duels.util.Loadable;
import me.realized.duels.util.Log;
import me.realized.duels.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserManagerImpl implements Loadable, Listener, UserManager {

    private static final String ADMIN_UPDATE_MESSAGE =
            "&9[Duels] &bDuels &fv%s &7is now available for download! Download at: &c%s";

    private static final int TOP_LIMIT = 10;
    private static final long TOP_TASK_DELAY = 100L;
    private static final long TOP_TASK_PERIOD = 200L; // 10 saniye
    private static final long SAVE_TASK_DELAY = 200L;
    private static final long SAVE_TASK_PERIOD = 600L;

    private final DuelsPlugin plugin;
    private final Config config;
    private final Lang lang;
    private final UserRepository userRepository;

    private final Map<UUID, UserData> users = new ConcurrentHashMap<UUID, UserData>();
    private final Map<String, UUID> names = new ConcurrentHashMap<String, UUID>();

    private volatile int defaultRating;
    private volatile int matchesToDisplay;
    private volatile boolean loaded;

    private volatile UserManager.TopEntry wins;
    private volatile UserManager.TopEntry losses;
    private volatile UserManager.TopEntry noKit;

    private final Map<Kit, UserManager.TopEntry> topRatings =
            new ConcurrentHashMap<Kit, UserManager.TopEntry>();

    private int topTask = -1;
    private int saveTask = -1;

    public UserManagerImpl(DuelsPlugin plugin, UserRepository userRepository) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.lang = plugin.getLang();
        this.userRepository = userRepository;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void handleLoad() {
        this.defaultRating = this.config.getDefaultRating();
        this.matchesToDisplay = this.config.getMatchesToDisplay();

        if (this.matchesToDisplay < 0) {
            this.matchesToDisplay = 0;
        }

        this.loaded = true;

        this.topTask = this.plugin.doSyncRepeat(new Runnable() {
            @Override
            public void run() {
                if (!loaded) {
                    return;
                }

                final List<Kit> kitsSnapshot =
                        new ArrayList<Kit>(plugin.getKitManager().getKits());

                plugin.doAsync(new Runnable() {
                    @Override
                    public void run() {
                        refreshTopEntries(kitsSnapshot);
                    }
                });
            }
        }, TOP_TASK_DELAY, TOP_TASK_PERIOD).getTaskId();

        this.saveTask = this.plugin.doAsyncRepeat(new Runnable() {
            @Override
            public void run() {
                if (!loaded) {
                    return;
                }

                flushDirtyUsers();
            }
        }, SAVE_TASK_DELAY, SAVE_TASK_PERIOD).getTaskId();

        // Reload durumunda sunucuda zaten aktif olan oyuncuların verilerini yükle (Veri kaybı ve restart zorunluluğunu engeller):
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final UUID uuid = player.getUniqueId();
            final String name = player.getName();
            this.plugin.doAsync(new Runnable() {
                @Override
                public void run() {
                    UserData data = loadUser(uuid, name);
                    if (data != null) {
                        names.put(name.toLowerCase(), uuid);
                        users.put(uuid, data);
                    }
                }
            });
        }
    }

    @Override
    public void handleUnload() {
        this.loaded = false;

        if (this.topTask != -1) {
            this.plugin.cancelTask(this.topTask);
            this.topTask = -1;
        }

        if (this.saveTask != -1) {
            this.plugin.cancelTask(this.saveTask);
            this.saveTask = -1;
        }

        this.saveUsers(new ArrayList<UserData>(this.users.values()), true);
        this.users.clear();
        this.names.clear();
        this.topRatings.clear();
        this.wins = null;
        this.losses = null;
        this.noKit = null;
    }

    private void refreshTopEntries(final List<Kit> kits) {
        if (!this.loaded) {
            return;
        }

        UserManager.TopEntry top;

        top = get(
                this.config.getTopUpdateInterval(),
                this.wins,
                new Function<User, Integer>() {
                    @Override
                    public Integer apply(User user) {
                        return user.getWins();
                    }
                },
                this.config.getTopWinsType(),
                this.config.getTopWinsIdentifier()
        );
        if (top != null) {
            this.wins = top;
        }

        top = get(
                this.config.getTopUpdateInterval(),
                this.losses,
                new Function<User, Integer>() {
                    @Override
                    public Integer apply(User user) {
                        return user.getLosses();
                    }
                },
                this.config.getTopLossesType(),
                this.config.getTopLossesIdentifier()
        );
        if (top != null) {
            this.losses = top;
        }

        top = get(
                this.config.getTopUpdateInterval(),
                this.noKit,
                new Function<User, Integer>() {
                    @Override
                    public Integer apply(User user) {
                        return user.getRating();
                    }
                },
                this.config.getTopNoKitType(),
                this.config.getTopNoKitIdentifier()
        );
        if (top != null) {
            this.noKit = top;
        }

        for (Kit key : new ArrayList<Kit>(this.topRatings.keySet())) {
            if (!kits.contains(key)) {
                this.topRatings.remove(key);
            }
        }

        for (final Kit kit : kits) {
            UserManager.TopEntry entry = this.topRatings.get(kit);

            top = get(
                    this.config.getTopUpdateInterval(),
                    entry,
                    new Function<User, Integer>() {
                        @Override
                        public Integer apply(User user) {
                            return user.getRating(kit);
                        }
                    },
                    this.config.getTopKitType().replace("%kit%", kit.getName()),
                    this.config.getTopKitIdentifier()
            );

            if (top != null) {
                this.topRatings.put(kit, top);
            }
        }
    }

    @Nullable
    public UserData get(@NotNull String name) {
        Objects.requireNonNull(name, "name");
        UUID uuid = this.names.get(name.toLowerCase());
        return uuid != null ? this.get(uuid) : null;
    }

    @Nullable
    public UserData get(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return this.users.get(uuid);
    }

    @Nullable
    public UserData get(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        return this.get(player.getUniqueId());
    }

    public List<UserData> getUsersSnapshot() {
        return new ArrayList<UserData>(this.users.values());
    }

    @Nullable
    public UserManager.TopEntry getTopWins() {
        return this.wins;
    }

    @Nullable
    public UserManager.TopEntry getTopLosses() {
        return this.losses;
    }

    @Nullable
    public UserManager.TopEntry getTopRatings() {
        return this.noKit;
    }

    @Nullable
    public UserManager.TopEntry getTopRatings(@NotNull Kit kit) {
        Objects.requireNonNull(kit, "kit");
        return this.topRatings.get(kit);
    }

    public String getNextUpdate(long creation) {
        return DateUtil.format(
                (creation + this.config.getTopUpdateInterval() - System.currentTimeMillis()) / 1000L
        );
    }

    private UserManager.TopEntry get(
            long interval,
            UserManager.TopEntry previous,
            Function<User, Integer> function,
            String type,
            String identifier
    ) {
        if (previous != null && System.currentTimeMillis() - previous.getCreation() < interval) {
            return null;
        }

        return new UserManager.TopEntry(type, identifier, this.top(function, TOP_LIMIT));
    }

    private List<UserManager.TopData> top(final Function<User, Integer> function, final int limit) {
        List<UserData> snapshot = new ArrayList<UserData>(this.users.values());

        PriorityQueue<UserManager.TopData> heap = new PriorityQueue<UserManager.TopData>(limit);

        for (UserData data : snapshot) {
            UserManager.TopData topData = new UserManager.TopData(
                    data.getUuid(),
                    data.getName(),
                    function.apply(data)
            );

            if (heap.size() < limit) {
                heap.offer(topData);
                continue;
            }

            UserManager.TopData smallest = heap.peek();
            if (smallest != null && topData.compareTo(smallest) > 0) {
                heap.poll();
                heap.offer(topData);
            }
        }

        List<UserManager.TopData> result = new ArrayList<UserManager.TopData>(heap);
        result.sort(Collections.reverseOrder());
        return Collections.unmodifiableList(result);
    }

    private UserData loadUser(final UUID uuid, final String name) {
        try {
            UserData user = this.userRepository.findByUuid(uuid);

            if (user == null) {
                user = new UserData(uuid, name, this.defaultRating, this.matchesToDisplay);
                final UserData createdUser = user;

                this.plugin.doSync(new Runnable() {
                    @Override
                    public void run() {
                        Bukkit.getPluginManager().callEvent(new UserCreateEvent(createdUser));
                    }
                });

                this.userRepository.save(user);
            } else {
                user.applyRuntimeSettings(this.defaultRating, this.matchesToDisplay);

                if (!name.equals(user.getName())) {
                    user.setName(name);
                    this.userRepository.save(user);
                }
            }

            user.markPersisted();
            return user;
        } catch (Exception ex) {
            Log.error(this, "An error occured while loading userdata of " + name + "!", ex);
            ex.printStackTrace();
            return new UserData(uuid, name, this.defaultRating, this.matchesToDisplay);
        }
    }

    private void saveUsers(Collection<UserData> users, boolean force) {
        if (users == null || users.isEmpty()) {
            return;
        }

        Map<UserData, Long> revisions = new HashMap<UserData, Long>();

        for (UserData user : users) {
            if (user == null) {
                continue;
            }

            if (!force && !user.isDirty()) {
                continue;
            }

            revisions.put(user, user.getRevision());
        }

        if (revisions.isEmpty()) {
            return;
        }

        try {
            this.userRepository.saveAll(new ArrayList<UserData>(revisions.keySet()));
            this.markPersisted(revisions);
        } catch (Exception ex) {
            Log.error(this, "Bulk userdata save failed, falling back to per-user saves.", ex);

            for (Map.Entry<UserData, Long> entry : revisions.entrySet()) {
                UserData user = entry.getKey();

                try {
                    this.userRepository.save(user);
                    user.markPersisted(entry.getValue());
                } catch (Exception inner) {
                    Log.error(this, "An error occured while saving userdata of " + user.getName() + "!", inner);
                }
            }
        }
    }

    private void flushDirtyUsers() {
        this.saveUsers(new ArrayList<UserData>(this.users.values()), false);
    }

    private void markPersisted(Map<UserData, Long> revisions) {
        for (Map.Entry<UserData, Long> entry : revisions.entrySet()) {
            entry.getKey().markPersisted(entry.getValue());
        }
    }

    @EventHandler
    public void on(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final String name = player.getName();


        UserData user = this.users.get(uuid);
        if (user != null) {
            String oldName = user.getName();

            if (oldName == null || !name.equals(oldName)) {
                if (oldName != null) {
                    this.names.remove(oldName.toLowerCase());
                }
                user.setName(name);
                this.names.put(name.toLowerCase(), uuid);
            }
            return;
        }

        this.plugin.doAsync(new Runnable() {
            @Override
            public void run() {
                UserData data = loadUser(uuid, name);
                if (data == null) {
                    plugin.doSync(new Runnable() {
                        @Override
                        public void run() {
                            Player online = Bukkit.getPlayer(uuid);
                            if (online != null && online.isOnline()) {
                                lang.sendMessage(online, "ERROR.data.load-failure", new Object[0]);
                            }
                        }
                    });
                    return;
                }

                names.put(name.toLowerCase(), uuid);
                users.put(uuid, data);
            }
        });
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final UserData user = this.users.remove(uuid);

        if (user != null) {
            String userName = user.getName();
            if (userName != null) {
                this.names.remove(userName.toLowerCase());
            }

            if (user.isDirty()) {
                final long revision = user.getRevision();

                this.plugin.doAsync(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            userRepository.save(user);
                            user.markPersisted(revision);
                        } catch (Exception ex) {
                            Log.error(UserManagerImpl.this,
                                    "An error occured while saving userdata of " + user.getName() + "!",
                                    ex);
                        }
                    }
                });
            }
        }
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    public UserManager.TopEntry getWins() {
        return this.wins;
    }

    public UserManager.TopEntry getLosses() {
        return this.losses;
    }

    public UserManager.TopEntry getNoKit() {
        return this.noKit;
    }
}
