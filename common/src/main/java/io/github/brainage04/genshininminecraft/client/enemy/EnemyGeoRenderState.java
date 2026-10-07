package io.github.brainage04.genshininminecraft.client.enemy;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class EnemyGeoRenderState extends LivingEntityRenderState implements GeoRenderState {
    private final Map<DataTicket<?>, Object> data = new Reference2ObjectOpenHashMap<>();
    @Override public Map<DataTicket<?>, Object> getDataMap() { return data; }
    // GeckoLib injects concrete superclass writers using its private map; override BOTH sides.
    @Override public <D> void addGeckolibData(DataTicket<D> ticket, D value) { data.put(ticket, value); }
    @Override public boolean hasGeckolibData(DataTicket<?> ticket) { return data.containsKey(ticket); }
}
