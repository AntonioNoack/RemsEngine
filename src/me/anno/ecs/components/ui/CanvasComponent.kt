package me.anno.ecs.components.ui

import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Order
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.interfaces.ControlReceiver
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Texture2D
import me.anno.image.raw.GPUImage
import me.anno.input.Key
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.io.zip.InnerTmpFile
import me.anno.ui.Panel
import me.anno.ui.Window
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.WindowStack
import me.anno.utils.pooling.JomlPools
import org.joml.Matrix4d

// done make ui elements Entities?
// done make them something special?
// done just make it so you can add components to panels? that would be a nice solution :),
// done then the parent will be a UI element, not an Entity


// todo focus / unfocus
// todo interactions with that UI
// todo enter ui, exit ui

// todo just set inFocus, then input works magically

// done how can we automatically adjust the viewport to the camera?
// done how can we disable drawing depth and such, if camera space?

// todo remove subpixel rendering in world space here

class CanvasComponent() : MeshComponentBase(), ControlReceiver {

    // todo this element always need glCullFace, or you see the back when it's transparent

    constructor(base: CanvasComponent) : this() {
        base.copyInto(this)
    }

    // this is a trick to continue the hierarchy using panels
    override fun listChildTypes(): String = "p"
    override fun getChildListNiceName(type: Char): String = "Panel"
    override fun getChildListByType(type: Char) = windowStack.map { it.panel }
    override fun getOptionsByType(type: Char) = PanelGroup.getPanelOptions(null)
    override fun addChildByType(index: Int, type: Char, child: PrefabSaveable) {
        addChild(child)
    }

    override fun addChild(child: PrefabSaveable) {
        if (child !is Panel) return
        child.parent = this
        windowStack.push(child)
    }

    override fun removeChild(child: PrefabSaveable) {
        super.removeChild(child)
        windowStack.removeAll {
            if (it.panel === child) {
                it.panel.parent = null
                true
            } else false
        }
    }

    // different spaces like in Unity: world space, camera space
    // todo allow custom meshes (?)
    enum class Space {
        WORLD_SPACE,
        CAMERA_SPACE
    }

    var space = Space.CAMERA_SPACE
        set(value) {
            if (field != value) {
                field = value
                val p = internalMesh.positions
                if (p != null) {
                    val pt = positionTemplate
                    for (i in pt.indices) {
                        p[i] = pt[i]
                    }
                }
            }
        }

    var style = DefaultConfig.style

    @NotSerializedProperty
    private val windowStack = WindowStack()

    @Group("Dimensions")
    @Range(1.0, 4096.0)
    @Order(-2)
    var width = 540

    @Group("Dimensions")
    @Range(1.0, 4096.0)
    @Order(-1)
    var height = 360

    @NotSerializedProperty
    var framebuffer: Framebuffer? = null

    @SerializedProperty
    var isTransparent: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                for (it in windowStack) {
                    it.isTransparent = value
                }
            }
        }

    @NotSerializedProperty
    var lastPointer: Int = -1

    @NotSerializedProperty
    val internalMesh = Mesh()

    init {
        // define shape
        val pt = positionTemplate
        internalMesh.positions = FloatArray(pt.size) { pt[it] }
        internalMesh.uvs = uvs
        internalMesh.indices = indices
    }

    override fun getMeshOrNull() = internalMesh

    private fun defineMesh() {
        val aspectRatio = width.toFloat() / height.toFloat()
        val pos = internalMesh.positions!!
        if (pos[0] != aspectRatio) {
            pos[0] = -aspectRatio; pos[3] = -aspectRatio
            pos[6] = +aspectRatio; pos[9] = +aspectRatio
            pos[12] = -aspectRatio; pos[15] = -aspectRatio
            pos[18] = +aspectRatio; pos[21] = +aspectRatio
            internalMesh.invalidateGeometry()
        }
    }

    override fun onUpdate(): Int {
        if (space == Space.WORLD_SPACE ||
            RenderView.currentInstance?.playMode == PlayMode.EDITING
        ) {
            defineMesh()
            render()
        }
        return 1
    }

    @DebugAction
    fun requestFocus() {
        windowStack[0].panel.requestFocus()
    }

    var material: Material? = null

    fun render() {
        GFX.checkIsGFXThread()
        GFX.check()
        var fb = framebuffer
        val width = width
        val height = height
        if (width < 1 || height < 1) return
        if (fb == null || fb.pointer != lastPointer) {
            fb = Framebuffer("canvas", width, height, 1, 1, false, DepthBufferType.NONE)
            useFrame(fb) {} // create textures
            (fb.getTexture0() as Texture2D).clamping = Clamping.CLAMP
            lastPointer = fb.pointer
            val prefab = Prefab("Material")
            val texture = fb.getTexture0()
            val image = GPUImage(texture, 4, true, hasOwnership = false)
            val texturePath = InnerTmpFile.InnerTmpImageFile(image)
            val materialPath = InnerTmpFile.InnerTmpPrefabFile(prefab)
            prefab.setProperty("diffuseMap", texturePath)
            prefab.setProperty("emissiveMap", texturePath)
            val materialInstance = prefab.getSampleInstance() as Material
            this.material = materialInstance
            framebuffer = fb
            internalMesh.material = materialPath
        }
        useFrame(width, height, true, fb) {
            fb.clearColor(0, true)
            render(width, height)
        }
        GFX.check()
    }

    fun render(width: Int, height: Int) {
        GFXState.depthMode.use(DepthMode.ALWAYS) {
            GFXState.blendMode.use(BlendMode.DEFAULT) {
                GFXState.cullMode.use(CullMode.BOTH) {
                    val rv = RenderView.currentInstance!!
                    val transform = JomlPools.mat4f.create()
                    if (space == Space.WORLD_SPACE) {
                        // I believe this should be correct: screen space = camera transform * world transform * world pos
                        transform.set(Matrix4d(RenderState.cameraMatrix).mul(entity!!.transform.globalTransform))
                        transform.invert()
                    }
                    val window = GFX.activeWindow!!
                    windowStack.updateTransform(window, transform, rv.x, rv.y, rv.width, rv.height, 0, 0, width, height)
                    windowStack.draw(0, 0, width, height, true, forceRedraw = true)
                    JomlPools.mat4f.sub(1)
                }
            }
        }
    }

    override fun clone() = CanvasComponent(this)

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as CanvasComponent
        dst.space = space
        dst.width = width
        dst.height = height
        dst.style = style
        dst.windowStack.clear()
        dst.windowStack.addAll(windowStack.map {
            val newPanel = it.panel.clone()
            newPanel.parent = dst
            Window(newPanel, isTransparent, it.isFullscreen, dst.windowStack, it.x, it.y)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        framebuffer?.destroy()
        framebuffer = null
        internalMesh.destroy()
        windowStack.destroy()
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectList(this, "panels", windowStack.map { it.panel })
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "panels" -> {
                windowStack.clear()
                windowStack.addAll(
                    values.filterIsInstance<Panel>()
                        .map {
                            it.parent = this
                            Window(it, isTransparent, windowStack, it.x, it.y)
                        }
                )
            }
            else -> super.readObjectArray(name, values)
        }
    }

    private inline fun findPanel(callback: (panel: Panel, x: Float, y: Float) -> Unit): Boolean {
        val x = windowStack.mouseX
        val y = windowStack.mouseY
        val xi = x.toInt()
        val yi = y.toInt()
        for (window in windowStack.reversed()) {
            val panel = window.panel.getPanelAt(xi, yi)
            if (panel != null) {
                callback(panel, x, y)
                return true
            }
        }
        return false
    }

    // todo do this like in ActionManager

    override fun onKeyDown(key: Key): Boolean {
        return findPanel { panel, x, y -> panel.onKeyDown(x, y, key) }
    }

    override fun onKeyUp(key: Key): Boolean {
        return findPanel { panel, x, y -> panel.onKeyUp(x, y, key) }
    }

    override fun onKeyTyped(key: Key): Boolean {
        return findPanel { panel, x, y -> panel.onKeyTyped(x, y, key) }
    }

    override fun onMouseClicked(button: Key, long: Boolean): Boolean {
        return findPanel { panel, x, y -> panel.onMouseClicked(x, y, button, long) }
    }

    override val className: String get() = "CanvasComponent"

    companion object {
        // a small z value against z-fighting
        private const val z = .001f
        val positionTemplate = floatArrayOf(
            // front
            -1f, -1f, +z, -1f, +1f, +z,
            +1f, -1f, +z, +1f, +1f, +z,
            // back
            -1f, -1f, -z, -1f, +1f, -z,
            +1f, -1f, -z, +1f, +1f, -z,
        )
        val uvs = floatArrayOf(
            // front
            0f, 1f, 0f, 0f,
            1f, 1f, 1f, 0f,
            // back
            1f, 1f, 1f, 0f,
            0f, 1f, 0f, 0f
        )
        val indices = intArrayOf(
            // front
            0, 2, 1, 1, 2, 3,
            // back
            5, 6, 4, 6, 5, 7
        )
    }
}