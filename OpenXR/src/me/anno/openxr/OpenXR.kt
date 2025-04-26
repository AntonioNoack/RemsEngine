package me.anno.openxr

import me.anno.Engine
import me.anno.Time
import me.anno.engine.ui.vr.VRRendering
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.openxr.OpenXRUtils.xrInstance
import org.apache.logging.log4j.LogManager
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_UNKNOWN
import org.lwjgl.openxr.XR10.XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO
import org.lwjgl.openxr.XR10.xrDestroyInstance
import org.lwjgl.openxr.XR10.xrDestroySession
import org.lwjgl.openxr.XrSpaceLocation
import kotlin.math.abs

abstract class OpenXR(val window: Long) : VRRendering() {

    companion object {
        private val LOGGER = LogManager.getLogger(OpenXR::class)
        const val VIEW_CONFIG_TYPE = XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO
        var sessionTestTimeout = 2 * SECONDS_TO_NANOS
    }

    abstract fun copyToDesktopWindow(w: Int, h: Int)
    abstract fun renderFrame(
        viewIndex: Int, w: Int, h: Int,
        predictedDisplayTime: Long,
        handLocations: XrSpaceLocation.Buffer?,
        colorTexture: Int, depthTexture: Int,
    )

    abstract fun beginRenderViews()

    var state = XR_SESSION_STATE_UNKNOWN

    val system = OpenXRSystem(window)
    var session: OpenXRSession? = null
    var hasBeenDestroyed = false
    private var lastSessionTest = 0L

    init {
        if (system.systemId != 0L) {
            session = OpenXRSession(window, system)
        }
        addShutdownHook()
    }

    fun validateSession(): OpenXRSession? {
        val time = Time.nanoTime
        if (abs(time - lastSessionTest) >= sessionTestTimeout) {
            lastSessionTest = time
            if (system.systemId == 0L) {
                system.systemId = system.createSystemId()
                if (system.systemId != 0L) {
                    session = OpenXRSession(window, system)
                }
            }
        }
        return session
    }

    fun addShutdownHook() {
        Engine.registerForShutdown(this::destroy)
    }

    fun destroy() {
        if (hasBeenDestroyed) return
        hasBeenDestroyed = true
        LOGGER.info("Shutting down")
        val session = session?.session
        if (session != null) {
            val result = xrDestroySession(session)
            LOGGER.info("Destroying session: {}", result)
        }
        val result = xrDestroyInstance(system.instance)
        LOGGER.info("Destroying instance: {}", result)
        xrInstance = null
    }

    override fun accumulateViewTransforms(): Int {
        val session = session ?: return 0
        val viewCount = session.viewCount
        val views = session.views
        for (i in 0 until viewCount) {
            val pose = views[i].pose()
            val pos = pose.`position$`()
            val rot = pose.orientation()
            position.add(pos.x(), pos.y(), pos.z())
            rotation.add(rot.x(), rot.y(), rot.z(), rot.w())
        }
        return viewCount
    }
}