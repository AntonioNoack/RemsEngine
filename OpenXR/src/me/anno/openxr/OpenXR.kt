package me.anno.openxr

import me.anno.Engine
import me.anno.Time
import me.anno.gpu.drawing.Perspective
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.openxr.OpenXRUtils.xrInstance
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_UNKNOWN
import org.lwjgl.openxr.XR10.XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO
import org.lwjgl.openxr.XR10.xrDestroyInstance
import org.lwjgl.openxr.XR10.xrDestroySession
import org.lwjgl.openxr.XrFovf
import org.lwjgl.openxr.XrSpaceLocation
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.tan

abstract class OpenXR(val window: Long) {

    companion object {

        private val LOGGER = LogManager.getLogger(OpenXR::class)
        const val VIEW_CONFIG_TYPE = XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO
        val sessionTestTimeout = 2 * SECONDS_TO_NANOS

        fun createProjectionFov(projectionMatrix: Matrix4f, fov: XrFovf, nearZ: Float, farZ: Float) {
            Perspective.setPerspectiveVR(
                projectionMatrix,
                tan(fov.angleLeft()),
                tan(fov.angleRight()),
                tan(fov.angleUp()),
                tan(fov.angleDown()),
                nearZ, farZ, farZ <= nearZ
            )
        }

        val nearZ = 0.01f
        val farZ = 100f
    }

    abstract fun copyToDesktopWindow(w: Int, h: Int)
    abstract fun renderFrame(
        viewIndex: Int, w: Int, h: Int,
        predictedDisplayTime: Long,
        handLocations: XrSpaceLocation.Buffer?,
        colorTexture: Int, depthTexture: Int,
    )

    var state = XR_SESSION_STATE_UNKNOWN

    val viewMatrix = Matrix4f()
    val projectionMatrix = Matrix4f()

    val position = Vector3f()
    val rotation = Quaternionf()

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

    open fun beginRenderViews() {
        val session = session ?: return
        position.set(0f)
        rotation.set(0f, 0f, 0f, 0f)
        val viewCount = session.viewCount
        val views = session.views
        for (i in 0 until viewCount) {
            val pose = views[i].pose()
            val pos = pose.`position$`()
            val rot = pose.orientation()
            position.add(pos.x(), pos.y(), pos.z())
            rotation.add(rot.x(), rot.y(), rot.z(), rot.w())
        }
        position.mul(1f / max(1, viewCount))
        rotation.normalize()
    }
}