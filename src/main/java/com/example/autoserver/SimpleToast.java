package com.example.autoserver;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;

public class SimpleToast implements Toast {
    private final Component title;
    private final Component message;
    private long startTime;

    public SimpleToast(Component title, Component message) {
        this.title = title;
        this.message = message;
    }

    @Override
    public Toast.Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long time) {
        if (startTime == 0) {
            startTime = time;
        }
        
        guiGraphics.drawString(toastComponent.getMinecraft().font, title, 30, 7, -1);
        guiGraphics.drawString(toastComponent.getMinecraft().font, message, 30, 18, -1);
        
        return time - startTime >= 3000 ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }
}
