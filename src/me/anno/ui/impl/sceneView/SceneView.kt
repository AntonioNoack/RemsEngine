package me.anno.ui.impl.sceneView

import me.anno.config.DefaultStyle.black
import me.anno.config.DefaultStyle.deepDark
import me.anno.gpu.GFX
import me.anno.gpu.GFX.editorHoverTime
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFX.editorTime
import me.anno.gpu.Shader
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.objects.blending.BlendMode
import me.anno.ui.base.TextPanel
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelFrame
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.style.Style
import org.joml.Matrix4fStack
import org.joml.Vector4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30.*

class SceneView(var root: Transform, style: Style): PanelFrame(null, style.getChild("sceneView")){

    var sceneWidth = 1920
    var sceneHeight = 1080

    init {
        weight = 1f
        backgroundColor = 0

        // todo add the top controls

        val topControls = PanelListX(style)
        topControls += WrapAlign.Top
        topControls += TextPanel("hi", style)

    }

    val camera = Camera()


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

        // calculate the correct size, such that we miss nothing
        // todo ideally we could zoom in the image etc...
        // todo only do this, if we are appended to a camera :), not if we are building a 3D scene

        GFX.drawRect(x,y,w,h, deepDark)

        var dx = 0
        var dy = 0
        var rw = w
        var rh = h

        if(w*sceneHeight > sceneWidth*h){
            rw = h * sceneWidth/sceneHeight
            dx = (w-rw)/2
        } else {
            rh = w * sceneHeight/sceneWidth
            dy = (h-rh)/2
        }

        GFX.clip(x+dx,y+dy,rw,rh)

        if(framebuffer.w != rw || framebuffer.h != rh){
            framebuffer.destroy()
            framebuffer = Framebuffer(rw, rh, 1, fpTargets = true, createDepthBuffer = true)
        }

        framebuffer.bind()

        GFX.check()
        GFX.drawRect(x+dx,y+dy,rw,rh, black)
        if(isInFocus){
            GFX.drawRect(x+dx,y+dy,2,2,black or 0xff0000)
            GFX.drawRect(x+dx+rw-2,y+dy,2,2,black or 0xff0000)
            GFX.drawRect(x+dx,y+dy+rh-2,2,2,black or 0xff0000)
            GFX.drawRect(x+dx+rw-2,y+dy+rh-2,2,2,black or 0xff0000)
        }

        glEnable(GL_DEPTH_TEST)
        glClearDepth(1.0)
        glDepthRange(-1.0, 1.0)
        glDepthFunc(GL_LEQUAL)
        glClear(GL_DEPTH_BUFFER_BIT)

        // draw the 3D stuff

        // todo 3D grids and gizmos for orientation

        val stack = Matrix4fStack(256)
        GFX.applyCameraTransform(camera, 0f, stack)

        stack.pushMatrix()
        // root.draw(stack, editorHoverTime, Vector4f(1f,1f,1f,1f))
        root.draw(stack, editorTime, Vector4f(1f,1f,1f,1f))
        stack.popMatrix()

        BlendMode.DEFAULT.apply()

        GL11.glDisable(GL11.GL_DEPTH_TEST)

        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        GFX.clip(x+dx,y+dy,rw,rh)

        framebuffer.bindTextures()

        sqrtDisplayShader.use()
        flat01.draw(sqrtDisplayShader)
        GFX.check()

        GFX.clip(x0, y0, x1, y1)

        super.draw(x0, y0, x1, y1)

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

}