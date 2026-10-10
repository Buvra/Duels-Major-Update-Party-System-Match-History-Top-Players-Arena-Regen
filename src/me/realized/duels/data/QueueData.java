package me.realized.duels.data;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.kit.KitImpl;
import me.realized.duels.queue.Queue;

public class QueueData {

    private String kit;
    private int bet;

    private QueueData() {
        // serializer için
    }

    public QueueData(Queue queue) {
        if (queue == null) {
            return;
        }

        if (queue.getKit() != null) {
            this.kit = queue.getKit().getName();
        }

        this.bet = queue.getBet();
    }

    public Queue toQueue(DuelsPlugin plugin) {
        if (plugin == null) {
            return null;
        }

        KitImpl kitImpl = null;

        if (this.kit != null) {
            kitImpl = plugin.getKitManager().get(this.kit);
        }

        return new Queue(plugin, kitImpl, this.bet);
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