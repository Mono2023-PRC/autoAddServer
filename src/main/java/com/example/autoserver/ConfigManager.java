package com.example.autoserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final String CONFIG_FILE_NAME = "autoserver_config.json";
    private static ConfigManager instance;
    private final Gson gson;
    private final Path configPath;
    private Config config;

    private ConfigManager() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        configPath = Minecraft.getInstance().gameDirectory.toPath().resolve(CONFIG_FILE_NAME);
        loadConfig();
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private void loadConfig() {
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                config = gson.fromJson(reader, Config.class);
            } catch (IOException e) {
                config = new Config();
            }
        } else {
            config = new Config();
            saveConfig();
        }
    }

    public void saveConfig() {
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getApiUrl() {
        return config.apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        config.apiUrl = apiUrl;
        saveConfig();
    }

    public boolean hasApiUrl() {
        return config.apiUrl != null && !config.apiUrl.isEmpty();
    }

    public static class Config {
        public String apiUrl = "";
    }
}
