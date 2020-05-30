package me.anno.ui.impl.sceneView

import me.anno.config.DefaultStyle.black
import me.anno.config.DefaultStyle.deepDark
import me.anno.gpu.GFX
import me.anno.gpu.GFX.deltaTime
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFX.editorTime
import me.anno.gpu.GFX.nullCamera
import me.anno.gpu.GFX.root
import me.anno.gpu.GFX.targetHeight
import me.anno.gpu.GFX.targetWidth
import me.anno.gpu.Shader
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.input.Input
import me.anno.input.Input.keysDown
import me.anno.input.Input.mouseKeysDown
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.objects.blending.BlendMode
import me.anno.ui.base.TextPanel
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelFrame
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.impl.CustomContainer
import me.anno.ui.style.Style
import me.anno.utils.clamp
import me.anno.utils.plus
import me.anno.utils.print
import me.anno.utils.times
import org.joml.Matrix4fStack
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30.*
import kotlin.math.max

// todo search elements
// todo search with tags
// todo tags for elements
// todo search properties

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

    var isInited = false
    var framebuffer = Framebuffer(1, 1,1,true, true)

    lateinit var sqrtDisplayShader: Shader

    fun init(){
        sqrtDisplayShader = Shader("" +
                "in vec2 attr0;\n" +
                "void main(){" +
                "   gl_Position = vec4(attr0*2.0-1.0,0.0,1.0);\n" +
                "   uv = attr0;\n" +
                "}", "" +
                "varying vec2 uv;\n", "" +
                "uniform sampler2D tex;\n" +
                "void main(){" +
                "   gl_FragColor = sqrt(texture(tex, uv));\n" +
                "}")
        isInited = true
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {

        GFX.check()

        if(!isInited) init()

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
        val (cameraTransform, cameraTime) = camera.getGlobalTransform(editorTime)

        if(camera.onlyShowTarget){
            if(w*targetHeight > targetWidth*h){
                rw = h * targetWidth/targetHeight
                dx = (w-rw)/2
            } else {
                rh = w * targetHeight/targetWidth
                dy = (h-rh)/2
            }
        }

        GFX.clip(x+dx,y+dy,rw,rh)

        if(framebuffer.w != rw || framebuffer.h != rh){
            framebuffer.destroy()
            framebuffer = Framebuffer(rw, rh, 1, fpTargets = true, createDepthBuffer = true)
        }

        framebuffer.bind()

        GFX.check()
        GFX.drawRect(x+dx,y+dy,rw,rh, black)

        if(camera.useDepth){
            glEnable(GL_DEPTH_TEST)
            glClearDepth(1.0)
            glDepthRange(-1.0, 1.0)
            glDepthFunc(GL_LEQUAL)
            glClear(GL_DEPTH_BUFFER_BIT)
        } else {
            glDisable(GL_DEPTH_TEST)
        }

        // draw the 3D stuff

        // todo 3D grids and gizmos for orientation

        val stack = Matrix4fStack(256)

        GFX.applyCameraTransform(camera, cameraTime, cameraTransform, stack)

        val white = camera.color[cameraTime]
        stack.pushMatrix()
        // root.draw(stack, editorHoverTime, Vector4f(1f,1f,1f,1f))
        nullCamera.draw(stack, editorTime, white)
        stack.popMatrix()
        stack.pushMatrix()
        root.draw(stack, editorTime, white)
        stack.popMatrix()

        BlendMode.DEFAULT.apply()

        glDisable(GL_DEPTH_TEST)

        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        GFX.clip(x+dx,y+dy,rw,rh)

        framebuffer.bindTextures()

        sqrtDisplayShader.use()
        flat01.draw(sqrtDisplayShader)
        GFX.check()

        GFX.clip(x0, y0, x1, y1)

        if(isInFocus){
            val redStarColor = black or 0xff0000
            GFX.drawRect(x+dx,y+dy,2,2, redStarColor)
            GFX.drawRect(x+dx+rw-2,y+dy,2,2, redStarColor)
            GFX.drawRect(x+dx,y+dy+rh-2,2,2, redStarColor)
            GFX.drawRect(x+dx+rw-2,y+dy+rh-2,2,2, redStarColor)
        }

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
            // todo transform into the correct space: from that camera to this camera
            val newPosition = oldPosition + step
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
        }
        if(1 in mouseKeysDown){
            // todo move the camera
            val scaleFactor = 10f
            val camera = GFX.selectedCamera
            val (cameraTransform, cameraTime) = camera.getGlobalTransform(editorTime)
            val oldRotation = camera.rotationYXZ[cameraTime]
            camera.putValue(camera.rotationYXZ, oldRotation + Vector3f(dy0 * scaleFactor, dx0 * scaleFactor, 0f))
            println("rotating")
        }
    }

    override fun onCharTyped(x: Float, y: Float, key: Int) {
        if(key.toChar() == ' '){
            GFX.pauseOrUnpause()
        } else {
            super.onCharTyped(x, y, key)
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

}