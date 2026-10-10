package me.realized.duels.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.config.converters.ConfigConverter9_10;
import me.realized.duels.util.EnumUtil;
import me.realized.duels.util.config.AbstractConfiguration;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class Config extends AbstractConfiguration<DuelsPlugin> {
    private int version;
    private boolean checkForUpdates;
    private boolean ctpPreventDuel;
    private boolean ctpPreventTag;
    private boolean pmPreventDuel;
    private boolean pmPreventTag;
    private boolean clxPreventDuel;
    private boolean clxPreventTag;
    private boolean autoUnvanish;
    private boolean setBackLocation;
    private boolean disableSkills;
    private boolean fuNoPowerLoss;
    private boolean fNoPowerLoss;
    private boolean duelzoneEnabled;
    private List<String> duelzones;
    private boolean myPetDespawn;
    private boolean preventBountyLoss;
    private boolean preventKDRChange;
    private String lhWinsCmd;
    private String lhWinsTitle;
    private String lhLossesCmd;
    private String lhLossesTitle;
    private boolean requiresClearedInventory;
    private boolean preventCreativeMode;
    private boolean ownInventoryEnabled;
    private boolean ownInventoryDropInventoryItems;
    private boolean ownInventoryUsePermission;
    private boolean kitSelectingEnabled;
    private boolean kitSelectingUsePermission;
    private boolean arenaSelectingEnabled;
    private boolean arenaSelectingUsePermission;
    private boolean itemBettingEnabled;
    private boolean itemBettingUsePermission;
    private boolean moneyBettingEnabled;
    private boolean moneyBettingUsePermission;
    private int expiration;
    private int maxDuration;
    private boolean startCommandsEnabled;
    private boolean startCommandsQueueOnly;
    private List<String> startCommands;
    private boolean endCommandsEnabled;
    private boolean endCommandsQueueOnly;
    private List<String> endCommands;
    private boolean projectileHitMessageEnabled;
    private List<String> projectileHitMessageTypes;
    private boolean preventInventoryOpen;
    private boolean protectKitItems;
    private boolean removeEmptyBottle;
    private boolean preventTpToMatchPlayers;
    private boolean forceAllowCombat;
    private boolean cancelIfMoved;
    private List<String> blacklistedWorlds;
    private boolean teleportToLastLocation;
    private int teleportDelay;
    private boolean spawnFirework;
    private boolean arenaOnlyEndMessage;
    private boolean displayInventories;
    private boolean preventItemDrop;
    private boolean preventItemPickup;
    private boolean limitTeleportEnabled;
    private double distanceAllowed;
    private boolean blockAllCommands;
    private List<String> whitelistedCommands;
    private List<String> blacklistedCommands;
    private List<String> queueBlacklistedCommands;
    private boolean ratingEnabled;
    private int kFactor;
    private int defaultRating;
    private boolean ratingQueueOnly;
    private boolean specRequiresClearedInventory;
    private boolean specUseSpectatorGamemode;
    private boolean specAddInvisibilityEffect;
    private List<String> specWhitelistedCommands;
    private boolean cdEnabled;
    private List<String> cdMessages;
    private List<String> titles;
    private boolean preventMovement;
    private boolean preventLaunchProjectile;
    private boolean preventPvp;
    private boolean preventInteract;
    private boolean displayKitRatings;
    private boolean displayNoKitRating;
    private boolean displayPastMatches;
    private int matchesToDisplay;
    private long topUpdateInterval;
    private String topWinsType;
    private String topWinsIdentifier;
    private String topLossesType;
    private String topLossesIdentifier;
    private String topKitType;
    private String topKitIdentifier;
    private String topNoKitType;
    private String topNoKitIdentifier;
    private int kitSelectorRows;
    private String kitSelectorFillerType;
    private short kitSelectorFillerData;
    private int arenaSelectorRows;
    private String arenaSelectorFillerType;
    private short arenaSelectorFillerData;
    private String settingsFillerType;
    private short settingsFillerData;
    private int queuesRows;
    private String queuesFillerType;
    private short queuesFillerData;
    private boolean inheritKitItemType;
    private double soupHeartsToRegen;
    private boolean soupRemoveEmptyBowl;
    private boolean soupCancelIfAlreadyFull;
    private final Map<String, MessageSound> sounds = new HashMap<>();

    public Config(final DuelsPlugin plugin) {
        super(plugin, "config");
    }

    @Override
    protected void loadValues(FileConfiguration configuration) throws Exception {
        final int prevVersion = configuration.getInt("config-version", 0);
        if (prevVersion < 10) {
            configuration = convert(new ConfigConverter9_10());
        } else if (prevVersion < getLatestVersion()) {
            configuration = convert(null);
        }
        version = configuration.getInt("config-version");
        checkForUpdates = configuration.getBoolean("check-for-updates", true);
        ctpPreventDuel = configuration.getBoolean("supported-plugins.CombatTagPlus.prevent-duel-if-tagged", true);
        ctpPreventTag = configuration.getBoolean("supported-plugins.CombatTagPlus.prevent-tag-in-duel", true);
        pmPreventDuel = configuration.getBoolean("supported-plugins.PvPManager.prevent-duel-if-tagged", true);
        pmPreventTag = configuration.getBoolean("supported-plugins.PvPManager.prevent-tag-in-duel", true);
        clxPreventDuel = configuration.getBoolean("supported-plugins.CombatLogX.prevent-duel-if-tagged", true);
        clxPreventTag = configuration.getBoolean("supported-plugins.CombatLogX.prevent-tag-in-duel", true);
        autoUnvanish = configuration.getBoolean("supported-plugins.Essentials.auto-unvanish", true);
        setBackLocation = configuration.getBoolean("supported-plugins.Essentials.set-back-location", true);
        disableSkills = configuration.getBoolean("supported-plugins.mcMMO.disable-skills-in-duel", true);
        fuNoPowerLoss = configuration.getBoolean("supported-plugins.FactionsUUID.no-power-loss-in-duel", true);
        fNoPowerLoss = configuration.getBoolean("supported-plugins.Factions.no-power-loss-in-duel", true);
        duelzoneEnabled = configuration.getBoolean("supported-plugins.WorldGuard.duelzone.enabled", false);
        duelzones = configuration.getStringList("supported-plugins.WorldGuard.duelzone.regions");
        myPetDespawn = configuration.getBoolean("supported-plugins.MyPet.despawn-pet-in-duel", false);
        preventBountyLoss = configuration.getBoolean("supported-plugins.BountyHunters.prevent-bounty-loss-in-duel", true);
        preventKDRChange = configuration.getBoolean("supported-plugins.SimpleClans.prevent-kdr-change", true);
        lhWinsCmd = configuration.getString("supported-plugins.LeaderHeads.wins.menu.command", "openwins");
        lhWinsTitle = configuration.getString("supported-plugins.LeaderHeads.wins.menu.title", "Duel Wins");
        lhLossesCmd = configuration.getString("supported-plugins.LeaderHeads.losses.menu.command", "openlosses");
        lhLossesTitle = configuration.getString("supported-plugins.LeaderHeads.losses.menu.title", "Duel Losses");
        requiresClearedInventory = configuration.getBoolean("request.requires-cleared-inventory", true);
        preventCreativeMode = configuration.getBoolean("request.prevent-creative-mode", false);
        ownInventoryEnabled = configuration.getBoolean("request.use-own-inventory.enabled", true);
        ownInventoryDropInventoryItems = configuration.getBoolean("request.use-own-inventory.drop-inventory-items", true);
        ownInventoryUsePermission = configuration.getBoolean("request.use-own-inventory.use-permission", false);
        kitSelectingEnabled = configuration.getBoolean("request.kit-selecting.enabled", true);
        kitSelectingUsePermission = configuration.getBoolean("request.kit-selecting.use-permission", false);
        arenaSelectingEnabled = configuration.getBoolean("request.arena-selecting.enabled", true);
        arenaSelectingUsePermission = configuration.getBoolean("request.arena-selecting.use-permission", false);
        itemBettingEnabled = configuration.getBoolean("request.item-betting.enabled", true);
        itemBettingUsePermission = configuration.getBoolean("request.item-betting.use-permission", false);
        moneyBettingEnabled = configuration.getBoolean("request.money-betting.enabled", true);
        moneyBettingUsePermission = configuration.getBoolean("request.money-betting.use-permission", false);
        expiration = Math.max(configuration.getInt("request.expiration", 30), 0);
        maxDuration = configuration.getInt("duel.match.max-duration", -1);
        startCommandsEnabled = configuration.getBoolean("duel.match.start-commands.enabled", false);
        startCommandsQueueOnly = configuration.getBoolean("duel.match.start-commands.queue-matches-only", false);
        startCommands = configuration.getStringList("duel.match.start-commands.commands");
        endCommandsEnabled = configuration.getBoolean("duel.match.end-commands.enabled", false);
        endCommandsQueueOnly = configuration.getBoolean("duel.match.end-commands.queue-matches-only", false);
        endCommands = configuration.getStringList("duel.match.end-commands.commands");
        projectileHitMessageEnabled = configuration.getBoolean("duel.projectile-hit-message.enabled", true);
        projectileHitMessageTypes = configuration.getStringList("duel.projectile-hit-message.types");
        preventInventoryOpen = configuration.getBoolean("duel.prevent-inventory-open", true);
        protectKitItems = configuration.getBoolean("duel.protect-kit-items", true);
        removeEmptyBottle = configuration.getBoolean("duel.remove-empty-bottle", true);
        preventTpToMatchPlayers = configuration.getBoolean("duel.prevent-teleport-to-match-players", true);
        forceAllowCombat = configuration.getBoolean("duel.force-allow-combat", true);
        cancelIfMoved = configuration.getBoolean("duel.cancel-if-moved", false);
        blacklistedWorlds = configuration.getStringList("duel.blacklisted-worlds");
        teleportToLastLocation = configuration.getBoolean("duel.teleport-to-last-location", false);
        teleportDelay = configuration.getInt("duel.teleport-delay", 5);
        spawnFirework = configuration.getBoolean("duel.spawn-firework", true);
        arenaOnlyEndMessage = configuration.getBoolean("duel.arena-only-end-message", false);
        displayInventories = configuration.getBoolean("duel.display-inventories", true);
        preventItemDrop = configuration.getBoolean("duel.prevent-item-drop", false);
        preventItemPickup = configuration.getBoolean("duel.prevent-item-pickup", true);
        limitTeleportEnabled = configuration.getBoolean("duel.limit-teleportation.enabled", true);
        distanceAllowed = configuration.getDouble("duel.limit-teleportation.distance-allowed", 5.0);
        blockAllCommands = configuration.getBoolean("duel.block-all-commands", false);
        whitelistedCommands = configuration.getStringList("duel.whitelisted-commands");
        blacklistedCommands = configuration.getStringList("duel.blacklisted-commands");
        queueBlacklistedCommands = configuration.getStringList("queue.blacklisted-commands");
        ratingEnabled = configuration.getBoolean("rating.enabled", true);
        kFactor = Math.max(configuration.getInt("rating.k-factor", 32), 1);
        defaultRating = Math.max(configuration.getInt("rating.default-rating", 1400), 0);
        ratingQueueOnly = configuration.getBoolean("rating.queue-matches-only", true);
        specRequiresClearedInventory = configuration.getBoolean("spectate.requires-cleared-inventory", false);
        specUseSpectatorGamemode = configuration.getBoolean("spectate.use-spectator-gamemode", false);
        specAddInvisibilityEffect = configuration.getBoolean("spectate.add-invisibility-effect", true);
        specWhitelistedCommands = configuration.getStringList("spectate.whitelisted-commands");
        cdEnabled = configuration.getBoolean("countdown.enabled", true);
        cdMessages = configuration.getStringList("countdown.messages");
        titles = configuration.getStringList("countdown.titles");

        if (cdMessages == null || cdMessages.isEmpty()) {
            cdMessages = java.util.Arrays.asList(
                "&eDüello &65 &esaniye içinde başlıyor...",
                "&eDüello &64 &esaniye içinde başlıyor...",
                "&eDüello &63 &esaniye içinde başlıyor...",
                "&eDüello &62 &esaniye içinde başlıyor...",
                "&eDüello &61 &esaniye içinde başlıyor...",
                "&aDüello başladı, başarılar!"
            );
        }
        if (titles == null || titles.isEmpty()) {
            titles = java.util.Arrays.asList(
                "&a5",
                "&e4",
                "&63",
                "&c2",
                "&41",
                "&a&lBAŞLADI!"
            );
        }

        preventMovement = configuration.getBoolean("countdown.prevent.movement", true);
        preventLaunchProjectile = configuration.getBoolean("countdown.prevent.launch-projectile", true);
        preventPvp = configuration.getBoolean("countdown.prevent.pvp", true);
        preventInteract = configuration.getBoolean("countdown.prevent.interact", true);
        displayKitRatings = configuration.getBoolean("stats.display-kit-ratings", true);
        displayNoKitRating = configuration.getBoolean("stats.display-nokit-rating", false);
        displayPastMatches = configuration.getBoolean("stats.display-past-matches", true);
        matchesToDisplay = Math.max(configuration.getInt("stats.matches-to-display", 10), 0);
        topUpdateInterval = Math.max(configuration.getInt("top.update-interval", 5), 1) * 60L * 1000L;
        topWinsType = configuration.getString("top.displayed-replacers.wins.type", "Wins");
        topWinsIdentifier = configuration.getString("top.displayed-replacers.wins.identifier", "wins");
        topLossesType = configuration.getString("top.displayed-replacers.losses.type", "Losses");
        topLossesIdentifier = configuration.getString("top.displayed-replacers.losses.identifier", "losses");
        topKitType = configuration.getString("top.displayed-replacers.kit.type", "%kit%");
        topKitIdentifier = configuration.getString("top.displayed-replacers.kit.identifier", "rating");
        topNoKitType = configuration.getString("top.displayed-replacers.no-kit.type", "No Kit");
        topNoKitIdentifier = configuration.getString("top.displayed-replacers.no-kit.identifier", "rating");
        kitSelectorRows = Math.min(Math.max(configuration.getInt("guis.kit-selector.rows", 2), 1), 5);
        kitSelectorFillerType = configuration.getString("guis.kit-selector.space-filler-item.type", "STAINED_GLASS_PANE");
        kitSelectorFillerData = (short) configuration.getInt("guis.kit-selector.space-filler-item.data", 0);
        arenaSelectorRows = Math.min(Math.max(configuration.getInt("guis.arena-selector.rows", 3), 1), 5);
        arenaSelectorFillerType = configuration.getString("guis.arena-selector.space-filler-item.type", "STAINED_GLASS_PANE");
        arenaSelectorFillerData = (short) configuration.getInt("guis.arena-selector.space-filler-item.data", 0);
        settingsFillerType = configuration.getString("guis.settings.space-filler-item.type", "STAINED_GLASS_PANE");
        settingsFillerData = (short) configuration.getInt("guis.settings.space-filler-item.data", 0);
        queuesRows = Math.min(Math.max(configuration.getInt("guis.queues.rows", 3), 1), 5);
        queuesFillerType = configuration.getString("guis.queues.space-filler-item.type", "STAINED_GLASS_PANE");
        queuesFillerData = (short) configuration.getInt("guis.queues.space-filler-item.data", 0);
        inheritKitItemType = configuration.getBoolean("guis.queues.inherit-kit-item-type", true);
        soupHeartsToRegen = Math.max(configuration.getDouble("soup.hearts-to-regen", 3.5), 0);
        soupRemoveEmptyBowl = configuration.getBoolean("soup.remove-empty-bowl", true);
        soupCancelIfAlreadyFull = configuration.getBoolean("soup.cancel-if-already-full", true);
        final ConfigurationSection sounds = configuration.getConfigurationSection("sounds");
        if (sounds != null) {
            for (final String name : sounds.getKeys(false)) {
                final ConfigurationSection sound = sounds.getConfigurationSection(name);
                final Sound type = EnumUtil.getByName(sound.getString("type"), Sound.class);
                if (type == null) {
                    continue;
                }
                this.sounds.put(name, new MessageSound(type, sound.getDouble("pitch"), sound.getDouble("volume"), sound.getStringList("trigger-messages")));
            }
        }
    }

    public void playSound(final Player player, final String message) {
        sounds.values().stream().filter(sound -> sound.getMessages().contains(message)).forEach(sound -> player.playSound(player.getLocation(), sound.getType(), sound.getVolume(), sound.getPitch()));
    }

    public MessageSound getSound(final String name) {
        return sounds.get(name);
    }

    public Set<String> getSounds() {
        return sounds.keySet();
    }


    public class MessageSound {
        private final Sound type;
        private final float pitch;
        private final float volume;
        private final List<String> messages;

        MessageSound(final Sound type, final double pitch, final double volume, final List<String> messages) {
            this.type = type;
            this.pitch = (float) pitch;
            this.volume = (float) volume;
            this.messages = messages;
        }

        public Sound getType() {
            return this.type;
        }

        public float getPitch() {
            return this.pitch;
        }

        public float getVolume() {
            return this.volume;
        }

        public List<String> getMessages() {
            return this.messages;
        }
    }

    public int getVersion() {
        return this.version;
    }

    public boolean isCheckForUpdates() {
        return this.checkForUpdates;
    }

    public boolean isCtpPreventDuel() {
        return this.ctpPreventDuel;
    }

    public boolean isCtpPreventTag() {
        return this.ctpPreventTag;
    }

    public boolean isPmPreventDuel() {
        return this.pmPreventDuel;
    }

    public boolean isPmPreventTag() {
        return this.pmPreventTag;
    }

    public boolean isClxPreventDuel() {
        return this.clxPreventDuel;
    }

    public boolean isClxPreventTag() {
        return this.clxPreventTag;
    }

    public boolean isAutoUnvanish() {
        return this.autoUnvanish;
    }

    public boolean isSetBackLocation() {
        return this.setBackLocation;
    }

    public boolean isDisableSkills() {
        return this.disableSkills;
    }

    public boolean isFuNoPowerLoss() {
        return this.fuNoPowerLoss;
    }

    public boolean isFNoPowerLoss() {
        return this.fNoPowerLoss;
    }

    public boolean isDuelzoneEnabled() {
        return this.duelzoneEnabled;
    }

    public List<String> getDuelzones() {
        return this.duelzones;
    }

    public boolean isMyPetDespawn() {
        return this.myPetDespawn;
    }

    public boolean isPreventBountyLoss() {
        return this.preventBountyLoss;
    }

    public boolean isPreventKDRChange() {
        return this.preventKDRChange;
    }

    public String getLhWinsCmd() {
        return this.lhWinsCmd;
    }

    public String getLhWinsTitle() {
        return this.lhWinsTitle;
    }

    public String getLhLossesCmd() {
        return this.lhLossesCmd;
    }

    public String getLhLossesTitle() {
        return this.lhLossesTitle;
    }

    public boolean isRequiresClearedInventory() {
        return this.requiresClearedInventory;
    }

    public boolean isPreventCreativeMode() {
        return this.preventCreativeMode;
    }

    public boolean isOwnInventoryDropInventoryItems() {
        return this.ownInventoryDropInventoryItems;
    }

    public boolean isOwnInventoryEnabled() {
        return this.ownInventoryEnabled;
    }

    public boolean isOwnInventoryUsePermission() {
        return this.ownInventoryUsePermission;
    }

    public boolean isKitSelectingEnabled() {
        return this.kitSelectingEnabled;
    }

    public boolean isKitSelectingUsePermission() {
        return this.kitSelectingUsePermission;
    }

    public boolean isArenaSelectingEnabled() {
        return this.arenaSelectingEnabled;
    }

    public boolean isArenaSelectingUsePermission() {
        return this.arenaSelectingUsePermission;
    }

    public boolean isItemBettingEnabled() {
        return this.itemBettingEnabled;
    }

    public boolean isItemBettingUsePermission() {
        return this.itemBettingUsePermission;
    }

    public boolean isMoneyBettingEnabled() {
        return this.moneyBettingEnabled;
    }

    public boolean isMoneyBettingUsePermission() {
        return this.moneyBettingUsePermission;
    }

    public int getExpiration() {
        return this.expiration;
    }

    public int getMaxDuration() {
        return this.maxDuration;
    }

    public boolean isStartCommandsEnabled() {
        return this.startCommandsEnabled;
    }

    public boolean isStartCommandsQueueOnly() {
        return this.startCommandsQueueOnly;
    }

    public List<String> getStartCommands() {
        return this.startCommands;
    }

    public boolean isEndCommandsEnabled() {
        return this.endCommandsEnabled;
    }

    public boolean isEndCommandsQueueOnly() {
        return this.endCommandsQueueOnly;
    }

    public List<String> getEndCommands() {
        return this.endCommands;
    }

    public boolean isProjectileHitMessageEnabled() {
        return this.projectileHitMessageEnabled;
    }

    public List<String> getProjectileHitMessageTypes() {
        return this.projectileHitMessageTypes;
    }

    public boolean isPreventInventoryOpen() {
        return this.preventInventoryOpen;
    }

    public boolean isProtectKitItems() {
        return this.protectKitItems;
    }

    public boolean isRemoveEmptyBottle() {
        return this.removeEmptyBottle;
    }

    public boolean isPreventTpToMatchPlayers() {
        return this.preventTpToMatchPlayers;
    }

    public boolean isForceAllowCombat() {
        return this.forceAllowCombat;
    }

    public boolean isCancelIfMoved() {
        return false;
    }

    public List<String> getBlacklistedWorlds() {
        return this.blacklistedWorlds;
    }

    public boolean isTeleportToLastLocation() {
        return this.teleportToLastLocation;
    }

    public int getTeleportDelay() {
        return this.teleportDelay;
    }

    public boolean isSpawnFirework() {
        return this.spawnFirework;
    }

    public boolean isArenaOnlyEndMessage() {
        return this.arenaOnlyEndMessage;
    }

    public boolean isDisplayInventories() {
        return this.displayInventories;
    }

    public boolean isPreventItemDrop() {
        return this.preventItemDrop;
    }

    public boolean isPreventItemPickup() {
        return this.preventItemPickup;
    }

    public boolean isLimitTeleportEnabled() {
        return this.limitTeleportEnabled;
    }

    public double getDistanceAllowed() {
        return this.distanceAllowed;
    }

    public boolean isBlockAllCommands() {
        return this.blockAllCommands;
    }

    public List<String> getWhitelistedCommands() {
        return this.whitelistedCommands;
    }

    public List<String> getBlacklistedCommands() {
        return this.blacklistedCommands;
    }

    public List<String> getQueueBlacklistedCommands() {
        return this.queueBlacklistedCommands;
    }

    public boolean isRatingEnabled() {
        return this.ratingEnabled;
    }

    public int getKFactor() {
        return this.kFactor;
    }

    public int getDefaultRating() {
        return this.defaultRating;
    }

    public boolean isRatingQueueOnly() {
        return this.ratingQueueOnly;
    }

    public boolean isSpecRequiresClearedInventory() {
        return this.specRequiresClearedInventory;
    }

    public boolean isSpecUseSpectatorGamemode() {
        return this.specUseSpectatorGamemode;
    }

    public boolean isSpecAddInvisibilityEffect() {
        return this.specAddInvisibilityEffect;
    }

    public List<String> getSpecWhitelistedCommands() {
        return this.specWhitelistedCommands;
    }

    public boolean isCdEnabled() {
        return this.cdEnabled;
    }

    public List<String> getCdMessages() {
        return this.cdMessages;
    }

    public List<String> getTitles() {
        return this.titles;
    }

    public boolean isPreventMovement() {
        return this.preventMovement;
    }

    public boolean isPreventLaunchProjectile() {
        return this.preventLaunchProjectile;
    }

    public boolean isPreventPvp() {
        return this.preventPvp;
    }

    public boolean isPreventInteract() {
        return this.preventInteract;
    }

    public boolean isDisplayKitRatings() {
        return this.displayKitRatings;
    }

    public boolean isDisplayNoKitRating() {
        return this.displayNoKitRating;
    }

    public boolean isDisplayPastMatches() {
        return this.displayPastMatches;
    }

    public int getMatchesToDisplay() {
        return this.matchesToDisplay;
    }

    public long getTopUpdateInterval() {
        return this.topUpdateInterval;
    }

    public String getTopWinsType() {
        return this.topWinsType;
    }

    public String getTopWinsIdentifier() {
        return this.topWinsIdentifier;
    }

    public String getTopLossesType() {
        return this.topLossesType;
    }

    public String getTopLossesIdentifier() {
        return this.topLossesIdentifier;
    }

    public String getTopKitType() {
        return this.topKitType;
    }

    public String getTopKitIdentifier() {
        return this.topKitIdentifier;
    }

    public String getTopNoKitType() {
        return this.topNoKitType;
    }

    public String getTopNoKitIdentifier() {
        return this.topNoKitIdentifier;
    }

    public int getKitSelectorRows() {
        return this.kitSelectorRows;
    }

    public String getKitSelectorFillerType() {
        return this.kitSelectorFillerType;
    }

    public short getKitSelectorFillerData() {
        return this.kitSelectorFillerData;
    }

    public int getArenaSelectorRows() {
        return this.arenaSelectorRows;
    }

    public String getArenaSelectorFillerType() {
        return this.arenaSelectorFillerType;
    }

    public short getArenaSelectorFillerData() {
        return this.arenaSelectorFillerData;
    }

    public String getSettingsFillerType() {
        return this.settingsFillerType;
    }

    public short getSettingsFillerData() {
        return this.settingsFillerData;
    }

    public int getQueuesRows() {
        return this.queuesRows;
    }

    public String getQueuesFillerType() {
        return this.queuesFillerType;
    }

    public short getQueuesFillerData() {
        return this.queuesFillerData;
    }

    public boolean isInheritKitItemType() {
        return this.inheritKitItemType;
    }

    public double getSoupHeartsToRegen() {
        return this.soupHeartsToRegen;
    }

    public boolean isSoupRemoveEmptyBowl() {
        return this.soupRemoveEmptyBowl;
    }

    public boolean isSoupCancelIfAlreadyFull() {
        return this.soupCancelIfAlreadyFull;
    }
}
