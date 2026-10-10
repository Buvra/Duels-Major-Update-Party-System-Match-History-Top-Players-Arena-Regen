package me.realized.duels.gui.confirm;

import me.realized.duels.request.RequestImpl;
import me.realized.duels.setting.Settings;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class OwnInventoryConfirmHolder implements InventoryHolder {

    public enum Type {
        SENDER,
        RECEIVER
    }

    private final Type type;
    private final Player player;
    private final Player target;
    private final Settings settings;
    private final RequestImpl request;
    private Inventory inventory;

    public OwnInventoryConfirmHolder(final Type type, final Player player, final Player target, final Settings settings, final RequestImpl request) {
        this.type = type;
        this.player = player;
        this.target = target;
        this.settings = settings;
        this.request = request;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public void setInventory(final Inventory inventory) {
        this.inventory = inventory;
    }

    public Type getType() {
        return type;
    }

    public Player getPlayer() {
        return player;
    }

    public Player getTarget() {
        return target;
    }

    public Settings getSettings() {
        return settings;
    }

    public RequestImpl getRequest() {
        return request;
    }
}
