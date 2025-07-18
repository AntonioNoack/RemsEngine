package me.anno.games

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.camera.control.OrbitControls
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel.generateRegularQuadHeightMesh
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.change.Path
import me.anno.ecs.components.FillSpace
import me.anno.engine.OfficialExtensions
import me.anno.engine.WindowRenderFlags
import me.anno.engine.raycast.RayHit
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.pipeline.Pipeline
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.random
import me.anno.tests.LOGGER
import me.anno.ui.Panel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.files.FileNames.toAllowedFilename
import me.anno.ui.utils.ThumbnailPanel
import me.anno.utils.Color.withAlpha
import me.anno.utils.OS.documents
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.UpdatingList
import me.anno.utils.types.Vectors.normalToQuaternionY
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// done:
//  - placing objects
//  - rotating objects
//  - object library to choose from
//  - deleting objects
// todo bug: placed items move when placing other items :/
// todo undo, redo

fun createTestTerrain(): MeshComponent {
    val terrainMesh = Mesh()
    val ts = 16
    generateRegularQuadHeightMesh(
        ts, ts, false,
        20f, terrainMesh, true
    )
    return MeshComponent(terrainMesh.ref)
}

class BuildCategory(val name: String, val items: List<Panel>)

/**
 * This is a sample, where you can comfortably build a scene, save and load it.
 * It may be integrated into the main editor one day.
 * */
fun main() {
    OfficialExtensions.initForTests()
    testUI3("World Builder") {

        WindowRenderFlags.enableVSync = true

        val saveFolder = documents.getChild("RemsEngine/WorldBuilder")
        saveFolder.tryMkdirs()

        val list = CustomList(true, style)

        val world = Entity()
        val player = LocalPlayer()
        LocalPlayer.currentLocalPlayer = player
        LocalPlayer.localPlayers.add(player)

        world.add(createTestTerrain())

        val mainFolder = getReference(
            "E:/Assets/Unity/Polygon/Construction.unitypackage/" +
                    "Assets/PolygonConstruction/Prefabs"
        )

        val buildItems = listOf(
            mainFolder.getChild("Generic"),
            mainFolder.getChild("Environments"),
            mainFolder.getChild("Buildings"),
            mainFolder.getChild("Props"),
        ).map {
            it.name to it.listChildren().filter { file ->
                file.lcExtension != "meta"
            }
        }

        val buildMenu = PanelList2D(false, style)
        val bmWrapper = ScrollPanelX(buildMenu, style)
        bmWrapper.alwaysScroll = true

        val sceneView = SceneView(PlayMode.PLAYING, style)
        val renderView = sceneView.renderView
        renderView.localPlayer = player

        var selected = buildItems.first().second.first()

        val tmpFile = saveFolder.getChild("tmp.json")

        var sampleInstance = Entity()
        var prefab = Prefab("Entity")
        fun refreshSampleInstance() {
            world.remove(sampleInstance)
            sampleInstance = prefab.newInstance() as Entity
            world.add(sampleInstance)
        }

        fun loadSampleInstance() {
            prefab = PrefabCache[tmpFile].waitFor()?.prefab ?: prefab
            refreshSampleInstance()
        }
        loadSampleInstance()

        fun saveTmpFile() {
            tmpFile.writeText(JsonStringWriter.toText(prefab, InvalidRef))
        }

        if (!tmpFile.exists) {
            saveTmpFile()
        }

        fun load(src: FileReference) {
            assertTrue(src.exists)
            tmpFile.writeFile(src) {
                it?.printStackTrace()
                loadSampleInstance()
            }
        }

        fun save(dst: FileReference) {
            dst.writeFile(tmpFile) {
                it?.printStackTrace()
            }
        }

        val buildCategories = buildItems.map { (name, files) ->
            val panels = files.map { file ->
                val itemPanel = ThumbnailPanel(file, style)
                itemPanel.addLeftClickListener {
                    // not working :/
                    // sceneView.requestFocus()
                    selected = file
                }
                itemPanel.minW = 100
                itemPanel.minH = 100
                itemPanel
            }
            BuildCategory(name, panels)
        }

        fun getSaveFiles(): List<FileReference> {
            return saveFolder.listChildren()
                .filter { !it.isDirectory && it.lcExtension == "json" }
        }

        val categories = listOf(
            BuildCategory(
                "Load",
                UpdatingList {
                    getSaveFiles().map { file ->
                        TextButton(NameDesc(file.nameWithoutExtension), style)
                            .addLeftClickListener {
                                load(file)
                            }
                    }
                }
            ),
            BuildCategory("Save", UpdatingList {
                getSaveFiles().map { file ->
                    // load all existing save files, and offer to override
                    TextButton(NameDesc(file.nameWithoutExtension), style)
                        .addLeftClickListener {
                            Menu.ask(it.windowStack, NameDesc("Override ${file.nameWithoutExtension}?")) {
                                save(file)
                            }
                        }
                } + TextButton(NameDesc("New Save"), style)
                    .addLeftClickListener {
                        Menu.askName(
                            it.windowStack,
                            NameDesc("New Save"), "Some Scene",
                            NameDesc("Save"), { name ->
                                when {
                                    // indicate whether the name is acceptable
                                    name.toAllowedFilename() != name -> 0xff0000
                                    saveFolder.getChild("$name.json").exists -> 0xffff00
                                    else -> 0x00ff00
                                }.withAlpha(255)
                            }) { name ->
                            if (name.toAllowedFilename() == name) {
                                save(saveFolder.getChild("$name.json"))
                            } else LOGGER.warn("Invalid name '$name'")
                        }
                    }
            })
        ) + buildCategories

        fun buildMainMenuItems() {
            buildMenu.clear()
            for (category in categories) {
                buildMenu.add(
                    TextButton(NameDesc(category.name), style)
                        .addLeftClickListener {
                            buildMenu.clear()
                            // add back button
                            buildMenu.add(
                                TextButton(NameDesc("Back"), style)
                                    .addLeftClickListener { buildMainMenuItems() })
                            // show all items
                            for (child in category.items) {
                                buildMenu.add(child)
                            }
                        })
            }
        }
        buildMainMenuItems()

        val camera = Camera()
        camera.use()

        val camEntity = Entity()
        val camBase = Entity()

        val controls = object : OrbitControls(), Renderable, FillSpace {
            var file: FileReference = InvalidRef

            var hitEntity: Entity? = null

            var hasValidLocation = false
            var placingRotation = 0f
            var placingScale = 1.0
            val rot = Quaternionf()
            val nor = Vector3f()
            var dynamicAngle = true
            var randomRotation = true

            val placingCenter = Vector3d()
            val placingTransform = Transform()
            override fun fill(pipeline: Pipeline, transform: Transform) {
                val mesh = MeshCache.getEntry(file).waitFor()
                if (mesh != null) pipeline.addMesh(mesh, this, placingTransform)
            }

            override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
                dstUnion.all()
            }

            fun updateSampleInstance() {
                val newFile = selected
                if (newFile != file) {
                    file = newFile
                    if (randomRotation) {
                        // after something has been placed, rotate around y randomly
                        randomizeAngle()
                    }
                }
            }

            fun randomizeAngle() {
                // we need finer rotation control, or need to snap this angle to sth
                placingRotation = TAUf * random().toFloat()
                placingRotation -= placingRotation % (PIf / 32)
            }

            fun raycast() {
                // trace ray onto surface
                val query = renderView.rayQuery()
                val hit = Raycast.raycast(world, query)
                if (hit) {
                    val result = query.result
                    definePlacingTransform(result)
                    findHitEntityForDeleting(result)
                } else {
                    hasValidLocation = false
                    hitEntity = null
                }
            }

            fun definePlacingTransform(result: RayHit) {

                val sample = placingTransform
                val normal = result.geometryNormalWS.safeNormalize()
                val newSample = MeshCache.getEntry(file).value
                if (newSample != null) {
                    val bounds = newSample.getBounds()
                    val placingOnWall = abs(normal.y) <= 0.1f
                    if (placingOnWall) {
                        // if is placing onto a wall,
                        //  use backside, not bottom side
                        // calculate "backside" properly
                        val angle = -placingRotation.toFloat()
                        // todo assume box instead of ellipsoid here... how?
                        placingCenter.set(
                            mix(bounds.minX, bounds.maxX, -sin(angle) * 0.5f + 0.5f),
                            bounds.centerY,
                            mix(bounds.minZ, bounds.maxZ, -cos(angle) * 0.5f + 0.5f)
                        )
                    } else {
                        placingCenter.set(bounds.centerX, bounds.minY, bounds.centerZ)
                    }
                    placingCenter.mul(-placingScale)
                } else placingCenter.set(0.0)

                // todo make dynamic angle a checkbox...
                if (dynamicAngle && abs(normal.y) in 0.1f..0.9f) {
                    nor.set(normal).normalToQuaternionY(rot)
                } else {
                    rot.rotationY(atan2(normal.x, normal.z))
                }

                sample.localRotation = sample.localRotation
                    .set(rot).rotateY(placingRotation)
                sample.localPosition = placingCenter
                    .rotate(sample.localRotation).add(result.positionWS)
                sample.localScale =
                    sample.localScale.set(placingScale)
                // todo why is smoothUpdate not working???
                sample.teleportUpdate()
                hasValidLocation = true
            }

            fun findHitEntityForDeleting(result: RayHit) {
                var hitEntity = result.component?.entity
                while (hitEntity != null && hitEntity.parentEntity?.parentEntity != world) {
                    hitEntity = hitEntity.parentEntity ?: break
                }
                this.hitEntity = hitEntity
            }

            override fun onUpdate() {
                super.onUpdate()
                updateSampleInstance()
                raycast()
            }

            override fun onKeyTyped(key: Key): Boolean {
                when (key) {
                    Key.KEY_KP_ADD -> placingScale *= 1.1
                    Key.KEY_KP_SUBTRACT -> placingScale /= 1.1
                    Key.KEY_KP_MULTIPLY -> dynamicAngle = !dynamicAngle
                    Key.KEY_KP_DIVIDE -> randomRotation = !randomRotation
                    Key.KEY_KP_DECIMAL -> renderView.renderMode =
                        if (renderView.renderMode == RenderMode.DEFAULT)
                            RenderMode.TAA else RenderMode.DEFAULT
                    else -> return super.onKeyTyped(key)
                }
                return true
            }

            override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean): Boolean {
                if (Input.isRightDown) super.onMouseWheel(x, y, dx, dy, byMouse)
                else placingRotation += dy * PIf / 32
                return true
            }

            fun invalidatePrefab() {
                saveTmpFile()
                loadSampleInstance()
            }

            override fun onMouseClicked(button: Key, long: Boolean): Boolean {
                if (hasValidLocation) {
                    if (button == Key.BUTTON_LEFT) {
                        val entity = prefab.add(Path.ROOT_PATH, 'e', "Entity", Path.generateRandomId())
                        prefab[entity, "position"] = placingTransform.localPosition
                        prefab[entity, "rotation"] = placingTransform.localRotation
                        prefab[entity, "scale"] = placingTransform.localScale
                        val meshComp = prefab.add(entity, 'c', "MeshComponent", "Mesh")
                        prefab[meshComp, "meshFile"] = file
                        invalidatePrefab()
                        randomizeAngle()
                    } else if (button == Key.BUTTON_RIGHT) {
                        val path = hitEntity?.prefabPath
                        if (path != null && path != Path.ROOT_PATH) {
                            prefab.remove(path)
                            invalidatePrefab()
                        } else LOGGER.info("No path to delete")
                    }
                }
                return true
            }
        }

        controls.camera = camera
        controls.movementSpeed = 100.0
        controls.needsClickToRotate = true
        controls.rotateRight = true
        controls.position.add(0.0, 10.0, 0.0)

        camEntity.add(camera)
        camBase.add(controls)
        camBase.add(camEntity)

        world.add(camBase)

        EditorState.prefabSource = world.ref
        sceneView.weight = 1f

        list.add(sceneView, 5f)
        list.add(bmWrapper, 1f)

        list
    }
}