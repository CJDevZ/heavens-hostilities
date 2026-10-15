package eu.cj4.skywars.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.cj4.skywars.impl.map.SkywarsMapConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import eu.cj4.minigame.api.MiniGameMap;

import java.util.List;
import java.util.Optional;

public record SkywarsMap(SkywarsMapConfig map, BlockPos offset, Vec3 centerPos, float borderWidth, List<BlockPos> spawnLocations) implements MiniGameMap {
    public static final Codec<SkywarsMap> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    SkywarsMapConfig.CODEC.fieldOf("map").forGetter(SkywarsMap::map),
                    BlockPos.CODEC.fieldOf("offset").forGetter(SkywarsMap::offset),
                    Vec3.CODEC.fieldOf("centerPos").forGetter(SkywarsMap::centerPos),
                    Codec.FLOAT.fieldOf("borderWidth").forGetter(SkywarsMap::borderWidth),
                    BlockPos.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("spawns").forGetter(SkywarsMap::spawnLocations)
            ).apply(instance, SkywarsMap::new));

    public void placeMap(ServerLevel world) {
        Optional<StructureTemplate> template = world.getStructureTemplateManager().get(map.template());
        template.ifPresent(structureTemplate -> {
            structureTemplate.placeInWorld(world, offset, new BlockPos(0, 0, 0), new StructurePlaceSettings(), world.getRandom(), 0);
        });
        for (BlockPos spawnLocation : spawnLocations) {
            spawnLocation = offset.offset(spawnLocation);
            placeHollowCube(world, spawnLocation, 2, Blocks.GLASS.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static void placeHollowCube(ServerLevel world, BlockPos origin, int radius, BlockState state, int flag) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    boolean isWall =
                            x == -radius || x == radius ||  // left or right face
                                    y == -radius || y == radius ||  // bottom or top face
                                    z == -radius || z == radius;    // front or back face

                    if (isWall) {
                        BlockPos pos = origin.offset(x, y, z);
                        world.setBlock(pos, state, flag);
                    }
                }
            }
        }
    }
}
