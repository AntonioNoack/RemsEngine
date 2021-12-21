package me.anno.ecs.components.ui

import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Order
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshBaseComponent
import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.image.raw.GPUImage
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.zip.InnerTmpFile
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.text.TextPanel

// todo make ui elements Entities?
// todo make them something special?
// todo just make it so you can add components to panels? that would be a nice solution :),
// todo then the parent will be a UI element, not an Entity

// todo StudioBase/UIBase instead of Panel? Some UI elements have pop-ups, and we should support that.

// todo we could use an enum to specify the options, but then we won't have new ones available
// todo -> panels need to become PrefabSaveable or similar

class CanvasComponent() : MeshBaseComponent() {

    constructor(base: CanvasComponent) : this() {
        base.copy(this)
    }

    // this is a trick to continue the hierarchy using panels
    override fun listChildTypes(): String = "p"
    override fun getChildListNiceName(type: Char): String = "Panel"
    override fun getChildListByType(type: Char): List<PrefabSaveable> {
        val panel = panel
        return if (panel == null) emptyList() else listOf(panel)
    }

    override fun getOptionsByType(type: Char) = PanelGroup.getPanelOptions()
    override fun addChildByType(index: Int, type: Char, child: PrefabSaveable) {
        add(child)
    }

    override fun add(child: PrefabSaveable) {
        // todo somehow set it...
        if (child === panel) return
        panel = child as? Panel ?: return
        // Hierarchy.add(root.prefab!!, prefabPath!!, this, child)
    }

    // different spaces like in Unity: world space, camera space
    enum class Space {
        WORLD_SPACE,
        CAMERA_SPACE
    }

    var space = Space.CAMERA_SPACE

    var style = DefaultConfig.style

    @Type("Panel/PrefabSaveable")
    var panel: Panel? = TextPanel(style)
        set(value) {
            field = value
            value?.parent = this
        }

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
        // todo can we somehow reuse its buffers?
        internalMesh.positions = floatArrayOf(
            -1f, -1f, 0f, -1f, +1f, 0f,
            +1f, -1f, 0f, +1f, +1f, 0f
        )
        internalMesh.indices = intArrayOf(
            0, 1, 2, 2, 3, 0
        )
    }

    override fun getMesh() = internalMesh

    // todo focus / unfocus
    // todo interactions with that UI
    // todo enter ui, exit ui

    // todo how can we automatically adjust the viewport to the camera?
    // todo how can we disable drawing depth and such, if camera space?

    override fun onVisibleUpdate(): Boolean {
        // todo if we need to validate the layout, validate it
        // todo if we need to redraw, redraw it
        return true
    }

    fun render() {
        var fb = framebuffer
        val width = width
        val height = height
        if (fb == null || fb.w != width || fb.h != height) {
            fb?.destroy()
            fb = Framebuffer("", width, height, 1, 1, false, DepthBufferType.NONE)
            val prefab = Prefab("Material")
            val image = GPUImage(fb.getColor0(), 4, true, hasOwnership = false)
            val texturePath = InnerTmpFile.InnerTmpImageFile(image)
            val materialPath = InnerTmpFile.InnerTmpPrefabFile(prefab)
            prefab.setProperty("diffuseMap", texturePath)
            framebuffer = fb
            internalMesh.material = materialPath
        }
        useFrame(fb) {
            // todo draw ui onto frame
        }
    }

    override fun clone() = CanvasComponent(this)

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as CanvasComponent
        clone.space = space
        clone.panel = panel?.clone()
        clone.width = width
        clone.height = height
        clone.style = style
    }

    override fun onDestroy() {
        super.onDestroy()
        framebuffer?.destroy()
        framebuffer = null
        internalMesh.destroy()
    }

    override val className get() = "CanvasComponent"


}