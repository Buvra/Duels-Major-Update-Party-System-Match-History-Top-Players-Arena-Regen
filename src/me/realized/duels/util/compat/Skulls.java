package me.realized.duels.util.compat;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import me.realized.duels.util.reflect.ReflectionUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

public final class Skulls {

    private static final Method GET_PROFILE;
    private static final Field PROFILE;

    private static final LoadingCache<Player, GameProfile> cache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .weakKeys()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build(new CacheLoader<Player, GameProfile>() {

                @Override
                public GameProfile load(@NotNull final Player player) throws InvocationTargetException, IllegalAccessException {
                    return getProfile(player);
                }
            }
        );

    static {
        final Class<?> CB_PLAYER = ReflectionUtil.getCBClass("entity.CraftPlayer");
        GET_PROFILE = ReflectionUtil.getMethod(CB_PLAYER, "getProfile");

        final Class<?> CB_SKULL_META = ReflectionUtil.getCBClass("inventory.CraftMetaSkull");
        PROFILE = ReflectionUtil.getDeclaredField(CB_SKULL_META, "profile");
    }

    private static GameProfile getProfile(final Player player) throws InvocationTargetException, IllegalAccessException {
       return (GameProfile) GET_PROFILE.invoke(player);
    }

    public static void setProfile(final SkullMeta meta, final Player player) {
        try {
            final GameProfile cached = cache.get(player);
            PROFILE.set(meta, cached);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Skulls() {}
}
