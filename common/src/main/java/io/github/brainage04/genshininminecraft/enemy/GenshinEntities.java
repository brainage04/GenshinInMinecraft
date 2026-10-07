package io.github.brainage04.genshininminecraft.enemy;

import io.github.brainage04.genshininminecraft.GenshinInMinecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Loader adapters register these shared definitions; no natural spawn placement is installed. */
public final class GenshinEntities {
    public static final Identifier HILICHURL_ID = Identifier.fromNamespaceAndPath(GenshinInMinecraft.MOD_ID, "hilichurl");
    public static final EntityType<Hilichurl> HILICHURL = EntityType.Builder.of(Hilichurl::new, MobCategory.CREATURE)
            .sized(.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8).updateInterval(2).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE, HILICHURL_ID));
    public static final Identifier BARON_BUNNY_ID = Identifier.fromNamespaceAndPath(GenshinInMinecraft.MOD_ID, "baron_bunny");
    /** Session-owned field object: never written to chunk entity storage or server saves. */
    public static final EntityType<BaronBunny> BARON_BUNNY = EntityType.Builder.of(BaronBunny::new, MobCategory.MISC)
            .sized(.49F, .6F).eyeHeight(.59F).clientTrackingRange(8).noLootTable().noSave().noSummon()
            .build(ResourceKey.create(Registries.ENTITY_TYPE, BARON_BUNNY_ID));
    public static final Identifier OVERLAY_MARKER_ID = Identifier.fromNamespaceAndPath(GenshinInMinecraft.MOD_ID, "overlay_marker");
    public static final EntityType<io.github.brainage04.genshininminecraft.world.OverlayMarker> OVERLAY_MARKER =
            EntityType.Builder.of(io.github.brainage04.genshininminecraft.world.OverlayMarker::new, MobCategory.MISC)
                    .sized(1, 3.2F).clientTrackingRange(10).noLootTable().noSummon()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, OVERLAY_MARKER_ID));
    private GenshinEntities() {}
}
