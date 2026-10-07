package io.github.brainage04.genshininminecraft.client.character;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;

/** Explicit API implementation: common compilation never relies on injected vanilla interfaces. */
public final class CharacterGeoRenderState implements GeoRenderState {
    public static final DataTicket<CharacterRenderInput> INPUT = DataTicket.create("genshin_character_input", CharacterRenderInput.class);
    private final Map<DataTicket<?>, Object> data = new Reference2ObjectOpenHashMap<>();
    private final CharacterRenderInput input;
    public CharacterGeoRenderState(CharacterRenderInput input) { this.input = input; }
    public CharacterRenderInput input() { return input; }
    @Override public Map<DataTicket<?>, Object> getDataMap() { return data; }
}
