package eu.cj4.skywars.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public record SkywarsTeam(BlockPos spawnPoint, AABB glassCase) {
}
