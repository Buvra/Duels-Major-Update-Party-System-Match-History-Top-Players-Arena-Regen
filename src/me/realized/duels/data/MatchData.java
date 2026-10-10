package me.realized.duels.data;

import me.realized.duels.api.user.MatchInfo;

public class MatchData implements MatchInfo {

    private String winner;
    private String loser;
    private String kit;
    private long time;
    private long duration;
    private double health;
    private double betAmount;

    private MatchData() {
        // serializer için
    }

    public MatchData(String winner, String loser, String kit, long time, long duration, double health, double betAmount) {
        this.winner = winner;
        this.loser = loser;
        this.kit = kit;
        this.time = time;
        this.duration = duration;
        this.health = health;
        this.betAmount = betAmount;
    }

    @Override
    public long getCreation() {
        return this.time;
    }

    public String getWinner() {
        return this.winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public String getLoser() {
        return this.loser;
    }

    public void setLoser(String loser) {
        this.loser = loser;
    }

    public String getKit() {
        return this.kit;
    }

    public void setKit(String kit) {
        this.kit = kit;
    }

    public long getTime() {
        return this.time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getDuration() {
        return this.duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public double getHealth() {
        return this.health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public double getBetAmount() {
        return this.betAmount;
    }

    public void setBetAmount(double betAmount) {
        this.betAmount = betAmount;
    }

    @Override
    public String toString() {
        return "MatchData{" +
                "winner='" + winner + '\'' +
                ", loser='" + loser + '\'' +
                ", kit='" + kit + '\'' +
                ", time=" + time +
                ", duration=" + duration +
                ", health=" + health +
                ", betAmount=" + betAmount +
                '}';
    }
}