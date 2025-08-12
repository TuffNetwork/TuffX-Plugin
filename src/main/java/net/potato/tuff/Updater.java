package net.potato.tuff;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class Updater {

    private final JavaPlugin plugin;
    private final String currentVersion;

    public Updater(JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public void scheduleCheck() {
        if (!plugin.getConfig().getBoolean("updater.enabled", true)) return;
        long interval = plugin.getConfig().getLong("updater.check-interval-minutes", 60) * 60 * 20;
        new BukkitRunnable() {
            @Override
            public void run() {
                checkForUpdates();
            }
        }.runTaskTimerAsynchronously(plugin, 0L, interval);
    }

    private void checkForUpdates() {
        String localLatestVersion = currentVersion;

        String versionUrl = "https://verytuffautoupdater.netlify.app/version-remote.json";

        try {
            URL url = new URL(versionUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);

            if (connection.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonObject versionInfo = JsonParser.parseReader(reader).getAsJsonObject();
                reader.close();

                String webLatestVersion = versionInfo.get("latestVersion").getAsString();

                if (isNewer(webLatestVersion, currentVersion)) {
                    plugin.getLogger().info("A new version is available: " + webLatestVersion + "! Downloading...");
                    String downloadUrl = versionInfo.get("downloadUrl").getAsString();
                    downloadUpdate(downloadUrl);
                } else {
                    plugin.getLogger().info("You are running the latest version (" + currentVersion + ").");
                }
            } else {
                plugin.getLogger().warning("Could not check for updates. (Response code: " + connection.getResponseCode() + ")");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("An error occurred while checking for updates: " + e.getMessage());
        }
    }

    private boolean isNewer(String latest, String current) {
        return !latest.trim().equalsIgnoreCase(current);
    }
    
    private void downloadUpdate(String url) {
        try {
            File updateFolder = plugin.getServer().getUpdateFolderFile();
            if (!updateFolder.exists()) {
                updateFolder.mkdirs();
            }
            File downloadTarget = new File(updateFolder, plugin.getName() + ".jar");

            URL downloadUrl = new URL(url);
            ReadableByteChannel rbc = Channels.newChannel(downloadUrl.openStream());
            FileOutputStream fos = new FileOutputStream(downloadTarget);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            rbc.close();

            plugin.getLogger().info("Successfully downloaded the new version to the 'update' folder.");
            plugin.getLogger().info("The update will be applied on the next server restart.");

            if (plugin.getConfig().getBoolean("updater.restart-on-update", false)) {
                plugin.getLogger().info("Restarting server to apply the update...");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
                    }
                }.runTask(plugin);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("The update download failed: " + e.getMessage());
        }
    }
}