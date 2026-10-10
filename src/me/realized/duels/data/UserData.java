package me.realized.duels.data;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import me.realized.duels.api.kit.Kit;
import me.realized.duels.api.user.MatchInfo;
import me.realized.duels.api.user.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UserData implements User {

    private UUID uuid;
    private volatile String name;

    private final AtomicInteger wins = new AtomicInteger();
    private final AtomicInteger losses = new AtomicInteger();

    private volatile boolean requests = true;

    private ConcurrentHashMap<String, Integer> rating;

    private final Object matchesLock = new Object();
    private List<MatchData> matches = new ArrayList<MatchData>();
    private final AtomicLong revision = new AtomicLong(1L);
    private final AtomicLong persistedRevision = new AtomicLong();

    // runtime config values, DB'ye yazılmak zorunda değil
    transient volatile int defaultRating;
    transient volatile int matchesToDisplay;

    private UserData() {
        // Jackson / serializer için boş constructor
    }

    public UserData(int defaultRating, int matchesToDisplay, Player player) {
        this(player.getUniqueId(), player.getName(), defaultRating, matchesToDisplay);
    }

    public UserData(UUID uuid, String name, int defaultRating, int matchesToDisplay) {
        this.uuid = uuid;
        this.name = name;
        this.defaultRating = defaultRating;
        this.matchesToDisplay = matchesToDisplay;
        this.rating = new ConcurrentHashMap<String, Integer>();
        this.matches = new ArrayList<MatchData>();
    }

    public void applyRuntimeSettings(int defaultRating, int matchesToDisplay) {
        this.defaultRating = defaultRating;
        this.matchesToDisplay = matchesToDisplay;

        if (this.rating == null) {
            this.rating = new ConcurrentHashMap<String, Integer>();
        }

        synchronized (this.matchesLock) {
            if (this.matches == null) {
                this.matches = new ArrayList<MatchData>();
            }
            this.refreshMatchesLocked();
        }
    }

    public void setWins(int wins) {
        this.wins.set(wins);
        this.touch();
    }

    public void setLosses(int losses) {
        this.losses.set(losses);
        this.touch();
    }

    public boolean canRequest() {
        return this.requests;
    }

    public void setRequests(boolean requests) {
        this.requests = requests;
        this.touch();
    }

    @NotNull
    public List<MatchInfo> getMatches() {
        synchronized (this.matchesLock) {
            return Collections.<MatchInfo>unmodifiableList(
                    new ArrayList<MatchInfo>(this.matches)
            );
        }
    }

    public List<MatchData> getMatchData() {
        synchronized (this.matchesLock) {
            return Collections.unmodifiableList(new ArrayList<MatchData>(this.matches));
        }
    }

    public int getRating() {
        return this.getRatingUnsafe(null);
    }

    public int getRating(Kit kit) {
        return this.getRatingUnsafe(kit);
    }

    public void resetRating() {
        this.setRating(null, this.defaultRating);
    }

    public void resetRating(@NotNull Kit kit) {
        this.setRating(kit, this.defaultRating);
    }

    public void reset() {
        this.wins.set(0);
        this.losses.set(0);

        synchronized (this.matchesLock) {
            this.matches.clear();
        }

        if (this.rating == null) {
            this.rating = new ConcurrentHashMap<String, Integer>();
        } else {
            this.rating.clear();
        }

        this.touch();
    }

    private int getRatingUnsafe(Kit kit) {
        if (this.rating == null) {
            return this.defaultRating;
        }

        String key = kit == null ? "-" : kit.getName();
        Integer value = this.rating.get(key);
        return value != null ? value.intValue() : this.defaultRating;
    }

    public void setRating(Kit kit, int rating) {
        if (this.rating == null) {
            this.rating = new ConcurrentHashMap<String, Integer>();
        }

        this.rating.put(kit == null ? "-" : kit.getName(), rating);
        this.touch();
    }

    public ConcurrentHashMap<String, Integer> getRatings() {
        if (this.rating == null) {
            this.rating = new ConcurrentHashMap<String, Integer>();
        }

        return this.rating;
    }

    public void setRatings(ConcurrentHashMap<String, Integer> rating) {
        this.rating = rating != null ? rating : new ConcurrentHashMap<String, Integer>();
        this.touch();
    }

    public void addWin() {
        this.wins.incrementAndGet();
        this.touch();
    }

    public void addLoss() {
        this.losses.incrementAndGet();
        this.touch();
    }

    public void addMatch(MatchData matchData) {
        if (matchData == null) {
            return;
        }

        synchronized (this.matchesLock) {
            if (this.matches == null) {
                this.matches = new ArrayList<MatchData>();
            }

            if (this.matchesToDisplay > 0 && this.matches.size() >= this.matchesToDisplay) {
                this.matches.remove(0);
            }

            this.matches.add(matchData);
        }

        this.touch();
    }

    public void setMatches(List<MatchData> matches) {
        synchronized (this.matchesLock) {
            this.matches = matches != null
                    ? new ArrayList<MatchData>(matches)
                    : new ArrayList<MatchData>();

            this.refreshMatchesLocked();
        }

        this.touch();
    }

    void refreshMatches() {
        synchronized (this.matchesLock) {
            this.refreshMatchesLocked();
        }
    }

    private void refreshMatchesLocked() {
        if (this.matches == null) {
            this.matches = new ArrayList<MatchData>();
            return;
        }

        if (this.matchesToDisplay > 0 && this.matches.size() > this.matchesToDisplay) {
            List<MatchData> division = Lists.newArrayList(
                    this.matches.subList(
                            this.matches.size() - this.matchesToDisplay,
                            this.matches.size()
                    )
            );
            this.matches.clear();
            this.matches.addAll(division);
        }
    }

    @Override
    public String toString() {
        List<MatchData> matchesSnapshot;
        synchronized (this.matchesLock) {
            matchesSnapshot = new ArrayList<MatchData>(this.matches);
        }

        return "UserData{" +
                "uuid=" + this.uuid +
                ", name='" + this.name + '\'' +
                ", wins=" + this.wins.get() +
                ", losses=" + this.losses.get() +
                ", requests=" + this.requests +
                ", matches=" + matchesSnapshot +
                ", rating=" + this.rating +
                '}';
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
        this.touch();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        this.touch();
    }

    public int getWins() {
        return this.wins.get();
    }

    public int getLosses() {
        return this.losses.get();
    }

    public long getRevision() {
        return this.revision.get();
    }

    public boolean isDirty() {
        return this.getRevision() > this.persistedRevision.get();
    }

    public void markPersisted() {
        this.markPersisted(this.getRevision());
    }

    public void markPersisted(long revision) {
        while (true) {
            long current = this.persistedRevision.get();
            if (revision <= current) {
                return;
            }

            if (this.persistedRevision.compareAndSet(current, revision)) {
                return;
            }
        }
    }

    private void touch() {
        this.revision.incrementAndGet();
    }
}
