package com.example.autoserver;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientChatEvent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
            public void onSuccess(String responseData) {
                processServerData(responseData);
            }

            @Override
            public void onFailure(String error) {
                instance.hasFetchedThisSession = false;
            }
        };
    }

    private static void processServerData(String responseData) {
        String defaultName = "自动添加的服务器";

        try {
            JsonObject json = JsonParser.parseString(responseData).getAsJsonObject();

            if (!json.has("ip")) {
                System.err.println("[AutoServer] Missing 'ip' field in response");
                return;
            }
            String ip = json.get("ip").getAsString();

            int port = 25565;
            if (json.has("port")) {
                try {
                    port = json.get("port").getAsInt();
                } catch (Exception e) {
                    System.err.println("[AutoServer] Invalid 'port' field, using default 25565");
                }
            }

            String name = defaultName;
            if (json.has("name")) {
                String n = json.get("name").getAsString();
                if (n != null && !n.isEmpty()) {
                    name = n;
                }
            }

            String address = formatServerAddress(ip, port);
            if (address == null) {
                System.err.println("[AutoServer] Invalid IP address: " + ip);
                return;
            }

            addServer(name, address);
        } catch (Exception e) {
            System.err.println("[AutoServer] Failed to parse server data: " + e.getMessage());
        }
    }

    private static String formatServerAddress(String ip, int port) {
        String cleanIp = ip.trim();
        if (cleanIp.startsWith("[") && cleanIp.endsWith("]")) {
            cleanIp = cleanIp.substring(1, cleanIp.length() - 1);
        }

        try {
            InetAddress addr = InetAddress.getByName(cleanIp);
            if (addr instanceof Inet6Address) {
                return "[" + cleanIp + "]:" + port;
            } else {
                return cleanIp + ":" + port;
            }
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static void addServer(String name, String ip) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            ServerList list = new ServerList(mc);
            list.load();

            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).ip.equals(ip)) {
                    mc.getToasts().addToast(new SimpleToast(
                            Component.literal("AutoServer"),
                            Component.literal("服务器已存在，跳过添加")
                    ));
                    return;
                }
            }

            ServerData data = new ServerData(name, ip, ServerData.Type.OTHER);
            list.add(data, false);
            list.save();
            mc.getToasts().addToast(new SimpleToast(
                    Component.literal("AutoServer"),
                    Component.literal("服务器添加成功: " + name)
            ));
        });
    }
}
