package io.github.brainage04.genshininminecraft.client;

import io.github.brainage04.genshininminecraft.network.TeleportListPayload;
import io.github.brainage04.genshininminecraft.network.TeleportRequestPayload;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Deliberately a destination list, not the later full Genshin map screen. */
public final class TeleportScreen extends Screen {
    private static Consumer<TeleportRequestPayload> sender;
    private final List<TeleportListPayload.Entry> points;
    private int page;
    public TeleportScreen(TeleportListPayload payload) {
        super(Component.translatable("screen.genshininminecraft.teleport"));
        points = payload.points();
    }
    public static void initialize(Consumer<TeleportRequestPayload> send) { sender = send; }
    public static void requestList() { if (sender != null) sender.accept(new TeleportRequestPayload("")); }
    public static void accept(TeleportListPayload payload) {
        if (CombatInput.managed()) Minecraft.getInstance().gui.setScreen(new TeleportScreen(payload));
    }
    public List<TeleportListPayload.Entry> points() { return points; }
    private int rows() { return Math.max(1, (height - 116) / 24); }
    @Override protected void init() {
        int width = Math.min(380, this.width - 32);
        int start = page * rows(), end = Math.min(points.size(), start + rows());
        for (int index = start; index < end; index++) {
            var point = points.get(index);
            addRenderableWidget(Button.builder(Component.literal((point.statue() ? "◇ " : "◆ ") + point.name()), button -> {
                if (sender != null) sender.accept(new TeleportRequestPayload(point.id()));
                onClose();
            }).bounds((this.width - width) / 2, 54 + (index - start) * 24, width, 20).build());
        }
        if (points.size() > rows()) {
            var previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> { page--; rebuildWidgets(); })
                    .bounds(this.width / 2 - 72, height - 56, 32, 20).build());
            previous.active = page > 0;
            var next = addRenderableWidget(Button.builder(Component.literal(">"), button -> { page++; rebuildWidgets(); })
                    .bounds(this.width / 2 + 40, height - 56, 32, 20).build());
            next.active = end < points.size();
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(this.width / 2 - 70, height - 28, 140, 20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
        graphics.centeredText(font, title, width / 2, 16, 0xffffffff);
        graphics.centeredText(font, Component.translatable(points.isEmpty() ? "screen.genshininminecraft.teleport.empty"
                : "screen.genshininminecraft.teleport.hint"), width / 2, 34, 0xffc9d9dc);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void tick() { if (!CombatInput.managed() || minecraft.player == null || !minecraft.player.isAlive()) onClose(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.gui.setScreen(null); }
}
