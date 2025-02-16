package me.anno.engine.ui.scenetabs

import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.anim.BoneData
import me.anno.ecs.components.anim.Retargeting
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.anim.SkeletonCache
import me.anno.ecs.components.collider.CollidingComponent
import me.anno.ecs.components.light.LightComponentBase
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.EngineBase
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.scenetabs.ECSSceneTabs.findName
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.gpu.OSWindow
import me.anno.graph.visual.Graph
import me.anno.image.thumbs.AssetThumbnails.getBoundsForRendering
import me.anno.input.Clipboard.setClipboardContent
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.io.files.inner.temporary.InnerTmpFile
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.length
import me.anno.ui.Style
import me.anno.ui.Window
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.files.FileExplorerOptions.copyNameDesc
import me.anno.ui.editor.files.FileExplorerOptions.copyPathDesc
import me.anno.utils.Color.black
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.white
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.toDegrees
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d

class ECSSceneTab(
    val inspector: PrefabInspector,
    val playMode: PlayMode,
    name: String = findName(inspector.reference)
) : TextPanel(name, DefaultConfig.style) {

    constructor(
        file: FileReference,
        playMode: PlayMode,
        name: String = findName(file)
    ) : this(PrefabInspector(file), playMode, name)

    init {
        padding.set(6, 2, 6, 2)
    }

    val file get() = inspector.reference

    // different tabs have different "cameras"
    var radius = 10f
    val position = Vector3d()
    val rotation = Quaternionf(defaultRotation)

    var isFirstTime = true

    override fun getCursor() = Cursor.hand

    private fun resetCamera(root: PrefabSaveable) {
        rotation.set(defaultRotation)
        when (root) {
            is MeshComponentBase -> {
                resetCamera2(root.getMesh() ?: return)
            }
            is Mesh -> resetCamera2(root)
            is Material, is LightComponentBase -> {
                radius = 2f
            }
            is Entity -> {
                root.validateTransform()
                val bounds = getBoundsForRendering(root)
                resetCamera(bounds, true)
            }
            is Animation -> {
                val skeleton = SkeletonCache[root.skeleton] ?: return
                val aabb = skeletalBounds(skeleton)
                // widen bounds by motion
                val motionBounds = AABBf()
                for (i in 0 until root.numFrames) {
                    val matrices = root.getMatrices(0, BoneData.tmpMatrices) ?: break
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
                val mat = JomlPools.mat4x3m.create().identity()
                root.fillSpace(mat, aabb)
                resetCamera(aabb, true)
                JomlPools.mat4x3m.sub(1)
                JomlPools.aabbd.sub(1)
            }
            is Skeleton -> {
                // find still bounds
                resetCamera(skeletalBounds(root), true)
            }
            is Retargeting -> {
                val bounds = AABBd()
                val srcSkeleton = SkeletonCache[root.srcSkeleton]
                val dstSkeleton = SkeletonCache[root.dstSkeleton]
                if (srcSkeleton != null) bounds.set(skeletalBounds(srcSkeleton))
                if (dstSkeleton != null) bounds.union(skeletalBounds(dstSkeleton))
                resetCamera(bounds, true)
            }
            is Graph -> {} // idc
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

    private fun resetCamera2(mesh: IMesh) {
        resetCamera(mesh.getBounds(), true)
    }

    private fun resetCamera(aabb: AABBf, translate: Boolean) {
        resetCamera(AABBd(aabb), translate)
    }

    private fun resetCamera(aabb: AABBd, translate: Boolean) {
        if (aabb.centerX.isFinite() && aabb.centerY.isFinite() && aabb.centerZ.isFinite()) {
            if (translate) position.set(aabb.centerX, aabb.centerY, aabb.centerZ)
            radius = length(aabb.deltaX, aabb.deltaY, aabb.deltaZ).toFloat()
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

        for (window in windowStack) {
            window.panel.forAll { panel ->
                if (panel is RenderView) {
                    applyRadius(panel)
                }
            }
        }
    }

    fun applyRadius(panel: RenderView) {
        val fov = panel.controlScheme?.settings?.fovY ?: 90f
        val radius = radius * 90f / fov
        panel.radius = radius
        panel.near = 1e-3f * radius
        panel.far = 1e10f * radius
        panel.orbitCenter.set(position)
        panel.orbitRotation.set(rotation)
        val cs = panel.controlScheme
        if (cs != null) {
            rotation
                .getEulerAnglesYXZ(cs.rotationTargetDegrees)
                .mul(1f.toDegrees())
        }
    }

    fun onStop() {
        for (window in windowStack) {
            window.panel.forAll { panel ->
                if (panel is RenderView) {
                    radius = panel.radius
                    position.set(panel.orbitCenter)
                    rotation.set(panel.orbitRotation)
                }
            }
        }
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        when (button) {
            Key.BUTTON_LEFT -> {
                PrefabCache.getPrefabAsync(file) { _, err ->
                    err?.printStackTrace()
                    try {
                        ECSSceneTabs.open(this, true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Menu.msg(windowStack, NameDesc(e.toString()))
                    }
                }
            }
            Key.BUTTON_RIGHT -> {
                openMenu(
                    windowStack, listOf(
                        MenuOption(NameDesc(if (playMode == PlayMode.EDITING) "Play" else "Edit")) { play() },
                        MenuOption(NameDesc("Play Fullscreen")) { playFullscreen() },
                        MenuOption(copyPathDesc) { setClipboardContent(file.absolutePath) },
                        MenuOption(copyNameDesc) { setClipboardContent(file.name) },
                        MenuOption(NameDesc("Save"), ::save),
                        MenuOption(NameDesc("Clear History")) {
                            ECSSceneTabs.open(this, true)
                            val history = inspector.prefab.history
                            if (history != null) {
                                val oldSize = history.numStates
                                history.clearToSize(1)
                                LogManager.enableLogger(LOGGER) // the user will want to see this
                                LOGGER.info("Removed ${oldSize - 1} items")
                            }
                        },
                        MenuOption(NameDesc("Close")) {
                            ECSSceneTabs.close(this, true)
                        },
                        MenuOption(NameDesc("Close Other Tabs")) {
                            ECSSceneTabs.ecsTabsRaw.clear()
                            ECSSceneTabs.project?.openTabs?.clear()
                            ECSSceneTabs.currentTab = null
                            ECSSceneTabs.open(this, true)
                            ECSSceneTabs.project?.save()
                        }
                    )
                )
            }
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    fun play() {
        val tab = ECSSceneTabs.currentTab!!
        val playMode = if (playMode == PlayMode.EDITING) PlayMode.PLAY_TESTING else PlayMode.EDITING
        ECSSceneTabs.open(ECSSceneTab(tab.inspector, playMode, findName(tab.file)), true)
    }

    fun playFullscreen() {
        // opens window with fullscreen attribute
        val style = style
        val panel = PanelListY(style)
        panel.add(SceneView(PlayMode.PLAY_TESTING, style).apply { weight = 1f })
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
        inspector.update()
        backgroundColor = when {
            ECSSceneTabs.currentTab == this -> mixARGB(originalBGColor, white, 0.2f)
            ECSSceneTabs.currentTab?.playMode == PlayMode.PLAY_TESTING -> mixARGB(originalBGColor, black, 0.1f)
            else -> mixARGB(originalBGColor, black, 0.2f)
        }
        if (ECSSceneTabs.currentTab == this && needsStart) {
            needsStart = false
            onStart()
        }
        if (ECSSceneTabs.currentTab == this) {
            val prefab = PrefabCache[file, true]
            val needsStar = prefab != null &&
                    prefab.wasModified && prefab.isWritable &&
                    prefab.source !is InnerTmpFile
            val hasStar = text.endsWith("*")
            if (needsStar != hasStar) {
                val name = findName(file)
                text = if (needsStar) "$name*" else name
                invalidateDrawing()
            }
        }
    }

    override fun onCopyRequested(x: Float, y: Float) = file

    class ECSTabDraggable(file: FileReference, val playMode: PlayMode, style: Style) :
        Draggable(file.absolutePath, "File", file, file.nameWithoutExtension, style)

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        return when (action) {
            "DragStart" -> {
                EngineBase.dragged = ECSTabDraggable(file, playMode, style)
                true
            }
            else -> super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
    }

    override fun getTooltipText(x: Float, y: Float) = file.absolutePath

    fun save() {
        // todo issue/bug: when we save something,
        //  because the file is replaced/written, and we immediately load it again,
        //  it might be empty at that point in time, and then throw exceptions in all kinds of places
        inspector.save()
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ECSSceneTab::class)
        private val defaultRotation = Quaternionf()
            .rotationYXZ((30f).toRadians(), (-10f).toRadians(), 0f)

        fun tryStartVR(window: OSWindow?, rv: RenderView?) {
            if (GFX.shallRenderVR) {
                LOGGER.warn("Already running VR")
                return
            }
            val rr = GFX.vrRenderingRoutine
            if (rr != null) {
                if (window != null && rv != null) {
                    GFX.shallRenderVR = rr.startSession(window, rv)
                    if (!GFX.shallRenderVR) LOGGER.warn("Failed to initialize VR")
                    else LOGGER.info("Started VR")
                } else LOGGER.warn(if (window == null) "Window is null" else "RenderView is missing")
            } else LOGGER.warn("VR isn't supported")
        }
    }
}