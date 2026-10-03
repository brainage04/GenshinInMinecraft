package io.github.brainage04.genshininminecraft.enemy;

import io.github.brainage04.genshininminecraft.GenshinInMinecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Loader adapters register this one shared definition; no natural spawn placement is installed. */
public final class GenshinEntities {
    public static final Identifier HILICHURL_ID = Identifier.fromNamespaceAndPath(GenshinInMinecraft.MOD_ID, "hilichurl");
    public static final EntityType<Hilichurl> HILICHURL = EntityType.Builder.of(Hilichurl::new, MobCategory.CREATURE)
            .sized(.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8).updateInterval(2).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE, HILICHURL_ID));
    private GenshinEntities() {}
}
