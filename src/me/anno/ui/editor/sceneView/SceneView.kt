package me.anno.ui.editor.sceneView

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.config.DefaultStyle.deepDark
import me.anno.gpu.GFX
import me.anno.gpu.GFX.deltaTime
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.GFX.select
import me.anno.gpu.GFX.windowStack
import me.anno.gpu.GFXx2D.drawRect
import me.anno.gpu.Window
import me.anno.gpu.blending.BlendDepth
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.ShaderPlus
import me.anno.input.Input
import me.anno.input.Input.mouseKeysDown
import me.anno.input.MouseButton
import me.anno.input.Touch.Companion.touches
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.objects.effects.ToneMappers
import me.anno.studio.RemsStudio.onSmallChange
import me.anno.studio.Scene
import me.anno.studio.RemsStudio.editorTime
import me.anno.studio.RemsStudio.editorTimeDilation
import me.anno.studio.RemsStudio.isPaused
import me.anno.studio.RemsStudio.nullCamera
import me.anno.studio.RemsStudio.root
import me.anno.studio.RemsStudio.selectedTransform
import me.anno.studio.RemsStudio.targetHeight
import me.anno.studio.RemsStudio.targetWidth
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.studio.StudioBase.Companion.shiftSlowdown
import me.anno.ui.base.ButtonPanel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelList
import me.anno.ui.custom.CustomContainer
import me.anno.ui.custom.data.CustomPanelData
import me.anno.ui.custom.data.ICustomDataCreator
import me.anno.ui.editor.files.addChildFromFile
import me.anno.ui.simple.SimplePanel
import me.anno.ui.style.Style
import me.anno.utils.*
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.*
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// done scene tabs
// todo scene selection
// done open/close scene tabs
// todo render a scene
// todo include scenes in large scene...

// todo search elements
// todo search with tags
// todo tags for elements
// todo search properties

// todo control click -> fullscreen view of this element?

// todo show the current mode with the cursor

// todo right click on input to get context menu, e.g. to reset
// todo switch animatedproperty when selecting another object

// todo key controls like in Blender:
// start command with s/g/...
// then specify axis if needed
// then say the number + change axis
// then press enter to apply the change

class SceneView(style: Style) : PanelList(null, style.getChild("sceneView")), ISceneView, ICustomDataCreator {

    constructor(sceneView: SceneView) : this(DefaultConfig.style) {
        camera = sceneView.camera
        isLocked2D = sceneView.isLocked2D
        mode = sceneView.mode
    }

    init {

        weight = 1f
        backgroundColor = 0

    }

    var camera = nullCamera

    override val usesFPBuffers: Boolean get() = camera.toneMapping != ToneMappers.RAW8
    override var isLocked2D = false

    val controls = ArrayList<SimplePanel>()

    val pad = 3
    val iconSize = 32

    // we need the depth for post processing effects like dof

    init {
        val is2DPanel = ButtonPanel("2D", style)
        is2DPanel.instantTextLoading = true
        controls += SimplePanel(
            is2DPanel,
            true, true,
            pad, pad,
            iconSize
        ).setOnClickListener {
            isLocked2D = !isLocked2D
            // control can be used to avoid rotating the camera
            if (isLocked2D && !Input.isControlDown) {
                val rot = camera.rotationYXZ
                val rot0z = rot[camera.lastLocalTime].z
                camera.putValue(rot, Vector3f(0f, 0f, rot0z))
            }
            is2DPanel.text = if (isLocked2D) "3D" else "2D"
            invalidateDrawing()
        }
        fun add(i: Int, name: String, mode: SceneDragMode){
            controls += SimplePanel(
                object: ButtonPanel(name, style){
                    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                        draw(isHovered, mouseDown || mode == this@SceneView.mode)
                    }
                },
                true, true,
                pad * 2 + iconSize * (i + 1), pad,
                iconSize
            ).setOnClickListener {
                this.mode = mode
                invalidateDrawing()
            }
        }
        add(0, "M", SceneDragMode.MOVE)
        add(1, "R", SceneDragMode.ROTATE)
        add(2, "S", SceneDragMode.SCALE)
        controls.forEach {
            children += it.drawable
        }
    }

    override fun getVisualState(): Any? = Quad(super.getVisualState(), editorTime, goodW, goodH)

    override fun tickUpdate() {
        super.tickUpdate()
        parseKeyInput()
        parseTouchInput()
        claimResources()
        updateSize()
    }

    var mode = SceneDragMode.MOVE

    var velocity = Vector3f()

    var inputDx = 0f
    var inputDy = 0f
    var inputDz = 0f

    // switch between manual control and autopilot for time :)
    // -> do this by disabling controls when playing, excepts when it's the inspector camera (?)
    val mayControlCamera get() = camera === nullCamera || isPaused
    var lastW = 0
    var lastH = 0
    var lastSizeUpdate = GFX.lastTime
    var goodW = 0
    var goodH = 0

    fun claimResources(){
        // this is expensive, so do it only when the time changed
        val edt = editorTimeDilation
        val et = editorTime
        // load the next five seconds of data
        root.claimResources(et, et + 5.0 * if (edt == 0.0) 1.0 else edt, 1f, 1f)
    }

    var dx = 0
    var dy = 0
    var rw = 0
    var rh = 0

    fun updateSize(){

        dx = 0
        dy = 0
        rw = w
        rh = h

        val camera = camera
        if (camera.onlyShowTarget) {
            if (w * targetHeight > targetWidth * h) {
                rw = h * targetWidth / targetHeight
                dx = (w - rw) / 2
            } else {
                rh = w * targetHeight / targetWidth
                dy = (h - rh) / 2
            }
        }

        // check if the size stayed the same;
        // because resizing all framebuffers is expensive (causes lag)
        val matchesSize = lastW == rw && lastH == rh
        val wasNotRecentlyUpdated = lastSizeUpdate + 1e8 < GFX.lastTime
        if (matchesSize) {
            if (wasNotRecentlyUpdated) {
                goodW = rw
                goodH = rh
            }
        } else {
            lastSizeUpdate = GFX.lastTime
            lastW = rw
            lastH = rh
        }

        if (goodW == 0 || goodH == 0) {
            goodW = rw
            goodH = rh
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        val mode =
            if (camera.toneMapping == ToneMappers.RAW8) ShaderPlus.DrawMode.COLOR
            else ShaderPlus.DrawMode.COLOR_SQUARED

        GFX.drawMode = mode

        GFX.check()

        // calculate the correct size, such that we miss nothing
        // todo ideally we could zoom in the image etc...
        // todo only do this, if we are appended to a camera :), not if we are building a 3D scene

        drawRect(x, y, w, h, deepDark)

        // preload resources :)
        // e.g. for video playback
        // we maybe could disable next frame fetching in Cache.kt...
        // todo doesn't work for auto-scaled videos... other plan?...

        drawRect(x + dx, y + dy, rw, rh, black)
        if(goodW > 0 && goodH > 0){
            Scene.draw(
                camera,
                x + dx, y + dy, goodW, goodH,
                editorTime, false,
                mode, this
            )
        }

        GFX.clip(x0, y0, x1, y1){

            BlendDepth(BlendMode.DEFAULT, false){
                controls.forEach {
                    it.draw(x, y, w, h, x0, y0, x1, y1)
                }
            }

            super.onDraw(x0, y0, x1, y1)

        }

    }

    fun resolveClick(clickX: Float, clickY: Float, rw: Int, rh: Int) {

        val camera = camera
        GFX.check()

        val fb: Framebuffer = FBStack["resolveClick", rw, rh, 1, false]
        val width = fb.w
        val height = fb.h

        val radius = 2
        val diameter = radius * 2 + 1

        fun getPixels(mode: ShaderPlus.DrawMode): IntArray {
            // draw only the clicked area?
            val buffer = IntArray(diameter * diameter)
            Frame(fb){
                Scene.draw(camera, 0, 0, rw, rh, editorTime, false, mode, this)
                GFX.check()
                val localX = (clickX - this.x).roundToInt()
                val localH = fb.h
                val localY = localH - 1 - (clickY - this.y).roundToInt()
                glFlush(); glFinish() // wait for everything to be drawn
                glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
                glReadPixels(
                    max(localX - radius, 0),
                    max(localY - radius, 0),
                    min(diameter, width),
                    min(diameter, height),
                    GL_RGBA, GL_UNSIGNED_BYTE, buffer
                )
            }
            return buffer
        }

        val idBuffer = getPixels(ShaderPlus.DrawMode.ID)
        val depthBuffer = getPixels(ShaderPlus.DrawMode.DEPTH)

        val depthImportance = 10
        var bestDistance = 256 * depthImportance + diameter * diameter
        var bestResult = 0

        // sometimes the depth buffer seems to contain copies of the idBuffer -.-
        // still, in my few tests, it seemed to have worked :)
        // (clicking on the camera line in front of a cubemap)
        // (idBuffer.joinToString { it.toUInt().toString(16) })
        // (depthBuffer.joinToString { it.toUInt().toString(16) })

        // convert that color to an id
        idBuffer.forEachIndexed { index, value ->
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
            val transform = (root.listOfAll + nullCamera).firstOrNull { it.clickId == bestResult }
            select(transform)
        } else select(null)
        GFX.check()

        invalidateDrawing()

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

        val acceleration = Vector3f(inputDx, inputDy, inputDz)

        velocity.mul(1f - dt)
        velocity.mulAdd(dt, acceleration)

        if (velocity.x != 0f || velocity.y != 0f || velocity.z != 0f) {
            val camera = camera
            val (cameraTransform, cameraTime) = camera.getGlobalTransform(editorTime)
            val oldPosition = camera.position[cameraTime]
            val step = (velocity * dt)
            val step2 = cameraTransform.transformDirection(step)
            // todo transform into the correct space: from that camera to this camera
            val newPosition = oldPosition + step2
            camera.position.addKeyframe(cameraTime, newPosition, 0.01)
            if(camera != nullCamera) onSmallChange("camera-move")
            invalidateDrawing()
        }

        // todo if camera.isOrthographic, then change fov instead of moving forward/backward

        inputDx = 0f
        inputDy = 0f
        inputDz = 0f

    }

    var lastTouchZoom = 0f
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
                    // rotate
                    // todo zoom
                    val dx = touches.sumByFloat { it.x - it.lastX } * size * 0.5f
                    val dy = touches.sumByFloat { it.y - it.lastY } * size * 0.5f
                    val (_, time) = camera.getGlobalTransform(editorTime)
                    val old = camera.rotationYXZ[time]
                    val rotationSpeed = -10f
                    camera.rotationYXZ.addKeyframe(time, old + Vector3f(dy * rotationSpeed, dx * rotationSpeed, 0f))
                    touches.forEach { it.update() }
                    invalidateDrawing()
                }
            }
            3 -> {
                // todo move the camera around? :)
                val first = touches.first()
                if (contains(first.x, first.y)) {
                    val dx = touches.sumByFloat { it.x - it.lastX } * size * 0.333f
                    val dy = touches.sumByFloat { it.y - it.lastY } * size * 0.333f
                    move(camera, dx, dy)
                    touches.forEach { it.update() }
                    invalidateDrawing()
                }
            }
        }
    }

    val global2normUI = Matrix4fArrayList()
    fun move(selected: Transform, dx0: Float, dy0: Float) {

        if (!mayControlCamera) return

        val (target2global, localTime) = (selected.parent ?: selected).getGlobalTransform(editorTime)

        val camera = camera
        val (camera2global, cameraTime) = camera.getGlobalTransform(editorTime)

        global2normUI.clear()
        GFX.applyCameraTransform(camera, cameraTime, camera2global, global2normUI)

        // val inverse = Matrix4f(global2normUI).invert()

        val global2target = Matrix4f(target2global).invert()

        // transforms: global to local
        // ->
        // camera local to global, then global to local
        //      obj   cam
        // v' = G2L * L2G * v
        val camera2target = Matrix4f(camera2global).mul(global2target)
        val target2camera = Matrix4f(camera2target).invert()

        // where the object is on screen
        val targetZonUI = target2camera.transform(Vector4f(0f,0f,0f,1f)).toVec3f()
        val targetZ = -targetZonUI.z
        val shiftSlowdown = shiftSlowdown
        val speed = shiftSlowdown * 2 * targetZ / h
        val dx = dx0 * speed
        val dy = dy0 * speed
        val pos1 = camera2target.transform(
            Vector4f(targetZonUI.x+dx, targetZonUI.y-dy, targetZonUI.z, 1f)
        ).toVec3f()

        val delta = dx - dy

        when (mode) {
            SceneDragMode.MOVE -> {

                // todo find the (truly) correct speed...
                // depends on FOV, camera and object transform

                // val camPos = camera2global.transformPosition(Vector3f())
                // val targetPos = target2global.transformPosition(Vector3f())
                // val uiZ = camPos.distance(targetPos)

                val oldPosition = selected.position[localTime]
                /*val localDelta = camera2target.transformDirection(
                    if (Input.isControlDown) Vector3f(0f, 0f, -delta)
                    else Vector3f(dx0, -dy0, 0f)
                ) * (uiZ / 6) // why ever 1/6...*/
                val localDelta = if (Input.isControlDown)
                    camera2target.transformDirection(Vector3f(0f, 0f, -delta)) * (targetZ / 6)
                else pos1
                selected.position.addKeyframe(localTime, oldPosition + localDelta)
                if (selected != nullCamera) onSmallChange("SceneView-move")
                invalidateDrawing()

            }
            SceneDragMode.SCALE -> {
                val oldScale = selected.scale[localTime]
                val localDelta = target2camera.transformDirection(
                    if(Input.isControlDown) Vector3f(0f, 0f, -delta)
                    else Vector3f(dx, dy, 0f)
                )
                val base = 2f
                selected.scale.addKeyframe(localTime, Vector3f(
                    oldScale.x * pow(base, localDelta.x),
                    oldScale.y * pow(base, localDelta.y),
                    oldScale.z * pow(base, localDelta.z)))
                if (selected != nullCamera) onSmallChange("SceneView-move")
                invalidateDrawing()
            }
            /*TransformMode.ROTATE -> {
                // todo transform rotation??? quaternions...
                val oldScale = selected.scale[localTime]
                val localDelta = global2ui.transformDirection(
                    if(Input.isControlDown) Vector3f(0f, 0f, -delta)
                    else Vector3f(dx0, -dy0, 0f)
                )
                selected.scale.addKeyframe(localTime, oldScale + localDelta)
            }*/
        }

    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        // fov is relative to height -> modified to depend on height
        val size = 20f * shiftSlowdown * (if (selectedTransform === camera) -1f else 1f) / GFX.height
        val dx0 = dx * size
        val dy0 = dy * size
        // move stuff, if mouse is down and no touch is down
        if (0 in mouseKeysDown && touches.size < 2) {
            // move the object
            val selected = selectedTransform
            if (selected != null) {
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
        val (_, cameraTime) = camera.getGlobalTransform(editorTime)
        val oldRotation = camera.rotationYXZ[cameraTime]
        camera.putValue(camera.rotationYXZ, oldRotation + Vector3f(dy0 * scaleFactor, dx0 * scaleFactor, 0f))
        if(camera != nullCamera) onSmallChange("SceneView-turn")
        invalidateDrawing()
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
            "ResetCamera" -> {
                camera.resetTransform()
                invalidateDrawing()
            }
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

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {

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
            goFullscreen()
        }
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {

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

            if (!isProcessed) {
                var rw = w
                var rh = h
                var dx = 0
                var dy = 0

                GFX.addGPUTask(w, h) {
                    val camera = camera
                    if (camera.onlyShowTarget) {
                        if (w * targetHeight > targetWidth * h) {
                            rw = h * targetWidth / targetHeight
                            dx = (w - rw) / 2
                        } else {
                            rh = w * targetHeight / targetWidth
                            dy = (h - rh) / 2
                        }
                    }
                    resolveClick(x - dx, y - dy, rw, rh)
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
                    camera = original
                }// else focus?
                invalidateDrawing()
            }
            // file -> paste object from file?
            // paste that object 1m in front of the camera?
            else -> super.onPaste(x, y, data, type)
        }
    }

    override fun onEmpty(x: Float, y: Float) {
        deleteSelectedTransform()
    }

    override fun onDeleteKey(x: Float, y: Float) {
        deleteSelectedTransform()
    }

    fun deleteSelectedTransform() {
        selectedTransform?.destroy()
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<File>) {
        files.forEach { file ->
            addChildFromFile(root, file, { })
        }
        invalidateDrawing()
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        moveDirectly(0f, 0f, -dy)
    }

    override fun toData() = CustomPanelData(this)

}