package me.anno.openxr

import me.anno.openxr.OpenXRUtils.checkXR
import me.anno.openxr.OpenXRUtils.intPtr
import me.anno.openxr.OpenXRUtils.longPtr
import me.anno.openxr.OpenXRUtils.printInstanceProperties
import me.anno.openxr.OpenXRUtils.ptr
import me.anno.openxr.OpenXRUtils.ptr1
import me.anno.openxr.OpenXRUtils.setupDebugging
import me.anno.openxr.OpenXRUtils.xrInstance
import org.apache.logging.log4j.LogManager
import org.lwjgl.PointerBuffer
import org.lwjgl.openxr.EXTDebugUtils.XR_EXT_DEBUG_UTILS_EXTENSION_NAME
import org.lwjgl.openxr.EXTHandTracking.XR_EXT_HAND_TRACKING_EXTENSION_NAME
import org.lwjgl.openxr.KHRCompositionLayerDepth.XR_KHR_COMPOSITION_LAYER_DEPTH_EXTENSION_NAME
import org.lwjgl.openxr.KHROpenGLEnable.XR_KHR_OPENGL_ENABLE_EXTENSION_NAME
import org.lwjgl.openxr.XR10.XR_CURRENT_API_VERSION
import org.lwjgl.openxr.XR10.XR_ERROR_FORM_FACTOR_UNAVAILABLE
import org.lwjgl.openxr.XR10.XR_ERROR_FORM_FACTOR_UNSUPPORTED
import org.lwjgl.openxr.XR10.XR_FORM_FACTOR_HEAD_MOUNTED_DISPLAY
import org.lwjgl.openxr.XR10.XR_TYPE_EXTENSION_PROPERTIES
import org.lwjgl.openxr.XR10.XR_TYPE_INSTANCE_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SYSTEM_GET_INFO
import org.lwjgl.openxr.XR10.xrCreateInstance
import org.lwjgl.openxr.XR10.xrEnumerateInstanceExtensionProperties
import org.lwjgl.openxr.XR10.xrGetSystem
import org.lwjgl.openxr.XrApplicationInfo
import org.lwjgl.openxr.XrExtensionProperties
import org.lwjgl.openxr.XrInstance
import org.lwjgl.openxr.XrInstanceCreateInfo
import org.lwjgl.openxr.XrSystemGetInfo
import java.nio.ByteBuffer

class OpenXRSystem(val window: Long) {

    companion object {
        private val LOGGER = LogManager.getLogger(OpenXRSystem::class)
    }

    var hasOpenGLExtension = false
    var hasHandTracking = false
    var hasDepth = false
    var hasDebug = false

    private fun checkExtensions() {
        checkXR(xrEnumerateInstanceExtensionProperties(null as ByteBuffer?, intPtr, null))
        val extCount = intPtr[0]
        LOGGER.info("Runtime supports $extCount extensions:")
        val extensions =
            XrExtensionProperties.Buffer(ByteBuffer.allocateDirect(extCount * XrExtensionProperties.SIZEOF))
        for (ext in extensions) ext.type(XR_TYPE_EXTENSION_PROPERTIES)
        checkXR(xrEnumerateInstanceExtensionProperties(null as ByteBuffer?, intPtr, extensions))
        for (i in 0 until extCount) {
            val extName = extensions[i].extensionNameString()
            LOGGER.info("Extension[$i]: $extName")
            when (extName) {
                XR_KHR_OPENGL_ENABLE_EXTENSION_NAME -> hasOpenGLExtension = true
                XR_EXT_HAND_TRACKING_EXTENSION_NAME -> hasHandTracking = true
                XR_KHR_COMPOSITION_LAYER_DEPTH_EXTENSION_NAME -> hasDepth = true
                XR_EXT_DEBUG_UTILS_EXTENSION_NAME -> hasDebug = true
            }
        }
        if (!hasOpenGLExtension) throw IllegalStateException("OpenGL isn't supported 😭")
    }

    private fun collectExtensions(): PointerBuffer {
        checkExtensions()
        val result = PointerBuffer.allocateDirect(4)
        result.put(XR_KHR_OPENGL_ENABLE_EXTENSION_NAME.ptr())
        if (hasHandTracking) result.put(XR_EXT_HAND_TRACKING_EXTENSION_NAME.ptr())
        if (hasDebug) result.put(XR_EXT_DEBUG_UTILS_EXTENSION_NAME.ptr())
        result.flip()
        return result
    }

    private fun createInstance(): XrInstance {
        val appInfo = XrApplicationInfo.calloc()
            .apiVersion(XR_CURRENT_API_VERSION)
            .applicationName("Rem's Studio".ptr1())
            .applicationVersion(1) // idk...
            .engineName("Rem's Engine".ptr1())
            .engineVersion(1) // idk...
        val instanceCreateInfo = XrInstanceCreateInfo.calloc()
            .type(XR_TYPE_INSTANCE_CREATE_INFO)
            .enabledExtensionNames(collectExtensions())
            .applicationInfo(appInfo)
            .next(0)
        LOGGER.info("Creating instance")
        val result = xrCreateInstance(instanceCreateInfo, ptr)
        instanceCreateInfo.free()
        appInfo.free()
        checkXR(result)
        LOGGER.info("Created instance")
        return XrInstance(ptr[0], instanceCreateInfo)
    }

    val instance = createInstance()

    init {
        xrInstance = instance
        setupDebugging(this, instance)
        printInstanceProperties(instance)
    }

    fun createSystemId(): Long {
        val systemGetInfo = XrSystemGetInfo.calloc()
            .type(XR_TYPE_SYSTEM_GET_INFO).next(0)
            .formFactor(XR_FORM_FACTOR_HEAD_MOUNTED_DISPLAY)
        return when (val result = xrGetSystem(instance, systemGetInfo, longPtr)) {
            XR_ERROR_FORM_FACTOR_UNAVAILABLE -> {
                LOGGER.warn("FORM_FACTOR_UNAVAILABLE")
                0L
            }
            XR_ERROR_FORM_FACTOR_UNSUPPORTED -> {
                LOGGER.warn("FORM_FACTOR_UNSUPPORTED")
                0L
            }
            else -> {
                systemGetInfo.free()
                checkXR(result)
                val systemId = longPtr[0]
                LOGGER.info("Got XrSystem with HMD id $systemId")
                systemId
            }
        }
    }

    var systemId = createSystemId()
}