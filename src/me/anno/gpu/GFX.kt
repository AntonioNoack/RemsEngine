package me.anno.gpu

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.fonts.FontManager
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.gpu.texture.Texture2D
import me.anno.input.Input
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isShiftDown
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.blending.BlendMode
import me.anno.ui.base.Panel
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelFrame
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.groups.PanelListY
import me.anno.utils.minus
import me.anno.utils.toVec3f
import me.anno.video.Frame
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Matrix4fStack
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.*


object GFX: GFXBase() {

    val nullCamera = Camera(null)

    init {
        nullCamera.name = "Inspector Camera"
        nullCamera.onlyShowTarget = false
    }

    var root = Transform()
    var selectedCamera = nullCamera
    var selectedTransform: Transform? = null
    var selectedProperty: AnimatedProperty<*>? = null

    var hoveredPanel: Panel? = null
    var hoveredWindow: Window? = null

    fun select(transform: Transform){
        selectedTransform = transform
        if(isShiftDown && transform is Camera){
            selectedCamera = transform
        }
    }

    var targetFPS = 30f

    var targetWidth = 1920
    var targetHeight = 1080

    val workerTasks = ConcurrentLinkedQueue<() -> Int>()
    val eventTasks = ConcurrentLinkedQueue<() -> Unit>()

    fun addTask(task: () -> Int){
        workerTasks += task
    }

    fun addEvent(event: () -> Unit){
        eventTasks += event
    }

    var isTestingRawFPS = false
    var editorTimeDilation = 0f

    val LOGGER = LogManager.getLogger()

    lateinit var gameInit: () -> Unit
    lateinit var gameLoop: (w: Int, h: Int) -> Boolean
    lateinit var shutdown: () -> Unit

    var windowX = 0
    var windowY = 0
    var windowWidth = 0
    var windowHeight = 0

    val flat01 = SimpleBuffer.flat01
    val defaultFont = DefaultConfig["font"]?.toString() ?: "Verdana"
    val matrixBuffer = BufferUtils.createFloatBuffer(16)

    lateinit var flatShader: Shader
    lateinit var flatShaderTexture: Shader
    lateinit var subpixelCorrectTextShader: Shader
    lateinit var shader3D: Shader
    lateinit var shader3DPolygon: Shader
    lateinit var shader3DYUV: Shader
    lateinit var shader3DARGB: Shader
    lateinit var shader3DBGRA: Shader
    lateinit var shader3DCircle: Shader
    val whiteTexture = Texture2D(1, 1)
    val stripeTexture = Texture2D(5, 1)
    val colorShowTexture = Texture2D(2,2)

    val flat01Name = flat01.getName()

    var rawDeltaTime = 0f
    var deltaTime = 0f

    var fps = 60f

    var lastTime = System.nanoTime() - (fps * 1e9).toLong() // to prevent wrong fps ;)

    var panelCtr = 0

    var editorTime = 0f
    var editorHoverTime = 0f

    var smoothSin = 0f
    var smoothCos = 0f

    var inFocus: Panel? = null
    fun requestFocus(panel: Panel){
        inFocus = panel
    }

    fun clip(x: Int, y: Int, w: Int, h: Int){
        // from the bottom to the top
        check()
        if(w < 1 || h < 1) throw java.lang.RuntimeException("w < 1 || h < 1 not allowed, got $w x $h")
        GL11.glViewport(x, height-y-h, w, h)
        check()
        // default
        windowX = x
        windowY = y
        windowWidth = w
        windowHeight = h
    }

    fun clip2(x1: Int, y1: Int, x2: Int, y2: Int) = clip(x1,y1,x2-x1,y2-y1)

    lateinit var windowStack: Stack<Window>

    fun getClickedPanelAndWindow(x: Float, y: Float) = getClickedPanelAndWindow(x.toInt(), y.toInt())
    fun getClickedPanelAndWindow(x: Int, y: Int): Pair<Panel, Window>? {
        for(root in windowStack.reversed()){
            val panel = getClickedPanel(root.panel, x, y)
            if(panel != null) return panel to root
        }
        return null
    }

    fun getClickedPanel(x: Float, y: Float) = getClickedPanel(x.toInt(), y.toInt())
    fun getClickedPanel(x: Int, y: Int): Panel? {
        for(root in windowStack.reversed()){
            val panel = getClickedPanel(root.panel, x, y)
            if(panel != null) return panel
        }
        return null
    }

    fun getClickedPanel(panel: Panel, x: Int, y: Int): Panel? {
        return if(panel.isVisible && (x - panel.x) in 0 until panel.w && (y - panel.y) in 0 until panel.h){
            if(panel is PanelGroup){
                for(child in panel.children.reversed()){
                    val clickedByChild = getClickedPanel(child,x,y)
                    if(clickedByChild != null){
                        return clickedByChild
                    }
                }
            }
            panel
        } else null
    }

    fun requestExit(){
        glfwSetWindowShouldClose(window, true)
    }

    override fun addCallbacks() {
        super.addCallbacks()
        Input.initForGLFW()
    }

    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Int){
        if(w == 0 || h == 0) return
        check()
        val shader = flatShader
        shader.use()
        shader.v2("pos", (x-windowX).toFloat()/windowWidth, 1f-(y-windowY).toFloat()/windowHeight)
        shader.v2("size", w.toFloat()/windowWidth, -h.toFloat()/windowHeight)
        shader.v4("color", color.r()/255f, color.g()/255f, color.b()/255f, color.a()/255f)
        flat01.draw(shader)
        check()
        // println("was drawing window ...")
    }

    // todo the background color is important for correct subpixel rendering, because we can't blend per channel
    fun drawText(x: Int, y: Int, fontSize: Int, text: String, color: Int, backgroundColor: Int) = writeText(x, y, defaultFont, fontSize, text, color, backgroundColor)
    fun drawText(x: Int, y: Int, font: String, fontSize: Int, text: String, color: Int, backgroundColor: Int) = writeText(x, y, font, fontSize, text, color, backgroundColor)
    fun writeText(x: Int, y: Int, font: String, fontSize: Int, text: String, color: Int, backgroundColor: Int): Pair<Int, Int> {
        check()
        val texture = FontManager.getString(font, fontSize.toFloat(), text) ?: return 0 to fontSize
        check()
        val w = texture.w
        val h = texture.h
        if(text.isNotBlank()){
            texture.bind()
            check()
            subpixelCorrectTextShader.use()
            check()
            subpixelCorrectTextShader.v2("pos", (x-windowX).toFloat()/windowWidth, 1f-(y-windowY).toFloat()/windowHeight)
            subpixelCorrectTextShader.v2("size", w.toFloat()/windowWidth, -h.toFloat()/windowHeight)
            subpixelCorrectTextShader.v4("textColor", color.r()/255f, color.g()/255f, color.b()/255f, color.a()/255f)
            subpixelCorrectTextShader.v3("backgroundColor", backgroundColor.r()/255f, backgroundColor.g()/255f, backgroundColor.b()/255f)
            flat01.draw(subpixelCorrectTextShader)
            check()
        } else {
            drawRect(x,y,w,h,backgroundColor or black)
        }
        return w to h
    }

    fun drawTexture(x: Int, y: Int, w: Int, h: Int, texture: Texture2D, color: Int){
        check()
        val shader = flatShaderTexture
        shader.use()
        shader.v2("pos", (x-windowX).toFloat()/windowWidth, 1f-(y-windowY).toFloat()/windowHeight)
        shader.v2("size", w.toFloat()/windowWidth, -h.toFloat()/windowHeight)
        shader.v4("color", color.r()/255f, color.g()/255f, color.b()/255f, color.a()/255f)
        texture.bind(0)
        flat01.draw(shader)
        check()
    }

    // todo use sqrt, sq for all our colors to ensure correct mixing
    // todo use float buffer as intermediate target
    fun drawColorHSBRect(hue: Float){

    }

    fun applyCameraTransform(camera: Camera, time: Float, cameraTransform: Matrix4f, stack: Matrix4fStack){
        val position = cameraTransform.transformProject(Vector3f(0f, 0f, 0f))
        val up = cameraTransform.transformProject(Vector3f(0f, 1f, 0f)) - position
        val lookAt = cameraTransform.transformProject(Vector3f(0f, 0f, -1f))
        stack
            .perspective(
                Math.toRadians(camera.fovDegrees.getValueAt(time).toDouble()).toFloat(),
                windowWidth*1f/windowHeight,
                camera.nearZ.getValueAt(time),
                camera.farZ.getValueAt(time))
            .lookAt(position, lookAt, up.normalize())
    }

    // todo generate text as mesh?

    fun shader3DUniforms(shader: Shader, stack: Matrix4fStack, w: Int, h: Int, color: Vector4f, isBillboard: Float){
        check()
        shader.use()
        stack.get(matrixBuffer)
        GL20.glUniformMatrix4fv(shader["transform"], false, matrixBuffer)
        val avgSize = if(w * targetHeight > h * targetWidth) w.toFloat() * targetHeight / targetWidth  else h.toFloat()
        // val avgSize = sqrt(w * h.toFloat())
        val sx = w / avgSize
        val sy = h / avgSize
        val avgSize2 = sqrt(windowWidth * windowHeight.toFloat())
        val sx2 = windowWidth / avgSize2
        val sy2 = windowHeight / avgSize2
        shader.v2("pos", -sx, sy)
        shader.v2("billboardSize", sy2, sx2)
        shader.v2("size", 2f * sx, -2f * sy)
        shader.v4("tint", color.x, color.y, color.z, color.w)
        shader.v1("isBillboard", isBillboard)
    }

    // todo masks for everything?
    // todo allow manipulation of uv coordinates?
    // todo tiling? -> array structure? copying the first element x*y*z times :D

    fun toRadians(f: Float) = Math.toRadians(f.toDouble()).toFloat()

    fun positiveFract(a: Float, b: Float): Float {
        val f = a % b
        return if(f < 0f) b + f
        else f
    }

    fun draw3DCircle(stack: Matrix4fStack, innerRadius: Float, startDegrees: Float, endDegrees: Float, color: Vector4f, isBillboard: Float){
        val shader = shader3DCircle
        shader3DUniforms(shader, stack, 1, 1, color, isBillboard)
        val angle1 = toRadians(positiveFract(startDegrees+180f, 360f)-180f)
        val angle2 = toRadians(positiveFract(endDegrees+180f, 360f)-180f)
        shader.v3("circleParams", innerRadius * innerRadius, angle1, angle2)
        flat01.draw(shader)
        check()
    }

    fun draw3D(stack: Matrix4fStack, buffer: StaticFloatBuffer, texture: Texture2D, w: Int, h:Int, color: Vector4f, isBillboard: Float){
        val shader = shader3D
        shader3DUniforms(shader, stack, w, h, color, isBillboard)
        texture.bind(0)
        buffer.draw(shader)
        check()
    }

    fun draw3D(stack: Matrix4fStack, buffer: StaticFloatBuffer, texture: Texture2D, color: Vector4f, isBillboard: Float){
        draw3D(stack, buffer, texture, texture.w, texture.h, color, isBillboard)
    }

    fun draw3DPolygon(stack: Matrix4fStack, buffer: StaticFloatBuffer,
                      texture: Texture2D, color: Vector4f,
                      inset: Float,
                      isBillboard: Float){
        val shader = shader3DPolygon
        shader3DUniforms(shader, stack, texture.w, texture.h, color, isBillboard)
        shader.v1("inset", inset)
        texture.bind(0)
        buffer.draw(shader)
        check()
    }

    fun draw3D(stack: Matrix4fStack, texture: Texture2D, color: Vector4f, isBillboard: Float){
        return draw3D(stack, flat01, texture, color, isBillboard)
    }

    fun draw3D(stack: Matrix4fStack, texture: Frame, color: Vector4f, isBillboard: Float){
        val shader = texture.get3DShader()
        shader3DUniforms(shader, stack, texture.w, texture.h, color, isBillboard)
        texture.bind(0)
        flat01.draw(shader)
        check()
    }

    fun String.endSpaceCount(): Int {
        var spaceCount = 0
        var index = lastIndex
        loop@while(index > -1){
            when(this[index]){
                ' ' -> spaceCount++
                '\t' -> spaceCount += 4
                else -> break@loop
            }
            index--
        }
        return spaceCount
    }

    fun getTextSize(fontSize: Int, text: String) = getTextSize(defaultFont, fontSize, text)
    fun getTextSize(font: String = "Verdana", fontSize: Int, text: String): Pair<Int, Int> {
        // count how many spaces there are at the end
        // todo get accurate space and tab widths
        val spaceWidth = text.endSpaceCount() * fontSize / 4
        val texture = FontManager.getString(font, fontSize.toFloat(), text) ?: return spaceWidth to fontSize
        return (texture.w + spaceWidth) to texture.h
    }

    fun initShaders(){

        // color only
        flatShader = Shader("" +
                "a2 $flat01Name;\n" +
                "u2 pos, size;\n" +
                "void main(){\n" +
                "   gl_Position = vec4((pos + $flat01Name * size)*2.-1., 0.0, 1.0);\n" +
                "}", "", "" +
                "u4 color;\n" +
                "void main(){\n" +
                "   gl_FragColor = color;\n" +
                "}")

        flatShaderTexture = Shader("" +
                "a2 $flat01Name;\n" +
                "u2 pos, size;\n" +
                "void main(){\n" +
                "   gl_Position = vec4((pos + $flat01Name * size)*2.-1., 0.0, 1.0);\n" +
                "   uv = $flat01Name;\n" +
                "}", "" +
                "varying vec2 uv;\n", "" +
                "uniform sampler2D tex;\n" +
                "u4 color;\n" +
                "void main(){\n" +
                "   gl_FragColor = color * texture(tex, uv);\n" +
                "}")

        // with texture
        subpixelCorrectTextShader = Shader("" +
                "a2 $flat01Name;\n" +
                "u2 pos, size;\n" +
                "void main(){\n" +
                "   gl_Position = vec4((pos + $flat01Name * size)*2.-1., 0.0, 1.0);\n" +
                "   uv = $flat01Name;\n" +
                "}", "" +
                "varying v2 uv;\n", "" +
                "uniform vec4 textColor;" +
                "uniform vec3 backgroundColor;\n" +
                "uniform sampler2D tex;\n" +
                "float brightness(vec3 color){" +
                "   return dot(color, vec3(1.));\n" +
                "}" +
                "void main(){\n" +
                "   vec3 textMask = texture(tex, uv).rgb;\n" +
                "   vec3 mixing = brightness(textColor.rgb) > brightness(backgroundColor) ? textMask.rgb : textMask.bgr;\n" +
                "   vec3 color = vec3(\n" +
                "       mix(backgroundColor.r, textColor.r, mixing.r),\n" +
                "       mix(backgroundColor.g, textColor.g, mixing.g),\n" +
                "       mix(backgroundColor.b, textColor.b, mixing.b));\n" +
                "   gl_FragColor = vec4(color, textColor.a);\n" +
                "}")

        subpixelCorrectTextShader.use()
        GL20.glUniform1i(subpixelCorrectTextShader["tex"], 0)

        // todo test this shader...
        // with texture

        val v3DBase = "" +
                "a2 $flat01Name;\n" +
                "u2 pos, size, billboardSize;\n" +
                "uniform mat4 transform;\n" +
                "uniform float isBillboard;\n" +
                "" +
                "vec4 billboardTransform(vec2 betterUV){" +
                "   vec4 pos0 = transform * vec4(0.0,0.0,0.0,1.0);\n" +
                "   pos0.xy += betterUV * billboardSize;\n" +
                "   return pos0;\n" +
                "}" +
                "" +
                "vec4 transform3D(vec2 betterUV){" +
                "   return transform * vec4(betterUV, 0.0, 1.0);\n" +
                "}"


        val v3D = v3DBase +
                "void main(){\n" +
                "   vec2 betterUV = (pos + $flat01Name * size);\n" +
                "   vec4 billboard = billboardTransform(betterUV);\n" +
                "   vec4 in3D = transform3D(betterUV);\n" +
                "   gl_Position = mix(in3D, billboard, isBillboard);\n" +
                "   uv = $flat01Name;\n" +
                "}"

        val v3DPolygon = v3DBase +
                "in vec2 attr1;\n" +
                "uniform float inset;\n" +
                "void main(){\n" +
                "   vec2 betterUV = (pos + $flat01Name * size);\n" +
                "   betterUV *= mix(1.0, attr1.r, inset);\n" +
                "   vec4 billboard = billboardTransform(betterUV);\n" +
                "   vec4 in3D = transform3D(betterUV);\n" +
                "   gl_Position = mix(in3D, billboard, isBillboard);\n" +
                "   uv = attr1.yx;\n" +
                "}"

        val y3D = "" +
                "varying v2 uv;\n"

        val f3D = "" +
                "uniform vec4 tint;" +
                "uniform sampler2D tex;\n" +
                "void main(){\n" +
                "   gl_FragColor = tint * texture(tex, uv);\n" +
                "   gl_FragColor.rgb *= gl_FragColor.rgb;\n" +
                "}"

        // todo anti-aliasing... -> taa?
        val f3DCircle = "" +
                "u4 tint;\n" + // rgba
                "u3 circleParams;\n" + // r², start, end
                "void main(){\n" +
                "   gl_FragColor = tint;\n" +
                "   gl_FragColor.rgb *= gl_FragColor.rgb;\n" +
                "   vec2 d0 = uv*2.-1.;\n" +
                "   float dst = dot(d0,d0);\n" +
                "   if(dst > 1.0 || dst < circleParams.r) discard;\n" +
                "   else {" +
                "       float angle = atan(d0.y,d0.x);\n" +
                "       if(circleParams.g < circleParams.b){" +
                "           if(angle < circleParams.g || angle > circleParams.b) discard;" +
                "       } else {" +
                "           if(angle > circleParams.b && angle < circleParams.g) discard;" +
                "       }" +
                "   }" +
                "}"


        val f3DYUV = "" +
                "uniform vec4 tint;" +
                "uniform sampler2D texY, texU, texV;\n" +
                "void main(){\n" +
                "   vec3 yuv = vec3(texture(texY, uv).r, texture(texU, uv).r, texture(texV, uv).r);\n" +
                "   yuv -= vec3(${16f/255f}, 0.5, 0.5);\n" +
                "   vec3 rgb = vec3(" +
                "       dot(yuv, vec3( 1.164,  0.000,  1.596))," +
                "       dot(yuv, vec3( 1.164, -0.392, -0.813))," +
                "       dot(yuv, vec3( 1.164,  2.017,  0.000)));\n" +
                "   gl_FragColor = vec4(tint.rgb * rgb, tint.a);\n" +
                "   gl_FragColor.rgb *= gl_FragColor.rgb;\n" +
                "}"

        shader3D = createCustomShader(v3D, y3D, f3D, listOf("tex"))
        shader3DPolygon = createCustomShader(v3DPolygon, y3D, f3D, listOf("tex"))
        shader3DCircle = Shader(v3D, y3D, f3DCircle)

        shader3DYUV = createCustomShader(v3D, y3D, f3DYUV, listOf("texY", "texU", "texV"))

        shader3DARGB = createCustomShader(v3D, y3D, "" +
                "uniform vec4 tint;" +
                "uniform sampler2D tex;\n" +
                "void main(){\n" +
                "   gl_FragColor = tint * texture(tex, uv).gbar;\n" +
                "   gl_FragColor.rgb *= gl_FragColor.rgb;\n" +
                "}", listOf("tex"))
        shader3DBGRA = createCustomShader(v3D, y3D, "" +
                "uniform vec4 tint;" +
                "uniform sampler2D tex;\n" +
                "void main(){\n" +
                "   gl_FragColor = tint * texture(tex, uv).bgra;\n" +
                "   gl_FragColor.rgb *= gl_FragColor.rgb;\n" +
                "}", listOf("tex"))

    }

    fun createCustomShader(v3D: String, y3D: String, fragmentShader: String, textures: List<String>): Shader {
        val shader = Shader(v3D, y3D, fragmentShader)
        shader.use()
        textures.forEachIndexed { index, name ->
            GL20.glUniform1i(shader[name], index)
        }
        return shader
    }

    override fun renderStep0() {
        whiteTexture.create(
            byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte()))
        whiteTexture.filtering(true)
        stripeTexture.createMonochrome(
            byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), 255.toByte()))
        colorShowTexture.create(
            intArrayOf(
                255,255,255,127, 255,255,255,255,
                255,255,255,255, 255,255,255,127
            ).map { it.toByte() }.toByteArray())
        colorShowTexture.filtering(true)
        initShaders()
        setIcon()
    }

    var previousSelectedTransform: Transform? = null
    override fun renderStep(){

        var workDone = 0
        while(workDone < 100){
            val nextTask = workerTasks.poll() ?: break
            workDone += nextTask()
        }

        while(eventTasks.isNotEmpty()){
            eventTasks.poll()!!.invoke()
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        glViewport(0, 0, width, height)

        clip(0, 0, width, height)

        check()


        glDisable(GL11.GL_DEPTH_TEST)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glEnable(GL_BLEND)
        BlendMode.DEFAULT.apply()
        glDisable(GL_CULL_FACE)
        glDisable(GL_ALPHA_TEST)

        check()

        val thisTime = System.nanoTime()
        rawDeltaTime = (thisTime - lastTime) * 1e-9f
        deltaTime = min(rawDeltaTime, 0.1f)
        fps += (1f / rawDeltaTime - fps) * 0.1f
        lastTime = thisTime

        editorTime = max(editorTime + deltaTime * editorTimeDilation, 0f)
        if(editorTime == 0f && editorTimeDilation < 0f){
            editorTimeDilation = 0f
        }

        smoothSin = sin(editorTime)
        smoothCos = cos(editorTime)

        gameLoop(width, height)

        check()

    }

    fun openMenu(x: Int, y: Int, title: String, options: List<Pair<String, (button: Int, isLong: Boolean) -> Boolean>>){
        val style = DefaultConfig.style.getChild("menu")
        val list = PanelListY(style)
        val window = Window(list, x, y)
        fun close(){
            windowStack.remove(window)
        }
        val padding = 4
        if(title.isNotEmpty()){
            val titlePanel = TextPanel(title, style)
            titlePanel.padding.left = padding
            titlePanel.padding.right = padding
            list += titlePanel
            list += SpacePanel(0, 1, style)
        }
        for((name, action) in options){
            val buttonView = TextPanel(name, style)
            buttonView.setOnClickListener { x, y, button, long ->
                if(action(button, long)){
                    close()
                }
            }
            buttonView.padding.left = padding
            buttonView.padding.right = padding
            list += buttonView
        }
        list += WrapAlign.LeftTop
        windowStack.add(window)
    }
    fun openMenu(x: Float, y: Float, title: String, options: List<Pair<String, (button: Int, isLong: Boolean) -> Boolean>>, delta: Int = 10){
        openMenu(x.roundToInt() - delta, y.roundToInt() - delta, title, options)
    }

    fun pauseOrUnpause(){
        editorTimeDilation = if(abs(editorTimeDilation) < 1e-3f || isShiftDown || isControlDown){
            (if(isControlDown) -1f else 1f) * (if(isShiftDown) 0.2f else 1f)
        } else 0f
    }

    fun check(){
        val error = glGetError()
        if(error != 0) throw RuntimeException("GLException: ${when(error){
            1282 -> "invalid operation"
            else -> "$error"
        }}")
    }

}