package io.github.dockyardmc.world.generators

import io.github.dockyardmc.registry.Biome
import io.github.dockyardmc.registry.Biomes
import io.github.dockyardmc.registry.Block
import io.github.dockyardmc.registry.Blocks

class VoidWorldGenerator: WorldGenerator {
    override fun getBlock(x: Int, y: Int, z: Int): Block = Blocks.AIR

    override fun getBiome(x: Int, y: Int, z: Int): Biome = Biomes.THE_VOID
}