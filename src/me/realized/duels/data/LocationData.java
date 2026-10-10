package me.realized.duels.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationData {

    private String world;
    private double x;
    private double y;
    private double z;
    private float pitch;
    private float yaw;

    public static LocationData fromLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new LocationData(location);
    }

    private LocationData() {
        // serializer için
    }

    private LocationData(World world, double x, double y, double z, float pitch, float yaw) {
        if (world != null) {
            this.world = world.getName();
        }
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    private LocationData(Location location) {
        this(
                location.getWorld(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getPitch(),
                location.getYaw()
        );
    }

    public World getWorld() {
        if (this.world == null) {
            return null;
        }
        return Bukkit.getWorld(this.world);
    }

    public Location toLocation() {
        World worldObj = this.getWorld();

        if (worldObj == null) {
            return null;
        }

        Location location = new Location(worldObj, this.x, this.y, this.z);
        location.setYaw(this.yaw);
        location.setPitch(this.pitch);

        return location;
    }

    public String getWorldName() {
        return this.world;
    }

    public void setWorldName(String world) {
        this.world = world;
    }

    public double getX() {
        return this.x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return this.y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return this.z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    @Override
    public String toString() {
        return "LocationData{" +
                "world='" + world + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", pitch=" + pitch +
                ", yaw=" + yaw +
                '}';
    }
}