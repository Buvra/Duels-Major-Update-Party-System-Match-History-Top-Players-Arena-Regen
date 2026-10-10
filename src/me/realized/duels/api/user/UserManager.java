/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.user;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import me.realized.duels.api.kit.Kit;
import me.realized.duels.api.user.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface UserManager {
    public boolean isLoaded();

    @Nullable
    public User get(@NotNull String var1);

    @Nullable
    public User get(@NotNull UUID var1);

    @Nullable
    public User get(@NotNull Player var1);

    @Nullable
    public TopEntry getTopWins();

    @Nullable
    public TopEntry getTopLosses();

    @Nullable
    public TopEntry getTopRatings();

    @Nullable
    public TopEntry getTopRatings(@NotNull Kit var1);

    public static class TopData
    implements Comparable<TopData> {
        private final UUID uuid;
        private final String name;
        private final int value;

        public TopData(@NotNull UUID uuid, @NotNull String name, int value) {
            Objects.requireNonNull(uuid, "uuid");
            Objects.requireNonNull(name, "name");
            this.uuid = uuid;
            this.name = name;
            this.value = value;
        }

        public UUID getUuid() {
            return this.uuid;
        }

        public String getName() {
            return this.name;
        }

        public int getValue() {
            return this.value;
        }

        @Override
        public int compareTo(@NotNull TopData data) {
            Objects.requireNonNull(data, "data");
            return Integer.compare(this.value, data.value);
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || this.getClass() != other.getClass()) {
                return false;
            }
            TopData topData = (TopData)other;
            return this.value == topData.value && Objects.equals(this.uuid, topData.uuid) && Objects.equals(this.name, topData.name);
        }

        public int hashCode() {
            return Objects.hash(this.uuid, this.name, this.value);
        }
    }

    public static class TopEntry {
        private final long creation;
        private final String type;
        private final String identifier;
        private final List<TopData> data;

        public TopEntry(@NotNull String type, @NotNull String identifier, @NotNull List<TopData> data) {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(identifier, "identifier");
            Objects.requireNonNull(data, "data");
            this.creation = System.currentTimeMillis();
            this.type = type;
            this.identifier = identifier;
            this.data = data;
        }

        public long getCreation() {
            return this.creation;
        }

        public String getType() {
            return this.type;
        }

        public String getIdentifier() {
            return this.identifier;
        }

        public List<TopData> getData() {
            return this.data;
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || this.getClass() != other.getClass()) {
                return false;
            }
            TopEntry topEntry = (TopEntry)other;
            return Objects.equals(this.type, topEntry.type) && Objects.equals(this.identifier, topEntry.identifier) && Objects.equals(this.data, topEntry.data);
        }

        public int hashCode() {
            return Objects.hash(this.type, this.identifier, this.data);
        }
    }
}
