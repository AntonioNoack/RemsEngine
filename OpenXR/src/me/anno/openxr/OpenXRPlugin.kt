package me.anno.openxr

import me.anno.engine.EngineBase
import me.anno.engine.ui.render.RenderView
import me.anno.extensions.plugins.Plugin
import me.anno.gpu.GFX
import me.anno.gpu.OSWindow
import me.anno.gpu.VRRenderingRoutine
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Texture2D

class OpenXRPlugin : Plugin(), VRRenderingRoutine {

    override fun onEnable() {
        super.onEnable()
        GFX.vrRenderingRoutine = this
    }

    private var instance: OpenXR? = null
    override val fb = Framebuffer("OpenXR", 1, 1, 1, TargetType.UInt8x4, DepthBufferType.TEXTURE)
    private val ct0 = Texture2D("OpenXR-Left", 1, 1, 1)
    private val ct1 = Texture2D("OpenXR-Right", 1, 1, 1)
    private val dt = Texture2D("OpenXR-Depth", 1, 1, 1)

    override var isActive = false
        private set

    init {
        fb.textures = listOf(ct0, ct1)
    }

    override fun startSession(window: OSWindow, rv: RenderView): Boolean {
        try {
            window.vsyncOverride = false
            instance = OpenXRRendering(window, rv, fb, ct0, ct1, dt)
            return true
        } catch (e: Exception) {
            window.vsyncOverride = null
            e.printStackTrace()
            return false
        }
    }

    override fun drawFrame(window: OSWindow): Boolean {
        val instance = instance ?: return false
        val session = instance.session
        if (session == null) {
            instance.validateSession()
            GFX.callOnGameLoop(EngineBase.instance!!, window)
            return true
        }
        val oldIsActive = isActive
        isActive = session.renderFrameMaybe(instance)
        if (!isActive) {
            if (oldIsActive) { // became inactive
                onInactive(window)
            }
            GFX.callOnGameLoop(EngineBase.instance!!, window)
        } else if (!oldIsActive) { // became active again
            onActive(window)
        }
        return if (!session.events.instanceAlive) {
            onInactive(window)
            this.instance = null
            false
        } else true
    }

    private fun onActive(window: OSWindow) {
        window.vsyncOverride = false
    }

    private fun onInactive(window: OSWindow) {
        // reset roll
        (instance as? OpenXRRendering)?.rv?.controlScheme?.rotationTarget?.z = 0.0
        window.vsyncOverride = null
    }
}