package me.realized.duels.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.arena.ArenaImpl;
import me.realized.duels.kit.KitImpl;
import org.bukkit.Location;

public class ArenaData {

    private String name;
    private boolean disabled;
    private Set<String> kits = new HashSet<String>();
    private Map<Integer, LocationData> positions = new HashMap<Integer, LocationData>();

    private ArenaData() {
        // serializer için
    }

    public ArenaData(ArenaImpl arena) {
        if (arena == null) {
            return;
        }

        this.name = arena.getName();
        this.disabled = arena.isDisabled();

        if (arena.getKits() != null) {
            for (KitImpl kit : arena.getKits()) {
                if (kit != null && kit.getName() != null) {
                    this.kits.add(kit.getName());
                }
            }
        }

        if (arena.getPositions() != null) {
            for (Map.Entry<Integer, Location> entry : arena.getPositions().entrySet()) {
                if (entry == null) {
                    continue;
                }

                Location location = entry.getValue();
                if (location == null || location.getWorld() == null) {
                    continue;
                }

                LocationData locationData = LocationData.fromLocation(location);
                if (locationData != null) {
                    this.positions.put(entry.getKey(), locationData);
                }
            }
        }
    }

    public ArenaImpl toArena(DuelsPlugin plugin) {
        if (plugin == null) {
            return null;
        }

        ArenaImpl arena = new ArenaImpl(plugin, this.name, this.disabled);

        if (this.kits != null) {
            for (String kitName : this.kits) {
                if (kitName == null) {
                    continue;
                }

                KitImpl kit = plugin.getKitManager().get(kitName);
                if (kit != null) {
                    arena.getKits().add(kit);
                }
            }
        }

        if (this.positions != null) {
            for (Map.Entry<Integer, LocationData> entry : this.positions.entrySet()) {
                if (entry == null || entry.getValue() == null) {
                    continue;
                }

                Location location = entry.getValue().toLocation();
                if (location != null) {
                    arena.getPositions().put(entry.getKey(), location);
                }
            }
        }

        arena.refreshGui(arena.isAvailable());
        return arena;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public Set<String> getKits() {
        return this.kits;
    }

    public void setKits(Set<String> kits) {
        this.kits = kits != null ? kits : new HashSet<String>();
    }

    public Map<Integer, LocationData> getPositions() {
        return this.positions;
    }

    public void setPositions(Map<Integer, LocationData> positions) {
        this.positions = positions != null ? positions : new HashMap<Integer, LocationData>();
    }

    @Override
    public String toString() {
        return "ArenaData{" +
                "name='" + name + '\'' +
                ", disabled=" + disabled +
                ", kits=" + kits +
                ", positions=" + positions +
                '}';
    }
}