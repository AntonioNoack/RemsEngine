package me.anno.openxr

import me.anno.Time
import me.anno.utils.pooling.ByteBufferPool
import org.apache.logging.log4j.LogManager
import org.lwjgl.PointerBuffer
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_TYPE_CONFORMANCE_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT
import org.lwjgl.openxr.EXTDebugUtils.xrCreateDebugUtilsMessengerEXT
import org.lwjgl.openxr.EXTHandTracking.XR_TYPE_SYSTEM_HAND_TRACKING_PROPERTIES_EXT
import org.lwjgl.openxr.KHROpenGLEnable.XR_TYPE_GRAPHICS_REQUIREMENTS_OPENGL_KHR
import org.lwjgl.openxr.KHROpenGLEnable.xrGetOpenGLGraphicsRequirementsKHR
import org.lwjgl.openxr.XR10.XR_ERROR_POSE_INVALID
import org.lwjgl.openxr.XR10.XR_EVENT_UNAVAILABLE
import org.lwjgl.openxr.XR10.XR_FRAME_DISCARDED
import org.lwjgl.openxr.XR10.XR_SUCCESS
import org.lwjgl.openxr.XR10.XR_TYPE_INSTANCE_PROPERTIES
import org.lwjgl.openxr.XR10.XR_TYPE_SYSTEM_PROPERTIES
import org.lwjgl.openxr.XR10.XR_VERSION_MAJOR
import org.lwjgl.openxr.XR10.XR_VERSION_MINOR
import org.lwjgl.openxr.XR10.XR_VERSION_PATCH
import org.lwjgl.openxr.XR10.xrGetInstanceProperties
import org.lwjgl.openxr.XR10.xrGetSystemProperties
import org.lwjgl.openxr.XR10.xrResultToString
import org.lwjgl.openxr.XrDebugUtilsMessengerCallbackDataEXT
import org.lwjgl.openxr.XrDebugUtilsMessengerCreateInfoEXT
import org.lwjgl.openxr.XrGraphicsRequirementsOpenGLKHR
import org.lwjgl.openxr.XrInstance
import org.lwjgl.openxr.XrInstanceProperties
import org.lwjgl.openxr.XrSystemHandTrackingPropertiesEXT
import org.lwjgl.openxr.XrSystemProperties
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer

object OpenXRUtils {

    private val LOGGER = LogManager.getLogger(OpenXRUtils::class)

    val intPtr: IntBuffer
    val longPtr: LongBuffer

    init {
        val ptrBuffer = ByteBufferPool.allocateDirect(8)
        intPtr = ptrBuffer.asIntBuffer()
        longPtr = ptrBuffer.asLongBuffer()
    }

    val ptr: PointerBuffer = PointerBuffer.allocateDirect(1)

    fun printSystemProperties(xr: OpenXRSystem, sp: XrSystemProperties, ht: XrSystemHandTrackingPropertiesEXT?) {
        LOGGER.info("System properties for ${sp.systemId()}, ${sp.systemNameString()}, vendor ${sp.vendorId()}")
        val gp = sp.graphicsProperties()
        LOGGER.info("Max layers: ${gp.maxLayerCount()}")
        LOGGER.info("Max swapchain size: ${gp.maxSwapchainImageWidth()} x ${gp.maxSwapchainImageHeight()}")
        val tp = sp.trackingProperties()
        LOGGER.info("Orientation tracking: ${tp.orientationTracking()}")
        LOGGER.info("Position tracking: ${tp.positionTracking()}")
        xr.hasHandTracking = if (ht != null) {
            LOGGER.info("Hand tracking: ${ht.supportsHandTracking()}")
            ht.supportsHandTracking()
        } else false
    }

    fun checkHandTrackingAndPrintSystemProperties(xr: OpenXRSystem, instance: XrInstance, systemId: Long) {
        // optional hand tracking queries
        val sp = XrSystemProperties.calloc()
            .type(XR_TYPE_SYSTEM_PROPERTIES)
            .next(0)
        val ht = if (xr.hasHandTracking) {
            val ht = XrSystemHandTrackingPropertiesEXT.calloc()
                .type(XR_TYPE_SYSTEM_HAND_TRACKING_PROPERTIES_EXT)
            sp.next(ht)
            ht
        } else null
        checkXR(xrGetSystemProperties(instance, systemId, sp))
        printSystemProperties(xr, sp, ht)
    }

    fun printInstanceProperties(instance: XrInstance) {
        val ip = XrInstanceProperties.create()
            .type(XR_TYPE_INSTANCE_PROPERTIES)
            .next(0)
        checkXR(xrGetInstanceProperties(instance, ip))
        LOGGER.info("Runtime-Name: ${ip.runtimeNameString()}")
        val version = ip.runtimeVersion()
        LOGGER.info(
            "Runtime-Version: " +
                    "${XR_VERSION_MAJOR(version)}." +
                    "${XR_VERSION_MINOR(version)}." +
                    "${XR_VERSION_PATCH(version)}"
        )
    }

    fun setupDebugging(xr: OpenXRSystem, instance: XrInstance) {
        if (xr.hasDebug) {
            val debugInfo = XrDebugUtilsMessengerCreateInfoEXT.calloc()
                .type(XR_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverities(
                    (XR_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT or
                            XR_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT or
                            XR_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT or
                            XR_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT).toLong()
                )
                .messageTypes(
                    (XR_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT or
                            XR_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT or
                            XR_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT or
                            XR_DEBUG_UTILS_MESSAGE_TYPE_CONFORMANCE_BIT_EXT).toLong()
                )
                .userCallback { severity, type, callbackData, _ ->
                    val data = XrDebugUtilsMessengerCallbackDataEXT.create(callbackData)
                    val typeName = when (type.toInt()) {
                        XR_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT -> "General"
                        XR_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT -> "Validation"
                        XR_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT -> "Performance"
                        XR_DEBUG_UTILS_MESSAGE_TYPE_CONFORMANCE_BIT_EXT -> "Conformance"
                        else -> type.toString()
                    }
                    val msg = "[$typeName] '${data.messageString()}', func: ${data.functionNameString()}"
                    when (severity.toInt()) {
                        XR_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT -> LOGGER.info(msg)
                        XR_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT -> LOGGER.fine(msg)
                        XR_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT -> LOGGER.error(msg)
                        else -> LOGGER.warn(msg)
                    }
                    0
                }
            val ptr = PointerBuffer.allocateDirect(1)
            checkXR(xrCreateDebugUtilsMessengerEXT(instance, debugInfo, ptr))
        }
    }

    fun checkOpenGLRequirements(systemId: Long, instance: XrInstance) {
        val openGLRequirements = XrGraphicsRequirementsOpenGLKHR.calloc()
            .type(XR_TYPE_GRAPHICS_REQUIREMENTS_OPENGL_KHR).next(0)
        checkXR(xrGetOpenGLGraphicsRequirementsKHR(instance, systemId, openGLRequirements))
        val min = openGLRequirements.minApiVersionSupported()
        val max = openGLRequirements.maxApiVersionSupported()
        LOGGER.info( // 4.3 to 4.6 for Meta Quest 3 in SteamVR, 4.0 to 4.6 in Meta Quest Link
            "Graphics Requirements: " +
                    "${XR_VERSION_MAJOR(min)}.${XR_VERSION_MINOR(min)} to " +
                    "${XR_VERSION_MAJOR(max)}.${XR_VERSION_MINOR(max)}"
        )
    }

    val stringBuffers = HashMap<String, ByteBuffer>() // keep them in memory, so we don't get any ugly segfaults
    fun String.ptr(): Long {
        return MemoryUtil.memAddress(ptr1())
    }

    fun String.ptr1(): ByteBuffer {
        return stringBuffers.getOrPut(this) {
            MemoryUtil.memUTF8(this)
        }
    }

    var xrInstance: XrInstance? = null
    val xrResultBuffer = ByteBufferPool.allocateDirect(256)
    fun checkXR(result: Int): Boolean {
        if (result == XR_SUCCESS) return false
        if (result == XR_EVENT_UNAVAILABLE) return true
        if (result == XR_ERROR_POSE_INVALID) {
            LOGGER.warn("[${Time.nanoTime}] Invalid pose")
            return true
        }
        if (result == XR_FRAME_DISCARDED) {
            LOGGER.warn("[${Time.nanoTime}] Frame discarded")
            return true
        }
        val instance = xrInstance ?: throw IllegalStateException("Error $result")
        if (xrResultToString(instance, result, xrResultBuffer) == XR_SUCCESS) {
            val stringLength = (0 until xrResultBuffer.capacity())
                .first { idx -> xrResultBuffer[idx].toInt() == 0 }
            val bytes = ByteArray(stringLength)
            xrResultBuffer.get(bytes).position(0)
            val string = bytes.decodeToString()
            throw IllegalStateException("Error: $string")
        } else throw IllegalStateException("Error getting error! $result -> ?")
    }
}