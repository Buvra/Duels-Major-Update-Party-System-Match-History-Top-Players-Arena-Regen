package me.realized.duels.party;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 500 CCU Practice/Duels altyapısı için optimize edilmiş, thread-safe Parti veri modeli.
 *
 * <p>Performans ve Bellek Optimizasyonları:
 * <ul>
 *   <li>Player nesneleri asla tutulmaz, sadece {@link UUID} saklanır (Memory Leak / Bellek Sızıntısı önlemi).</li>
 *   <li>Tüm üye ve davet listeleri {@link ConcurrentHashMap} tabanlı setler kullanır (Thread-Safety).</li>
 *   <li>Tüm varlık sorguları (contains, isLeader, isFull) O(1) zaman karmaşıklığına sahiptir.</li>
 * </ul>
 */
public class Party {

    public static final int MAX_MEMBERS = 5;

    private volatile UUID leader;
    private final Set<UUID> members = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private final Set<UUID> invites = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    /**
     * Yeni bir parti nesnesi oluşturur. Lider otomatik olarak üyelere eklenir.
     *
     * @param leader Parti liderinin UUID'si.
     */
    public Party(@NotNull final UUID leader) {
        Objects.requireNonNull(leader, "leader");
        this.leader = leader;
        this.members.add(leader);
    }

    /**
     * Parti liderinin UUID'sini döner.
     */
    @NotNull
    public UUID getLeader() {
        return leader;
    }

    /**
     * Parti liderini günceller (Lider devretme işlemlerinde kullanılır).
     */
    public void setLeader(@NotNull final UUID leader) {
        Objects.requireNonNull(leader, "leader");
        this.leader = leader;
        this.members.add(leader);
    }

    /**
     * Belirtilen UUID'nin parti lideri olup olmadığını kontrol eder. O(1)
     */
    public boolean isLeader(@Nullable final UUID uuid) {
        return uuid != null && leader.equals(uuid);
    }

    /**
     * Partinin dolu olup olmadığını kontrol eder (Maksimum 5 kişi). O(1)
     */
    public boolean isFull() {
        return members.size() >= MAX_MEMBERS;
    }

    /**
     * Belirtilen UUID'nin partide olup olmadığını kontrol eder. O(1)
     */
    public boolean contains(@Nullable final UUID uuid) {
        return uuid != null && members.contains(uuid);
    }

    /**
     * Partideki toplam üye sayısını döner. O(1)
     */
    public int size() {
        return members.size();
    }

    /**
     * Partiye yeni üye ekler. Parti doluysa veya üye zaten varsa false döner.
     */
    public boolean addMember(@NotNull final UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        if (isFull()) {
            return false;
        }
        invites.remove(uuid);
        return members.add(uuid);
    }

    /**
     * Belirtilen üyeyi partiden çıkarır.
     */
    public boolean removeMember(@Nullable final UUID uuid) {
        return uuid != null && members.remove(uuid);
    }

    /**
     * Bekleyen bir davet ekler.
     */
    public boolean addInvite(@NotNull final UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return invites.add(uuid);
    }

    /**
     * Bekleyen daveti kaldırır.
     */
    public boolean removeInvite(@Nullable final UUID uuid) {
        return uuid != null && invites.remove(uuid);
    }

    /**
     * Belirtilen UUID'nin aktif bir daveti olup olmadığını kontrol eder. O(1)
     */
    public boolean hasInvite(@Nullable final UUID uuid) {
        return uuid != null && invites.contains(uuid);
    }

    /**
     * Değiştirilemez üye listesi döner.
     */
    @NotNull
    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    /**
     * Değiştirilemez davet listesi döner.
     */
    @NotNull
    public Set<UUID> getInvites() {
        return Collections.unmodifiableSet(invites);
    }

    /**
     * Partideki tüm çevrimiçi üye sayısını hesaplar.
     */
    public int getOnlineCount() {
        int count = 0;
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Partideki tüm çevrimiçi üyelere mesaj gönderir.
     * Geçici koleksiyon veya stream oluşturulmaz, doğrudan döngü ile GC yükü sıfıra indirilir.
     */
    public void broadcast(@NotNull final String message) {
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }

    /**
     * Partiyi temizler.
     */
    public void clear() {
        members.clear();
        invites.clear();
    }
}
