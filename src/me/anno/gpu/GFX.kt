package me.anno.gpu

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.fonts.FontManager
import me.anno.gpu.GFX.v4
import me.anno.gpu.ShaderLib.flatShader
import me.anno.gpu.ShaderLib.flatShaderTexture
import me.anno.gpu.ShaderLib.shader3D
import me.anno.gpu.ShaderLib.shader3DCircle
import me.anno.gpu.ShaderLib.shader3DMasked
import me.anno.gpu.ShaderLib.shader3DPolygon
import me.anno.gpu.ShaderLib.shader3DSVG
import me.anno.gpu.ShaderLib.shader3DYUV
import me.anno.gpu.ShaderLib.subpixelCorrectTextShader
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.ClampMode
import me.anno.gpu.texture.FilteringMode
import me.anno.gpu.texture.Texture2D
import me.anno.input.Input
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.input.MouseButton
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.objects.blending.BlendMode
import me.anno.objects.effects.MaskType
import me.anno.objects.geometric.Circle
import me.anno.objects.modes.UVProjection
import me.anno.studio.Build.isDebug
import me.anno.studio.Studio
import me.anno.studio.Studio.editorTime
import me.anno.studio.Studio.editorTimeDilation
import me.anno.studio.Studio.eventTasks
import me.anno.studio.Studio.root
import me.anno.studio.Studio.selectedInspectable
import me.anno.studio.Studio.selectedTransform
import me.anno.studio.Studio.targetHeight
import me.anno.studio.Studio.targetWidth
import me.anno.ui.base.Panel
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.groups.PanelListY
import me.anno.utils.clamp
import me.anno.utils.f1
import me.anno.utils.minus
import me.anno.video.Frame
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30.*
import java.lang.Exception
import java.lang.Math
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.*

// todo split the rendering in two parts:
// todo - without blending (no alpha, video or polygons)
// todo - with blending
// todo enqueue all objects for rendering
// todo sort blended objects by depth, if rendering with depth

// todo show frame times for better inspecting of fps xD

// todo ffmpeg requires 100MB RAM per instance -> do we really need multiple instances, or does one work fine?
// todo or keep only a certain amount of ffmpeg instances running?


object GFX : GFXBase1() {

    private val LOGGER = LogManager.getLogger(GFX::class)!!

    // for final rendering we need to use the GPU anyways;
    // so just use a static variable
    var isFinalRendering = false
    var drawMode = ShaderPlus.DrawMode.COLOR
    val isFakeColorRendering get() = drawMode != ShaderPlus.DrawMode.COLOR
    var supportsAnisotropicFiltering = false
    var anisotropy = 1f

    var hoveredPanel: Panel? = null
    var hoveredWindow: Window? = null

    fun select(transform: Transform?) {
        selectedInspectable = transform
        selectedTransform = transform
    }

    val gpuTasks = ConcurrentLinkedQueue<() -> Int>()
    val audioTasks = ConcurrentLinkedQueue<() -> Int>()

    fun addAudioTask(task: () -> Int) {
        // could be optimized for release...
        audioTasks += task
    }

    fun addGPUTask(task: () -> Int) {
        gpuTasks += task
    }

    lateinit var gameInit: () -> Unit
    lateinit var gameLoop: (w: Int, h: Int) -> Boolean
    lateinit var shutdown: () -> Unit

    val loadTexturesSync = Stack<Boolean>()

    init {
        loadTexturesSync.push(false)
    }

    var windowX = 0
    var windowY = 0
    var windowWidth = 0
    var windowHeight = 0
    // val windowSize get() = WindowSize(windowX, windowY, windowWidth, windowHeight)

    val flat01 = SimpleBuffer.flat01
    // val flat01Cube = SimpleBuffer.flat01Cube

    // val defaultFont = DefaultConfig["font"]?.toString() ?: "Verdana"
    val matrixBuffer = BufferUtils.createFloatBuffer(16)

    var rawDeltaTime = 0f
    var deltaTime = 0f

    val editorVideoFPS get() = if (editorTimeDilation == 0.0) 10.0 else 10.0 / (max(0.333, abs(editorTimeDilation)))
    var currentEditorFPS = 60f

    var lastTime = System.nanoTime() - (editorVideoFPS * 1e9).toLong() // to prevent wrong fps ;)

    var editorHoverTime = 0.0

    var smoothSin = 0.0
    var smoothCos = 0.0

    var drawnTransform: Transform? = null

    val menuSeparator = "-----"

    val inFocus = HashSet<Panel>()
    val inFocus0 get() = inFocus.firstOrNull()

    fun requestFocus(panel: Panel?, exclusive: Boolean) {
        if (exclusive) inFocus.clear()
        if (panel != null) inFocus += panel
    }

    fun clip(x: Int, y: Int, w: Int, h: Int) {
        // from the bottom to the top
        check()
        if (w < 1 || h < 1) throw java.lang.RuntimeException("w < 1 || h < 1 not allowed, got $w x $h")
        GL11.glViewport(x, height - y - h, w, h)
        check()
        // default
        windowX = x
        windowY = y
        windowWidth = w
        windowHeight = h
    }

    fun clip(size: me.anno.gpu.size.WindowSize) = clip(size.x, size.y, size.w, size.h)

    fun clip2(x1: Int, y1: Int, x2: Int, y2: Int) = clip(x1, y1, x2 - x1, y2 - y1)

    lateinit var windowStack: Stack<Window>

    fun getPanelAndWindowAt(x: Float, y: Float) = getPanelAndWindowAt(x.toInt(), y.toInt())
    fun getPanelAndWindowAt(x: Int, y: Int): Pair<Panel, Window>? {
        for (root in windowStack.reversed()) {
            val panel = getPanelAt(root.panel, x, y)
            if (panel != null) return panel to root
        }
        return null
    }

    fun getPanelAt(x: Float, y: Float) = getPanelAt(x.toInt(), y.toInt())
    fun getPanelAt(x: Int, y: Int): Panel? {
        for (root in windowStack.reversed()) {
            val panel = getPanelAt(root.panel, x, y)
            if (panel != null) return panel
        }
        return null
    }

    fun getPanelAt(panel: Panel, x: Int, y: Int): Panel? {
        return if (panel.isVisible && (x - panel.x) in 0 until panel.w && (y - panel.y) in 0 until panel.h) {
            if (panel is PanelGroup) {
                for (child in panel.children.reversed()) {
                    val clickedByChild = getPanelAt(child, x, y)
                    if (clickedByChild != null) {
                        return clickedByChild
                    }
                }
            }
            panel
        } else null
    }

    fun requestExit() {
        glfwSetWindowShouldClose(window, true)
    }

    override fun addCallbacks() {
        super.addCallbacks()
        Input.initForGLFW()
    }

    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Vector4f) {
        if (w == 0 || h == 0) return
        check()
        val shader = flatShader
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4("color", color.x, color.y, color.z, color.w)
        flat01.draw(shader)
        check()
    }

    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        if (w == 0 || h == 0) return
        check()
        val shader = flatShader
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4("color", color.r() / 255f, color.g() / 255f, color.b() / 255f, color.a() / 255f)
        flat01.draw(shader)
        check()
    }

    fun drawRect(x: Float, y: Float, w: Float, h: Float, color: Int) {
        check()
        val shader = flatShader
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4("color", color.r() / 255f, color.g() / 255f, color.b() / 255f, color.a() / 255f)
        flat01.draw(shader)
        check()
    }

    // the background color is important for correct subpixel rendering, because we can't blend per channel
    fun drawText(
        x: Int, y: Int, font: String, fontSize: Int, bold: Boolean, italic: Boolean, text: String,
        color: Int, backgroundColor: Int, widthLimit: Int, centerX: Boolean = false
    ) =
        writeText(x, y, font, fontSize, bold, italic, text, color, backgroundColor, widthLimit, centerX)

    fun writeText(
        x: Int, y: Int,
        font: String, fontSize: Int,
        bold: Boolean, italic: Boolean,
        text: String,
        color: Int,
        backgroundColor: Int,
        widthLimit: Int,
        centerX: Boolean = false
    ): Pair<Int, Int> {

        check()
        val texture =
            FontManager.getString(font, fontSize.toFloat(), text, italic, bold, widthLimit) ?: return 0 to fontSize
        // check()
        val w = texture.w
        val h = texture.h
        if (text.isNotBlank()) {
            texture.bind(true, ClampMode.CLAMP)
            val shader = subpixelCorrectTextShader
            // check()
            shader.use()
            var x2 = x
            if (centerX) x2 -= w / 2
            shader.v2("pos", (x2 - windowX).toFloat() / windowWidth, 1f - (y - windowY).toFloat() / windowHeight)
            shader.v2("size", w.toFloat() / windowWidth, -h.toFloat() / windowHeight)
            shader.v4("textColor", color.r() / 255f, color.g() / 255f, color.b() / 255f, color.a() / 255f)
            shader.v3(
                "backgroundColor",
                backgroundColor.r() / 255f,
                backgroundColor.g() / 255f,
                backgroundColor.b() / 255f
            )
            flat01.draw(shader)
            check()
        } else {
            drawRect(x, y, w, h, backgroundColor or black)
        }
        return w to h
    }

    // fun getTextSize(fontSize: Int, bold: Boolean, italic: Boolean, text: String) = getTextSize(defaultFont, fontSize, bold, italic, text)
    fun getTextSize(
        font: String,
        fontSize: Int,
        bold: Boolean,
        italic: Boolean,
        text: String,
        widthLimit: Int
    ): Pair<Int, Int> {
        // count how many spaces there are at the end
        // get accurate space and tab widths
        val spaceWidth = 0//text.endSpaceCount() * fontSize / 4
        val texture = FontManager.getString(font, fontSize.toFloat(), text, bold, italic, widthLimit)
            ?: return spaceWidth to fontSize
        return (texture.w + spaceWidth) to texture.h
    }

    fun drawTexture(x: Int, y: Int, w: Int, h: Int, texture: Texture2D, color: Int, tiling: Vector4f?) {
        check()
        val shader = flatShaderTexture
        shader.use()
        posSize(shader, x, y, w, h)
        shader.v4("color", color.r() / 255f, color.g() / 255f, color.b() / 255f, color.a() / 255f)
        if (tiling != null) shader.v4("tiling", tiling)
        else shader.v4("tiling", 1f, 1f, 0f, 0f)
        texture.bind(0, texture.nearest, texture.clampMode)
        flat01.draw(shader)
        check()
    }

    fun getFlatTransform(x: Float, y: Float, w: Int, h: Int): Matrix4fArrayList {
        // todo correct transform...
        val matrix = Matrix4fArrayList()
        matrix.translate((x - windowX).toFloat() / windowWidth, -(y - windowY).toFloat() / windowHeight, 0f)
        val scale = h.toFloat() / windowHeight
        // w.toFloat()/windowWidth
        matrix.scale(scale)
        return matrix
    }

    fun drawTexture(x: Int, y: Int, w: Int, h: Int, texture: Frame, color: Int, tiling: Vector4f?) {
        draw3D(
            getFlatTransform(x.toFloat(), y.toFloat(), w, h),
            texture, color.v4(),
            FilteringMode.LINEAR, ClampMode.CLAMP, tiling, UVProjection.Planar
        )
    }

    fun drawCircle(
        x: Int, y: Int, w: Int, h: Int, innerRadius: Float, startDegrees: Float, endDegrees: Float, color: Vector4f
    ) {
        // not perfect, but pretty good
        // anti-aliasing for the rough edges
        // not very economical, could be improved
        color.w /= 25f
        for (dx in 0 until 5) {
            for (dy in 0 until 5) {
                draw3DCircle(
                    getFlatTransform(x + dx / 3f - 1.63f, y + dy / 3f - 1.63f, w, h),
                    innerRadius, startDegrees, endDegrees, color
                )
            }
        }
    }

    fun posSize(shader: Shader, x: Int, y: Int, w: Int, h: Int) {
        shader.v2("pos", (x - windowX).toFloat() / windowWidth, 1f - (y - windowY).toFloat() / windowHeight)
        shader.v2("size", w.toFloat() / windowWidth, -h.toFloat() / windowHeight)
    }

    fun posSize(shader: Shader, x: Float, y: Float, w: Float, h: Float) {
        shader.v2("pos", (x - windowX) / windowWidth, 1f - (y - windowY) / windowHeight)
        shader.v2("size", w / windowWidth, -h / windowHeight)
    }

    fun applyCameraTransform(camera: Camera, time: Double, cameraTransform: Matrix4f, stack: Matrix4fArrayList) {
        val offset = camera.getEffectiveOffset(time)
        val cameraTransform2 = if (offset != 0f) {
            Matrix4f(cameraTransform).translate(0f, 0f, offset)
        } else cameraTransform
        val fov = camera.getEffectiveFOV(time, offset)
        val near = camera.getEffectiveNear(time, offset)
        val far = camera.getEffectiveFar(time, offset)
        val position = cameraTransform2.transformProject(Vector3f(0f, 0f, 0f))
        val up = cameraTransform2.transformProject(Vector3f(0f, 1f, 0f)) - position
        val lookAt = cameraTransform2.transformProject(Vector3f(0f, 0f, -1f))
        stack
            .perspective(
                Math.toRadians(fov.toDouble()).toFloat(),
                windowWidth * 1f / windowHeight, near, far
            )
            .lookAt(position, lookAt, up.normalize())
    }

    fun shader3DUniforms(
        shader: Shader, stack: Matrix4fArrayList,
        w: Int, h: Int, color: Vector4f,
        tiling: Vector4f?, filtering: FilteringMode,
        uvProjection: UVProjection?
    ) {
        check()

        shader.use()
        stack.pushMatrix()

        val doScale2 = (uvProjection?.doScale ?: true) && w != h

        shader.v1("filtering", filtering.id)
        shader.v2("textureDeltaUV", 1f / w, 1f / h)

        // val avgSize = sqrt(w * h.toFloat())
        if (doScale2) {
            val avgSize =
                if (w * targetHeight > h * targetWidth) w.toFloat() * targetHeight / targetWidth else h.toFloat()
            val sx = w / avgSize
            val sy = h / avgSize
            stack.scale(sx, -sy, 1f)
        } else {
            stack.scale(1f, -1f, 1f)
        }

        stack.get(matrixBuffer)
        GL20.glUniformMatrix4fv(shader["transform"], false, matrixBuffer)
        stack.popMatrix()

        shaderColor(shader, "tint", color)
        if (tiling != null) shader.v4("tiling", tiling)
        else shader.v4("tiling", 1f, 1f, 0f, 0f)
        shader.v1("drawMode", drawMode.id)
        shader.v1("uvProjection", uvProjection?.id ?: UVProjection.Planar.id)

    }


    fun shader3DUniforms(shader: Shader, stack: Matrix4f, color: Vector4f) {
        check()
        shader.use()
        stack.get(matrixBuffer)
        glUniformMatrix4fv(shader["transform"], false, matrixBuffer)
        shaderColor(shader, "tint", color)
        shader.v4("tiling", 1f, 1f, 0f, 0f)
        shader.v1("drawMode", drawMode.id)
    }

    fun shaderColor(shader: Shader, name: String, color: Vector4f) {
        if (isFakeColorRendering) {
            val id = drawnTransform!!.clickId
            shader.v4(name, id.b() / 255f, id.g() / 255f, id.r() / 255f, 1f)
        } else {
            shader.v4(name, color.x, color.y, color.z, color.w)
        }
    }

    fun toRadians(f: Float) = Math.toRadians(f.toDouble()).toFloat()
    fun toRadians(f: Double) = Math.toRadians(f)

    fun draw3DCircle(
        stack: Matrix4fArrayList,
        innerRadius: Float,
        startDegrees: Float,
        endDegrees: Float,
        color: Vector4f
    ) {
        val shader = shader3DCircle
        shader3DUniforms(shader, stack, 1, 1, color, null, FilteringMode.NEAREST, null)
        var a0 = startDegrees
        var a1 = endDegrees
        // if the two arrows switch sides, flip the circle
        // todo do this for angle difference > 360°...
        if (a0 > a1) {// first start for checker pattern
            val tmp = a0
            a0 = a1
            a1 = tmp - 360f
        }
        val angle0 = toRadians(a0)
        val angle1 = toRadians(a1)
        shader.v3("circleParams", 1f - innerRadius, angle0, angle1)
        Circle.drawBuffer(shader)
        check()
    }

    fun draw3DMasked(
        stack: Matrix4fArrayList, color: Vector4f,
        maskType: MaskType,
        useMaskColor: Float, offsetColor: Vector4f,
        pixelSize: Float,
        isInverted: Float, blurDeltaUV: Vector2f
    ) {
        val shader = shader3DMasked.shader
        shader3DUniforms(shader, stack, 1, 1, color, null, FilteringMode.NEAREST, null)
        shader.v4("offsetColor", offsetColor.x, offsetColor.y, offsetColor.z, offsetColor.w)
        shader.v1("useMaskColor", useMaskColor)
        shader.v1("invertMask", isInverted)
        shader.v1("maskType", maskType.id)
        shader.v2("pixelating", pixelSize * windowHeight / windowWidth, pixelSize)
        shader.v2("blurDeltaUV", blurDeltaUV)
        shader.v1("maxSteps", pixelSize * windowHeight)
        flat01.draw(shader)
        check()
    }

    fun draw3D(
        stack: Matrix4fArrayList, buffer: StaticFloatBuffer, texture: Texture2D, w: Int, h: Int, color: Vector4f,
        filtering: FilteringMode, clampMode: ClampMode, tiling: Vector4f?
    ) {
        val shader = shader3D.shader
        shader3DUniforms(shader, stack, w, h, color, tiling, filtering, null)
        texture.bind(0, filtering, clampMode)
        buffer.draw(shader)
        check()
    }

    fun draw3D(
        stack: Matrix4fArrayList, buffer: StaticFloatBuffer, texture: Texture2D, color: Vector4f,
        filtering: FilteringMode, clampMode: ClampMode, tiling: Vector4f?
    ) {
        draw3D(stack, buffer, texture, texture.w, texture.h, color, filtering, clampMode, tiling)
    }

    fun draw3DPolygon(
        stack: Matrix4fArrayList, buffer: StaticFloatBuffer,
        texture: Texture2D, color: Vector4f,
        inset: Float,
        filtering: FilteringMode, clampMode: ClampMode
    ) {
        val shader = shader3DPolygon.shader
        shader3DUniforms(shader, stack, texture.w, texture.h, color, null, filtering, null)
        shader.v1("inset", inset)
        texture.bind(0, filtering, clampMode)
        buffer.draw(shader)
        check()
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: Frame, color: Vector4f,
        filtering: FilteringMode, clampMode: ClampMode, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        if (!texture.isLoaded) throw RuntimeException("Frame must be loaded to be rendered!")
        val shader = texture.get3DShader().shader
        shader3DUniforms(shader, stack, texture.w, texture.h, color, tiling, filtering, uvProjection)
        texture.bind(0, filtering, clampMode)
        if (shader == shader3DYUV.shader) {
            val w = texture.w
            val h = texture.h
            shader.v2("uvCorrection", w.toFloat() / ((w + 1) / 2 * 2), h.toFloat() / ((h + 1) / 2 * 2))
        }
        uvProjection.getBuffer().draw(shader)
        check()
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: Texture2D, color: Vector4f,
        filtering: FilteringMode, clampMode: ClampMode, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        draw3D(stack, texture, texture.w, texture.h, color, filtering, clampMode, tiling, uvProjection)
    }

    fun draw3D(
        stack: Matrix4fArrayList, texture: Texture2D, w: Int, h: Int, color: Vector4f,
        filtering: FilteringMode, clampMode: ClampMode, tiling: Vector4f?, uvProjection: UVProjection
    ) {
        val shader = shader3D.shader
        shader3DUniforms(shader, stack, w, h, color, tiling, filtering, uvProjection)
        texture.bind(0, filtering, clampMode)
        uvProjection.getBuffer().draw(shader)
        check()
    }

    fun draw3DSVG(
        stack: Matrix4fArrayList, buffer: StaticFloatBuffer, texture: Texture2D, color: Vector4f,
        filtering: FilteringMode, clampMode: ClampMode
    ) {
        val shader = shader3DSVG.shader
        shader3DUniforms(shader, stack, texture.w, texture.h, color, null, filtering, null)
        texture.bind(0, filtering, clampMode)
        buffer.draw(shader)
        check()
    }

    override fun renderStep0() {
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1) // opengl is evil ;), for optimizations, we might set it back
        supportsAnisotropicFiltering = GL.getCapabilities().GL_EXT_texture_filter_anisotropic
        LOGGER.info("OpenGL supports Anisotropic Filtering? $supportsAnisotropicFiltering")
        if (supportsAnisotropicFiltering) {
            val max = glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT)
            anisotropy = min(max, DefaultConfig["gpu.filtering.anisotropic.max", 16f])
        }
        TextureLib.init()
        ShaderLib.init()
        setIcon()
    }

    fun workQueue(queue: ConcurrentLinkedQueue<() -> Int>) {
        // async work section

        var workDone = 0
        val workTime0 = System.nanoTime()
        while (workDone < 100) {
            val nextTask = queue.poll() ?: break
            workDone += nextTask()
            val workTime1 = System.nanoTime()
            val workTime = abs(workTime1 - workTime0) * 1e-9f
            if (workTime * editorVideoFPS > 1f) {// work is too slow
                break
            }
        }

    }

    fun clearStack() {
        Framebuffer.stack.clear()
    }

    fun ensureEmptyStack() {
        if (Framebuffer.stack.size > 0) {
            /*Framebuffer.stack.forEach {
                println(it)
            }
            throw RuntimeException("Catched ${Framebuffer.stack.size} items on the Framebuffer.stack")
            exitProcess(1)*/
        }
        Framebuffer.stack.clear()
    }

    override fun renderStep() {

        ensureEmptyStack()

        // Framebuffer.bindNull()

        workQueue(gpuTasks)

        // Framebuffer.stack.pop()

        ensureEmptyStack()

        // rendering and editor section

        updateTime()

        // updating the local times must be done before the events, because
        // the worker thread might have invalidated those
        updateLastLocalTime(root, editorTime)

        while (eventTasks.isNotEmpty()) {
            try {
                eventTasks.poll()!!.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Texture2D.textureBudgetUsed = 0

        /*Framebuffer.bindNull()
        glViewport(0, 0, width, height)
        Framebuffer.bindNull()*/
        glBindTexture(GL_TEXTURE_2D, 0)

        check()

        glDisable(GL_DEPTH_TEST)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glEnable(GL_BLEND)
        BlendMode.DEFAULT.apply()
        glDisable(GL_CULL_FACE)
        glDisable(GL_ALPHA_TEST)

        check()

        ensureEmptyStack()

        gameLoop(width, height)

        ensureEmptyStack()

        check()

    }

    fun updateLastLocalTime(parent: Transform, time: Double) {
        val localTime = parent.getLocalTime(time)
        parent.lastLocalTime = localTime
        parent.children.forEach { child ->
            updateLastLocalTime(child, localTime)
        }
    }

    fun updateTime() {

        val thisTime = System.nanoTime()
        rawDeltaTime = (thisTime - lastTime) * 1e-9f
        deltaTime = min(rawDeltaTime, 0.1f)
        val newFPS = 1f / rawDeltaTime
        currentEditorFPS = min(currentEditorFPS + (newFPS - currentEditorFPS) * 0.05f, newFPS)
        lastTime = thisTime

        editorTime = max(editorTime + deltaTime * editorTimeDilation, 0.0)
        if(editorTimeDilation != 0.0){
            Studio.updateInspector()
        }
        if (editorTime == 0.0 && editorTimeDilation < 0.0) {
            editorTimeDilation = 0.0
        }

        smoothSin = sin(editorTime)
        smoothCos = cos(editorTime)

    }

    fun openMenuComplex(
        x: Int,
        y: Int,
        title: String,
        options: List<Pair<String, (button: MouseButton, isLong: Boolean) -> Boolean>>
    ) {
        val style = DefaultConfig.style.getChild("menu")
        val list = PanelListY(style)
        list += WrapAlign.LeftTop
        val container =
            ScrollPanelY(list, Padding(1), style, AxisAlignment.MIN)
        container += WrapAlign.LeftTop
        lateinit var window: Window
        fun close() {
            windowStack.remove(window)
        }

        val padding = 4
        if (title.isNotEmpty()) {
            val titlePanel = TextPanel(title, style)
            titlePanel.padding.left = padding
            titlePanel.padding.right = padding
            list += titlePanel
            list += SpacePanel(0, 1, style)
        }
        for ((index, element) in options.withIndex()) {
            val (name, action) = element
            if (name == menuSeparator) {
                if (index != 0) {
                    list += SpacePanel(0, 1, style)
                }
            } else {
                val buttonView = TextPanel(name, style)
                buttonView.setOnClickListener { _, _, button, long ->
                    if (action(button, long)) {
                        close()
                    }
                }
                buttonView.padding.left = padding
                buttonView.padding.right = padding
                list += buttonView
            }
        }
        val maxWidth = max(300, GFX.width)
        val maxHeight = max(300, GFX.height)
        container.calculateSize(maxWidth, maxHeight)
        container.applyPlacement(min(container.minW, maxWidth), min(container.minH, maxHeight))
        // println("size for window: ${container.w} ${container.h}")
        val wx = clamp(x, 0, max(GFX.width - container.w, 0))
        val wy = clamp(y, 0, max(GFX.height - container.h, 0))
        window = Window(container, false, wx, wy)
        windowStack.add(window)
    }

    fun openMenuComplex(
        x: Float,
        y: Float,
        title: String,
        options: List<Pair<String, (button: MouseButton, isLong: Boolean) -> Boolean>>,
        delta: Int = 10
    ) {
        openMenuComplex(x.roundToInt() - delta, y.roundToInt() - delta, title, options)
    }

    fun openMenu(x: Int, y: Int, title: String, options: List<Pair<String, () -> Any>>, delta: Int = 10) {
        return openMenu(x.toFloat(), y.toFloat(), title, options, delta)
    }

    fun openMenu(x: Float, y: Float, title: String, options: List<Pair<String, () -> Any>>, delta: Int = 10) {
        openMenuComplex(x.roundToInt() - delta, y.roundToInt() - delta, title, options.map { (key, value) ->
            Pair(key, { b: MouseButton, _: Boolean ->
                if (b.isLeft) {
                    value(); true
                } else false
            })
        })
    }

    var glThread: Thread? = null
    fun check() {
        if (isDebug) {
            val currentThread = Thread.currentThread()
            if (currentThread != glThread) {
                if (glThread == null) {
                    glThread = currentThread
                    currentThread.name = "OpenGL"
                } else {
                    throw RuntimeException("GFX.check() called from wrong thread! Always use GFX.addGPUTask { ... }")
                }
            }
            val error = glGetError()
            if (error != 0) {
                Framebuffer.stack.forEach {
                    println(it)
                }
                throw RuntimeException(
                    "GLException: ${when (error) {
                        1280 -> "invalid enum"
                        1281 -> "invalid value"
                        1282 -> "invalid operation"
                        1283 -> "stack overflow"
                        1284 -> "stack underflow"
                        1285 -> "out of memory"
                        1286 -> "invalid framebuffer operation"
                        else -> "$error"
                    }}"
                )
            }
        }
    }

    fun ask(question: String, onYes: () -> Unit) {
        openMenu(mouseX, mouseY, question, listOf(
            "Yes" to onYes,
            "No" to {}
        ))
    }

    fun ask(question: String, onYes: () -> Unit, onNo: () -> Unit) {
        openMenu(
            mouseX, mouseY, question, listOf(
                "Yes" to onYes,
                "No" to onNo
            )
        )
    }

    fun showFPS() {
        loadTexturesSync.push(true)
        clip(0, 0, width, height)
        drawText(1, 1, "SansSerif", 12, false, false, currentEditorFPS.f1(), -1, 0, -1)
        loadTexturesSync.pop()
    }

}