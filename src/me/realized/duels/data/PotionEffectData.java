package me.realized.duels.data;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectData {

    private String type;
    private int duration;
    private int amplifier;

    public static PotionEffectData fromPotionEffect(PotionEffect effect) {
        if (effect == null) {
            return null;
        }
        return new PotionEffectData(effect);
    }

    private PotionEffectData() {
        // serializer için
    }

    private PotionEffectData(PotionEffect effect) {
        if (effect.getType() != null) {
            this.type = effect.getType().getName();
        }
        this.duration = effect.getDuration();
        this.amplifier = effect.getAmplifier();
    }

    public PotionEffect toPotionEffect() {
        if (this.type == null) {
            return null;
        }

        PotionEffectType effectType = PotionEffectType.getByName(this.type.toUpperCase());
        if (effectType == null) {
            return null;
        }

        return new PotionEffect(effectType, this.duration, this.amplifier);
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getAmplifier() {
        return this.amplifier;
    }

    public void setAmplifier(int amplifier) {
        this.amplifier = amplifier;
    }

    @Override
    public String toString() {
        return "PotionEffectData{" +
                "type='" + type + '\'' +
                ", duration=" + duration +
                ", amplifier=" + amplifier +
                '}';
    }
}