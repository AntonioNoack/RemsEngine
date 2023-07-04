package me.anno.tests.openxr

import org.lwjgl.PointerBuffer
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.openxr.*
import org.lwjgl.openxr.EXTHandTracking.XR_EXT_HAND_TRACKING_EXTENSION_NAME
import org.lwjgl.openxr.EXTHandTracking.XR_TYPE_SYSTEM_HAND_TRACKING_PROPERTIES_EXT
import org.lwjgl.openxr.KHRCompositionLayerCylinder.XR_KHR_COMPOSITION_LAYER_CYLINDER_EXTENSION_NAME
import org.lwjgl.openxr.KHRCompositionLayerDepth.XR_KHR_COMPOSITION_LAYER_DEPTH_EXTENSION_NAME
import org.lwjgl.openxr.KHROpenGLEnable.*
import org.lwjgl.openxr.XR10.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.nio.LongBuffer

fun main() {
    // https://learn.microsoft.com/en-us/windows/mixed-reality/develop/advanced-concepts/using-the-windows-mixed-reality-simulator
    // -> doesn't work with OpenGL nor Vulkan :/
    // https://gitlab.freedesktop.org/monado/monado/-/jobs/44014701
    // -> works for OpenGL ❤
    initOpenXR()
}

fun printSystemProperties(sp: XrSystemProperties, handTrackingExt: Boolean) {
    println("System properties for ${sp.systemId()}, ${sp.systemNameString()}, vendor ${sp.vendorId()}")
    val gp = sp.graphicsProperties()
    println("Max layers: ${gp.maxLayerCount()}")
    println("Max swapchain size: ${gp.maxSwapchainImageWidth()} x ${gp.maxSwapchainImageHeight()}")
    val tp = sp.trackingProperties()
    println("Orientation tracking: ${tp.orientationTracking()}")
    println("Position tracking: ${tp.positionTracking()}")
    if (handTrackingExt) {
        // what?!?, why is this legal?
        val ht = XrSystemHandTrackingPropertiesEXT.create(sp.next())
        println("Hand tracking: ${ht.supportsHandTracking()}")
    }
}

fun printSupportedViewConfigs(instance: XrInstance, systemId: Long) {
    val viewConfigCountB = ByteBuffer.allocateDirect(4).asIntBuffer()
    xrResult(instance, xrEnumerateViewConfigurationViews(instance, systemId, 0, viewConfigCountB, null))
    val viewConfigCount = viewConfigCountB[0]
    println("Runtime supports $viewConfigCount view configs")
    val views =
        XrViewConfigurationView.Buffer(ByteBuffer.allocateDirect(viewConfigCount * XrViewConfigurationView.SIZEOF))
    xrResult(instance, xrEnumerateViewConfigurationViews(instance, systemId, viewConfigCount, viewConfigCountB, views))
    val props = XrViewConfigurationProperties.create()
    for (i in 0 until viewConfigCount) {
        xrGetViewConfigurationProperties(instance, systemId, views[i].type(), props)
        println("[$i] type: ${props.viewConfigurationType()}, fov mutable? ${props.fovMutable()}")
    }
}

fun printViewConfigViewInfo(instance: XrInstance) {
    // ...
}

fun printReferenceSpaces(instance: XrInstance, session: XrSession) {
    val spaceCount = ByteBuffer.allocateDirect(4).asIntBuffer()
    xrResult(instance, xrEnumerateReferenceSpaces(session, spaceCount, null))
    val spaces = ByteBuffer.allocateDirect(4 * spaceCount[0]).asIntBuffer()
    xrResult(instance, xrEnumerateReferenceSpaces(session, spaceCount, spaces))
    println("Supported reference spaces (${spaceCount[0]}):")
    for (i in 0 until spaceCount[0]) {
        println(
            when (val space = spaces[i]) {
                XR_REFERENCE_SPACE_TYPE_LOCAL -> "Local"
                XR_REFERENCE_SPACE_TYPE_STAGE -> "stage"
                XR_REFERENCE_SPACE_TYPE_VIEW -> "view"
                else -> "Unknown ($space)"
            }
        )
    }
}

val countBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
fun initOpenXR() {
    xrResult(null, xrEnumerateInstanceExtensionProperties(null as ByteBuffer?, countBuffer, null))
    val extCount = countBuffer[0]
    println("Runtime supports $extCount extensions:")
    val extensions = XrExtensionProperties.Buffer(ByteBuffer.allocateDirect(extCount * XrExtensionProperties.SIZEOF))
    for (ext in extensions) ext.type(XR_TYPE_EXTENSION_PROPERTIES)
    xrResult(null, xrEnumerateInstanceExtensionProperties(null as ByteBuffer?, countBuffer, extensions))
    var hasOpenGLExtension = false
    var hasHandTracking = false
    var hasCylinder = false // ???
    var hasDepth = false
    for (i in 0 until extCount) {
        val extName = extensions[i].extensionNameString()
        println("Extension[$i]: $extName")
        when (extName) {
            XR_KHR_OPENGL_ENABLE_EXTENSION_NAME -> hasOpenGLExtension = true
            XR_EXT_HAND_TRACKING_EXTENSION_NAME -> hasHandTracking = true
            XR_KHR_COMPOSITION_LAYER_CYLINDER_EXTENSION_NAME -> hasCylinder = true
            XR_KHR_COMPOSITION_LAYER_DEPTH_EXTENSION_NAME -> hasDepth = true
        }
    }
    if (!hasOpenGLExtension) throw IllegalStateException("OpenGL isn't supported 😭")
    val instanceCreateInfo = XrInstanceCreateInfo.create()
    instanceCreateInfo.type(XR_TYPE_INSTANCE_CREATE_INFO) // peinlich...
    val extensionsB = PointerBuffer.allocateDirect(3)
    extensionsB.put(XR_KHR_OPENGL_ENABLE_EXTENSION_NAME.ptr())
    if (hasHandTracking) extensionsB.put(XR_EXT_HAND_TRACKING_EXTENSION_NAME.ptr())
    if (hasCylinder) extensionsB.put(XR_KHR_COMPOSITION_LAYER_CYLINDER_EXTENSION_NAME.ptr())
    extensionsB.flip()
    instanceCreateInfo.enabledExtensionNames(extensionsB)
    val appInfo = XrApplicationInfo.create()
    appInfo.apiVersion(XR_CURRENT_API_VERSION)
    appInfo.applicationName("Rem's Studio".ptr1())
    appInfo.applicationVersion(1) // idk...
    appInfo.engineName("Rem's Engine".ptr1())
    appInfo.engineVersion(1) // idk...
    instanceCreateInfo.applicationInfo(appInfo)
    val instanceB = PointerBuffer.allocateDirect(1)
    println("Creating instance")
    xrResult(null, xrCreateInstance(instanceCreateInfo, instanceB))
    println("Created instance")
    val instance = XrInstance(instanceB[0], instanceCreateInfo)
    getInstanceProperties(instance)
    val systemGetInfo = XrSystemGetInfo.create()
    systemGetInfo.type(XR_TYPE_SYSTEM_GET_INFO)
    systemGetInfo.formFactor(XR_FORM_FACTOR_HEAD_MOUNTED_DISPLAY)
    val systemIdB = LongBuffer.allocate(1)
    xrResult(instance, xrGetSystem(instance, systemGetInfo, systemIdB))
    val systemId = systemIdB[0]
    println("Got XrSystem with HMD id $systemId")
    // optional hand tracking queries
    if (hasHandTracking) {
        val sp = XrSystemProperties.create()
        sp.type(XR_TYPE_SYSTEM_PROPERTIES)
        val ht = XrSystemHandTrackingPropertiesEXT.create()
        ht.type(XR_TYPE_SYSTEM_HAND_TRACKING_PROPERTIES_EXT)
        sp.next(ht)
        xrResult(instance, xrGetSystemProperties(instance, systemId, sp))
        hasHandTracking = ht.supportsHandTracking()
    }
    printSupportedViewConfigs(instance, systemId)
    val viewType = XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO
    val viewCountB = IntBuffer.allocate(1)
    xrResult(instance, xrEnumerateViewConfigurationViews(instance, systemId, viewType, viewCountB, null))
    val viewConfigViews =
        XrViewConfigurationView.Buffer(ByteBuffer.allocateDirect(viewCountB[0] * XrViewConfigurationView.SIZEOF))
    xrResult(instance, xrEnumerateViewConfigurationViews(instance, systemId, viewType, viewCountB, viewConfigViews))
    printViewConfigViewInfo(instance)

    val openGLRequirements = XrGraphicsRequirementsOpenGLKHR.create()
    openGLRequirements.type(XR_TYPE_GRAPHICS_REQUIREMENTS_OPENGL_KHR)

    xrResult(instance, xrGetOpenGLGraphicsRequirementsKHR(instance, systemId, openGLRequirements))
    // todo check OpenGL version?

    val graphicsBindingGL = XrGraphicsBindingOpenGLWin32KHR.create()
    graphicsBindingGL.type(XR_TYPE_GRAPHICS_BINDING_OPENGL_WIN32_KHR) // Linux? Wayland?

    // todo create desktop window with size of left eye?

    println("Using OpenGL version ${glGetString(GL_VERSION)}")
    println("Using OpenGL renderer ${glGetString(GL_RENDERER)}")

    // todo init gl (?)

    val state = XR_SESSION_STATE_UNKNOWN
    val sessionCreateInfo = XrSessionCreateInfo.create()
    sessionCreateInfo.type(XR_TYPE_SESSION_CREATE_INFO)
    sessionCreateInfo.next(graphicsBindingGL)
    sessionCreateInfo.systemId(systemId)

    val sessionB = PointerBuffer.allocateDirect(1)
    xrResult(instance, xrCreateSession(instance, sessionCreateInfo, sessionB))
    val session = XrSession(sessionB[0], instance)

    println("Created session with OpenGL 😃")

    if (hasHandTracking) {

    }

    // todo continue
    // https://github.com/KHeresy/openxr-simple-example/blob/master/main.cpp
}

val stringPtrs = ArrayList<ByteBuffer>() // keep them in memory
fun String.ptr(): Long {
    return MemoryUtil.memAddress(ptr1())
}

fun String.ptr1(): ByteBuffer {
    val buffer = MemoryUtil.memUTF8(this)
    stringPtrs.add(buffer)
    return buffer
}

fun getInstanceProperties(instance: XrInstance): XrInstanceProperties {
    val ip = XrInstanceProperties.create()
    xrResult(instance, xrGetInstanceProperties(instance, ip))
    println("Runtime-Name: ${ip.runtimeName()}")
    println(
        "Runtime-Version: " +
                "${XR_VERSION_MAJOR(ip.runtimeVersion())}." +
                "${XR_VERSION_MINOR(ip.runtimeVersion())}." +
                "${XR_VERSION_PATCH(ip.runtimeVersion())}"
    )
    return ip
}

val xrResultBuffer = ByteBuffer.allocateDirect(256)
fun xrResult(instance: XrInstance?, result: Int) {
    if (result == XR_SUCCESS) return
    if (instance == null) {
        throw IllegalStateException("Error $result")
    }
    if (xrResultToString(instance, result, xrResultBuffer) == XR_SUCCESS) {
        val stringLength = (0 until xrResultBuffer.capacity()).indexOfFirst { it == 0 }
        val string = String(CharArray(stringLength) { xrResultBuffer[it].toChar() })
        throw IllegalStateException("Error: $string")
    } else throw IllegalStateException("Error getting error! $result -> ?")
}