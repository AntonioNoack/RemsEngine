package me.anno.gpu

import me.anno.engine.ui.render.RenderView
import me.anno.gpu.framebuffer.Framebuffer
import org.apache.logging.log4j.LogManager

/**
 * Delegation for VR rendering:
 * when multiple additional displays (eyes) may be present, where additional points of view shall be rendered.
 * */
interface VRRenderingRoutine {

    /**
     * returns true on success
     * */
    fun startSession(window: OSWindow, rv: RenderView): Boolean

    /**
     * returns whether the next frame still shall be drawn by this context
     * */
    fun drawFrame(window: OSWindow): Boolean

    fun setRenderView(rv: RenderView)

    val fb: Framebuffer
    val isActive: Boolean

    companion object {
        private val LOGGER = LogManager.getLogger(VRRenderingRoutine::class)

        var vrRoutine: VRRenderingRoutine? = null
        var shallRenderVR = false

        /**
         * Tries to start VR. Call this, when you change the main RenderView.
         * */
        fun tryStartVR(window: OSWindow?, rv: RenderView?) {
            if (shallRenderVR) {
                if (rv != null) vrRoutine?.setRenderView(rv)
                else LOGGER.warn("Already running VR")
                return
            }

            val vrRoutine = vrRoutine
            if (vrRoutine == null) {
                LOGGER.warn("VR isn't supported")
                return
            }

            if (window == null) {
                LOGGER.warn("Window is null")
                return
            }

            if (rv == null) {
                LOGGER.warn("RenderView is missing")
                return
            }

            val started = vrRoutine.startSession(window, rv)
            shallRenderVR = started

            if (started) LOGGER.info("Started VR")
            else LOGGER.warn("Failed to initialize VR")
        }
    }
}