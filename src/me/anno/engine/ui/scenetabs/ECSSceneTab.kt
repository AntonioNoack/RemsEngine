package me.anno.engine.ui.scenetabs

import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.anim.SkeletonCache
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.CollidingComponent
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
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.dragging.Draggable
import me.anno.utils.Color.black
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Quaterniond
import org.joml.Vector3d

// todo darken panels when in play-testing mode

class ECSSceneTab(
    val inspector: PrefabInspector,
    val file: FileReference,
    val playMode: PlayMode,
    name: String = findName(file)
) : TextPanel(name, DefaultConfig.style) {

    constructor(
        file: FileReference,
        playMode: PlayMode,
        name: String = findName(file)
    ) : this(PrefabInspector(file), file, playMode, name)

    init {
        // LOGGER.info("Created tab with ${inspector.prefab.countTotalChanges(true)}+ changes")
        padding.set(6, 2, 6, 2)
    }

    // different tabs have different "cameras"
    var radius = 50.0
    var position = Vector3d()
    var rotation = Quaterniond()
        .rotateX(20.0.toRadians())

    var isFirstTime = true

    override fun getCursor() = Cursor.hand

    private fun resetCamera(root: PrefabSaveable) {
        rotation.identity()
            .rotateX(20.0.toRadians())
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
            is Animation -> {
                val skeleton = SkeletonCache[root.skeleton] ?: return
                val aabb = skeletalBounds(skeleton)
                // widen bounds by motion
                val motionBounds = AABBf()
                for (i in 0 until root.numFrames) {
                    val matrices = root.getMatrices(0, AnimGameItem.tmpMatrices) ?: break
                    for (j in skeleton.bones.indices) {
                        val offset = matrices[j]
                        motionBounds.union(offset.m30, offset.m31, offset.m32)
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
            is CollidingComponent -> {
                val aabb = JomlPools.aabbd.create().clear()
                val mat = JomlPools.mat4x3d.create().identity()
                root.fillSpace(mat, aabb)
                resetCamera(aabb, true)
                JomlPools.mat4x3d.sub(1)
                JomlPools.aabbd.sub(1)
            }
            is Skeleton -> {
                // find still bounds
                resetCamera(skeletalBounds(root), true)
            }
            else -> LOGGER.warn("Please implement bounds for class ${root.className}")
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
        if (aabb.centerX.isFinite() && aabb.centerY.isFinite() && aabb.centerZ.isFinite()) {
            if (translate) position.set(aabb.centerX.toDouble(), aabb.centerY.toDouble(), aabb.centerZ.toDouble())
            radius = length(aabb.deltaX, aabb.deltaY, aabb.deltaZ).toDouble()
        }
    }

    private fun resetCamera(aabb: AABBd, translate: Boolean) {
        if (aabb.centerX.isFinite() && aabb.centerY.isFinite() && aabb.centerZ.isFinite()) {
            if (translate) position.set(aabb.centerX, aabb.centerY, aabb.centerZ)
            radius = length(aabb.deltaX, aabb.deltaY, aabb.deltaZ)
        }
    }

    var needsStart = true

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
                    val radius = radius
                    it.radius = radius
                    it.near = 1e-3 * radius
                    it.far = 1e10 * radius
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
            button.isLeft -> {
                try {
                    ECSSceneTabs.open(this, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Menu.msg(windowStack, NameDesc(e.toString()))
                }
            }
            button.isRight -> {
                openMenu(windowStack, listOf(
                    MenuOption(NameDesc(if (playMode == PlayMode.EDITING) "Play" else "Edit")) { play() },
                    MenuOption(NameDesc("Play Fullscreen")) { playFullscreen() },
                    MenuOption(NameDesc("Copy Path")) { Input.setClipboardContent(file.absolutePath) },
                    MenuOption(NameDesc("Copy Name")) { Input.setClipboardContent(file.name) },
                    MenuOption(NameDesc("Close")) { ECSSceneTabs.close(this, true) },
                    MenuOption(NameDesc("Close All")) {
                        val tabs = ECSSceneTabs.children3.reversed()
                        val lastTab = tabs.last()
                        for (tab in tabs) ECSSceneTabs.close(tab, lastTab == tab)
                    }
                ))
            }
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    fun play() {
        val tab = ECSSceneTabs.currentTab!!
        val playMode = if (playMode == PlayMode.EDITING) PlayMode.PLAY_TESTING else PlayMode.EDITING
        ECSSceneTabs.open(ECSSceneTab(tab.inspector, tab.file, playMode, findName(tab.file)), true)
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

    override fun onUpdate() {
        super.onUpdate()
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