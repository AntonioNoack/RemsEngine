package me.anno.tests.mesh.formats.minecraft

import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.StaticMeshManager
import me.anno.ecs.systems.Systems
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.PipelineStageImpl
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.mesh.vox.meshing.NeedsFace
import me.anno.mesh.vox.model.VoxelModel
import me.anno.tests.LOGGER
import me.anno.utils.Clock
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import me.anno.utils.OS.downloads
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.structures.arrays.BooleanArrayList
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.AnyToInt
import me.anno.utils.types.Booleans.toInt
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.log2
import kotlin.math.max

class DenseI32Model(val colors: IntArray) : VoxelModel(16, 16, 16) {
    override fun getIndex(x: Int, y: Int, z: Int): Int {
        return y.shl(8) or z.shl(4) or x
    }

    override fun getBlock(x: Int, y: Int, z: Int): Int {
        return colors[getIndex(x, y, z)]
    }
}

class MonoModel(val color: Int) : VoxelModel(16, 16, 16) {
    override fun getBlock(x: Int, y: Int, z: Int): Int = color
}

fun stripEnd(name: String, end: String): String {
    return if (name.endsWith(end)) name.substring(0, name.length - end.length)
    else name
}

fun stripStart(name: String, start: String): String {
    return if (name.startsWith(start)) name.substring(start.length)
    else name
}

private val colorMap = HashMap<String?, Int>(256)
fun blockToColor(block: Map<String, Any>): Int {
    val name = block["Name"].toString()
    return colorMap.getOrPut(name) { blockToColor0(name) }
}

private val starts = listOf(
    "minecraft:", "infested_", "cracked_", "polished_", "chiseled_", "potted_", "smooth_", "cut_"
)
private val ends = listOf(
    "_stained_glass_pane", "_stained_glass", "_carpet", "_concrete_powder", "_concrete",
    "_slab", "_stairs", "_wool", "_wall", "_door", "_trapdoor", "_glazed_terracotta", "_terracotta",
    "_shulker_box", "_bed", "_fence_gate", "_fence", "_planks", "_pressure_plate", "_tulip", "_block",
    "_pillar", "_wood", "_bricks", "_brick"
)

fun blockToColor0(name0: String): Int {
    var name = name0
    for (start in starts) {
        name = stripStart(name, start)
    }
    for (end in ends) {
        name = stripEnd(name, end)
    }
    return when (name) {
        "stone", "bricks", "terracotta", "lodestone" -> 0x777777
        "quartz" -> 0xccc7bb
        "dirt", "farmland" -> 0x75432f
        "coarse_dirt" -> 0x9e6047
        "gravel" -> 0x555555
        "wheat" -> 0xb3a268
        "anvil", "chipped_anvil" -> 0x444444
        "iron_ore" -> 0x776655
        "coal_ore" -> 0x222222
        "gold_ore" -> 0x887766
        "redstone_ore" -> 0xff6666
        "deepslate_copper_ore" -> 0x422e0d
        "deepslate_redstone_ore" -> 0xaa2222
        "deepslate_emerald_ore" -> 0x22aa22
        "lapis_ore" -> 0x5577ff
        "diamond_ore" -> 0x77ffff
        "diamond" -> 0x22ffff
        "bedrock" -> 0x000000
        "water", "seagrass", "tall_seagrass", "kelp_plant" ->
            return (0xaaccff).withAlpha(200)
        "kelp" -> 0x2b1c17 // right?
        "lava" -> 0xffaa33
        "snow" -> 0xffffff
        "deepslate", "deepslate_tile", "cobbled_deepslate", "deepslate_tiles" -> 0x333333
        "obsidian", "crying_obsidian", "ender_chest", "enchanting_table" -> 0x201122
        "cut_sandstone", "sandstone", "sand" -> 0xd9d0b6
        "red_sandstone", "red_sand" -> 0xbd7e7e
        "nether", "red_nether", "red_mushroom", "nether_wart",
        "crimson" -> 0x730e05
        "netherrack" -> 0xa83328
        "wall_torch", "glowstone", "lantern", "torch", "light" -> 0xffff77
        "soul_wall_torch", "soul_torch" -> 0x9999ff
        "andesite" -> 0x888888
        "cobblestone" -> 0x666666
        "red", "poppy" -> 0xff3333
        "brick" -> 0xc9483e
        "beehive" -> 0xc9902c
        "white", "skeleton_skull", "skeleton_wall_skull", "cake", "bone", "lily_of_the_valley", "cobweb" -> 0xdddddd
        "dark_prismarine" -> 0x254520
        "prismarine" -> 0x5ea6a3
        "sea_lantern" -> 0xcccccc
        "cyan", "warped" -> 0x33ffff
        "purple" -> 0xff33ff
        "green", "melon", "zombie_head", "cactus" -> 0x33ff33
        "black", "dragon_egg", "wither_skeleton_skull", "blackstone", "spawner", "wither_rose",
        "gilded_blackstone", "smithing_table" -> 0x333333
        "waxed_cut_copper" -> 0x996a42
        "waxed_exposed_cut_copper" -> 0x948250
        "waxed_weathered_cut_copper" -> 0x5ea68f
        "waxed_oxidized_cut_copper" -> 0x38ab86
        "orange", "pumpkin", "shroomlight" -> 0xffaa33
        "granite" -> 0xc77e65
        "end_stone", "end_rod", "end_portal" -> 0xd6c096
        "lapis" -> 0x3377ff
        "slime", "emerald", "creeper_head" -> 0x77ff77
        "dropper", "dispenser", "hopper", "cauldron", "water_cauldron" -> 0x444444
        "lime" -> 0xc4ff77
        "brown" -> 0x5e351d
        "magenta" -> 0xd424ce
        "blue", "blue_orchid" -> 0x3333ff
        "pink" -> 0xeb6ee6
        "yellow", "sponge", "wet_sponge", "sunflower", "bee_nest", "dandelion" -> 0xffff33
        "light_gray" -> 0xcccccc
        "gray" -> 0x777777
        "light_blue" -> 0xaabbee
        "oak_leaves", "azalea_leaves" -> 0x8cf591
        "jungle_leaves", "azalea_bush" -> 0x49d150
        "birch_leaves", "acacia_leaves" -> 0x95ed9a
        "dark_oak_leaves" -> 0x418045
        "spruce_leaves" -> 0x4e7351
        "rail", "iron_bars" -> 0x555555
        "detector_rail" -> 0x665555
        "powered_rail" -> 0x666055
        "redstone", "tnt", "redstone_wall_torch", "redstone_torch" -> 0xff2222
        "heavy_weighted", "gold" -> 0xffc900
        "iron", "light_weighted", "chain" -> 0xaaaaaa
        "raw_iron" -> 0x776655
        "player_head", "player_wall_head" -> 0x777777 // could be any color
        "jungle" -> 0x7d4b09
        "coal" -> 0x111111
        "diorite" -> 0xbbbbbb
        "grass", "tall_grass", "large_fern", "rose_bush", "moss", "azalea" -> 0x77ff77
        "oxeye_daisy", "azure_bluet", "peony" -> 0xe6d8b1
        "mycelium" -> 0xbd61c7
        "mossy_stone", "mossy_cobblestone" -> 0x364a31
        "furnace" -> 0x555555
        "birch" -> 0xb8aa86
        "birch_log" -> 0xdeccb4
        "acacia_log" -> 0x524b42
        "acacia" -> 0xa36615
        "sugar_cane" -> 0xb5935b
        "oak_log", "composter" -> 0x523723
        "oak", "stripped_oak", "stripped_oak_log", "lectern", "scaffolding", "bookshelf", "campfire", "petrified_oak",
        "piston", "sticky_piston", "chest", "trapped_chest", "piston_head", "redstone_lamp", "note", "crafting_table",
        "fletching_table", "grindstone", "cartography_table", "dead_bush", "jukebox" -> 0xbd7659
        "stripped_dark_oak_log", "dark_oak_log" -> 0x2e231a
        "dark_oak", "stripped_dark_oak" -> 0x664325
        "stripped_jungle_log" -> 0x634832
        "stripped_spruce_log", "spruce", "stripped_spruce" -> 0x4f3b2b
        "spruce_log" -> 0x261b12
        "jungle_log" -> 0x333118
        "mushroom_stem" -> 0xeeeeee
        "basalt" -> 0x38354d
        "fire" -> 0xfa901e
        "allium" -> 0xc164d1
        "clay" -> 0xafb9ba
        "purpur", "shulker_box", "chorus_flower" -> 0xd096d6
        "repeating_command", "chain_command", "command", "jigsaw", "daylight_detector" -> 0x998866
        "glass", "glass_pane", "beacon" -> 0xdddddd
        "podzol" -> 0x734a2a
        "ice" -> return 0xaabbee.withAlpha(200)
        "packed_ice" -> return 0x3333ff.withAlpha(200)
        "soul_sand", "brown_mushroom" -> 0x59412e
        "redstone_wire", "fern", "repeater", "candle", "tripwire", "twisting_vines", "sea_pickle",
        "ladder", "vine", "lever", "cave_vines", "cave_vines_plant", "brewing_stand", "lightning_rod",
        "tripwire_hook", "barrier", "comparator", "air", "flower_pot", "warped_roots" -> return 0 // air
        else -> when {
            name.endsWith("_banner") || name.endsWith("_candle") || name.endsWith("_button")
                    || name.endsWith("_sign") || name.endsWith("_coral_fan") ->
                return 0 // air
            name.endsWith("_coral") -> 0x777777
            name.endsWith("_sapling") -> 0xcc5555
            else -> {
                LOGGER.warn("Unknown block $name")
                0xff00ff
            }
        }
    }.withAlpha(if ("glass" in name0) 200 else 255)
}

fun main() {

    val clock = Clock("ReadWorld")
    val worker = ProcessingGroup("Chunks", 1f)

    ECSRegistry.init()

    // load a section of a Minecraft world
    val scene = Entity("Scene")

    val lights = Entity("Lights", scene)
    val sky = Skybox()
    lights.add(sky)
    val light = Entity("Sun", lights)
    val light1 = DirectionalLight()
    light.add(light1)
    sky.applyOntoSun(light, light1, 20f)

    Systems.registerSystem(StaticMeshManager()) // performance hack

    clock.stop("Scene Basics")

    val file = downloads.getChild("r.0.0.mca")
    val bytes = file.readByteBufferSync(false)

    clock.stop("Reading bytes")

    val region = RegionFormatReader.read(bytes)

    clock.stop("Reading structure")

    val chunkData = createArrayList(1024) {
        ArrayList<VoxelModel?>()
    }

    val crash = AtomicBoolean(false)
    worker.processUnbalanced(0, 1024, 4) { i0, i1 ->
        for (ci in i0 until i1) {
            val chunk = region.chunks[ci] ?: continue

            // other value: DataVersion
            @Suppress("UNCHECKED_CAST")
            val level = chunk.properties["Level"] as Map<String, Any> // also contains TileEntities

            @Suppress("UNCHECKED_CAST")
            val sections = level["Sections"] as List<Map<String, Any>>
            for (section in sections) {
                val y = AnyToInt.getInt(section["Y"], 0) // it's a Byte, but who knows

                @Suppress("UNCHECKED_CAST")
                val palette = section["Palette"] as? List<Map<String, Any>> ?: continue // Name"", Properties{}
                val paletteColors = palette.map { blockToColor(it) }.toIntArray()
                val packedBlockIds = section["BlockStates"] as? LongArray
                val chunkSize = 16 * 16 * 16

                fun addModel(solid: VoxelModel?) {
                    val list = chunkData[ci]
                    for (i in list.size..y) {
                        list.add(null)
                    }
                    list[y] = solid
                }

                fun addModel(colors: IntArray) {
                    val hasBlocks = colors.any { it.a() > 0 }
                    addModel(if (hasBlocks) DenseI32Model(colors) else null)
                }

                if (palette.size == 1) {
                    val color = paletteColors[0]
                    if (color != 0) {
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
                            addModel(colors)
                        } else {
                            val bits = BooleanArrayList.valueOf(packedBlockIds)
                            val colors = IntArray(chunkSize) {
                                val offset = it * bitsPerValue
                                var sum = 0
                                for (i in 0 until bitsPerValue) {
                                    sum += bits[offset + i].toInt() shl (bitsPerValue - 1 - i)
                                }
                                paletteColors.getOrNull(sum) ?: (0xff00ff or black)
                            }
                            addModel(colors)
                        }
                    } catch (e: IndexOutOfBoundsException) {
                        if (!crash.getAndSet(true)) e.printStackTrace()
                        continue
                    }
                }
            }
        }
    }

    clock.stop("Loading models")

    val materials = listOf(false, true)
        .map { isTransparent ->
            listOf(Material().apply {
                if (isTransparent) {
                    pipelineStage = PipelineStageImpl.GLASS_PASS
                    metallicMinMax.set(1f)
                    roughnessMinMax.set(0f)
                }
            }.ref)
        }

    val needsFace = listOf(false, true).map { isTransparent ->
        if (isTransparent) {
            NeedsFace { inside, outside -> inside.a() in 1 until 255 && outside.a() == 0 }
        } else {
            NeedsFace { inside, outside -> inside.a() == 255 && outside.a() != 255 }
        }
    }
    val entities = arrayOfNulls<Entity>(1024)
    worker.processUnbalanced(0, 1024, 4) { i0, i1 ->
        for (ci in i0 until i1) {
            val chunk = region.chunks[ci] ?: continue

            @Suppress("UNCHECKED_CAST")
            val level = chunk.properties["Level"] as? Map<String, Any> ?: continue
            val xPos = AnyToInt.getInt(level["xPos"], 0)
            val zPos = AnyToInt.getInt(level["zPos"], 0)
            val chunkEntity = Entity("$xPos,$zPos")
            chunkEntity.setPosition(xPos * 16.0, 0.0, zPos * 16.0)
            val data = chunkData[ci]
            entities[ci] = chunkEntity
            for (y in data.indices) {
                for (materialId in 0 until 2) {
                    val model = data[y] ?: continue
                    model.center0()
                    val x = ci and 31
                    val z = ci shr 5
                    val neighbors = BlockSide.entries.map {
                        val nx = x + it.x
                        val nz = z + it.z
                        if (nx in 0 until 32 && nz in 0 until 32) {
                            val ny = y + it.y
                            chunkData[nx + nz.shl(5)].getOrNull(ny)
                        } else null
                    }
                    val mesh = model.createMesh(null, { xi, yi, zi ->
                        val side = getNeighborSide(xi, yi, zi)
                        val neighbor = if (side != null) neighbors[side.ordinal] else null
                        neighbor?.getBlock(x and 15, y and 15, z and 15) ?: 0
                    }, needsFace[materialId])
                    mesh.materials = materials[materialId]
                    Entity("$y", chunkEntity)
                        .setPosition(0.0, y * 16.0, 0.0)
                        .add(MeshComponent(mesh))
                }
            }
        }
    }

    for (entity in entities) {
        entity ?: continue
        scene.add(entity)
    }

    clock.stop("Creating meshes")
    worker.stop()

    testSceneWithUI("Minecraft World", scene)
}

fun getNeighborSide(xi: Int, yi: Int, zi: Int): BlockSide? {
    return when {
        yi < 0 && xi in 0 until 16 && zi in 0 until 16 -> BlockSide.NY
        yi >= 16 && xi in 0 until 16 && zi in 0 until 16 -> BlockSide.PY
        xi < 0 && yi in 0 until 16 && zi in 0 until 16 -> BlockSide.NX
        xi >= 16 && yi in 0 until 16 && zi in 0 until 16 -> BlockSide.PX
        zi < 0 && xi in 0 until 16 && yi in 0 until 16 -> BlockSide.NZ
        zi >= 16 && xi in 0 until 16 && yi in 0 until 16 -> BlockSide.PZ
        else -> null
    }
}
