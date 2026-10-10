package me.realized.duels.data;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.kit.KitImpl;
import me.realized.duels.queue.Queue;
import me.realized.duels.queue.sign.QueueSignImpl;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public class QueueSignData {

    private LocationData location;
    private String kit;
    private int bet;

    private QueueSignData() {
        // serializer için
    }

    public QueueSignData(QueueSignImpl sign) {
        if (sign == null) {
            return;
        }

        this.location = LocationData.fromLocation(sign.getLocation());

        Queue queue = sign.getQueue();
        if (queue != null) {
            this.kit = queue.getKit() != null ? queue.getKit().getName() : null;
            this.bet = queue.getBet();
        }
    }

    public QueueSignImpl toQueueSign(DuelsPlugin plugin) {
        if (plugin == null || this.location == null) {
            return null;
        }

        Location bukkitLocation = this.location.toLocation();
        if (bukkitLocation == null || bukkitLocation.getWorld() == null) {
            return null;
        }

        Block block = bukkitLocation.getBlock();
        if (block == null || !(block.getState() instanceof Sign)) {
            return null;
        }

        KitImpl kitImpl = this.kit != null ? plugin.getKitManager().get(this.kit) : null;

        Queue queue = plugin.getQueueManager().get(kitImpl, this.bet);
        if (queue == null) {
            plugin.getQueueManager().create(kitImpl, this.bet);
            queue = plugin.getQueueManager().get(kitImpl, this.bet);
        }

        if (queue == null) {
            return null;
        }

        String kitName = this.kit != null
                ? this.kit
                : plugin.getLang().getMessage("GENERAL.none");

        String[] replacements = new String[] {
                "kit", kitName,
                "bet_amount", String.valueOf(this.bet)
        };

        String lines = plugin.getLang().getMessage("SIGN.format", (Object[]) replacements);
        return new QueueSignImpl(bukkitLocation, lines, queue);
    }

    public LocationData getLocation() {
        return this.location;
    }

    public void setLocation(LocationData location) {
        this.location = location;
    }

    public String getKit() {
        return this.kit;
    }

    public void setKit(String kit) {
        this.kit = kit;
    }

    public int getBet() {
        return this.bet;
    }

    public void setBet(int bet) {
        this.bet = bet;
    }
}