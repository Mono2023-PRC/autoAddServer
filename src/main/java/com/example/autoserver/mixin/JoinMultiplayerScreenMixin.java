package com.example.autoserver.mixin;

import com.example.autoserver.AutoServerMod;
import com.example.autoserver.ConfigManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void addAutoServerButtons(CallbackInfo ci) {
        int buttonWidth = 80;
        int buttonHeight = 20;
        int spacing = 5;
        
        int x = this.width / 2 - buttonWidth - spacing / 2;
        int y = this.height - 35;
        
        this.addRenderableWidget(Button.builder(
                Component.literal("设置API"),
                button -> AutoServerMod.openApiSettingsScreen(this)
        ).bounds(x, y, buttonWidth, buttonHeight).build());
        
        this.addRenderableWidget(Button.builder(
                Component.literal("刷新"),
                button -> AutoServerMod.refreshServer()
        ).bounds(x + buttonWidth + spacing, y, buttonWidth, buttonHeight).build());
        
        ConfigManager config = ConfigManager.getInstance();
        if (config.hasApiUrl()) {
            AutoServerMod.fetchAndAddServer();
        }
    }
}
