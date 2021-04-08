package me.anno.objects

import me.anno.cache.instances.LastModifiedCache
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.GFX.windowHeight
import me.anno.gpu.GFX.windowWidth
import me.anno.gpu.GFXx3D.draw3DVideo
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Type
import me.anno.objects.modes.UVProjection
import me.anno.objects.text.Text
import me.anno.studio.rems.Scene
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.files.ImportFromFile.addChildFromFile
import me.anno.ui.editor.frames.FrameSizeInput
import me.anno.ui.style.Style
import me.anno.utils.files.LocalFile.toGlobalFile
import org.joml.*
import org.lwjgl.opengl.GL11.*
import java.io.File
import kotlin.math.roundToInt

class SoftLink(var file: File) : GFXTransform(null) {

    constructor() : this(File(""))

    var softChild = Transform()

    /**
     * which camera is chosen from the scene
     * */
    var cameraIndex = 0

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
                val fb = FBStack["SoftLink", rx, ry, 1, false]
                Frame(fb) {
                    Frame.bind()
                    glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
                    drawSceneWithPostProcessing(time)
                }
                draw3DVideo(
                    this, time, stack, fb.textures[0], color, Filtering.LINEAR, Clamping.CLAMP,
                    null, UVProjection.Planar
                )
            }
        } else {
            drawScene(stack, time, color)
        }
    }

    fun updateCache() {
        val lm = LastModifiedCache[file] to cameraIndex
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
        val wasFinalRendering = isFinalRendering
        isFinalRendering = true
        updateCache()
        val camera = lastCamera ?: Camera()
        Scene.draw(
            camera, softChild, 0, 0, windowWidth, windowHeight, time, true,
            ShaderPlus.DrawMode.COLOR, null
        )
        isFinalRendering = wasFinalRendering
    }

    override fun drawChildrenAutomatically(): Boolean = false

    fun load() {
        children.clear()
        if (listOfInheritance.count { it is SoftLink } > maxDepth) {// preventing loops
            softChild = Text("Too many links!")
        } else {
            if (file.exists()) {
                if (file.isDirectory) {
                    softChild = Text("Use scene files!")
                } else {
                    addChildFromFile(Transform(), file, false, false) { transform ->
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
        list += FrameSizeInput("Resolution", resolution[lastLocalTime].run { "${x().roundToInt()} x ${y().roundToInt()}" }, style)
            .setChangeListener { w, h -> putValue(resolution, Vector2f(w.toFloat(), h.toFloat()), true) }
            .setIsSelectedListener { show(resolution) }
        // not ready yet
        // link += vi("Enable Postprocessing", "", "", null, renderToTexture, style){ renderToTexture = it }
    }

    override fun save(writer: BaseWriter) {
        synchronized(this) { children.clear() }
        super.save(writer)
        writer.writeObject(this, "resolution", resolution)
        writer.writeFile("file", file)
        writer.writeInt("cameraIndex", cameraIndex)
        writer.writeBoolean("renderToTexture", renderToTexture)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "resolution" -> resolution.copyFrom(value)
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
            else -> super.readInt(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "file" -> file = value.toGlobalFile()
            else -> super.readString(name, value)
        }
    }

    override val areChildrenImmutable: Boolean = true

    override fun getDefaultDisplayName(): String = Dict["Linked Object", "obj.softLink"]
    override fun getClassName() = "SoftLink"

    companion object {
        const val maxDepth = 5
    }

}