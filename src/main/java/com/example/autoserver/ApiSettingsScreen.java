package com.example.autoserver;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ApiSettingsScreen extends Screen {
    private final Screen parentScreen;
    private EditBox apiUrlField;
    private final ConfigManager configManager;

    public ApiSettingsScreen(Screen parentScreen) {
        super(Component.literal("设置API地址"));
        this.parentScreen = parentScreen;
        this.configManager = ConfigManager.getInstance();
    }

    @Override
    protected void init() {
        super.init();
        
        int textFieldWidth = 200;
        int textFieldHeight = 20;
        int x = this.width / 2 - textFieldWidth / 2;
        int y = this.height / 2 - 30;
        
        apiUrlField = new EditBox(this.font, x, y, textFieldWidth, textFieldHeight, Component.literal("API地址"));
        apiUrlField.setMaxLength(100);
        apiUrlField.setValue(configManager.getApiUrl());
        apiUrlField.setHint(Component.literal("请输入域名:端口"));
        this.addWidget(apiUrlField);
        
        int buttonWidth = 100;
        int buttonHeight = 20;
        int buttonY = y + 35;
        
        this.addRenderableWidget(Button.builder(
                Component.literal("保存"),
                button -> {
                    String url = apiUrlField.getValue().trim();
                    if (!url.isEmpty()) {
                        configManager.setApiUrl(url);
                        AutoServerMod.fetchAndAddServer();
                    }
                    this.minecraft.setScreen(parentScreen);
                }
        ).bounds(this.width / 2 - buttonWidth - 5, buttonY, buttonWidth, buttonHeight).build());
        
        this.addRenderableWidget(Button.builder(
                Component.literal("取消"),
                button -> this.minecraft.setScreen(parentScreen)
        ).bounds(this.width / 2 + 5, buttonY, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 50, 16777215);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.setScreen(parentScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
