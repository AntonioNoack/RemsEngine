package me.anno.openxr

import me.anno.engine.EngineBase
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.Renderers
import me.anno.extensions.plugins.Plugin
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.OSWindow
import me.anno.gpu.VRRenderingRoutine
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Texture2D
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL30C.glFramebufferTexture2D
import org.lwjgl.openxr.XrSpaceLocation

class OpenXRPlugin : Plugin(), VRRenderingRoutine {

    override fun onEnable() {
        super.onEnable()
        GFX.vrRenderingRoutine = this
    }

    private var instance: OpenXR? = null
    override val fb = Framebuffer("OpenXR", 1, 1, 1, TargetType.UInt8x4, DepthBufferType.TEXTURE)
    private val ct = Texture2D("OpenXR-Depth", 1, 1, 1)
    private val dt = Texture2D("OpenXR-Depth", 1, 1, 1)

    init {
        fb.textures = listOf(ct)
    }

    val simpleShader = Shader(
        "simple", coordsList, coordsUVVertexShader, uvList, listOf(
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "" +
                "void main(){\n" +
                "   result = vec4(1,0,0,1);\n" +
                "}\n"
    )

    override fun startSession(window: OSWindow, rv: RenderView): Boolean {
        try {
            window.vsyncOverride = false
            instance = object : OpenXR(window.pointer) {
                var needsFilling = true
                override fun copyToDesktopWindow(framebuffer: Int, w: Int, h: Int) {
                    GFX.callOnGameLoop(EngineBase.instance!!, window)
                    needsFilling = true
                }

                private val camera get() = rv.editorCamera

                fun updateCamera(
                    px: Float, py: Float, pz: Float,
                    rx: Float, ry: Float, rz: Float, rw: Float
                ) {
                    rv.enableOrbiting = false
                    rv.orbitCenter.set(px, py, pz)
                    rv.orbitRotation.set(rx, ry, rz, rw)
                    rv.updateEditorCameraTransform()
                }

                override fun beginRenderViews() {
                    super.beginRenderViews()
                    val view0 = viewConfigViews[0]
                    val w = view0.recommendedImageRectWidth()
                    val h = view0.recommendedImageRectHeight()
                    val position = position
                    val rotation = rotation
                    updateCamera(position.x, position.y, position.z, rotation.x, rotation.y, rotation.z, rotation.w)
                }

                private fun defineTexture(w: Int, h: Int, ct: Texture2D, colorTexture: Int, session: Int) {
                    ct.width = w
                    ct.height = h
                    ct.pointer = colorTexture
                    ct.session = session
                    ct.wasCreated = true
                }

                private fun attachTexture(target: Int, colorTexture: Int) {
                    glFramebufferTexture2D(GL_FRAMEBUFFER, target, GL_TEXTURE_2D, colorTexture, 0)
                }

                override fun renderFrame(
                    viewIndex: Int, w: Int, h: Int, predictedDisplayTime: Long, handLocations: XrSpaceLocation.Buffer?,
                    framebuffer: Int, colorTexture: Int, depthTexture: Int
                ) {

                    // todo fill pipeline only once
                    // todo use our perspective matrix, not the one given by the engine

                    val pose = views[viewIndex].pose()
                    val pos = pose.`position$`()
                    val rot = pose.orientation()
                    updateCamera(pos.x(), pos.y(), pos.z(), rot.x(), rot.y(), rot.z(), rot.w())

                    // todo define frustum culling properly
                    val camera = camera
                    rv.prepareDrawScene(w, h, 1f, camera, camera, 0f, true)

                    if (depthTexture < 0 && !dt.isCreated()) {
                        dt.create(TargetType.DEPTH16)
                    }
                    val depthTextureI = if (depthTexture < 0) dt.pointer else depthTexture
                    val session = GFXState.session
                    fb.width = w
                    fb.height = h
                    fb.pointer = framebuffer
                    fb.session = session
                    defineTexture(w, h, ct, colorTexture, session)
                    defineTexture(w, h, dt, depthTextureI, session)
                    fb.bind()
                    attachTexture(GL_COLOR_ATTACHMENT0, colorTexture)
                    attachTexture(GL_DEPTH_ATTACHMENT, depthTextureI)
                    Framebuffer.drawBuffers1(0)
                    fb.checkIsComplete()
                    rv.drawScene(
                        w, h, Renderers.pbrRenderer,
                        fb, changeSize = false, hdr = false, sky = true
                    )
                }
            }
            initFramebuffers()
            // GFXState.newSession() // todo remove this, when everything is working :/
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    override fun drawFrame(): Boolean {
        val instance = instance!!
        instance.renderFrameMaybe()
        if (!instance.events.instanceAlive) {
            // ensure we'd crash next time
            this.instance = null
        }
        return instance.events.instanceAlive
    }
}