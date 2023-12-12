package me.anno.tests.mesh.minecraft

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.unique.StaticMeshManager
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.mesh.vox.model.VoxelModel
import me.anno.utils.Color.black
import me.anno.utils.LOGGER
import me.anno.utils.OS.downloads
import me.anno.utils.types.AnyToInt
import me.anno.utils.types.Booleans.toInt
import java.util.*
import kotlin.math.log2
import kotlin.math.max

class SimpleModel(val colors: IntArray) : VoxelModel(16, 16, 16) {
    override fun getBlock(x: Int, y: Int, z: Int): Int {
        return colors[y.shl(8) or z.shl(4) or x]
    }
}

class MonoModel(val color: Int) : VoxelModel(16, 16, 16) {
    override fun getBlock(x: Int, y: Int, z: Int): Int = color
}

fun blockToColor(block: Map<String, Any>): Int {
    var name = block["Name"].toString()
    if (name == "minecraft:air") return 0
    if (name.startsWith("minecraft:")) name = name.substring("minecraft:".length)
    return when (name) {
        "stone", "stone_brick_slab", "stone_bricks", "brick_slab" -> 0x777777
        "smooth_quartz", "smooth_stone_slab" -> 0x888888
        "quartz_block", "quartz_slab", "quartz_stairs", "quartz_pillar",
        "chiseled_quartz_block" -> 0xccc7bb
        "dirt" -> 0x75432f
        "coarse_dirt" -> 0x9e6047
        "gravel" -> 0x555555
        "anvil" -> 0x444444
        "iron_ore" -> 0x776655
        "coal_ore" -> 0x222222
        "gold_ore" -> 0x887766
        "redstone_ore" -> 0xff6666
        "lapis_ore" -> 0x5577ff
        "diamond_ore" -> 0x77ffff
        "bedrock" -> 0x000000
        "water", "seagrass", "tall_seagrass", "kelp" -> 0xaaccff
        "lava" -> 0xffaa33
        "snow" -> 0xffffff
        "obsidian" -> 0x201122
        "cut_sandstone", "chiseled_sandstone", "sandstone_stairs", "sandstone_slab" -> 0xd9d0b6
        "chiseled_red_sandstone", "red_sandstone", "red_sandstone_stairs", "smooth_red_sandstone",
        "cut_red_sandstone", "red_sandstone_slab", "red_sandstone_wall" -> 0xbd7e7e
        "nether_brick_slab", "nether_brick_fence", "nether_brick_stairs",
        "nether_bricks", "red_nether_brick_slab" -> 0x730e05
        "netherrack" -> 0xa83328
        "wall_torch", "glowstone" -> 0xffff77
        "andesite", "polished_andesite" -> 0x888888
        "cobblestone", "cobblestone_stairs", "cobblestone_slab", "cobblestone_wall" -> 0x666666
        "red_carpet", "red_wool", "red_concrete", "red_concrete_powder",
        "red_terracotta" -> 0xff3333
        "white_concrete", "white_wool", "white_terracotta",
        "white_carpet", "white_stained_glass", "white_stained_glass_pane" -> 0xdddddd
        "cyan_concrete", "cyan_wool", "cyan_terracotta",
        "cyan_stained_glass" -> 0x33ffff
        "purple_wool", "purple_terracotta", "purple_stained_glass" -> 0xff33ff
        "green_concrete", "green_wool", "green_carpet" -> 0x33ff33
        "black_wool", "black_terracotta", "black_stained_glass_pane",
        "black_carpet" -> 0x333333
        "orange_wool", "orange_terracotta", "orange_carpet",
        "orange_stained_glass", "orange_concrete" -> 0xffaa33
        "lime_wool", "lime_terracotta", "lime_carpet" -> 0xc4ff77
        "brown_wool", "brown_stained_glass_pane", "brown_carpet" -> 0x5e351d
        "magenta_wool", "magenta_stained_glass", "magenta_shulker_box",
        "magenta_terracotta" -> 0xd424ce
        "blue_wool", "blue_carpet", "blue_stained_glass_pane" -> 0x3333ff
        "pink_wool", "pink_carpet", "pink_stained_glass_pane" -> 0xeb6ee6
        "yellow_concrete", "yellow_wool", "yellow_terracotta",
        "wet_sponge", "yellow_carpet" -> 0xffff33
        "light_gray_concrete", "light_gray_concrete_powder",
        "light_gray_wool", "light_gray_stained_glass_pane",
        "light_gray_stained_glass", "light_gray_carpet" -> 0xcccccc
        "gray_concrete", "gray_concrete_powder", "gray_wool",
        "gray_terracotta", "gray_stained_glass", "gray_carpet",
        "gray_stained_glass_pane" -> 0x777777
        "light_blue_concrete", "light_blue_concrete_powder", "light_blue_wool",
        "light_blue_terracotta", "light_blue_stained_glass_pane" -> 0xaabbee
        "oak_leaves", "azalea_leaves" -> 0x8cf591
        "jungle_leaves" -> 0x49d150
        "birch_leaves", "acacia_leaves" -> 0x95ed9a
        "dark_oak_leaves" -> 0x418045
        "spruce_leaves" -> 0x4e7351
        "rail", "iron_bars" -> 0x555555
        "redstone_block", "tnt" -> 0xff2222
        "iron_block", "iron_door", "iron_trapdoor" -> 0xaaaaaa
        "raw_iron_block" -> 0x776655
        "jungle_stairs", "jungle_fence", "jungle_trapdoor", "jungle_planks", "jungle_slab" -> 0x7d4b09
        "coal_block" -> 0x111111
        "grass_block", "tall_grass", "large_fern" -> 0x77ff77
        "oxeye_daisy", "azure_bluet" -> 0xe6d8b1
        "mycelium" -> 0xbd61c7
        "mossy_cobblestone_wall", "mossy_stone_bricks", "mossy_cobblestone" -> 0x364a31
        "oak_fence", "oak_trapdoor", "oak_planks", "oak_door", "oak_fence_gate", "oak_slab" -> 0xbd7659
        "birch_fence", "birch_fence_gate", "birch_door", "birch_trapdoor", "birch_slab",
        "birch_stairs" -> 0xb8aa86
        "birch_log" -> 0xdeccb4
        "acacia_log" -> 0x524b42
        "acacia_trapdoor", "acacia_stairs", "acacia_door", "acacia_slab" -> 0xa36615
        "oak_log" -> 0x523723
        "stripped_dark_oak_log", "dark_oak_log" -> 0x2e231a
        "dark_oak_slab", "dark_oak_door", "dark_oak_trapdoor", "dark_oak_stairs",
        "stripped_dark_oak_wood", "dark_oak_fence" -> 0x664325
        "stripped_jungle_log" -> 0x634832
        "stripped_spruce_log", "spruce_slab" -> 0x4f3b2b
        "spruce_log" -> 0x261b12
        "jungle_log" -> 0x333118
        "mushroom_stem" -> 0xeeeeee
        "basalt" -> 0x38354d
        "fire" -> 0xfa901e
        "redstone_wire", "glass", "fern", "grass", "repeater", "magenta_candle", "candle", "pink_candle",
        "cyan_candle", "orange_candle", "orange_wall_banner", "birch_wall_sign", "light_blue_wall_banner",
        "tripwire", "dead_brain_coral_fan", "oak_wall_sign", "birch_sign", "acacia_button",
        "acacia_wall_sign", "ladder", "vine", "dark_oak_button", "dark_oak_wall_sign" -> return 0 // air
        else -> {
            LOGGER.warn("Unknown block $block")
            0xff00ff
        }
    } or black
}

fun main() {

    // load a section of a Minecraft world
    // todo separate transparent/non-transparent, and make glass transparent
    val world = Entity()
    world.add(StaticMeshManager()) // hack
    val file = downloads.getChild("r.0.0.mca")
    val region = RegionFormatReader.read(file.readByteBufferSync(false))
    val chunkData = Array(1024) {
        ArrayList<VoxelModel?>()
    }

    for (ci in region.chunks.indices) {
        val chunk = region.chunks[ci] ?: continue
        // other value: DataVersion
        val level = chunk.properties["Level"] as Map<String, Any> // also contains TileEntities
        val sections = level["Sections"] as List<Map<String, Any>>
        for (section in sections) {
            val y = AnyToInt.getInt(section["Y"], 0) // it's a Byte, but who knows
            val palette = section["Palette"] as? List<Map<String, Any>> ?: continue // Name"", Properties{}
            val paletteColors = palette.map { blockToColor(it) }.toIntArray()
            val packedBlockIds = section["BlockStates"] as? LongArray
            val chunkSize = 16 * 16 * 16
            fun addModel(model: VoxelModel) {
                val list = chunkData[ci]
                for (i in list.size..y) {
                    list.add(null)
                }
                list[y] = model
            }
            if (palette.size == 1) {
                val color = paletteColors[0]
                if (color == 0) {
                    // air -> skip
                    continue
                } else {
                    // create mono-colored cube
                    addModel(MonoModel(color))
                }
            } else {
                try {
                    packedBlockIds ?: continue
                    val bitsPerValue = max(log2(palette.size.toFloat()).toInt() + 1, 4)
                    val mask = (1 shl bitsPerValue) - 1
                    val idsPerValue = 64 / bitsPerValue
                    if (idsPerValue * packedBlockIds.size >= chunkSize) {
                        val colors = IntArray(chunkSize) {
                            val v = packedBlockIds[it / idsPerValue] ushr ((it % idsPerValue) * bitsPerValue)
                            paletteColors[v.toInt() and mask]
                        }
                        addModel(SimpleModel(colors))
                    } else {
                        val bits = BitSet.valueOf(packedBlockIds)
                        val colors = IntArray(chunkSize) {
                            val offset = it * bitsPerValue
                            var sum = 0
                            for (i in 0 until bitsPerValue) {
                                sum += bits[offset + i].toInt() shl (bitsPerValue - 1 - i)
                            }
                            paletteColors[sum]
                        }
                        addModel(SimpleModel(colors))
                    }
                } catch (e: ArrayIndexOutOfBoundsException) {
                    e.printStackTrace()
                    continue
                }
            }
        }
    }

    for (ci in region.chunks.indices) {
        val chunk = region.chunks[ci] ?: continue
        val level = chunk.properties["Level"] as? Map<String, Any> ?: continue
        val xPos = AnyToInt.getInt(level["xPos"], 0)
        val zPos = AnyToInt.getInt(level["zPos"], 0)
        val chunkEntity = Entity("$xPos,$zPos", world)
        chunkEntity.setPosition(xPos * 16.0, 0.0, zPos * 16.0)
        val data = chunkData[ci]
        for (y in data.indices) {
            val model = data[y] ?: continue
            model.center0()
            val x = ci and 31
            val z = ci shr 5
            val neighbors = BlockSide.values.map {
                val nx = x + it.x
                val nz = z + it.z
                if (nx in 0 until 32 && nz in 0 until 32) {
                    val ny = y + it.y
                    chunkData[nx + nz.shl(5)].getOrNull(ny)
                } else null
            }
            val mesh = model.createMesh(null, null, { xi, yi, zi ->
                val side = when {
                    yi < 0 && xi in 0 until 16 && zi in 0 until 16 -> BlockSide.NY
                    yi >= 16 && xi in 0 until 16 && zi in 0 until 16 -> BlockSide.PY
                    xi < 0 && yi in 0 until 16 && zi in 0 until 16 -> BlockSide.NX
                    xi >= 16 && yi in 0 until 16 && zi in 0 until 16 -> BlockSide.PX
                    zi < 0 && xi in 0 until 16 && yi in 0 until 16 -> BlockSide.NZ
                    zi >= 16 && xi in 0 until 16 && yi in 0 until 16 -> BlockSide.PZ
                    else -> null
                }
                val neighbor = if (side != null) neighbors[side.ordinal] else null
                neighbor != null && neighbor.getBlock(x and 15, y and 15, z and 15) != 0
            })
            Entity("$y", chunkEntity)
                .setPosition(0.0, y * 16.0, 0.0)
                .add(MeshComponent(mesh))
        }
    }

    testSceneWithUI("Minecraft World", world)
}
