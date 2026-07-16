package com.example.autoserver;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;

@Mod(value = AutoServerMod.MOD_ID, dist = Dist.CLIENT)
public class AutoServerMod {
    public static final String MOD_ID = "autoserver";
    private static boolean added = false;

    @SubscribeEvent
    public void onScreenOpen(ScreenEvent.Opening event) {
        if (!added && Minecraft.getInstance().level == null) {
            addServer("我的服务器", "your.server.ip:25565");
            added = true;
        }
    }

    private void addServer(String name, String ip) {
        ServerList list = new ServerList(Minecraft.getInstance());
        list.load();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).ip.equals(ip)) return;
        }
        ServerData data = new ServerData(name, ip, ServerData.Type.SERVER);
        list.add(data, false);
        list.save();
    }
}
