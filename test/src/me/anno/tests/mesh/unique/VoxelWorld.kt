package me.anno.tests.mesh.unique

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.material.Material.Companion.defaultMaterial
import me.anno.ecs.systems.Updatable
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.tests.mesh.unique.ItemPanel.Companion.previewBlockIds
import me.anno.tests.utils.TestWorld
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.NineTilePanel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.white
import me.anno.utils.types.Booleans.hasFlag
import org.apache.logging.log4j.LogManager
import org.joml.Vector3i

private val LOGGER = LogManager.getLogger("VoxelWorld")

/**
 * load/unload a big voxel world without much stutter;
 * glBufferData() unfortunately lags every once in a while, but that should be fine,
 * because it's a few times and then newer again
 *
 * (Minecraft like)
 *
 * done dynamic chunk unloading
 * done load/save system
 * done block placing
 *
 * todo first person player controller with simple physics
 * todo inventory system
 * */
fun main() {

    // val inHand = Inventory(1)
    val inventory = Inventory(9)

    val saveSystem = SaveLoadSystem("UniqueMeshRenderer")
    val world = object : TestWorld() {
        override fun generateChunk(chunkX: Int, chunkY: Int, chunkZ: Int, chunk: ByteArray) {
            super.generateChunk(chunkX, chunkY, chunkZ, chunk)
            saveSystem.get(Vector3i(chunkX, chunkY, chunkZ), false) { data ->
                for ((k, v) in data) {
                    val index = getIndex(k.x, k.y, k.z)
                    if (index in chunk.indices) {
                        chunk[index] = v
                    } else LOGGER.warn("Out of bounds: $k/$v")
                }
            }
        }
    }
    world.timeoutMillis = 250

    val material = defaultMaterial
    val chunkRenderer = ChunkRenderer(material, world)
    val chunkLoader = ChunkLoader(chunkRenderer, world)
    val scene = Entity("Scene")
    scene.add(chunkRenderer)
    scene.add(chunkLoader)

    val sun = DirectionalLight()
    sun.shadowMapCascades = 3
    sun.autoUpdate = 0
    val sunEntity = Entity("Sun")
        .setScale(100.0)
    sunEntity.add(object : Component(), Updatable {
        // move shadows with player
        var ctr = 0
        override fun update(instances: Collection<Component>) {
            val rv = RenderView.currentInstance
            if (rv != null && (ctr++ % 64) == 0) {
                sunEntity.transform.localPosition =
                    sunEntity.transform.localPosition
                        .set(rv.orbitCenter)
                        .apply { y = world.sizeY * 0.5 }
                        .round()
                sunEntity.transform.teleportUpdate()
                sunEntity.validateTransform()
                sun.needsUpdate1 = true
            }
        }
    })
    sunEntity.add(sun)
    val sky = Skybox()
    sky.applyOntoSun(sunEntity, sun, 50f)
    scene.add(sky)
    scene.add(sunEntity)

    testUI3("Unique Mesh Renderer") {
        val sceneUI = testScene(scene) {
            it.editControls = CreativeControls(
                it.renderer, scene, world,
                saveSystem, chunkLoader
            )
        }

        val container = NineTilePanel(style)
        container.add(sceneUI)
        val inventoryUI = PanelListX(style)
        inventoryUI.alignmentX = AxisAlignment.CENTER
        inventoryUI.alignmentY = AxisAlignment.MAX

        for ((idx, slot) in inventory.slots.withIndex()) {
            slot.type = previewBlockIds.getOrNull(idx) ?: break
            slot.count = 10
        }

        for ((index, slot) in inventory.slots.withIndex()) {
            val panel = ItemPanel(slot)
            panel.setTooltip(TestWorld.blockNames[slot.type]?.name)
            if (index.hasFlag(1)) {
                panel.backgroundColor = mixARGB(panel.backgroundColor, white, 0.05f)
            }
            inventoryUI.add(panel)
        }
        container.add(inventoryUI)
        container
    }
}

