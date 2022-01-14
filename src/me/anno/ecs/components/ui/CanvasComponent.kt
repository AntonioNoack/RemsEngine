package me.anno.ecs.components.ui

import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Order
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshBaseComponent
import me.anno.ecs.interfaces.ControlReceiver
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.Window
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.image.raw.GPUImage
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.zip.InnerTmpFile
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.utils.WindowStack
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.*

// todo make ui elements Entities?
// todo make them something special?
// todo just make it so you can add components to panels? that would be a nice solution :),
// todo then the parent will be a UI element, not an Entity


// todo focus / unfocus
// todo interactions with that UI
// todo enter ui, exit ui

// todo how can we automatically adjust the viewport to the camera?
// todo how can we disable drawing depth and such, if camera space?

class CanvasComponent() : MeshBaseComponent(), ControlReceiver {

    constructor(base: CanvasComponent) : this() {
        base.copy(this)
    }

    // this is a trick to continue the hierarchy using panels
    override fun listChildTypes(): String = "p"
    override fun getChildListNiceName(type: Char): String = "Panel"
    override fun getChildListByType(type: Char) = windowStack.map { it.panel }
    override fun getOptionsByType(type: Char) = PanelGroup.getPanelOptions()
    override fun addChildByType(index: Int, type: Char, child: PrefabSaveable) {
        add(child)
    }

    override fun add(child: PrefabSaveable) {
        if (child !is Panel) return
        child.prefabPath = prefabPath!! + Triple(child.name, 0, 'p')
        windowStack.push(child)
    }

    override fun removeChild(child: PrefabSaveable) {
        super.removeChild(child)
        windowStack.removeAll { it.panel === child }
    }

    // different spaces like in Unity: world space, camera space
    // todo best render camera space separately
    // todo allow custom meshes (?)
    enum class Space {
        WORLD_SPACE,
        CAMERA_SPACE
    }

    var space = Space.CAMERA_SPACE

    var style = DefaultConfig.style

    @NotSerializedProperty
    private val windowStack = WindowStack()

    @Group("Dimensions")
    @Range(0.0, 4096.0)
    @Order(-2)
    var width = 540

    @Group("Dimensions")
    @Range(0.0, 4096.0)
    @Order(-1)
    var height = 360

    @NotSerializedProperty
    var framebuffer: Framebuffer? = null

    @NotSerializedProperty
    val internalMesh = Mesh()

    init {
        // define shape
        internalMesh.positions = floatArrayOf(
            -1f, -1f, 0f, -1f, +1f, 0f,
            +1f, -1f, 0f, +1f, +1f, 0f
        )
        internalMesh.indices = intArrayOf(
            0, 1, 2, 1, 2, 3,
            1, 0, 2, 2, 1, 3
        )
    }

    override fun getMesh() = internalMesh

    private fun defineMesh() {
        val pos = internalMesh.positions!!
        val x = width.toFloat() / height.toFloat()
        val oldX = pos[0]
        if (x != oldX) {
            pos[0] = -x; pos[3] = -x
            pos[6] = +x; pos[9] = +x
            internalMesh.invalidateGeometry()
        }
    }

    override fun onVisibleUpdate(): Boolean {
        defineMesh()
        render()
        return true
    }

    // todo just set inFocus, then input works magically

    @DebugAction
    fun requestFocus() {
        windowStack[0].panel.requestFocus()
    }

    fun render() {
        GFX.checkIsGFXThread()
        GFX.check()
        var fb = framebuffer
        val width = width
        val height = height
        if (fb == null) {
            fb = Framebuffer("canvas", width, height, 1, 1, false, DepthBufferType.NONE)
            useFrame(fb) {} // create textures
            val prefab = Prefab("Material")
            val image = GPUImage(fb.getColor0(), 4, true, hasOwnership = false)
            val texturePath = InnerTmpFile.InnerTmpImageFile(image)
            val materialPath = InnerTmpFile.InnerTmpPrefabFile(prefab)
            prefab.setProperty("diffuseMap", texturePath)
            framebuffer = fb
            internalMesh.material = materialPath
        }
        val rv = RenderView.currentInstance
        val transform = Matrix4f(Matrix4d(RenderView.cameraMatrix).mul(entity!!.transform.globalTransform))
            .invert() // I believe this should be correct: screen space = camera transform * world transform * world pos
        windowStack.updateTransform(transform, rv.x, rv.y, rv.w, rv.h, 0, 0, width, height)
        useFrame(width, height, true, fb) {
            Frame.bind()
            glClearColor(0f, 0f, 0f, 1f)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
            OpenGL.depthMode.use(DepthMode.ALWAYS) {
                OpenGL.blendMode.use(BlendMode.DEFAULT) {
                    OpenGL.cullMode.use(0) {
                        windowStack.draw(width, height, false, forceRedraw = true, fb)
                    }
                }
            }
        }
        GFX.check()
    }

    override fun clone() = CanvasComponent(this)

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as CanvasComponent
        clone.space = space
        clone.width = width
        clone.height = height
        clone.style = style
        clone.windowStack.clear()
        clone.windowStack.addAll(windowStack.map { Window(it.panel, it.isFullscreen, clone.windowStack, it.x, it.y) })
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
                windowStack.addAll(values.filterIsInstance<Panel>().map { Window(it, windowStack, it.x, it.y) })
            }
            else -> super.readObjectArray(name, values)
        }
    }

    override val className get() = "CanvasComponent"

}