/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.extension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import me.realized.duels.api.Duels;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DuelsExtension {
    protected Duels api;
    private String name;
    private File folder;
    private File file;
    private File dataFolder;
    private boolean enabled;
    private File configFile;
    private FileConfiguration config;

    final void init(Duels api, String name, File folder, File file) {
        this.api = api;
        this.name = name;
        this.folder = folder;
        this.file = file;
        this.dataFolder = new File(folder, name);
        this.configFile = new File(this.dataFolder, "config.yml");
    }

    @NotNull
    public Duels getApi() {
        return this.api;
    }

    @NotNull
    public final String getName() {
        return this.name;
    }

    @NotNull
    public File getFolder() {
        return this.folder;
    }

    @NotNull
    public File getFile() {
        return this.file;
    }

    @NotNull
    public File getDataFolder() {
        return this.dataFolder;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        if (enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
        this.enabled = enabled;
    }

    public void saveResource(@NotNull String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath");
        resourcePath = resourcePath.replace('\\', '/');
        try (InputStream in = this.getResource(resourcePath);){
            if (in == null) {
                throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + this.file);
            }
            if (!this.dataFolder.exists()) {
                this.dataFolder.mkdir();
            }
            File outFile = new File(this.dataFolder, resourcePath);
            int lastIndex = resourcePath.lastIndexOf(47);
            File outDir = new File(this.dataFolder, resourcePath.substring(0, Math.max(lastIndex, 0)));
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            try (FileOutputStream out = new FileOutputStream(outFile);){
                int len;
                byte[] buf = new byte[1024];
                while ((len = in.read(buf)) > 0) {
                    ((OutputStream)out).write(buf, 0, len);
                }
            }
        }
        catch (IOException ex) {
            this.api.error("Could not save resource '" + resourcePath + "'", ex);
        }
    }

    @Nullable
    public InputStream getResource(@NotNull String filename) {
        Objects.requireNonNull(filename, "filename");
        try {
            URL url = this.getClass().getClassLoader().getResource(filename);
            if (url == null) {
                return null;
            }
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        }
        catch (IOException ex) {
            this.api.error("Could not find resource with filename '" + filename + "'", ex);
            return null;
        }
    }

    public FileConfiguration getConfig() {
        if (this.config == null) {
            this.reloadConfig();
        }
        return this.config;
    }

    public void reloadConfig() {
        if (!this.configFile.exists()) {
            this.saveResource("config.yml");
        }
        this.config = YamlConfiguration.loadConfiguration((File)this.configFile);
    }

    public void saveConfig() {
        try {
            this.getConfig().save(this.configFile);
        }
        catch (IOException ex) {
            this.api.error("Failed to save config", ex);
        }
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    @Deprecated
    @Nullable
    public String getRequiredVersion() {
        return null;
    }
}
