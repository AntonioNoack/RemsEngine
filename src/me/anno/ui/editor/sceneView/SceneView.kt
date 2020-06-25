package me.anno.ui.editor.sceneView

import me.anno.config.DefaultStyle.black
import me.anno.config.DefaultStyle.deepDark
import me.anno.gpu.GFX
import me.anno.gpu.GFX.deltaTime
import me.anno.gpu.GFX.editorTime
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.input.Input
import me.anno.input.Input.keysDown
import me.anno.input.Input.mouseKeysDown
import me.anno.objects.Camera
import me.anno.studio.Scene
import me.anno.studio.Studio.dragged
import me.anno.studio.Studio.targetHeight
import me.anno.studio.Studio.targetWidth
import me.anno.ui.base.TextPanel
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelFrame
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.editor.CustomContainer
import me.anno.ui.style.Style
import me.anno.utils.*
import org.joml.Matrix4f
import org.joml.Matrix4fStack
import org.joml.Vector3f
import org.joml.Vector4f
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

    var camera = GFX.nullCamera

    // use a framebuffer, where we draw sq(color)
    // then we use a shader to draw sqrt(sq(color))
    // this should give correct color mixing <3

    // we need the depth for post processing effects like dof
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

        // for(i in 0 until 1000)
        Scene.draw(null, camera, x+dx,y+dy,rw,rh, GFX.editorTime, false)

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

    var dx = 0
    var dy = 0
    var dz = 0

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
            camera.position.addKeyframe(cameraTime, newPosition, 0.01f)
        }

        dx = 0
        dy = 0
        dz = 0

    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        val size = (if(Input.isShiftDown) 4f else 20f) * (if(GFX.selectedTransform is Camera) -1f else 1f) / max(GFX.width,GFX.height)
        val oldX = x-dx
        val oldY = y-dy
        val dx0 = dx*size
        val dy0 = dy*size
        val delta = dx0-dy0
        // todo fix this code, then move it to the action manager
        if(0 in mouseKeysDown){
            // move the object
            val selected = GFX.selectedTransform
            if(selected != null){

                val (target2global, localTime) = selected.getGlobalTransform(editorTime)

                val camera = camera
                val (camera2global, cameraTime) = camera.getGlobalTransform(editorTime)

                val global2normUI = Matrix4fStack(3)
                GFX.applyCameraTransform(camera, cameraTime, camera2global, global2normUI)

                val inverse = Matrix4f(global2normUI).invert()

                // transforms: global to local
                // ->
                // camera local to global, then global to local
                //      obj   cam
                // v' = G2L * L2G * v
                val global2ui = camera2global.mul(target2global.invert())

                fun xTo01(x: Float) = ((x-this.x)/this.w)*2-1
                fun yTo01(y: Float) = ((y-this.y)/this.h)*2-1

                when(mode){
                    TransformMode.MOVE -> {

                        // todo find the (truly) correct speed...
                        // todo depends on FOV, camera and object transform

                        val uiZ = camera2global.transformPosition(Vector3f()).distance(target2global.transformPosition(Vector3f()))
                        println(uiZ)

                        val oldPosition = selected.position[localTime]
                        val localDelta = global2ui.transformDirection(
                            if(Input.isControlDown) Vector3f(0f, 0f, -delta)
                            else Vector3f(dx0, -dy0, 0f)
                        ) * (uiZ/4)
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
            }
        }
    }

    fun turn(dx: Float, dy: Float){
        // todo move the camera
        // todo only do, if not locked
        val size = (if(Input.isShiftDown) 4f else 20f) * (if(GFX.selectedTransform is Camera) -1f else 1f) / max(GFX.width,GFX.height)
        val dx0 = dx*size
        val dy0 = dy*size
        val scaleFactor = -10f
        val camera = camera
        val (cameraTransform, cameraTime) = camera.getGlobalTransform(editorTime)
        val oldRotation = camera.rotationYXZ[cameraTime]
        camera.putValue(camera.rotationYXZ, oldRotation + Vector3f(dy0 * scaleFactor, dx0 * scaleFactor, 0f))
    }

    // todo undo, redo by serialization of the scene
    // todo switch animatedproperty when selecting another object
    override fun onCharTyped(x: Float, y: Float, key: Int) {
        when(key.toChar().toLowerCase()){
            // todo global actions
            'r' -> mode = TransformMode.MOVE
            't' -> mode = TransformMode.SCALE
            'z', 'y' -> mode = TransformMode.ROTATE
            else -> super.onCharTyped(x, y, key)
        }
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when(action){
            "SetMode(MOVE)" -> mode = TransformMode.MOVE
            "SetMode(SCALE)" -> mode = TransformMode.SCALE
            "SetMode(ROTATE)" -> mode = TransformMode.ROTATE
            "ResetCamera" -> { GFX.selectedCamera.resetTransform() }
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

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when(type){
            "Transform" -> {
                val original = dragged?.getOriginal() ?: return
                if(original is Camera){
                    camera = original
                }// else focus?
            }
            else -> super.onPaste(x, y, data, type)
        }
    }

    override fun getClassName() = "SceneView"

}