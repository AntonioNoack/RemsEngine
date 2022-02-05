package me.anno.objects

import me.anno.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.config.DefaultConfig
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.drawing.GFXx3D.draw3DVideo
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.Dict
import me.anno.objects.modes.UVProjection
import me.anno.objects.text.Text
import me.anno.studio.rems.Scene
import me.anno.studio.rems.ui.StudioFileImporter.addChildFromFile
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.editor.frames.FrameSizeInput
import me.anno.ui.style.Style
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.ValueWithDefault
import me.anno.utils.structures.ValueWithDefault.Companion.writeMaybe
import me.anno.utils.structures.ValueWithDefaultFunc
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector2f
import org.joml.Vector4fc
import org.lwjgl.opengl.GL11.*
import kotlin.math.roundToInt

class SoftLink(var file: FileReference) : GFXTransform(null) {

    constructor() : this(InvalidRef)

    init {
        timelineSlot.setDefault(0)
    }

    var softChild = Transform()

    /**
     * which camera is chosen from the scene
     * */
    var cameraIndex = 0

    // uv
    val tiling = AnimatedProperty.tiling()
    val uvProjection = ValueWithDefault(UVProjection.Planar)
    val clampMode = ValueWithDefault(Clamping.MIRRORED_REPEAT)

    // filtering
    val filtering = ValueWithDefaultFunc { DefaultConfig["default.video.nearest", Filtering.LINEAR] }

    var resolution = AnimatedProperty.vec2(Vector2f(1920f, 1080f))

    /**
     * to apply LUTs, effects and such
     * */
    var renderToTexture = false

    init {
        isCollapsedI.setDefault(true)
    }

    private var lastModified: Any? = null
    private var lastCamera: Camera? = null

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {
        super.onDraw(stack, time, color)
        if (renderToTexture) {
            // render to texture to keep all post-processing settings
            val resolution = resolution[time]
            val rx = resolution.x().roundToInt()
            val ry = resolution.y().roundToInt()
            if (rx > 0 && ry > 0 && rx * ry < 16e6) {
                val fb = FBStack["SoftLink", rx, ry, 4, false, 1, true]
                useFrame(fb) {
                    Frame.bind()
                    glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
                    drawSceneWithPostProcessing(time)
                }
                draw3DVideo(
                    this, time, stack, fb.textures[0], color, filtering.value, clampMode.value,
                    tiling[time], uvProjection.value
                )
            }
        } else {
            drawScene(stack, time, color)
        }
    }

    fun updateCache() {
        val lm = file.lastModified to cameraIndex
        if (lm != lastModified) {
            lastModified = lm
            load()
        }
    }

    private val tmpMatrix0 = Matrix4f()
    fun drawScene(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {
        updateCache()
        val camera = lastCamera
        if (camera != null) {
            val cameraTransform = camera.getLocalTransform(time, this)
            val inv = tmpMatrix0.set(cameraTransform).invert()
            stack.next {
                stack.mul(inv)
                drawChild(stack, time, color, softChild)
            }
        } else {
            drawChild(stack, time, color, softChild)
        }
    }

    fun drawSceneWithPostProcessing(time: Double) {
        updateCache()
        val camera = lastCamera ?: Camera()
        val size = resolution[time]
        val w = StrictMath.max(size.x().roundToInt(), 4)
        val h = StrictMath.max(size.y().roundToInt(), 4)
        val wasFinalRendering = isFinalRendering
        isFinalRendering = true
        Scene.draw(
            camera, softChild,
            0, 0, w, h,
            time, true,
            Renderer.colorRenderer, null
        )
        isFinalRendering = wasFinalRendering
    }

    override fun drawChildrenAutomatically(): Boolean = false

    fun load() {
        children.clear()
        if (listOfInheritance.count { it is SoftLink } > maxDepth) {// preventing loops
            softChild = Text("Too many links!")
        } else {
            if (file.exists) {
                if (file.isDirectory) {
                    softChild = Text("Use scene files!")
                } else {
                    addChildFromFile(
                        Transform(),
                        file,
                        FileContentImporter.SoftLinkMode.COPY_CONTENT,
                        false
                    ) { transform ->
                        softChild = transform
                        lastCamera = transform.listOfAll
                            .filterIsInstance<Camera>()
                            .toList()
                            .getOrNull(cameraIndex - 1)// 1 = first, 0 = none
                    }
                }
            } else {
                softChild = Text("File Not Found!")
            }
        }
    }

    override fun claimResources(pTime0: Double, pTime1: Double, pAlpha0: Float, pAlpha1: Float) {
        super.claimResources(pTime0, pTime1, pAlpha0, pAlpha1)
        softChild.claimResources(pTime0, pTime1, pAlpha0, pAlpha1)
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        val link = getGroup("Link Data", "", "softLink")
        link += vi("File", "Where the data is to be loaded from", "", null, file, style) { file = it }
        link += vi(
            "Camera Index", "Which camera should be chosen, 0 = none, 1 = first, ...", "",
            Type.INT_PLUS, cameraIndex, style
        ) { cameraIndex = it }
        list += FrameSizeInput(
            "Resolution",
            resolution[lastLocalTime].run { "${x().roundToInt()} x ${y().roundToInt()}" },
            style
        )
            .setChangeListener { w, h -> putValue(resolution, Vector2f(w.toFloat(), h.toFloat()), true) }
            .setIsSelectedListener { show(resolution) }
        list += vi("Tiling", "(tile count x, tile count y, offset x, offset y)", tiling, style)
        list += vi(
            "UV-Projection", "Can be used for 360°-Videos", null, uvProjection.value, style
        ) { uvProjection.value = it }
        list +=
            vi(
                "Filtering", "Pixelated look?", "texture.filtering", null, filtering.value, style
            ) { filtering.value = it }
        list +=
            vi(
                "Clamping", "For tiled images", "texture.clamping", null, clampMode.value, style
            ) { clampMode.value = it }
        // not ready yet
        link += vi("Enable Postprocessing", "", "", null, renderToTexture, style) { renderToTexture = it }
    }

    override fun save(writer: BaseWriter) {
        synchronized(this) { children.clear() }
        super.save(writer)
        writer.writeObject(this, "resolution", resolution)
        writer.writeFile("file", file)
        writer.writeInt("cameraIndex", cameraIndex)
        writer.writeBoolean("renderToTexture", renderToTexture)
        writer.writeObject(this, "tiling", tiling)
        writer.writeMaybe(this, "filtering", filtering)
        writer.writeMaybe(this, "clamping", clampMode)
        writer.writeMaybe(this, "uvProjection", uvProjection)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "resolution" -> resolution.copyFrom(value)
            "tiling" -> tiling.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "renderToTexture" -> renderToTexture = value
            else -> super.readBoolean(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "cameraIndex" -> cameraIndex = value
            "filtering" -> filtering.value = filtering.value.find(value)
            "clamping" -> clampMode.value = Clamping.values().firstOrNull { it.id == value } ?: return
            "uvProjection" -> uvProjection.value = UVProjection.values().firstOrNull { it.id == value } ?: return
            else -> super.readInt(name, value)
        }
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "file" -> file = value?.toGlobalFile() ?: InvalidRef
            else -> super.readString(name, value)
        }
    }

    override fun readFile(name: String, value: FileReference) {
        when (name) {
            "file" -> file = value
            else -> super.readFile(name, value)
        }
    }

    override val areChildrenImmutable: Boolean = true

    override val defaultDisplayName: String = Dict["Linked Object", "obj.softLink"]
    override val className get() = "SoftLink"

    companion object {
        const val maxDepth = 5
    }

}