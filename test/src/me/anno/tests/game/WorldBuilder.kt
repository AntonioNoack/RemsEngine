package me.anno.tests.game

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.camera.control.OrbitControls
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.terrain.TerrainUtils.generateRegularQuadHeightMesh
import me.anno.ecs.components.player.LocalPlayer
import me.anno.engine.ECSRegistry
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.studio.StudioBase
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.editor.files.FileExplorerEntry
import me.anno.utils.types.Vectors.normalToQuaternion
import me.anno.utils.types.Vectors.normalToQuaternion2
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import kotlin.math.PI

// done:
//  - placing objects
//  - rotating objects
//  - object library to choose from
// todo:
//  - deleting objects
//  - align objects by normal

fun createTestTerrain(): MeshComponent {
    val terrainMesh = Mesh()
    val ts = 16
    generateRegularQuadHeightMesh(
        ts, ts, 0, ts, false,
        20f, terrainMesh,
        { 0f },
        { -1 }
    )
    return MeshComponent(terrainMesh.ref)
}

fun main() {
    testUI3("World Builder") {

        ECSRegistry.init()

        val instance = StudioBase.instance
        if (instance != null) {
            instance.showFPS = false
            instance.enableVSync = true
        }

        val list = PanelListY(style)

        val world = Entity()
        val player = LocalPlayer()
        LocalPlayer.currentLocalPlayer = player
        LocalPlayer.localPlayers.add(player)

        world.add(createTestTerrain())

        val mainFolder = getReference(
            "E:/Assets/Unity/Polygon_Construction_Unity_Package_2017_4.unitypackage/" +
                    "Assets/PolygonConstruction/Prefabs"
        )
        val buildItems = listOf(
            mainFolder.getChild("Generic"),
            mainFolder.getChild("Environments"),
            mainFolder.getChild("Buildings"),
            mainFolder.getChild("Props"),
        ).map { it.listChildren()!! }.flatten()

        val buildMenu = PanelListX(style)
        val bmWrapper = ScrollPanelX(buildMenu, style)

        val sceneView = SceneView(EditorState, PlayMode.PLAYING, style)
        val renderView = sceneView.renderer
        renderView.localPlayer = player

        var selected = buildItems.first()
        for (file in buildItems) {
            val itemPanel = FileExplorerEntry(null, false, file, style)
            itemPanel.addLeftClickListener {
                buildMenu.invalidateDrawing()
                // not working :/
                // sceneView.requestFocus()
                selected = file
            }
            itemPanel.minW = 100
            itemPanel.minH = 100
            itemPanel.showTitle = false
            buildMenu.add(itemPanel)
        }

        val camera = Camera()
        camera.use()

        val camEntity = Entity()
        val camBase = Entity()

        val controls = object : OrbitControls() {
            var file: FileReference = InvalidRef
            var sample: Entity? = null
            var hitEntity: Entity? = null
            var hasValidLocation = false
            var placingRotation = 0.0
            var placingScale = 1.0
            val rot = Quaternionf()
            val nor = Vector3f()
            var dynamicAngle = true
            override fun onUpdate(): Int {

                // create sample instance
                val newFile = selected
                if (newFile != file || sample == null) {
                    val newSample = MeshCache[newFile]
                    if (newSample != null) {
                        sample?.destroy()
                        file = newFile
                        val bounds = newSample.getBounds()
                        val entity = Entity()
                        entity.add(MeshComponent(newFile))
                        entity.position = entity.position
                            .set(
                                -bounds.centerX.toDouble(),
                                -bounds.minY.toDouble(),
                                -bounds.centerZ.toDouble()
                            )
                        val proxy = Entity() // to center the mesh
                        proxy.add(entity)
                        world.add(proxy)
                        world.invalidateOwnAABB()
                        sample = proxy
                    }
                }

                val sample = sample
                if (sample != null) {
                    // trace ray onto surface
                    val pos = camEntity.transform.globalPosition
                    val dir = renderView.getMouseRayDirection()
                    val hit = Raycast.raycast(
                        world, pos, dir, 0.0,
                        0.0, 1e6, -1, -1, setOf(sample),
                    )
                    if (hit != null) {
                        // if ray hits something, place pseudo object there
                        sample.position = sample.position.set(hit.positionWS)
                        // rotation based on normal :3
                        if (dynamicAngle) nor.set(hit.geometryNormalWS).normalToQuaternion2(rot)
                        else rot.identity()
                        sample.rotation = sample.rotation
                            .set(rot)
                            .rotateY(placingRotation)
                        sample.scale = sample.scale.set(placingScale)
                        hasValidLocation = true
                        var hitEntity = hit.component?.entity
                        while (hitEntity != null && hitEntity.parentEntity != world) {
                            hitEntity = hitEntity.parentEntity ?: break
                        }
                        this.hitEntity = hitEntity
                    } else {
                        hasValidLocation = false
                        hitEntity = null
                    }
                }

                super.onUpdate()
                return 1
            }

            override fun onKeyTyped(key: Int): Boolean {
                when (key) {
                    GLFW.GLFW_KEY_KP_ADD -> placingScale *= 1.1
                    GLFW.GLFW_KEY_KP_SUBTRACT -> placingScale /= 1.1
                    GLFW.GLFW_KEY_KP_MULTIPLY -> dynamicAngle = !dynamicAngle
                    else -> return super.onKeyTyped(key)
                }
                return true
            }

            override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean): Boolean {
                if (Input.isRightDown) super.onMouseWheel(x, y, dx, dy, byMouse)
                else placingRotation += dy * PI / 32
                return true
            }

            override fun onMouseClicked(button: MouseButton, long: Boolean): Boolean {
                if (hasValidLocation) {
                    if (button.isLeft) {
                        sample = null // forget this object = place it
                    } else if (button.isRight) {
                        // delete existing object
                        hitEntity?.destroy()
                    }
                }
                return true
            }
        }
        controls.camera = camera
        controls.base = camBase
        controls.movementSpeed = 3f
        controls.needsClickToRotate = true
        controls.rotateRight = true
        controls.position.add(0f, 10f, 0f)

        camEntity.add(camera)
        camEntity.add(controls)
        camBase.add(camEntity)

        world.add(camBase)

        EditorState.prefabSource = world.ref
        sceneView.weight = 1f

        list.add(sceneView)
        list.add(bmWrapper)

        list
    }
}