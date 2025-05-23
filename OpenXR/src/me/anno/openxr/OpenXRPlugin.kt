package me.anno.openxr

import me.anno.Engine
import me.anno.engine.EngineBase
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.vr.VRRenderingRoutine
import me.anno.extensions.plugins.Plugin
import me.anno.gpu.OSWindow
import me.anno.gpu.RenderStep.callOnGameLoop
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Texture2D
import me.anno.openxr.OpenXRController.Companion.xrControllers
import org.apache.logging.log4j.LogManager
import org.joml.Vector4f

class OpenXRPlugin : Plugin(), VRRenderingRoutine {

    companion object {
        private val LOGGER = LogManager.getLogger(OpenXRPlugin::class)
    }

    override fun onEnable() {
        super.onEnable()
        if (VRRenderingRoutine.vrRoutine == null)
            VRRenderingRoutine.vrRoutine = this
    }

    private var instance: OpenXR? = null
    private var rv: RenderView? = null

    private val framebuffer = Framebuffer("OpenXR", 1, 1, 1, TargetType.UInt8x4, DepthBufferType.TEXTURE)
    private val depthTexture = Texture2D("OpenXR-Depth", 1, 1, 1)
    override val leftTexture = Texture2D("OpenXR-Left", 1, 1, 1)
    override val rightTexture = Texture2D("OpenXR-Right", 1, 1, 1)
    override val leftView: Vector4f? get() = null
    override val rightView: Vector4f? get() = null
    override val previewGamma: Float get() = 2f

    override var isActive = false
        private set

    init {
        framebuffer.textures = listOf(leftTexture, rightTexture)
    }

    override fun setRenderView(rv: RenderView) {
        this.rv = rv
    }

    override fun startSession(window: OSWindow, rv: RenderView): Boolean {
        try {
            this.rv = rv
            instance = OpenXRRendering(window, rv, framebuffer, leftTexture, rightTexture, depthTexture)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun ensureInstanceRunning(window: OSWindow) {
        val rv = rv // todo test this, however we can test it... disconnect and reconnect meta link cable
        if (instance?.hasBeenDestroyed != false && rv != null && !Engine.shutdown) {
            LOGGER.info("Session was destroyed -> recreating it")
            instance = OpenXRRendering(window, rv, framebuffer, leftTexture, rightTexture, depthTexture)
        }
    }

    override fun drawFrame(window: OSWindow): Boolean {
        ensureInstanceRunning(window)
        val instance = instance ?: return false
        val session = instance.session
        if (session == null) {
            instance.validateSession()
            callOnGameLoop(EngineBase.instance!!, window)
            return true
        } else {
            val oldIsActive = isActive
            isActive = session.renderFrameMaybe(instance)
            if (!isActive) {
                if (oldIsActive) { // became inactive
                    onInactive(window)
                }
                callOnGameLoop(EngineBase.instance!!, window)
            } else if (!oldIsActive) { // became active again
                onActive(window)
            }
            if (!session.events.instanceAlive) {
                onInactive(window)
                this.instance = null
            }
            return true
        }
    }

    private fun onActive(window: OSWindow) {
        window.vsyncOverride = false
    }

    private fun onInactive(window: OSWindow) {
        // reset roll
        (instance as? OpenXRRendering)?.rv?.controlScheme?.rotationTargetDegrees?.z = 0f
        // reset vsync
        window.vsyncOverride = null
        // disable controllers
        for (i in xrControllers.lastIndex downTo 0) {
            xrControllers[i].isConnected = false
        }
    }
}