package com.example.autoserver;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientChatEvent;

@Mod(value = AutoServerMod.MOD_ID, dist = Dist.CLIENT)
public class AutoServerMod {
    public static final String MOD_ID = "autoserver";
    private static AutoServerMod instance;
    private final ConfigManager configManager;
    private final AsyncServerFetcher fetcher;
    private boolean hasFetchedThisSession;

    public AutoServerMod() {
        instance = this;
        configManager = ConfigManager.getInstance();
        fetcher = new AsyncServerFetcher();
        hasFetchedThisSession = false;
    }

    public static AutoServerMod getInstance() {
        return instance;
    }

    public static void openApiSettingsScreen(Screen parent) {
        Minecraft.getInstance().setScreen(new ApiSettingsScreen(parent));
    }

    public static void refreshServer() {
        if (instance != null && instance.configManager.hasApiUrl()) {
            instance.hasFetchedThisSession = false;
            instance.fetcher.resetAndRetry(instance.configManager.getApiUrl(), instance.createCallback());
        }
    }

    public static void fetchAndAddServer() {
        if (instance != null && instance.configManager.hasApiUrl() && !instance.hasFetchedThisSession) {
            instance.hasFetchedThisSession = true;
            instance.fetcher.fetchServerIp(instance.configManager.getApiUrl(), instance.createCallback());
        }
    }

    private AsyncServerFetcher.ServerFetchCallback createCallback() {
        return new AsyncServerFetcher.ServerFetchCallback() {
            @Override
            public void onSuccess(String ip) {
                addServer("自动添加服务器", ip);
            }

            @Override
            public void onFailure(String error) {
                instance.hasFetchedThisSession = false;
            }
        };
    }

    private static void addServer(String name, String ip) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            ServerList list = new ServerList(mc);
            list.load();
            
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).ip.equals(ip)) {
                    return;
                }
            }
            
            ServerData data = new ServerData(name, ip, ServerData.Type.OTHER);
            list.add(data, false);
            list.save();
        });
    }
}
