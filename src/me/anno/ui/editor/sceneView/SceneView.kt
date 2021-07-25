package me.anno.ui.editor.sceneView

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.config.DefaultStyle.deepDark
import me.anno.gpu.GFX
import me.anno.gpu.GFX.addGPUTask
import me.anno.gpu.GFX.deltaTime
import me.anno.gpu.GFX.windowStack
import me.anno.gpu.RenderState
import me.anno.gpu.RenderState.renderDefault
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.Window
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.StableWindowSize
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.input.Input
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isShiftDown
import me.anno.input.Input.mouseKeysDown
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.input.MouseButton
import me.anno.input.Touch.Companion.touches
import me.anno.io.files.FileReference
import me.anno.language.translation.Dict
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.objects.effects.ToneMappers
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.studio.StudioBase.Companion.shiftSlowdown
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.editorTime
import me.anno.studio.rems.RemsStudio.editorTimeDilation
import me.anno.studio.rems.RemsStudio.isPaused
import me.anno.studio.rems.RemsStudio.nullCamera
import me.anno.studio.rems.RemsStudio.project
import me.anno.studio.rems.RemsStudio.root
import me.anno.studio.rems.Scene
import me.anno.studio.rems.Selection
import me.anno.studio.rems.Selection.selectTransform
import me.anno.studio.rems.Selection.selectedTransform
import me.anno.studio.rems.ui.TransformFileImporter.addChildFromFile
import me.anno.studio.rems.ui.TransformTreeView.Companion.zoomToObject
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelList
import me.anno.ui.custom.CustomContainer
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.simple.SimplePanel
import me.anno.ui.style.Style
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.length
import me.anno.utils.Maths.pow
import me.anno.utils.OS
import me.anno.utils.bugs.SumOf.sumOf
import me.anno.utils.files.Files.use
import me.anno.utils.types.Vectors.plus
import me.anno.utils.types.Vectors.times
import me.anno.utils.types.Vectors.toVec3f
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.*
import java.awt.image.BufferedImage
import java.lang.Math.toDegrees
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

// todo disable ui circles via some check-button at the top bar

// todo search elements
// todo search with tags
// todo search properties

// todo control click -> fullscreen view of this element?

// todo right click on input to get context menu, e.g. to reset

// todo key controls like in Blender:
// start command with s/g/...
// then specify axis if needed
// then say the number + change axis
// then press enter to apply the change

open class SceneView(style: Style) : PanelList(null, style.getChild("sceneView")), ISceneView {

    constructor(sceneView: SceneView) : this(DefaultConfig.style) {
        camera = sceneView.camera
        isLocked2D = sceneView.isLocked2D
        mode = sceneView.mode
    }

    companion object {
        private val LOGGER = LogManager.getLogger(SceneView::class)
    }

    init {

        weight = 1f
        backgroundColor = 0

    }

    var camera = nullCamera ?: Camera()

    override val usesFPBuffers: Boolean get() = camera.toneMapping != ToneMappers.RAW8
    override var isLocked2D = camera.rotationYXZ.isDefaultValue()

    val controls = ArrayList<SimplePanel>()

    val iconSize = style.getSize("fontSize", 12) * 2
    val pad = (iconSize + 4) / 8

    val borderThickness = style.getSize("blackWhiteBorderThickness", 2)

    // we need the depth for post processing effects like dof

    init {
        val is2DPanel = TextButton(
            if (isLocked2D) "2D" else "3D", "Lock the camera; use control to keep the angle",
            "ui.sceneView.3dSwitch", true, style
        )
        is2DPanel.instantTextLoading = true
        controls += SimplePanel(
            is2DPanel,
            true, true,
            pad, pad,
            iconSize
        ).setOnClickListener {
            isLocked2D = !isLocked2D
            // control can be used to avoid rotating the camera
            if (isLocked2D && !isControlDown) {
                val rot = camera.rotationYXZ
                val rot0z = rot[camera.lastLocalTime].z()
                camera.putValue(rot, Vector3f(0f, 0f, rot0z), true)
            }
            is2DPanel.text = if (isLocked2D) "2D" else "3D"
            invalidateDrawing()
        }
        fun add(i: Int, mode: SceneDragMode) {
            controls += SimplePanel(
                object : TextButton(mode.displayName, true, style) {
                    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                        draw(isHovered, mouseDown || mode == this@SceneView.mode)
                    }
                }.apply {
                    setTooltip(mode.description)
                },
                true, true,
                pad * 2 + iconSize * (i + 1), pad,
                iconSize
            ).setOnClickListener {
                this.mode = mode
                invalidateDrawing()
            }
        }
        add(0, SceneDragMode.MOVE)
        add(1, SceneDragMode.ROTATE)
        add(2, SceneDragMode.SCALE)
        controls += SimplePanel(
            TextButton(
                "\uD83D\uDCF7",
                "Take a screenshot", "ui.sceneView.takeScreenshot",
                true, style
            ),
            true, true,
            pad * 3 + iconSize * (3 + 1), pad,
            iconSize
        ).setOnClickListener {
            takeScreenshot()
        }
    }

    init {
        controls.forEach {
            children += it.drawable
        }
    }

    open fun onInteraction() {
        GFX.lastTouchedCamera = camera
    }

    override fun getVisualState(): Any? =
        Triple(editorTime, stableSize.stableWidth, stableSize.stableHeight) to
                Pair(Input.isKeyDown('l'), Input.isKeyDown('n'))

    override fun tickUpdate() {
        super.tickUpdate()
        parseKeyInput()
        parseTouchInput()
        claimResources()
        updateStableSize()
    }

    fun updateStableSize(){
        stableSize.updateSize(w - 2 * borderThickness, h - 2 * borderThickness, camera.onlyShowTarget)
    }

    var mode = SceneDragMode.MOVE
        set(value) {
            field = value
            val selectedTransform = selectedTransform
            if (selectedTransform != null) {
                Selection.select(
                    selectedTransform,
                    when (value) {
                        SceneDragMode.MOVE -> selectedTransform.position
                        SceneDragMode.SCALE -> selectedTransform.scale
                        SceneDragMode.ROTATE -> selectedTransform.rotationYXZ
                    }
                )
            }
        }

    var velocity = Vector3f()

    var inputDx = 0f
    var inputDy = 0f
    var inputDz = 0f

    // switch between manual control and autopilot for time :)
    // -> do this by disabling controls when playing, excepts when it's the inspector camera (?)
    val mayControlCamera get() = camera === nullCamera || isPaused

    fun claimResources() {
        // this is expensive, so do it only when the time changed
        val edt = editorTimeDilation
        val et = editorTime
        val loadedTimeSeconds = 3.0
        // load the next 3 seconds of data
        root.claimResources(et, et + loadedTimeSeconds * if (edt == 0.0) 1.0 else edt, 1f, 1f)
    }

    private val stableSize = StableWindowSize()

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        val mode = if (camera.toneMapping == ToneMappers.RAW8)
            Renderer.colorRenderer
        else
            Renderer.colorSqRenderer

        // GFX.drawMode = mode

        GFX.check()

        val bt = borderThickness
        val bth = bt / 2

        updateStableSize()

        val dx = stableSize.dx + borderThickness
        val dy = stableSize.dy + borderThickness

        drawRect(x, y, w, h, -1)
        drawRect(x + bth, y + bth, w - 2 * bth, h - 2 * bth, black)
        drawRect(x + bt, y + bt, w - 2 * bt, h - 2 * bt, deepDark)

        val wx = stableSize.stableWidth
        val wy = stableSize.stableHeight
        val rw = min(wx, w - 2 * bt)
        val rh = min(wy, h - 2 * bt)
        val x00 = x + dx
        val y00 = y + dy
        if (rw > 0 && rh > 0) {
            GFX.clip2(// why just -1*bt instead of -2*bt???
                max(x0, x00), max(y0, y00),
                min(x1, x00 + rw), min(y1, y00 + rh)
            ) {
                Scene.draw(
                    camera, root,
                    x00, y00, wx, wy,
                    editorTime, false,
                    mode, this
                )
            }
        }

        GFX.clip2(x0, y0, x1, y1) {

            renderDefault {
                for (control in controls) {
                    control.draw(x, y, w, h, x0, y0, x1, y1)
                }
            }

            super.onDraw(x0, y0, x1, y1)

        }

    }

    fun takeScreenshot() {

        val folder = OS.pictures.getChild("Screenshots")!!
        folder.mkdirs()

        val format = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")
        var name = format.format(Date())
        if (folder.getChild("$name.png")!!.exists) name += "_${System.nanoTime()}"
        name += ".png"
        if (folder.getChild(name)!!.exists) return // image already exists somehow...

        val w = stableSize.stableWidth
        val h = stableSize.stableHeight

        addGPUTask(w, h) {

            GFX.check()

            val fb: Framebuffer = FBStack["screenshot", w, h, 4, false, 8]

            GFX.check()

            fun getPixels(renderer: Renderer): IntArray {
                // draw only the clicked area?
                val buffer = IntArray(w * h)
                useFrame(fb, renderer) {
                    GFX.check()
                    Scene.draw(camera, root, 0, 0, w, h, editorTime, true, renderer, this)
                    fb.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                    useFrame(fb.msBuffer) {
                        Frame.bind()
                        glFlush(); glFinish() // wait for everything to be drawn
                        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
                        glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
                        GFX.check()
                    }
                }
                return buffer
            }

            GFX.check()

            val data = getPixels(Renderer.colorRenderer)
            thread {

                val image = BufferedImage(w, h, 1)

                for (i in data.indices) {
                    val abgr = data[i] // rgba, but little endian
                    val argb = rgba(abgr.b(), abgr.g(), abgr.r(), abgr.a())
                    image.raster.dataBuffer.setElem(i, argb)
                }

                // todo actions for console messages, e.g. opening a file
                val file = folder.getChild(name)
                use(file.outputStream()) { ImageIO.write(image, "png", it) }
                LOGGER.info(
                    Dict["Saved screenshot to %1", "ui.sceneView.savedScreenshot"].replace(
                        "%1",
                        file.toString()
                    )
                )

            }
        }

    }

    fun getPixels(
        camera: Camera,
        diameter: Int,
        localX: Int,
        localY: Int,
        width: Int,
        height: Int,
        fb: Framebuffer,
        renderer: Renderer
    ): IntArray {
        val buffer = IntArray(diameter * diameter)
        val dx = x + stableSize.dx
        val dy = y + stableSize.dy
        useFrame(dx, dy, fb.w, fb.h, false, fb, renderer) {
            val radius = diameter / 2
            val x0 = max(localX - radius, 0)
            val y0 = max(localY - radius, 0)
            val x1 = min(localX + radius + 1, fb.w)
            val y1 = min(localY + radius + 1, fb.h)
            Frame.bind()
            // draw only the clicked area
            RenderState.scissorTest.use(true) {
                glScissor(x0, y0, x1 - x0, y1 - y0)
                Scene.draw(camera, root, dx, dy, width, height, editorTime, false, renderer, this)
                glFlush(); glFinish() // wait for everything to be drawn
                glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
                glReadPixels(x0, y0, x1 - x0, y1 - y0, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
            }
        }
        return buffer
    }

    fun resolveClick(clickX: Float, clickY: Float, width: Int, height: Int, callback: (Transform?) -> Unit) {

        val camera = camera
        GFX.check()

        val fb: Framebuffer = FBStack["resolveClick", GFX.width, GFX.height, 4, false, 1]

        val radius = 2
        val diameter = radius * 2 + 1

        val localX = clickX.toInt()
        val localY = GFX.height - clickY.toInt()

        val idBuffer = getPixels(camera, diameter, localX, localY, width, height, fb, Renderer.idRenderer)
        val depthBuffer = getPixels(camera, diameter, localX, localY, width, height, fb, Renderer.depthRenderer01)

        val depthImportance = 10
        var bestDistance = 256 * depthImportance + diameter * diameter
        var bestResult = 0

        // sometimes the depth buffer seems to contain copies of the idBuffer -.-
        // still, in my few tests, it seemed to have worked :)
        // (clicking on the camera line in front of a cubemap)
        // LOGGER.info(idBuffer.joinToString { it.toUInt().toString(16) })
        // LOGGER.info(depthBuffer.joinToString { it.toUInt().toString(16) })

        // convert that color to an id
        for ((index, value) in idBuffer.withIndex()) {
            val depth = depthBuffer[index] and 255
            val result = value.and(0xffffff)
            val x = (index % diameter) - radius
            val y = (index / diameter) - radius
            val distance = depth * depthImportance + x * x + y * y
            val isValid = result > 0
            if (isValid && distance <= bestDistance) {
                bestDistance = distance
                bestResult = result
            }
        }

        // find the transform with the id to select it
        if (bestResult > 0) {
            var transform = root.listOfAll.firstOrNull { it.clickId == bestResult }
            if (transform == null) {// transformed, so it works without project as well
                val nullCamera = project?.nullCamera
                if (nullCamera != null && nullCamera.clickId == bestResult) {
                    transform = nullCamera
                }
            }
            callback(transform)
            // selectTransform(transform)
        } else {
            callback(null)
            // selectTransform(null)
        }
        GFX.check()

    }

    // todo camera movement in orthographic view is a bit broken

    fun parseKeyInput() {

        if (!mayControlCamera) return

        val dt = clamp(deltaTime, 0f, 0.1f)

        move(dt)

    }

    fun moveDirectly(dx: Float, dy: Float, dz: Float) {
        val defaultFPS = 60f
        val dt = 0.2f
        val scale = defaultFPS * dt
        this.inputDx += dx * scale
        this.inputDy += dy * scale
        this.inputDz += dz * scale
    }

    fun move(dt: Float) {

        val camera = camera
        val (cameraTransform, cameraTime) = camera.getGlobalTransformTime(editorTime)

        val radius = camera.orbitRadius[cameraTime]
        val speed = if (radius == 0f) 1f else 0.1f + 0.9f * radius
        val acceleration = Vector3f(inputDx, inputDy, inputDz).mul(speed)

        velocity.mul(1f - dt)
        velocity.mulAdd(dt, acceleration)

        if (velocity.x != 0f || velocity.y != 0f || velocity.z != 0f) {
            val oldPosition = camera.position[cameraTime]
            val step = (velocity * dt)
            val step2 = cameraTransform.transformDirection(step)
            val newPosition = oldPosition + step2
            if (camera == nullCamera) {
                camera.position.addKeyframe(cameraTime, newPosition, 0.01)
            } else {
                RemsStudio.incrementalChange("Move Camera") {
                    camera.position.addKeyframe(cameraTime, newPosition, 0.01)
                }
            }
            invalidateDrawing()
        }

        // todo if camera.isOrthographic, then change fov instead of moving forward/backward

        inputDx = 0f
        inputDy = 0f
        inputDz = 0f

    }

    fun parseTouchInput() {

        if (!mayControlCamera) return

        // todo rotate/move our camera or the selected object?
        val size = -20f * shiftSlowdown / GFX.height
        when (touches.size) {
            2 -> {
                val first = touches.first()
                if (contains(first.x, first.y)) {
                    // this gesture started on this view -> this is our gesture
                    // rotating is the hardest on a touchpad, because we need to click right
                    // -> rotation
                    // axes: angle, zoom,
                    val dx = sumOf(touches) { it.x - it.lastX } * size * 0.5f
                    val dy = sumOf(touches) { it.y - it.lastY } * size * 0.5f

                    val t0 = touches[0]
                    val t1 = touches[1]

                    val d1 = length(t1.x - t0.x, t1.y - t0.y)
                    val d0 = length(t1.lastX - t0.lastX, t1.lastY - t0.lastY)

                    val minDistance = 10
                    if (d1 > minDistance && d0 > minDistance) {
                        val time = cameraTime
                        val oldCamZoom = camera.orbitRadius[time]
                        if (oldCamZoom == 0f) {
                            // todo delta zoom for cameras without orbit
                        } else {
                            val newZoom = oldCamZoom * d0 / d1
                            camera.putValue(camera.orbitRadius, newZoom, false)
                        }
                    }

                    val (_, time) = camera.getGlobalTransformTime(editorTime)
                    val old = camera.rotationYXZ[time]
                    val rotationSpeed = -10f
                    if (!isLocked2D) {
                        camera.rotationYXZ.addKeyframe(time, old + Vector3f(dy * rotationSpeed, dx * rotationSpeed, 0f))
                    } else {
                        // move camera? completely ignore, what is selected
                    }
                    touches.forEach { it.update() }
                    invalidateDrawing()
                }
            }
            3 -> {
                // very slow... but we can move around with a single finger, so it shouldn't matter...
                // move the camera around
                val first = touches.first()
                val speed = 10f / 3f
                if (contains(first.x, first.y)) {
                    val dx = speed * sumOf(touches) { it.x - it.lastX } * size
                    val dy = speed * sumOf(touches) { it.y - it.lastY } * size
                    move(camera, dx, dy)
                    touches.forEach { it.update() }
                    invalidateDrawing()
                }
            }
        }
    }

    val global2normUI = Matrix4fArrayList()

    private val global2target = Matrix4f()
    private val camera2target = Matrix4f()
    private val target2camera = Matrix4f()

    fun move(selected: Transform, dx0: Float, dy0: Float) {

        if (!mayControlCamera) return
        if (dx0 == 0f && dy0 == 0f) return

        val (target2global, localTime) = (selected.parent ?: selected).getGlobalTransformTime(editorTime)

        val camera = camera
        val (camera2global, cameraTime) = camera.getGlobalTransformTime(editorTime)

        global2normUI.clear()
        GFX.applyCameraTransform(camera, cameraTime, camera2global, global2normUI)

        // val inverse = Matrix4f(global2normUI).invert()

        val global2target = global2target.set(target2global).invert()

        // transforms: global to local
        // ->
        // camera local to global, then global to local
        //      obj   cam
        // v' = G2L * L2G * v
        val camera2target = camera2target.set(camera2global).mul(global2target)
        val target2camera = target2camera.set(camera2target).invert()

        // where the object is on screen
        val targetZonUI = target2camera.transform(Vector4f(0f, 0f, 0f, 1f)).toVec3f()
        val targetZ = -targetZonUI.z
        val shiftSlowdown = shiftSlowdown
        val speed = shiftSlowdown * 2 * targetZ / h * pow(0.02f, camera.orthographicness[cameraTime])
        val dx = dx0 * speed
        val dy = dy0 * speed
        val pos1 = camera2target.transform(
            Vector4f(targetZonUI.x + dx, targetZonUI.y - dy, targetZonUI.z, 1f)
        ).toVec3f()

        val delta0 = dx0 - dy0
        val delta = dx - dy

        when (mode) {
            SceneDragMode.MOVE -> {

                // todo find the (truly) correct speed...
                // depends on FOV, camera and object transform

                val oldPosition = selected.position[localTime]
                val localDelta = if (isControlDown)
                    camera2target.transformDirection(Vector3f(0f, 0f, -delta)) * (targetZ / 6)
                else pos1
                invalidateDrawing()
                RemsStudio.incrementalChange("Move Object") {
                    selected.position.addKeyframe(localTime, oldPosition + localDelta)
                    RemsStudio.updateSceneViews()
                }

            }
            SceneDragMode.SCALE -> {
                val speed2 = 1f / h
                val oldScale = selected.scale[localTime]
                val localDelta = target2camera.transformDirection(
                    if (isControlDown) Vector3f(dx0, dy0, 0f)
                    else Vector3f(delta0)
                )
                val base = 2f
                invalidateDrawing()
                RemsStudio.incrementalChange("Scale Object") {
                    selected.scale.addKeyframe(
                        localTime, Vector3f(
                            oldScale.x() * pow(base, localDelta.x * speed2),
                            oldScale.y() * pow(base, localDelta.y * speed2),
                            oldScale.z() * pow(base, localDelta.z * speed2)
                        )
                    )
                    RemsStudio.updateSceneViews()
                }
            }
            SceneDragMode.ROTATE -> {
                // todo transform rotation??? quaternions...
                val centerX = x + w / 2
                val centerY = y + h / 2
                val mdx = (mouseX - centerX).toDouble()
                val mdy = (mouseY - centerY).toDouble()
                val oldDegree = toDegrees(atan2(mdy - dy0, mdx - dx0)).toFloat()
                val newDegree = toDegrees(atan2(mdy, mdx)).toFloat()
                val deltaDegree = newDegree - oldDegree
                val speed2 = 20f / h
                val oldRotation = selected.rotationYXZ[localTime]
                val localDelta =
                    if (isControlDown) Vector3f(dx0 * speed2, -dy0 * speed2, 0f)
                    else Vector3f(0f, 0f, -deltaDegree)
                invalidateDrawing()
                RemsStudio.incrementalChange("Rotate Object") {
                    selected.rotationYXZ.addKeyframe(localTime, oldRotation + localDelta)
                    RemsStudio.updateSceneViews()
                }
            }
        }

    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        // fov is relative to height -> modified to depend on height
        val size = 20f * shiftSlowdown / GFX.height
        val dx0 = dx * size
        val dy0 = dy * size
        // move stuff, if mouse is down and no touch is down
        if (0 in mouseKeysDown && touches.size < 2) {
            // move the object
            val selected = selectedTransform
            if (selected != null && selected != camera) {
                move(selected, dx, dy)
            } else {
                moveDirectly(-dx0, +dy0, 0f)
            }
        }
    }

    fun turn(dx: Float, dy: Float) {
        if (!mayControlCamera) return
        if (isLocked2D) return
        // move the camera
        val size = 20f * shiftSlowdown * (if (selectedTransform is Camera) -1f else 1f) / max(GFX.width, GFX.height)
        val dx0 = dx * size
        val dy0 = dy * size
        val scaleFactor = -10f
        val camera = camera
        val cameraTime = cameraTime
        val oldRotation = camera.rotationYXZ[cameraTime]
        // if(camera.orthographicness[cameraTime] > 0.5f) scaleFactor = -scaleFactor
        invalidateDrawing()
        RemsStudio.incrementalChange("Turn Camera") {
            camera.putValue(camera.rotationYXZ, oldRotation + Vector3f(dy0 * scaleFactor, dx0 * scaleFactor, 0f), false)
            RemsStudio.updateSceneViews()
        }
    }

    val cameraTime get() = camera.getGlobalTransformTime(editorTime).second
    val firstCamera get() = root.listOfAll.filterIsInstance<Camera>().firstOrNull()

    fun rotateCameraTo(rotation: Vector3f) {
        camera.putValue(camera.rotationYXZ, rotation, true)
    }

    fun rotateCamera(delta: Vector3f) {
        val oldRot = camera.rotationYXZ[cameraTime]
        camera.putValue(
            camera.rotationYXZ,
            oldRot + delta,
            true
        )
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "SetMode(MOVE)" -> {
                mode = SceneDragMode.MOVE
                invalidateDrawing()
            }
            "SetMode(SCALE)" -> {
                mode = SceneDragMode.SCALE
                invalidateDrawing()
            }
            "SetMode(ROTATE)" -> {
                mode = SceneDragMode.ROTATE
                invalidateDrawing()
            }
            "Cam0", "ResetCamera" -> {
                val firstCamera = firstCamera
                if (firstCamera == null) {
                    camera.resetTransform(true)
                } else {
                    // copy the transform
                    val firstCameraTime = firstCamera.getGlobalTransformTime(editorTime).second
                    RemsStudio.largeChange("Reset Camera") {
                        camera.cloneTransform(firstCamera, firstCameraTime)
                    }
                }
                invalidateDrawing()
            }
            "Cam5" -> {// switch between orthographic and perspective
                camera.putValue(camera.orthographicness, 1f - camera.orthographicness[cameraTime], true)
            }
            // todo control + numpad does not work
            "Cam1" -> rotateCameraTo(Vector3f(0f, if (isControlDown) 180f else 0f, 0f))// default view
            "Cam3" -> rotateCameraTo(Vector3f(0f, if (isControlDown) -90f else +90f, 0f))// rotate to side view
            "Cam7" -> rotateCameraTo(Vector3f(if (isControlDown) +90f else -90f, 0f, 0f)) // look from above
            "Cam4" -> rotateCamera(
                if (isShiftDown) {// left
                    Vector3f(0f, 0f, -15f)
                } else {
                    Vector3f(0f, -15f, 0f)
                }
            )
            "Cam6" -> rotateCamera(
                if (isShiftDown) {// right
                    Vector3f(0f, 0f, +15f)
                } else {
                    Vector3f(0f, +15f, 0f)
                }
            )
            "Cam8" -> rotateCamera(Vector3f(-15f, 0f, 0f)) // up
            "Cam2" -> rotateCamera(Vector3f(+15f, 0f, 0f)) // down
            "Cam9" -> rotateCamera(Vector3f(0f, 180f, 0f)) // look at back; rotate by 90 degrees on y axis
            "MoveLeft" -> this.inputDx--
            "MoveRight" -> this.inputDx++
            "MoveUp" -> this.inputDy++
            "MoveDown" -> this.inputDy--
            "MoveForward" -> this.inputDz--
            "MoveBackward", "MoveBack" -> this.inputDz++
            "Turn" -> turn(dx, dy)
            "TurnLeft" -> turn(-1f, 0f)
            "TurnRight" -> turn(1f, 0f)
            "TurnUp" -> turn(0f, -1f)
            "TurnDown" -> turn(0f, 1f)
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    fun goFullscreen() {
        // don't open, if it's already fullscreen
        if (windowStack.peek()?.panel !is SceneView) {
            val view = SceneView(this)
            val window = Window(view)
            windowStack.push(window)
        }
    }

    fun resolveClick(x: Float, y: Float, onClick: (Transform?) -> Unit) {
        val w = stableSize.stableWidth
        val h = stableSize.stableHeight
        addGPUTask(w, h) {
            resolveClick(x, y, w, h, onClick)
        }
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        onInteraction()
        invalidateDrawing()
        if (button.isLeft) {
            val xi = x.toInt()
            val yi = y.toInt()
            for (it in controls) {
                if (it.contains(xi, yi)) {
                    it.drawable.onMouseClicked(x, y, button, false)
                    return
                }
            }
            // goFullscreen()
            // zoom on that object instead
            resolveClick(x, y) {
                if (it != null) {
                    zoomToObject(it)
                }
            }
        }
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        onInteraction()
        invalidateDrawing()
        if ((parent as? CustomContainer)?.clicked(x, y) != true) {

            var isProcessed = false
            val xi = x.toInt()
            val yi = y.toInt()
            controls.forEach {
                if (it.contains(xi, yi)) {
                    it.drawable.onMouseClicked(x, y, button, long)
                    isProcessed = true
                }
            }

            if (!isProcessed && wasInFocus) {
                resolveClick(x, y) {
                    selectTransform(it)
                    invalidateDrawing()
                }
            }
        }
    }

    // sadly doesn't work well; glfw/windows cursor is only changed when moved
    /*override fun getCursor() = when(mode){
        TransformMode.MOVE -> Cursor.drag
        TransformMode.SCALE -> if(Input.isShiftDown) Cursor.vResize else Cursor.hResize
        TransformMode.ROTATE -> Cursor.crossHair
        else -> null
    }*/

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when (type) {
            "Transform" -> {
                val original = dragged?.getOriginal() ?: return
                if (original is Camera) {
                    RemsStudio.largeChange("Changed Scene-View Camera to ${original.name}") {
                        camera = original
                    }
                }// else focus?
                invalidateDrawing()
            }
            // file -> paste object from file?
            // paste that object 1m in front of the camera?
            else -> super.onPaste(x, y, data, type)
        }
    }

    override fun onEmpty(x: Float, y: Float) {
        onInteraction()
        deleteSelectedTransform()
    }

    override fun onDeleteKey(x: Float, y: Float) {
        onInteraction()
        deleteSelectedTransform()
    }

    fun deleteSelectedTransform() {
        invalidateDrawing()
        RemsStudio.largeChange("Deleted Component") {
            selectedTransform?.destroy()
        }
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        files.forEach { file -> addChildFromFile(root, file, FileContentImporter.SoftLinkMode.ASK, true) { } }
        invalidateDrawing()
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        onInteraction()
        invalidateDrawing()
        RemsStudio.incrementalChange("Zoom In / Out") {
            val radius = camera.orbitRadius[cameraTime]
            if (radius == 0f) {
                // no orbiting
                moveDirectly(0f, 0f, -0.5f * dy)
            } else {
                val delta = -dy * shiftSlowdown
                val factor = pow(1.02f, delta)
                val newOrbitDistance = radius * factor
                camera.putValue(camera.orbitRadius, newOrbitDistance, false)
                if (camera == nullCamera) {
                    camera.putValue(camera.farZ, camera.farZ[cameraTime] * factor, false)
                    camera.putValue(camera.nearZ, camera.nearZ[cameraTime] * factor, false)
                }
            }
        }
    }

}