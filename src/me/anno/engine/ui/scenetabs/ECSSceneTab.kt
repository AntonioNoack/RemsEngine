package me.anno.engine.ui.scenetabs

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.light.LightComponentBase
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.scenetabs.ECSSceneTabs.findName
import me.anno.gpu.Cursor
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.io.files.FileReference
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.length
import me.anno.maths.Maths.mixARGB
import me.anno.mesh.assimp.AnimGameItem
import me.anno.studio.StudioBase
import me.anno.ui.Window
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.dragging.Draggable
import me.anno.utils.pooling.JomlPools
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Vector3d

// todo darken panels when in play-testing mode

class ECSSceneTab(
    val inspector: PrefabInspector,
    val file: FileReference,
    val playMode: PlayMode,
    name: String = file.nameWithoutExtension
) : TextPanel(name, DefaultConfig.style) {

    constructor(
        file: FileReference,
        classNameIfNull: String,
        playMode: PlayMode,
        name: String = file.nameWithoutExtension
    ) : this(PrefabInspector(file, classNameIfNull), file, playMode, name)

    constructor(
        file: FileReference, playMode: PlayMode,
        name: String = file.nameWithoutExtension
    ) : this(PrefabInspector(file), file, playMode, name)

    init {
        LOGGER.info("Created tab with ${inspector.prefab.countTotalChanges(true)}+ changes")
        padding.set(6, 2, 6, 2)
    }

    // different tabs have different "cameras"
    var radius = 50.0
    var position = Vector3d()
    var rotation = Vector3d(-20.0, 0.0, 0.0)

    var isFirstTime = true

    override fun getCursor() = Cursor.hand

    private fun resetCamera(root: PrefabSaveable) {
        rotation.set(-20.0, 0.0, 0.0)
        when (root) {
            is MeshComponentBase -> {
                root.ensureBuffer()
                resetCamera2(root.getMesh() ?: return)
            }
            is Mesh -> resetCamera2(root)
            is Material, is LightComponentBase -> {
                radius = 2.0
            }
            is Entity -> {
                root.validateTransform()
                root.validateAABBs()
                resetCamera(root.aabb, true)
            }
            is Collider -> {
                val aabb = JomlPools.aabbd.create().clear()
                val mat = JomlPools.mat4x3d.create().identity()
                val vec = JomlPools.vec3d.create()
                root.union(mat, aabb, vec, false)
                resetCamera(aabb, false)
                JomlPools.mat4x3d.sub(1)
                JomlPools.vec3d.sub(1)
                JomlPools.aabbd.sub(1)
            }
            is Animation -> {
                val skeleton = SkeletonCache[root.skeleton] ?: return
                val aabb = skeletalBounds(skeleton)
                // widen bounds by motion
                val motionBounds = AABBf()
                for (i in 0 until root.numFrames) {
                    val matrices = root.getMatrices(0, AnimGameItem.tmpMatrices) ?: break
                    for (j in skeleton.bones.indices) {
                        val offset = matrices[j]
                        motionBounds.union(offset.m30(), offset.m31(), offset.m32())
                    }
                }
                aabb.minX += motionBounds.minX
                aabb.minY += motionBounds.minY
                aabb.minZ += motionBounds.minZ
                aabb.maxX += motionBounds.maxX
                aabb.maxY += motionBounds.maxY
                aabb.maxZ += motionBounds.maxZ
                resetCamera(aabb, true)
            }
            is Skeleton -> {
                // find still bounds
                resetCamera(skeletalBounds(root), true)
            }
            else -> LOGGER.warn("Please implement bounds for ${root.className}")
        }
    }

    private fun skeletalBounds(skeleton: Skeleton): AABBf {
        val aabb = AABBf()
        for (bone in skeleton.bones) {
            aabb.union(bone.bindPosition)
        }
        return aabb
    }

    private fun resetCamera2(mesh: Mesh) {
        resetCamera(mesh.aabb, true)
    }

    private fun resetCamera(aabb: AABBf, translate: Boolean) {
        if (aabb.avgX().isFinite() && aabb.avgY().isFinite() && aabb.avgZ().isFinite()) {
            if (translate) position.set(aabb.avgX().toDouble(), aabb.avgY().toDouble(), aabb.avgZ().toDouble())
            radius = length(aabb.deltaX(), aabb.deltaY(), aabb.deltaZ()).toDouble()
        }
    }

    private fun resetCamera(aabb: AABBd, translate: Boolean) {
        if (aabb.avgX().isFinite() && aabb.avgY().isFinite() && aabb.avgZ().isFinite()) {
            if (translate) position.set(aabb.avgX(), aabb.avgY(), aabb.avgZ())
            radius = length(aabb.deltaX(), aabb.deltaY(), aabb.deltaZ())
        }
    }

    var needsStart = false

    fun onStart() {

        // todo when first created, center around scene, and adjust the radius

        if (isFirstTime) {
            val root = inspector.root
            isFirstTime = false
            resetCamera(root)
        }

        for (window in window?.windowStack ?: emptyList()) {
            window.panel.forAll {
                if (it is RenderView) {
                    it.radius = radius
                    it.position.set(position)
                    it.rotation.set(rotation)
                }
            }
        }

    }

    fun onStop() {
        try {
            for (window in windowStack) {
                window.panel.forAll {
                    if (it is RenderView) {
                        radius = it.radius
                        position.set(it.position)
                        rotation.set(it.rotation)
                        // early exit
                        throw RuntimeException()
                    }
                }
            }
        } catch (_: RuntimeException) {
        }
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        when {
            button.isLeft -> ECSSceneTabs.open(this)
            button.isRight -> {
                openMenu(windowStack, listOf(
                    MenuOption(NameDesc(if (playMode == PlayMode.EDITING) "Play" else "Edit")) {
                        play()
                    },
                    MenuOption(NameDesc("Play Fullscreen")) {
                        playFullscreen()
                    },
                    MenuOption(NameDesc("Copy Path")) {
                        Input.setClipboardContent(onCopyRequested(0f, 0f).toString())
                    },
                    MenuOption(NameDesc("Close")) {
                        ECSSceneTabs.close(this)
                    }
                ))
            }
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    fun play() {
        val tab = ECSSceneTabs.currentTab!!
        val playMode = if (playMode == PlayMode.EDITING) PlayMode.PLAY_TESTING else PlayMode.EDITING
        ECSSceneTabs.open(ECSSceneTab(tab.inspector, tab.file, playMode, findName(tab.file)))
    }

    fun playFullscreen() {
        // opens window with fullscreen attribute
        val style = style
        val panel = PanelListY(style)
        panel.add(SceneView(EditorState, PlayMode.PLAY_TESTING, style).apply { weight = 1f })
        panel.add(ConsoleOutputPanel.createConsoleWithStats(true, style))
        val window = object : Window(panel, false, windowStack) {
            override fun destroy() {
                super.destroy()
                // reset state
                inspector.prefab.invalidateInstance()
            }
        }
        windowStack.push(window)
    }

    override fun tickUpdate() {
        super.tickUpdate()
        backgroundColor = when {
            ECSSceneTabs.currentTab == this -> 0xff777777.toInt()
            else -> mixARGB(originalBGColor, black, 0.2f)
        }
        if (ECSSceneTabs.currentTab == this && needsStart) {
            needsStart = false
            onStart()
        }
    }

    override fun onCopyRequested(x: Float, y: Float) = file

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        return when (action) {
            "DragStart" -> {
                val title = file.nameWithoutExtension
                val stringContent = file.absolutePath
                StudioBase.dragged = Draggable(stringContent, "File", file, title, style)
                true
            }
            else -> super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
    }

    override fun getTooltipText(x: Float, y: Float) = file.absolutePath

    fun save() {
        inspector.save()
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ECSSceneTab::class)
    }

}