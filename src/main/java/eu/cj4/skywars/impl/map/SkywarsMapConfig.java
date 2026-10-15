package eu.cj4.skywars.impl.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public record SkywarsMapConfig(Identifier template, ResourceKey<Biome> biome) {
    public static final Codec<SkywarsMapConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("template").forGetter(SkywarsMapConfig::template),
            ResourceKey.codec(Registries.BIOME).optionalFieldOf("biome", Biomes.THE_VOID).forGetter(SkywarsMapConfig::biome)
    ).apply(instance, SkywarsMapConfig::new));
}
