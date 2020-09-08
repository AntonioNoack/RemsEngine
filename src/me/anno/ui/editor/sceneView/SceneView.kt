package me.anno.ui.editor.sceneView

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.deepDark
import me.anno.gpu.GFX
import me.anno.gpu.GFX.deltaTime
import me.anno.gpu.GFX.select
import me.anno.gpu.GFX.windowStack
import me.anno.gpu.Window
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.ShaderPlus
import me.anno.input.Input
import me.anno.input.Input.mouseKeysDown
import me.anno.input.MouseButton
import me.anno.input.Touch.Companion.touches
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.objects.blending.BlendMode
import me.anno.studio.Scene
import me.anno.studio.Studio
import me.anno.studio.Studio.dragged
import me.anno.studio.Studio.editorTime
import me.anno.studio.Studio.nullCamera
import me.anno.studio.Studio.root
import me.anno.studio.Studio.selectedTransform
import me.anno.studio.Studio.shiftSlowdown
import me.anno.studio.Studio.targetHeight
import me.anno.studio.Studio.targetWidth
import me.anno.ui.base.groups.PanelFrame
import me.anno.ui.editor.CustomContainer
import me.anno.ui.simple.SimplePanel
import me.anno.ui.style.Style
import me.anno.utils.clamp
import me.anno.utils.plus
import me.anno.utils.sumByFloat
import me.anno.utils.times
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// todo search elements
// todo search with tags
// todo tags for elements
// todo search properties

// todo control click -> fullscreen view of this element?

// todo show the current mode with the cursor

// todo right click on input to get context menu, e.g. to reset

class SceneView(style: Style): PanelFrame(null, style.getChild("sceneView")){

    constructor(sceneView: SceneView): this(DefaultConfig.style){
        camera = sceneView.camera
        isLocked2D = sceneView.isLocked2D
        mode = sceneView.mode
    }

    init {

        weight = 1f
        backgroundColor = 0

    }

    var camera = nullCamera
    var isLocked2D = false

    val controls = ArrayList<SimplePanel>()

    val iconSize = 32
    val iconBorder = 2

    // we need the depth for post processing effects like dof

    init {
        /*controls += SimplePanel(
            IconPanel("checked.png", style),
            true, true,
            3, 3,
            iconSize
        ).setOnClickListener {

        }*/
        /*controls += SimplePanel(
            TextPanel("H", style),
            true, true,
            3 + iconSize + iconBorder, 3,
            iconSize
        )*/
    }

    var mode = TransformMode.MOVE

    var velocity = Vector3f()

    var dx = 0
    var dy = 0
    var dz = 0

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {

        GFX.ensureEmptyStack()

        // todo switch between manual control and autopilot for time :)

        GFX.check()

        parseKeyInput()
        parseTouchInput()

        // calculate the correct size, such that we miss nothing
        // todo ideally we could zoom in the image etc...
        // todo only do this, if we are appended to a camera :), not if we are building a 3D scene

        GFX.drawRect(x,y,w,h, deepDark)

        var dx = 0
        var dy = 0
        var rw = w
        var rh = h

        val camera = camera
        if(camera.onlyShowTarget){
            if(w * targetHeight > targetWidth *h){
                rw = h * targetWidth / targetHeight
                dx = (w-rw)/2
            } else {
                rh = w * targetHeight / targetWidth
                dy = (h-rh)/2
            }
        }

        GFX.ensureEmptyStack()

        // for(i in 0 until 1000)
        Scene.draw(null, camera, x+dx,y+dy,rw,rh, editorTime, flipY = false, drawMode = ShaderPlus.DrawMode.COLOR)

        GFX.ensureEmptyStack()

        GFX.clip(x0, y0, x1, y1)

        BlendMode.DEFAULT.apply()

        /*if(isInFocus){
            val redStarColor = black or 0xff0000
            GFX.drawRect(x+dx,y+dy,2,2, redStarColor)
            GFX.drawRect(x+dx+rw-2,y+dy,2,2, redStarColor)
            GFX.drawRect(x+dx,y+dy+rh-2,2,2, redStarColor)
            GFX.drawRect(x+dx+rw-2,y+dy+rh-2,2,2, redStarColor)
        }*/

        GFX.drawText(x+2, y+2, "Verdana", 12,
            false, false, mode.displayName, -1, 0, -1)

        GFX.drawText(x+16, y+2, "Verdana", 12,
            false, false, if(isLocked2D) "2D" else "3D", -1, 0, -1)

        controls.forEach {
            it.draw(x, y, w, h, x0, y0, x1, y1)
        }

        GFX.ensureEmptyStack()

        super.draw(x0, y0, x1, y1)

        GFX.ensureEmptyStack()

    }

    fun resolveClick(clickX: Float, clickY: Float, rw: Int, rh: Int){

        val camera = camera
        GFX.check()

        val fb: Framebuffer? = null//FBStack[rw, rh, false]
        val width = fb?.w ?: GFX.width
        val height = fb?.h ?: GFX.height
        GFX.clip(0, 0, width, height)

        val radius = 2
        val diameter = radius * 2 + 1
        fun getPixels(mode: ShaderPlus.DrawMode): IntArray {
            // draw only the clicked area?
            Scene.draw(fb, camera, 0, 0, rw, rh, editorTime, false, mode)
            GFX.check()
            fb?.bind() ?: Framebuffer.bindNull()
            val localX = (clickX - this.x).roundToInt()
            val localH = fb?.h ?: GFX.height
            val localY = localH - 1 - (clickY - this.y).roundToInt()
            glFlush(); glFinish() // wait for everything to be drawn
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
            val buffer = IntArray(diameter * diameter)
            glReadPixels(
                max(localX-radius, 0),
                max(localY-radius, 0),
                min(diameter, width),
                min(diameter, height),
                GL_RGBA, GL_UNSIGNED_BYTE, buffer)
            Framebuffer.unbind()
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
        // println(idBuffer.joinToString { it.toUInt().toString(16) })
        // println(depthBuffer.joinToString { it.toUInt().toString(16) })

        // convert that color to an id
        idBuffer.forEachIndexed { index, value ->
            val depth = depthBuffer[index] and 255
            val result = value.and(0xffffff)
            val x = (index % diameter) - radius
            val y = (index / diameter) - radius
            val distance = depth * depthImportance + x*x+y*y
            val isValid = result > 0
            if(isValid && distance <= bestDistance){
                bestDistance = distance
                bestResult = result
            }
        }
        // find the transform with the id to select it
        if(bestResult > 0){
            val transform = (root.listOfAll + nullCamera).firstOrNull { it.clickId == bestResult }
            select(transform)
            // println("clicked color ${bestResult.toUInt().toString(16)}, transform: $transform")
            // println((root.listOfAll + nullCamera).map { it.clickId })
        } else select(null)
        GFX.check()
    }

    // todo camera movement in orthographic view is a bit broken

    fun parseKeyInput(){

        val dt = clamp(deltaTime, 0f, 0.1f)

        // clamped just in case we get multiple mouse movement events in one frame
        val acceleration = Vector3f(
            clamp(dx, -1, 1).toFloat(),
            clamp(dy, -1, 1).toFloat(),
            clamp(dz, -1, 1).toFloat())

        velocity.mul(1f - dt)
        velocity.mulAdd(dt, acceleration)

        if(velocity.x != 0f || velocity.y != 0f || velocity.z != 0f){
            val camera = camera
            val (cameraTransform, cameraTime) = camera.getGlobalTransform(editorTime)
            val oldPosition = camera.position[cameraTime]
            val step = (velocity * dt)
            val step2 = cameraTransform.transformDirection(step)
            // todo transform into the correct space: from that camera to this camera
            val newPosition = oldPosition + step2
            camera.position.addKeyframe(cameraTime, newPosition, 0.01)
            showChanges()
        }

        dx = 0
        dy = 0
        dz = 0

    }

    var lastTouchZoom = 0f
    fun parseTouchInput(){
        // todo rotate/move our camera or the selected object?
        val size = - 20f * shiftSlowdown / GFX.height
        when(touches.size){
            2 -> {
                val first = touches.first()
                if(contains(first.x, first.y)){
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
                    showChanges()
                }
            }
            3 -> {
                // todo move the camera around? :)
                val first = touches.first()
                if(contains(first.x, first.y)){
                    val dx = touches.sumByFloat { it.x - it.lastX } * size * 0.333f
                    val dy = touches.sumByFloat { it.y - it.lastY } * size * 0.333f
                    move(camera, dx, dy)
                    touches.forEach { it.update() }
                    showChanges()
                }
            }
        }
    }

    fun move(selected: Transform, dx0: Float, dy0: Float){

        val delta = dx0-dy0

        val (target2global, localTime) = (selected.parent ?: selected).getGlobalTransform(editorTime)

        val camera = camera
        val (camera2global, cameraTime) = camera.getGlobalTransform(editorTime)

        val global2normUI = Matrix4fArrayList()
        GFX.applyCameraTransform(camera, cameraTime, camera2global, global2normUI)

        // val inverse = Matrix4f(global2normUI).invert()

        // transforms: global to local
        // ->
        // camera local to global, then global to local
        //      obj   cam
        // v' = G2L * L2G * v
        val global2ui = camera2global.mul(target2global.invert())

        when(mode){
            TransformMode.MOVE -> {

                // todo find the (truly) correct speed...
                // depends on FOV, camera and object transform

                val camPos = camera2global.transformPosition(Vector3f())
                val targetPos = target2global.transformPosition(Vector3f())
                val uiZ = camPos.distance(targetPos)

                val oldPosition = selected.position[localTime]
                val localDelta = global2ui.transformDirection(
                    if(Input.isControlDown) Vector3f(0f, 0f, -delta)
                    else Vector3f(dx0, -dy0, 0f)
                ) * (uiZ/6) // why ever 1/6...
                selected.position.addKeyframe(localTime, oldPosition + localDelta)
            }
            /*TransformMode.SCALE -> {
                val oldScale = selected.scale[localTime]
                val localDelta = global2ui.transformDirection(
                    if(Input.isControlDown) Vector3f(0f, 0f, -delta)
                    else Vector3f(dx0, -dy0, 0f)
                )
                selected.scale.addKeyframe(localTime, oldScale + localDelta)
            }
            TransformMode.ROTATE -> {
                // todo transform rotation??? quaternions...
                val oldScale = selected.scale[localTime]
                val localDelta = global2ui.transformDirection(
                    if(Input.isControlDown) Vector3f(0f, 0f, -delta)
                    else Vector3f(dx0, -dy0, 0f)
                )
                selected.scale.addKeyframe(localTime, oldScale + localDelta)
            }*/
        }

        showChanges()
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        // fov is relative to height -> modified to depend on height
        val size = 20f * shiftSlowdown * (if(selectedTransform === camera) -1f else 1f) / GFX.height
        val oldX = x-dx
        val oldY = y-dy
        val dx0 = dx*size
        val dy0 = dy*size
        // move stuff, if mouse is down and no touch is down
        if(0 in mouseKeysDown && touches.size < 2){
            // move the object
            val selected = selectedTransform
            if(selected != null){
                move(selected, dx0, dy0)
            }
        }
    }

    fun turn(dx: Float, dy: Float){
        if(isLocked2D) return
        // move the camera
        // todo only do, if not locked
        val size = 20f * shiftSlowdown * (if(selectedTransform is Camera) -1f else 1f) / max(GFX.width,GFX.height)
        val dx0 = dx*size
        val dy0 = dy*size
        val scaleFactor = -10f
        val camera = camera
        val (_, cameraTime) = camera.getGlobalTransform(editorTime)
        val oldRotation = camera.rotationYXZ[cameraTime]
        camera.putValue(camera.rotationYXZ, oldRotation + Vector3f(dy0 * scaleFactor, dx0 * scaleFactor, 0f))
        if(camera == selectedTransform) showChanges()
    }

    fun showChanges(){
        Studio.updateInspector()
        // AudioManager.requestUpdate()
    }

    // todo undo, redo by serialization of the scene
    // todo switch animatedproperty when selecting another object

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when(action){
            "SetMode(MOVE)" -> mode = TransformMode.MOVE
            "SetMode(SCALE)" -> mode = TransformMode.SCALE
            "SetMode(ROTATE)" -> mode = TransformMode.ROTATE
            "ResetCamera" -> { camera.resetTransform() }
            "MoveLeft" -> this.dx--
            "MoveRight" -> this.dx++
            "MoveUp" -> this.dy++
            "MoveDown" -> this.dy--
            "MoveForward" -> this.dz--
            "MoveBackward", "MoveBack" -> this.dz++
            "Turn" -> turn(dx, dy)
            "TurnLeft" -> turn(-1f, 0f)
            "TurnRight" -> turn(1f, 0f)
            "TurnUp" -> turn(0f, -1f)
            "TurnDown" -> turn(0f, 1f)
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    fun goFullscreen(){
        // todo check if it's already fullscreen...
        // GFX.toggleFullscreen()
        val root = rootPanel
        val view = SceneView(this)
        val window = Window(view, 0, 0)
        windowStack.push(window)
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        goFullscreen()
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        if((parent as? CustomContainer)?.clicked(x,y) != true){

            var isProcessed = false
            val xi = x.toInt()
            val yi = y.toInt()
            controls.forEach {
                if(it.contains(xi, yi)){
                    it.drawable.onMouseClicked(x, y, button, long)
                    isProcessed = true
                }
            }

            if(!isProcessed){
                var rw = w
                var rh = h
                var dx = 0
                var dy = 0

                GFX.addGPUTask {
                    val camera = camera
                    if(camera.onlyShowTarget){
                        if(w * targetHeight > targetWidth *h){
                            rw = h * targetWidth / targetHeight
                            dx = (w-rw)/2
                        } else {
                            rh = w * targetHeight / targetWidth
                            dy = (h-rh)/2
                        }
                    }
                    resolveClick(x-dx, y-dy, rw, rh)
                    35
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
        when(type){
            "Transform" -> {
                val original = dragged?.getOriginal() ?: return
                if(original is Camera){
                    camera = original
                }// else focus?
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

    fun deleteSelectedTransform(){
        selectedTransform?.removeFromParent()
        selectedTransform?.onDestroy()
        select(null)
    }

    override fun getClassName() = "SceneView"

}