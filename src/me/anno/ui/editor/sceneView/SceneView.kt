package me.anno.ui.editor.sceneView

import me.anno.config.DefaultStyle.black
import me.anno.config.DefaultStyle.deepDark
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.gpu.GFX.deltaTime
import me.anno.gpu.GFX.editorTime
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.input.Input
import me.anno.input.Input.keysDown
import me.anno.input.Input.mouseKeysDown
import me.anno.objects.Camera
import me.anno.studio.Scene
import me.anno.ui.base.TextPanel
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelFrame
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.editor.CustomContainer
import me.anno.ui.style.Style
import me.anno.utils.clamp
import me.anno.utils.plus
import me.anno.utils.times
import org.joml.Vector3f
import kotlin.math.max

// todo search elements
// todo search with tags
// todo tags for elements
// todo search properties

// todo control click -> fullscreen view of this element?

// todo show the current mode with the cursor

class SceneView(style: Style): PanelFrame(null, style.getChild("sceneView")){

    init {

        weight = 1f
        backgroundColor = 0

        // todo add the top controls

        val topControls = PanelListX(style)
        topControls += WrapAlign.Top
        topControls += TextPanel("hi", style)

    }

    // use a framebuffer, where we draw sq(color)
    // then we use a shader to draw sqrt(sq(color))
    // this should give correct color mixing <3

    var framebuffer = Framebuffer(1, 1,1,true, true)
    var mode = TransformMode.MOVE

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {

        GFX.check()

        parseKeyInput()

        // calculate the correct size, such that we miss nothing
        // todo ideally we could zoom in the image etc...
        // todo only do this, if we are appended to a camera :), not if we are building a 3D scene

        GFX.drawRect(x,y,w,h, deepDark)

        var dx = 0
        var dy = 0
        var rw = w
        var rh = h


        val camera = GFX.selectedCamera
        if(camera.onlyShowTarget){
            if(w* GFX.targetHeight > GFX.targetWidth *h){
                rw = h * GFX.targetWidth / GFX.targetHeight
                dx = (w-rw)/2
            } else {
                rh = w * GFX.targetHeight / GFX.targetWidth
                dy = (h-rh)/2
            }
        }

        Scene.draw(null, framebuffer, x+dx,y+dy,rw,rh, GFX.editorTime, false)

        GFX.clip(x0, y0, x1, y1)

        if(isInFocus){
            val redStarColor = black or 0xff0000
            GFX.drawRect(x+dx,y+dy,2,2, redStarColor)
            GFX.drawRect(x+dx+rw-2,y+dy,2,2, redStarColor)
            GFX.drawRect(x+dx,y+dy+rh-2,2,2, redStarColor)
            GFX.drawRect(x+dx+rw-2,y+dy+rh-2,2,2, redStarColor)
        }

        GFX.drawText(x+2, y+2, "Verdana", 12,
            false, false, mode.displayName, -1, 0)

        super.draw(x0, y0, x1, y1)

    }

    var velocity = Vector3f()

    fun parseKeyInput(){

        val dt = clamp(deltaTime, 0f, 0.1f)

        val acceleration = Vector3f()

        if(isInFocus){

            if('W'.toInt() in keysDown) acceleration.z--
            if('S'.toInt() in keysDown) acceleration.z++
            if('A'.toInt() in keysDown) acceleration.x--
            if('D'.toInt() in keysDown) acceleration.x++
            if('Q'.toInt() in keysDown) acceleration.y--
            if('E'.toInt() in keysDown) acceleration.y++

        }

        velocity.mul(1f - dt)
        velocity.mulAdd(dt, acceleration)

        if(velocity.x != 0f || velocity.y != 0f || velocity.z != 0f){
            val camera = GFX.selectedCamera
            val (cameraTransform, cameraTime) = camera.getGlobalTransform(editorTime)
            val oldPosition = camera.position[cameraTime]
            val step = (velocity * dt)
            val step2 = cameraTransform.transformDirection(step)
            // todo transform into the correct space: from that camera to this camera
            val newPosition = oldPosition + step2
            camera.position.addKeyframe(cameraTime, newPosition, 0.01f)
        }


    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        val size = (if(Input.isShiftDown) 4f else 20f) * (if(GFX.selectedTransform is Camera) -1f else 1f) / max(GFX.width,GFX.height)
        val dx0 = dx*size
        val dy0 = dy*size
        val delta = dx0-dy0
        // todo drag objects vs move the camera
        if(0 in mouseKeysDown){
            // todo move the object
            val selected = GFX.selectedTransform
            if(selected != null){

                val (localTransform, localTime) = selected.getGlobalTransform(editorTime)
                val (cameraTransform, _) = GFX.selectedCamera.getGlobalTransform(editorTime)
                // transforms: global to local
                // ->
                // camera local to global, then global to local
                //      obj   cam
                // v' = G2L * L2G * v
                val transform = cameraTransform.mul(localTransform.invert())

                when(mode){
                    TransformMode.MOVE -> {
                        // todo find the correct speed...
                        // todo depends on FOV, camera and object transform
                        val oldPosition = selected.position[localTime]
                        val localDelta = transform.transformDirection(
                            if(Input.isControlDown) Vector3f(0f, 0f, -delta)
                            else Vector3f(dx0, -dy0, 0f)
                        )
                        selected.position.addKeyframe(localTime, oldPosition + localDelta)
                    }
                    TransformMode.SCALE -> {
                        val oldScale = selected.scale[localTime]
                        val localDelta = transform.transformDirection(
                            if(Input.isControlDown) Vector3f(0f, 0f, -delta)
                            else Vector3f(dx0, -dy0, 0f)
                        )
                        selected.scale.addKeyframe(localTime, oldScale + localDelta)
                    }
                }
            }
        }
        if(1 in mouseKeysDown){
            // todo move the camera
            // todo only do, if not locked
            val scaleFactor = 10f
            val camera = GFX.selectedCamera
            val (cameraTransform, cameraTime) = camera.getGlobalTransform(editorTime)
            val oldRotation = camera.rotationYXZ[cameraTime]
            camera.putValue(camera.rotationYXZ, oldRotation + Vector3f(dy0 * scaleFactor, dx0 * scaleFactor, 0f))
        }
    }

    // todo undo, redo by serialization of the scene
    // todo switch animatedproperty when selecting another object

    override fun onCharTyped(x: Float, y: Float, key: Int) {
        when(key.toChar().toLowerCase()){
            // todo global actions
            ' ' -> GFX.pauseOrUnpause()
            'r' -> mode = TransformMode.MOVE
            't' -> mode = TransformMode.SCALE
            'z', 'y' -> mode = TransformMode.ROTATE
            else -> super.onCharTyped(x, y, key)
        }
    }

    override fun onGotAction(x: Float, y: Float, action: String) {
        when(action){
            "setMode(move)" -> mode = TransformMode.MOVE
            "setMode(scale)" -> mode = TransformMode.SCALE
            "setMode(rotate)" -> mode = TransformMode.ROTATE
            else -> super.onGotAction(x, y, action)
        }
    }

    override fun onKeyDown(x: Float, y: Float, key: Int) {
        super.onKeyDown(x, y, key)
    }

    override fun onMouseClicked(x: Float, y: Float, button: Int, long: Boolean) {
        if((parent as? CustomContainer)?.clicked(x,y) != true){
            super.onMouseClicked(x, y, button, long)
        }
    }

    /*override fun getCursor() = when(mode){
        TransformMode.MOVE -> Cursor.drag
        TransformMode.SCALE -> if(Input.isShiftDown) Cursor.vResize else Cursor.hResize
        TransformMode.ROTATE -> Cursor.crossHair
        else -> null
    }*/

}