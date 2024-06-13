package me.anno.tests.openxr

import me.anno.gpu.RenderDoc
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Floats.toRadians
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE
import org.lwjgl.glfw.GLFW.GLFW_TRUE
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwGetWindowSize
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.glfw.GLFW.glfwSwapInterval
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import org.lwjgl.glfw.GLFWNativeWGL.glfwGetWGLContext
import org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window
import org.lwjgl.opengl.EXTTextureSRGB.GL_SRGB8_ALPHA8_EXT
import org.lwjgl.opengl.EXTTextureSRGB.GL_SRGB8_EXT
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C.GL_RGB8
import org.lwjgl.opengl.GL11C.GL_RGBA8
import org.lwjgl.opengl.GL14C.GL_DEPTH_COMPONENT16
import org.lwjgl.opengl.GL14C.GL_DEPTH_COMPONENT24
import org.lwjgl.opengl.GL14C.GL_DEPTH_COMPONENT32
import org.lwjgl.opengl.GL30C.GL_DEPTH_COMPONENT32F
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_TYPE_CONFORMANCE_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
import org.lwjgl.openxr.EXTDebugUtils.XR_EXT_DEBUG_UTILS_EXTENSION_NAME
import org.lwjgl.openxr.EXTDebugUtils.XR_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT
import org.lwjgl.openxr.EXTDebugUtils.xrCreateDebugUtilsMessengerEXT
import org.lwjgl.openxr.EXTHandTracking.XR_EXT_HAND_TRACKING_EXTENSION_NAME
import org.lwjgl.openxr.EXTHandTracking.XR_TYPE_SYSTEM_HAND_TRACKING_PROPERTIES_EXT
import org.lwjgl.openxr.KHRCompositionLayerCylinder.XR_KHR_COMPOSITION_LAYER_CYLINDER_EXTENSION_NAME
import org.lwjgl.openxr.KHRCompositionLayerDepth.XR_KHR_COMPOSITION_LAYER_DEPTH_EXTENSION_NAME
import org.lwjgl.openxr.KHRCompositionLayerDepth.XR_TYPE_COMPOSITION_LAYER_DEPTH_INFO_KHR
import org.lwjgl.openxr.KHROpenGLEnable.XR_KHR_OPENGL_ENABLE_EXTENSION_NAME
import org.lwjgl.openxr.KHROpenGLEnable.XR_TYPE_GRAPHICS_BINDING_OPENGL_WIN32_KHR
import org.lwjgl.openxr.KHROpenGLEnable.XR_TYPE_GRAPHICS_REQUIREMENTS_OPENGL_KHR
import org.lwjgl.openxr.KHROpenGLEnable.XR_TYPE_SWAPCHAIN_IMAGE_OPENGL_KHR
import org.lwjgl.openxr.KHROpenGLEnable.xrGetOpenGLGraphicsRequirementsKHR
import org.lwjgl.openxr.XR10
import org.lwjgl.openxr.XR10.XR_CURRENT_API_VERSION
import org.lwjgl.openxr.XR10.XR_ENVIRONMENT_BLEND_MODE_OPAQUE
import org.lwjgl.openxr.XR10.XR_EVENT_UNAVAILABLE
import org.lwjgl.openxr.XR10.XR_FORM_FACTOR_HANDHELD_DISPLAY
import org.lwjgl.openxr.XR10.XR_FORM_FACTOR_HEAD_MOUNTED_DISPLAY
import org.lwjgl.openxr.XR10.XR_REFERENCE_SPACE_TYPE_LOCAL
import org.lwjgl.openxr.XR10.XR_REFERENCE_SPACE_TYPE_STAGE
import org.lwjgl.openxr.XR10.XR_REFERENCE_SPACE_TYPE_VIEW
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_EXITING
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_FOCUSED
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_IDLE
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_LOSS_PENDING
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_READY
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_STOPPING
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_SYNCHRONIZED
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_UNKNOWN
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_VISIBLE
import org.lwjgl.openxr.XR10.XR_SUCCESS
import org.lwjgl.openxr.XR10.XR_SWAPCHAIN_USAGE_COLOR_ATTACHMENT_BIT
import org.lwjgl.openxr.XR10.XR_SWAPCHAIN_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT
import org.lwjgl.openxr.XR10.XR_SWAPCHAIN_USAGE_SAMPLED_BIT
import org.lwjgl.openxr.XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION
import org.lwjgl.openxr.XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW
import org.lwjgl.openxr.XR10.XR_TYPE_EVENT_DATA_BUFFER
import org.lwjgl.openxr.XR10.XR_TYPE_EVENT_DATA_INSTANCE_LOSS_PENDING
import org.lwjgl.openxr.XR10.XR_TYPE_EVENT_DATA_INTERACTION_PROFILE_CHANGED
import org.lwjgl.openxr.XR10.XR_TYPE_EVENT_DATA_REFERENCE_SPACE_CHANGE_PENDING
import org.lwjgl.openxr.XR10.XR_TYPE_EVENT_DATA_SESSION_STATE_CHANGED
import org.lwjgl.openxr.XR10.XR_TYPE_EXTENSION_PROPERTIES
import org.lwjgl.openxr.XR10.XR_TYPE_FRAME_BEGIN_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_FRAME_END_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_FRAME_STATE
import org.lwjgl.openxr.XR10.XR_TYPE_FRAME_WAIT_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_INSTANCE_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_INSTANCE_PROPERTIES
import org.lwjgl.openxr.XR10.XR_TYPE_REFERENCE_SPACE_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SESSION_BEGIN_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SESSION_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SWAPCHAIN_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SWAPCHAIN_IMAGE_ACQUIRE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SWAPCHAIN_IMAGE_RELEASE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SWAPCHAIN_IMAGE_WAIT_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SYSTEM_GET_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SYSTEM_PROPERTIES
import org.lwjgl.openxr.XR10.XR_TYPE_VIEW
import org.lwjgl.openxr.XR10.XR_TYPE_VIEW_CONFIGURATION_VIEW
import org.lwjgl.openxr.XR10.XR_VERSION_MAJOR
import org.lwjgl.openxr.XR10.XR_VERSION_MINOR
import org.lwjgl.openxr.XR10.XR_VERSION_PATCH
import org.lwjgl.openxr.XR10.XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO
import org.lwjgl.openxr.XR10.XR_VIEW_STATE_ORIENTATION_VALID_BIT
import org.lwjgl.openxr.XR10.xrAcquireSwapchainImage
import org.lwjgl.openxr.XR10.xrBeginFrame
import org.lwjgl.openxr.XR10.xrBeginSession
import org.lwjgl.openxr.XR10.xrCreateInstance
import org.lwjgl.openxr.XR10.xrCreateReferenceSpace
import org.lwjgl.openxr.XR10.xrCreateSession
import org.lwjgl.openxr.XR10.xrCreateSwapchain
import org.lwjgl.openxr.XR10.xrDestroyInstance
import org.lwjgl.openxr.XR10.xrDestroySession
import org.lwjgl.openxr.XR10.xrEndFrame
import org.lwjgl.openxr.XR10.xrEndSession
import org.lwjgl.openxr.XR10.xrEnumerateInstanceExtensionProperties
import org.lwjgl.openxr.XR10.xrEnumerateReferenceSpaces
import org.lwjgl.openxr.XR10.xrEnumerateSwapchainFormats
import org.lwjgl.openxr.XR10.xrEnumerateSwapchainImages
import org.lwjgl.openxr.XR10.xrEnumerateViewConfigurationViews
import org.lwjgl.openxr.XR10.xrGetInstanceProperties
import org.lwjgl.openxr.XR10.xrGetSystem
import org.lwjgl.openxr.XR10.xrGetSystemProperties
import org.lwjgl.openxr.XR10.xrGetViewConfigurationProperties
import org.lwjgl.openxr.XR10.xrLocateViews
import org.lwjgl.openxr.XR10.xrPollEvent
import org.lwjgl.openxr.XR10.xrReleaseSwapchainImage
import org.lwjgl.openxr.XR10.xrResultToString
import org.lwjgl.openxr.XR10.xrWaitFrame
import org.lwjgl.openxr.XR10.xrWaitSwapchainImage
import org.lwjgl.openxr.XrApplicationInfo
import org.lwjgl.openxr.XrCompositionLayerBaseHeader
import org.lwjgl.openxr.XrCompositionLayerDepthInfoKHR
import org.lwjgl.openxr.XrCompositionLayerProjection
import org.lwjgl.openxr.XrCompositionLayerProjectionView
import org.lwjgl.openxr.XrDebugUtilsMessengerCallbackDataEXT
import org.lwjgl.openxr.XrDebugUtilsMessengerCreateInfoEXT
import org.lwjgl.openxr.XrEventDataBuffer
import org.lwjgl.openxr.XrEventDataSessionStateChanged
import org.lwjgl.openxr.XrExtensionProperties
import org.lwjgl.openxr.XrFovf
import org.lwjgl.openxr.XrFrameBeginInfo
import org.lwjgl.openxr.XrFrameEndInfo
import org.lwjgl.openxr.XrFrameState
import org.lwjgl.openxr.XrFrameWaitInfo
import org.lwjgl.openxr.XrGraphicsBindingOpenGLWin32KHR
import org.lwjgl.openxr.XrGraphicsRequirementsOpenGLKHR
import org.lwjgl.openxr.XrInstance
import org.lwjgl.openxr.XrInstanceCreateInfo
import org.lwjgl.openxr.XrInstanceProperties
import org.lwjgl.openxr.XrPosef
import org.lwjgl.openxr.XrQuaternionf
import org.lwjgl.openxr.XrReferenceSpaceCreateInfo
import org.lwjgl.openxr.XrSession
import org.lwjgl.openxr.XrSessionBeginInfo
import org.lwjgl.openxr.XrSessionCreateInfo
import org.lwjgl.openxr.XrSpace
import org.lwjgl.openxr.XrSwapchain
import org.lwjgl.openxr.XrSwapchainCreateInfo
import org.lwjgl.openxr.XrSwapchainImageAcquireInfo
import org.lwjgl.openxr.XrSwapchainImageBaseHeader
import org.lwjgl.openxr.XrSwapchainImageOpenGLKHR
import org.lwjgl.openxr.XrSwapchainImageReleaseInfo
import org.lwjgl.openxr.XrSwapchainImageWaitInfo
import org.lwjgl.openxr.XrSystemGetInfo
import org.lwjgl.openxr.XrSystemHandTrackingPropertiesEXT
import org.lwjgl.openxr.XrSystemProperties
import org.lwjgl.openxr.XrVector3f
import org.lwjgl.openxr.XrView
import org.lwjgl.openxr.XrViewConfigurationProperties
import org.lwjgl.openxr.XrViewConfigurationView
import org.lwjgl.openxr.XrViewLocateInfo
import org.lwjgl.openxr.XrViewState
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.windows.User32.GetDC
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import kotlin.math.max


/**
 * My test device: Meta Quest 3
 * Working with SteamVR
 * Not yet working on Meta Link
 * */
fun main() {
    val debugRendering = false
    if (debugRendering) {
        RenderDoc.forceLoadRenderDoc()
    }
    initGLFW()
    if (debugRendering) {
        runSimpleRenderLoop()
    } else {
        initOpenXR()
    }
}

fun runSimpleRenderLoop() {
    initGL(emptyArray())
    val fov = XrFovf.calloc()
        .angleUp(45f.toRadians())
        .angleDown((-45f).toRadians())
        .angleRight(45f.toRadians())
        .angleLeft((-45f).toRadians())
    val projectionMatrix = Matrix4f()
    createProjectionFov(projectionMatrix, fov, 0.01f, 100f)
    val viewMatrix = Matrix4f()
    val pos = XrVector3f.calloc()
    val rot0 = Quaternionf()
    val rot = XrQuaternionf.calloc()
    val ws = IntArray(1)
    val hs = IntArray(1)
    while (!glfwWindowShouldClose(window)) {
        glfwPollEvents()
        glfwGetWindowSize(window, ws, hs)
        rot0.rotateY(0.1f)
        rot.set(rot0.x, rot0.y, rot0.z, rot0.w)
        createViewMatrix(viewMatrix, pos, rot)
        renderFrame(
            ws[0], hs[0],0, -1, null,
            projectionMatrix, viewMatrix, 0, 0, 0
        )
        glfwSwapBuffers(window)
    }
}

var window = 0L

fun initGLFW() {
    assertTrue(glfwInit())
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6)
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
    // glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    window = glfwCreateWindow(800, 600, "OpenXR", 0, 0)
    assertNotEquals(0, window)
    glfwMakeContextCurrent(window)
    GL.createCapabilities()
}

fun printSystemProperties(sp: XrSystemProperties, ht: XrSystemHandTrackingPropertiesEXT?) {
    println("System properties for ${sp.systemId()}, ${sp.systemNameString()}, vendor ${sp.vendorId()}")
    val gp = sp.graphicsProperties()
    println("Max layers: ${gp.maxLayerCount()}")
    println("Max swapchain size: ${gp.maxSwapchainImageWidth()} x ${gp.maxSwapchainImageHeight()}")
    val tp = sp.trackingProperties()
    println("Orientation tracking: ${tp.orientationTracking()}")
    println("Position tracking: ${tp.positionTracking()}")
    hasHandTracking = if (ht != null) {
        println("Hand tracking: ${ht.supportsHandTracking()}")
        ht.supportsHandTracking()
    } else false
}

var hasOpenGLExtension = false
var hasHandTracking = false
var hasCylinder = false // what is that???
var hasDepth = false
var hasDebug = false

lateinit var framebuffers: Array<IntArray>

fun checkExtensions() {
    checkXR(xrEnumerateInstanceExtensionProperties(null as ByteBuffer?, countBuffer, null))
    val extCount = countBuffer[0]
    println("Runtime supports $extCount extensions:")
    val extensions = XrExtensionProperties.Buffer(ByteBuffer.allocateDirect(extCount * XrExtensionProperties.SIZEOF))
    for (ext in extensions) ext.type(XR_TYPE_EXTENSION_PROPERTIES)
    checkXR(xrEnumerateInstanceExtensionProperties(null as ByteBuffer?, countBuffer, extensions))
    for (i in 0 until extCount) {
        val extName = extensions[i].extensionNameString()
        println("Extension[$i]: $extName")
        when (extName) {
            XR_KHR_OPENGL_ENABLE_EXTENSION_NAME -> hasOpenGLExtension = true
            XR_EXT_HAND_TRACKING_EXTENSION_NAME -> hasHandTracking = true
            XR_KHR_COMPOSITION_LAYER_CYLINDER_EXTENSION_NAME -> hasCylinder = true
            XR_KHR_COMPOSITION_LAYER_DEPTH_EXTENSION_NAME -> hasDepth = true
            XR_EXT_DEBUG_UTILS_EXTENSION_NAME -> hasDebug = true
        }
    }
    if (!hasOpenGLExtension) throw IllegalStateException("OpenGL isn't supported 😭")
}

fun setupDebugging(instance: XrInstance) {
    if (hasDebug) {
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
            .userCallback { severity, type, callbackData, userData ->
                val data = XrDebugUtilsMessengerCallbackDataEXT.create(callbackData)
                System.err.println("[Debug,$severity,$type] '${data.messageString()}', func: ${data.functionNameString()}")
                0
            }
        val ptr = PointerBuffer.allocateDirect(1)
        checkXR(xrCreateDebugUtilsMessengerEXT(instance, debugInfo, ptr))
    }
}

val countBuffer: IntBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
fun initOpenXR() {
    // glfwSwapInterval(0) // we don't want to be limited by the desktop refresh rate
    checkExtensions() // causes segfault down the line on SteamVR?
    val extensionsB = PointerBuffer.allocateDirect(4)
    extensionsB.put(XR_KHR_OPENGL_ENABLE_EXTENSION_NAME.ptr())
    if (hasHandTracking) extensionsB.put(XR_EXT_HAND_TRACKING_EXTENSION_NAME.ptr())
    if (hasCylinder) extensionsB.put(XR_KHR_COMPOSITION_LAYER_CYLINDER_EXTENSION_NAME.ptr())
    if (hasDebug) extensionsB.put(XR_EXT_DEBUG_UTILS_EXTENSION_NAME.ptr())
    extensionsB.flip()
    val appInfo = XrApplicationInfo.calloc()
        .apiVersion(XR_CURRENT_API_VERSION)
        .applicationName("Rem's Studio".ptr1())
        .applicationVersion(1) // idk...
        .engineName("Rem's Engine".ptr1())
        .engineVersion(1) // idk...
    val instanceCreateInfo = XrInstanceCreateInfo.calloc()
        .type(XR_TYPE_INSTANCE_CREATE_INFO)
        .enabledExtensionNames(extensionsB)
        .applicationInfo(appInfo)
        .next(0)
    val instanceB = PointerBuffer.allocateDirect(1)
    println("Creating instance")
    checkXR(xrCreateInstance(instanceCreateInfo, instanceB))
    println("Created instance")
    instance = XrInstance(instanceB[0], instanceCreateInfo)
    val instance = instance!!
    setupDebugging(instance)
    getInstanceProperties(instance)
    val systemGetInfo = XrSystemGetInfo.calloc()
        .type(XR_TYPE_SYSTEM_GET_INFO)
        .formFactor(
            if (true) XR_FORM_FACTOR_HEAD_MOUNTED_DISPLAY
            else XR_FORM_FACTOR_HANDHELD_DISPLAY
        )
        .next(0)
    println("Getting System")
    val systemIdB = ByteBuffer.allocateDirect(8)
        .order(ByteOrder.nativeOrder()).asLongBuffer()
    checkXR(xrGetSystem(instance, systemGetInfo, systemIdB))
    val systemId = systemIdB[0]
    println("Got XrSystem with HMD id $systemId")

    // optional hand tracking queries
    val sp = XrSystemProperties.calloc()
        .type(XR_TYPE_SYSTEM_PROPERTIES)
        .next(0)
    val ht = if (hasHandTracking) {
        val ht = XrSystemHandTrackingPropertiesEXT.calloc()
            .type(XR_TYPE_SYSTEM_HAND_TRACKING_PROPERTIES_EXT)
        sp.next(ht)
        ht
    } else null
    checkXR(xrGetSystemProperties(instance, systemId, sp))
    printSystemProperties(sp, ht)

    // printSupportedViewConfigs(instance, systemId)

    val viewConfigType = XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO
    val viewCountBuffer = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).asIntBuffer()
    checkXR(xrEnumerateViewConfigurationViews(instance, systemId, viewConfigType, viewCountBuffer, null))

    val viewCount = viewCountBuffer[0]
    val viewConfigViews = XrViewConfigurationView.calloc(viewCount)
    for (i in 0 until viewCount) {
        viewConfigViews[i]
            .type(XR_TYPE_VIEW_CONFIGURATION_VIEW)
            .next(0)
    }
    checkXR(xrEnumerateViewConfigurationViews(instance, systemId, viewConfigType, viewCountBuffer, viewConfigViews))
    // printSupportedViewConfigs(instance, systemId, viewConfigType, viewConfigViews)

    val openGLRequirements = XrGraphicsRequirementsOpenGLKHR.calloc()
        .type(XR_TYPE_GRAPHICS_REQUIREMENTS_OPENGL_KHR)
        .next(0)

    checkXR(xrGetOpenGLGraphicsRequirementsKHR(instance, systemId, openGLRequirements))
    println( // 4.3 to 4.6 for Meta Quest 3
        "got graphics requirements: " +
                "${XR_VERSION_MAJOR(openGLRequirements.minApiVersionSupported())}." +
                "${XR_VERSION_MINOR(openGLRequirements.minApiVersionSupported())} to " +
                "${XR_VERSION_MAJOR(openGLRequirements.maxApiVersionSupported())}." +
                "${XR_VERSION_MINOR(openGLRequirements.maxApiVersionSupported())}"
    )

    val hGLRC = glfwGetWGLContext(window)
    val hDC = GetDC(glfwGetWin32Window((window)))
    println("wglContext: $hGLRC, dc: $hDC")

    val graphicsBinding = XrGraphicsBindingOpenGLWin32KHR.calloc()
        .type(XR_TYPE_GRAPHICS_BINDING_OPENGL_WIN32_KHR) // Linux? Wayland?
        // -> todo linux needs different swapchain code
        // https://gitlab.freedesktop.org/monado/demos/openxr-simple-example/-/blob/master/main.c?ref_type=heads
        .next(0)
        .hDC(hDC)
        .hGLRC(hGLRC)

    val sessionCreateInfo = XrSessionCreateInfo.calloc()
        .type(XR_TYPE_SESSION_CREATE_INFO)
        .next(graphicsBinding)
        .systemId(systemId)

    val sessionPointer = PointerBuffer.allocateDirect(1)
    checkXR(xrCreateSession(instance, sessionCreateInfo, sessionPointer))
    val session = XrSession(sessionPointer[0], instance)

    val identityPos = XrVector3f.calloc()
    val identityRot = XrQuaternionf.calloc()
    val identityPose = XrPosef.calloc()
        .`position$`(identityPos.set(0f, 0f, 0f))
        .orientation(identityRot.set(0f, 0f, 0f, 1f))

    val playSpaceCreateInfo = XrReferenceSpaceCreateInfo.calloc()
        .type(XR_TYPE_REFERENCE_SPACE_CREATE_INFO)
        .next(0)
        .referenceSpaceType(XR_REFERENCE_SPACE_TYPE_LOCAL)
        .poseInReferenceSpace(identityPose)
    val playSpacePtr = PointerBuffer.allocateDirect(1)
    checkXR(xrCreateReferenceSpace(session, playSpaceCreateInfo, playSpacePtr))
    val playSpace = XrSpace(playSpacePtr[0], session)

    val formatCount = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
    checkXR(xrEnumerateSwapchainFormats(session, formatCount, null))
    val formats = ByteBuffer.allocateDirect(8 * formatCount[0]).order(ByteOrder.nativeOrder()).asLongBuffer()
    checkXR(xrEnumerateSwapchainFormats(session, formatCount, formats))

    println("All formats: ${(0 until formatCount[0]).map { formats[it] }}")
    fun getSwapchainFormat(preferredFormats: List<Int>, fallback: Boolean): Long {
        var bestFormat = if (fallback) formats[0] else -1
        var bestValue = preferredFormats.size
        for (i in 0 until formatCount[0]) {
            val format = formats[i]
            val idx = preferredFormats.indexOf(format.toInt())
            if (idx in 0 until bestValue) {
                bestFormat = format
                bestValue = idx
            }
        }
        return bestFormat
    }

    val colorFormat = getSwapchainFormat(
        listOf(
            GL_SRGB8_ALPHA8_EXT,
            GL_SRGB8_EXT,
            GL_RGBA8,
            GL_RGB8,
        ), true
    )
    val depthFormat = getSwapchainFormat(
        listOf(
            GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT32,
            GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT16,
        ), false
    )
    if (depthFormat < 0) {
        hasDepth = false
    }

    val swapchains = ArrayList<XrSwapchain>(viewCount)
    val swapchainImages = ArrayList<XrSwapchainImageOpenGLKHR.Buffer>(viewCount)

    val i0 = IntArray(0)
    framebuffers = Array(viewCount) { i0 }

    val ptr = PointerBuffer.allocateDirect(1)
    fun createSwapchainImage(i: Int, format: Long, usage: Int) {
        val sci = XrSwapchainCreateInfo.calloc()
            .type(XR_TYPE_SWAPCHAIN_CREATE_INFO)
            .next(0)
            .usageFlags(usage.toLong())
            .createFlags(0)
            .format(format)
            .sampleCount(viewConfigViews[i].recommendedSwapchainSampleCount())
            .width(viewConfigViews.recommendedImageRectWidth())
            .height(viewConfigViews.recommendedImageRectHeight())
            .faceCount(1)
            .arraySize(1)
            .mipCount(1)
        checkXR(xrCreateSwapchain(session, sci, ptr))
        val swapchain = XrSwapchain(ptr[0], session)
        swapchains.add(swapchain)

        // how many images are needed, e.g., for triple buffering
        val ico = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
        checkXR(xrEnumerateSwapchainImages(swapchain, ico, null))

        val imageCount = ico[0]
        val images = XrSwapchainImageOpenGLKHR.calloc(imageCount)
        for (j in 0 until imageCount) {
            images[j]
                .type(XR_TYPE_SWAPCHAIN_IMAGE_OPENGL_KHR)
                .next(0)
        }
        checkXR(
            xrEnumerateSwapchainImages(
                swapchain, ico,
                XrSwapchainImageBaseHeader.Buffer(images.address(), images.capacity()) // 😱
            )
        )
        swapchainImages.add(images)
        if (usage.hasFlag(XR_SWAPCHAIN_USAGE_COLOR_ATTACHMENT_BIT)) {
            framebuffers[i] = IntArray(imageCount)
        }
    }

    for (i in 0 until viewCount) {
        createSwapchainImage(i, colorFormat, XR_SWAPCHAIN_USAGE_SAMPLED_BIT or XR_SWAPCHAIN_USAGE_COLOR_ATTACHMENT_BIT)
    }

    if (hasDepth) {
        for (i in 0 until viewCount) {
            createSwapchainImage(i, depthFormat, XR_SWAPCHAIN_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT)
        }
    }

    val views = XrView.calloc(viewCount)
    for (i in 0 until viewCount) {
        views[i]
            .type(XR_TYPE_VIEW)
            .next(0)
    }

    val projectionViews = XrCompositionLayerProjectionView.calloc(viewCount)
    for (i in 0 until viewCount) {
        val pv = projectionViews[i]
            .type(XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW)
            .next(0)
        val subImage = pv.subImage()
            .swapchain(swapchains[i])
            .imageArrayIndex(0)
        val view = viewConfigViews[i]
        val rect = subImage.imageRect()
        rect.offset().set(0, 0)
        rect.extent().set(view.recommendedImageRectWidth(), view.recommendedImageRectHeight())
    }

    if (hasDepth) {
        val infos = XrCompositionLayerDepthInfoKHR.calloc(viewCount)
        for (i in 0 until viewCount) {
            val info = infos[i]
                .type(XR_TYPE_COMPOSITION_LAYER_DEPTH_INFO_KHR)
                .next(0)
                .minDepth(0f)
                .maxDepth(1f)
                .nearZ(0.01f)
                .farZ(100f)
            val subImage = info.subImage()
                .swapchain(swapchains[viewCount + i])
                .imageArrayIndex(0)
            val view = viewConfigViews[i]
            val rect = subImage.imageRect()
            rect.offset().set(0, 0)
            rect.extent().set(view.recommendedImageRectWidth(), view.recommendedImageRectHeight())
            projectionViews[i].next(info)
        }
    }

    // todo define hand actions

    initGL(framebuffers)

    var t0 = System.nanoTime()
    var fps = 0

    val eventBuffer = ByteBuffer.allocateDirect(max(XrEventDataBuffer.SIZEOF, XrEventDataSessionStateChanged.SIZEOF))
    val stateEvent = XrEventDataSessionStateChanged(eventBuffer)
    val runtimeEvent = XrEventDataBuffer(eventBuffer)
    val frameState = XrFrameState.calloc()
    val frameWaitInfo = XrFrameWaitInfo.calloc()
    val viewLocateInfo = XrViewLocateInfo.calloc()
    val waitInfo = XrSwapchainImageWaitInfo.calloc()
    val acquireInfo = XrSwapchainImageAcquireInfo.create()
    val frameBeginInfo = XrFrameBeginInfo.create()
    val frameEndInfo = XrFrameEndInfo.calloc()
    val releaseInfo = XrSwapchainImageReleaseInfo.calloc()
    val projectionLayer = XrCompositionLayerProjection.calloc()
    val projectionMatrix = Matrix4f()
    val viewMatrix = Matrix4f()
    val viewState = XrViewState.calloc()
    val idx = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer()
    var canSkipRendering = true
    var sessionRunning = false // todo is this the correct initial state?
    while (!glfwWindowShouldClose(window)) {
        glfwPollEvents()

        while (true) {
            runtimeEvent
                .type(XR_TYPE_EVENT_DATA_BUFFER)
                .next(0)
            if (checkXR(xrPollEvent(instance, runtimeEvent))) break
            when (runtimeEvent.type()) {
                // todo monitor state changes, e.g. for whether we should render
                XR_TYPE_EVENT_DATA_INSTANCE_LOSS_PENDING -> {}
                XR_TYPE_EVENT_DATA_SESSION_STATE_CHANGED -> {
                    val newState = stateEvent.state()
                    println("new state: $newState")
                    when (newState) {
                        XR_SESSION_STATE_IDLE, XR_SESSION_STATE_UNKNOWN -> {
                            canSkipRendering = true
                        }
                        XR_SESSION_STATE_FOCUSED, XR_SESSION_STATE_SYNCHRONIZED,
                        XR_SESSION_STATE_VISIBLE -> {
                            canSkipRendering = false
                        }
                        XR_SESSION_STATE_READY -> {
                            if (!sessionRunning) {
                                println("Starting session")
                                val beginInfo = XrSessionBeginInfo.calloc()
                                    .type(XR_TYPE_SESSION_BEGIN_INFO)
                                    .next(0)
                                    .primaryViewConfigurationType(viewConfigType)
                                checkXR(xrBeginSession(session, beginInfo))
                                println("Session started")
                                sessionRunning = true
                            }
                            canSkipRendering = false
                        }
                        XR_SESSION_STATE_STOPPING -> {
                            if (sessionRunning) {
                                checkXR(xrEndSession(session))
                                println("Session ended")
                                sessionRunning = false
                            }
                            canSkipRendering = true
                        }
                        XR_SESSION_STATE_LOSS_PENDING,
                        XR_SESSION_STATE_EXITING -> {
                            checkXR(xrDestroySession(session))
                            glfwSetWindowShouldClose(window, true)
                            canSkipRendering = true
                            println("Destroying session")
                        }
                    }
                }
                XR_TYPE_EVENT_DATA_INTERACTION_PROFILE_CHANGED -> {}
                XR_TYPE_EVENT_DATA_BUFFER -> {}
                XR_TYPE_EVENT_DATA_REFERENCE_SPACE_CHANGE_PENDING -> {}
                else -> {
                    println("Unhandled event: ${runtimeEvent.type()}")
                }
            }
        }

        if (canSkipRendering) {
            continue
        }

        // wait for v-sync to catch up ^^
        frameState.type(XR_TYPE_FRAME_STATE).next(0)
        frameWaitInfo.type(XR_TYPE_FRAME_WAIT_INFO).next(0)
        checkXR(xrWaitFrame(session, frameWaitInfo, frameState))

        viewLocateInfo.type(XR10.XR_TYPE_VIEW_LOCATE_INFO).next(0)
            .viewConfigurationType(viewConfigType)
            .displayTime(frameState.predictedDisplayTime())
            .space(playSpace)

        viewState.type(XR10.XR_TYPE_VIEW_STATE).next(0)

        checkXR(xrLocateViews(session, viewLocateInfo, viewState, viewCountBuffer, views))

        // todo here's more action stuff to be done

        frameBeginInfo.type(XR_TYPE_FRAME_BEGIN_INFO).next(0)
        checkXR(xrBeginFrame(session, frameBeginInfo))

        if (frameState.shouldRender()) {
            for (i in 0 until viewCount) {
                createProjectionFov(projectionMatrix, views[i].fov(), 0.01f, 100f)
                createViewMatrix(viewMatrix, views[i].pose().`position$`(), views[i].pose().orientation())

                idx.put(0, 0)
                acquireInfo
                    .type(XR_TYPE_SWAPCHAIN_IMAGE_ACQUIRE_INFO)
                    .next(0)
                checkXR(xrAcquireSwapchainImage(swapchains[i], acquireInfo, idx))

                waitInfo
                    .type(XR_TYPE_SWAPCHAIN_IMAGE_WAIT_INFO)
                    .next(0)
                checkXR(xrWaitSwapchainImage(swapchains[i], waitInfo))

                val colorAcquiredIndex = idx[0]
                val depthAcquiredIndex =
                    if (hasDepth) {
                        acquireInfo
                            .type(XR_TYPE_SWAPCHAIN_IMAGE_ACQUIRE_INFO)
                            .next(0)
                        checkXR(xrAcquireSwapchainImage(swapchains[viewCount + i], acquireInfo, idx))
                        waitInfo
                            .type(XR_TYPE_SWAPCHAIN_IMAGE_WAIT_INFO)
                            .next(0)
                        checkXR(xrWaitSwapchainImage(swapchains[viewCount + i], waitInfo))
                        idx[0]
                    } else -1

                projectionViews[i].pose(views[i].pose())
                projectionViews[i].fov(views[i].fov())

                val w = viewConfigViews[i].recommendedImageRectWidth()
                val h = viewConfigViews[i].recommendedImageRectHeight()

                renderFrame(
                    w, h, frameState.predictedDisplayTime(),
                    i, null, projectionMatrix, viewMatrix,
                    framebuffers[i][colorAcquiredIndex],
                    swapchainImages[i][colorAcquiredIndex].image(),
                    if (hasDepth) swapchainImages[viewCount + i][depthAcquiredIndex].image() else -1
                )

                releaseInfo
                    .type(XR_TYPE_SWAPCHAIN_IMAGE_RELEASE_INFO)
                    .next(0)
                checkXR(xrReleaseSwapchainImage(swapchains[i], releaseInfo))

                if (hasDepth) {
                    releaseInfo
                        .type(XR_TYPE_SWAPCHAIN_IMAGE_RELEASE_INFO)
                        .next(0)
                    checkXR(xrReleaseSwapchainImage(swapchains[viewCount + i], releaseInfo))
                }
            }
        }

        projectionLayer
            .type(XR_TYPE_COMPOSITION_LAYER_PROJECTION)
            .next(0)
            .layerFlags(0)
            .space(playSpace)
            .views(projectionViews)

        var submittedLayerCount = 1
        val submittedLayers = XrCompositionLayerBaseHeader
            .create(projectionLayer.address()) // 😱

        if (!viewState.viewStateFlags().hasFlag(XR_VIEW_STATE_ORIENTATION_VALID_BIT.toLong())) {
            println("Submitting no layers, because orientation is invalid")
            submittedLayerCount = 0
        }

        if (!frameState.shouldRender()) {
            println("Submitting no layers, because shouldRender is false")
            submittedLayerCount = 0
        }

        ptr.put(0, submittedLayers)
        frameEndInfo
            .type(XR_TYPE_FRAME_END_INFO)
            .next(0)
            .displayTime(frameState.predictedDisplayTime())
            .layerCount(submittedLayerCount)
            .layers(ptr)
            .environmentBlendMode(XR_ENVIRONMENT_BLEND_MODE_OPAQUE)
        checkXR(xrEndFrame(session, frameEndInfo))

        fps++
        val t1 = System.nanoTime()
        if (t1 - t0 > 1e9) {
            t0 = t1
            println("Running with $fps fps")
            fps = 0
        }
    }

    xrDestroyInstance(instance)
    glfwDestroyWindow(window)
    glfwTerminate()
}

val stringPtrs = ArrayList<ByteBuffer>() // keep them in memory, so we don't get any ugly segfaults
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
        .type(XR_TYPE_INSTANCE_PROPERTIES)
        .next(0)
    checkXR(xrGetInstanceProperties(instance, ip))
    println("Runtime-Name: ${ip.runtimeNameString()}")
    val version = ip.runtimeVersion()
    println(
        "Runtime-Version: " +
                "${XR_VERSION_MAJOR(version)}." +
                "${XR_VERSION_MINOR(version)}." +
                "${XR_VERSION_PATCH(version)}"
    )
    return ip
}

var instance: XrInstance? = null
val xrResultBuffer: ByteBuffer = ByteBuffer.allocateDirect(256)
fun checkXR(result: Int): Boolean {
    if (result == XR_SUCCESS) return false
    if (result == XR_EVENT_UNAVAILABLE) return true
    val instance = instance ?: throw IllegalStateException("Error $result")
    if (xrResultToString(instance, result, xrResultBuffer) == XR_SUCCESS) {
        val stringLength = (0 until xrResultBuffer.capacity())
            .first { idx -> xrResultBuffer[idx].toInt() == 0 }
        val bytes = ByteArray(stringLength)
        xrResultBuffer.get(bytes).position(0)
        val string = bytes.decodeToString()
        throw IllegalStateException("Error: $string")
    } else throw IllegalStateException("Error getting error! $result -> ?")
}