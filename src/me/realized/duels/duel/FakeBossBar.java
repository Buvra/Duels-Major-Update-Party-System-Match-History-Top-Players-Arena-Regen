package me.realized.duels.duel;

import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.EntityEnderDragon;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class FakeBossBar {

    private final Player player;
    private final EntityEnderDragon dragon;
    private boolean spawned;

    public FakeBossBar(Player player, String title, float progress) {
        this.player = player;
        this.dragon = new EntityEnderDragon(((CraftWorld) player.getWorld()).getHandle());
        this.dragon.setCustomName(title);
        this.dragon.setCustomNameVisible(false);
        this.dragon.setInvisible(true);
        this.dragon.setHealth(this.toDragonHealth(progress));
        this.spawned = false;
        this.updateLocation();
    }

    public Player getPlayer() {
        return this.player;
    }

    public void spawn() {
        if (!this.player.isOnline() || this.spawned) {
            return;
        }

        this.updateLocation();
        this.sendPacket(new PacketPlayOutSpawnEntityLiving(this.dragon));
        this.sendMetadata();
        this.spawned = true;
    }

    public void destroy() {
        if (!this.spawned) {
            return;
        }

        this.sendPacket(new PacketPlayOutEntityDestroy(this.dragon.getId()));
        this.spawned = false;
    }

    public void update(String title, float progress) {
        if (!this.player.isOnline()) {
            return;
        }

        this.dragon.setCustomName(title);
        this.dragon.setHealth(this.toDragonHealth(progress));
        this.updateLocation();

        if (!this.spawned) {
            this.spawn();
            return;
        }

        this.sendMetadata();
        this.sendPacket(new PacketPlayOutEntityTeleport(this.dragon));
    }

    private void updateLocation() {
        Location base = this.player.getLocation();
        this.dragon.setLocation(base.getX(), -200.0D, base.getZ(), 0.0F, 0.0F);
    }

    private float toDragonHealth(float progress) {
        float clamped = Math.max(1.0F, Math.min(100.0F, progress));
        return Math.max(1.0F, Math.min(200.0F, clamped * 2.0F));
    }

    private void sendMetadata() {
        DataWatcher watcher = this.dragon.getDataWatcher();
        this.sendPacket(new PacketPlayOutEntityMetadata(this.dragon.getId(), watcher, true));
    }

    private void sendPacket(Packet<?> packet) {
        if (!this.player.isOnline()) {
            return;
        }

        ((CraftPlayer) this.player).getHandle().playerConnection.sendPacket(packet);
    }
}