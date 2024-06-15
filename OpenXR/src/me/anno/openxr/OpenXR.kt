package me.anno.openxr

import me.anno.gpu.GFX
import me.anno.openxr.OpenXRUtils.checkHandTrackingAndPrintSystemProperties
import me.anno.openxr.OpenXRUtils.checkXR
import me.anno.openxr.OpenXRUtils.intPtr
import me.anno.openxr.OpenXRUtils.longPtr
import me.anno.openxr.OpenXRUtils.printInstanceProperties
import me.anno.openxr.OpenXRUtils.ptr
import me.anno.openxr.OpenXRUtils.ptr1
import me.anno.openxr.OpenXRUtils.setupDebugging
import me.anno.openxr.OpenXRUtils.viewConfigType
import me.anno.openxr.OpenXRUtils.xrInstance
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWNativeGLX.glfwGetGLXContext
import org.lwjgl.glfw.GLFWNativeWGL.glfwGetWGLContext
import org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Display
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Window
import org.lwjgl.opengl.EXTTextureSRGB.GL_SRGB8_ALPHA8_EXT
import org.lwjgl.opengl.EXTTextureSRGB.GL_SRGB8_EXT
import org.lwjgl.opengl.GL46C.GL_DEPTH_COMPONENT16
import org.lwjgl.opengl.GL46C.GL_DEPTH_COMPONENT24
import org.lwjgl.opengl.GL46C.GL_DEPTH_COMPONENT32
import org.lwjgl.opengl.GL46C.GL_DEPTH_COMPONENT32F
import org.lwjgl.opengl.GL46C.GL_RGB8
import org.lwjgl.opengl.GL46C.GL_RGBA8
import org.lwjgl.openxr.EXTDebugUtils.XR_EXT_DEBUG_UTILS_EXTENSION_NAME
import org.lwjgl.openxr.EXTHandTracking.XR_EXT_HAND_TRACKING_EXTENSION_NAME
import org.lwjgl.openxr.KHRCompositionLayerDepth.XR_KHR_COMPOSITION_LAYER_DEPTH_EXTENSION_NAME
import org.lwjgl.openxr.KHRCompositionLayerDepth.XR_TYPE_COMPOSITION_LAYER_DEPTH_INFO_KHR
import org.lwjgl.openxr.KHROpenGLEnable.XR_KHR_OPENGL_ENABLE_EXTENSION_NAME
import org.lwjgl.openxr.KHROpenGLEnable.XR_TYPE_GRAPHICS_BINDING_OPENGL_WIN32_KHR
import org.lwjgl.openxr.KHROpenGLEnable.XR_TYPE_GRAPHICS_BINDING_OPENGL_XLIB_KHR
import org.lwjgl.openxr.KHROpenGLEnable.XR_TYPE_GRAPHICS_REQUIREMENTS_OPENGL_KHR
import org.lwjgl.openxr.KHROpenGLEnable.XR_TYPE_SWAPCHAIN_IMAGE_OPENGL_KHR
import org.lwjgl.openxr.KHROpenGLEnable.xrGetOpenGLGraphicsRequirementsKHR
import org.lwjgl.openxr.XR10
import org.lwjgl.openxr.XR10.XR_CURRENT_API_VERSION
import org.lwjgl.openxr.XR10.XR_FORM_FACTOR_HEAD_MOUNTED_DISPLAY
import org.lwjgl.openxr.XR10.XR_REFERENCE_SPACE_TYPE_LOCAL
import org.lwjgl.openxr.XR10.XR_SWAPCHAIN_USAGE_COLOR_ATTACHMENT_BIT
import org.lwjgl.openxr.XR10.XR_SWAPCHAIN_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT
import org.lwjgl.openxr.XR10.XR_SWAPCHAIN_USAGE_SAMPLED_BIT
import org.lwjgl.openxr.XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW
import org.lwjgl.openxr.XR10.XR_TYPE_EXTENSION_PROPERTIES
import org.lwjgl.openxr.XR10.XR_TYPE_INSTANCE_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_REFERENCE_SPACE_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SESSION_ACTION_SETS_ATTACH_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SESSION_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SWAPCHAIN_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SWAPCHAIN_IMAGE_ACQUIRE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SWAPCHAIN_IMAGE_RELEASE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SWAPCHAIN_IMAGE_WAIT_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SYSTEM_GET_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_VIEW
import org.lwjgl.openxr.XR10.XR_TYPE_VIEW_CONFIGURATION_VIEW
import org.lwjgl.openxr.XR10.XR_VERSION_MAJOR
import org.lwjgl.openxr.XR10.XR_VERSION_MINOR
import org.lwjgl.openxr.XR10.xrAcquireSwapchainImage
import org.lwjgl.openxr.XR10.xrAttachSessionActionSets
import org.lwjgl.openxr.XR10.xrCreateInstance
import org.lwjgl.openxr.XR10.xrCreateReferenceSpace
import org.lwjgl.openxr.XR10.xrCreateSession
import org.lwjgl.openxr.XR10.xrCreateSwapchain
import org.lwjgl.openxr.XR10.xrEnumerateInstanceExtensionProperties
import org.lwjgl.openxr.XR10.xrEnumerateSwapchainFormats
import org.lwjgl.openxr.XR10.xrEnumerateSwapchainImages
import org.lwjgl.openxr.XR10.xrEnumerateViewConfigurationViews
import org.lwjgl.openxr.XR10.xrGetSystem
import org.lwjgl.openxr.XR10.xrLocateViews
import org.lwjgl.openxr.XR10.xrReleaseSwapchainImage
import org.lwjgl.openxr.XR10.xrWaitSwapchainImage
import org.lwjgl.openxr.XrApplicationInfo
import org.lwjgl.openxr.XrCompositionLayerDepthInfoKHR
import org.lwjgl.openxr.XrCompositionLayerProjectionView
import org.lwjgl.openxr.XrExtensionProperties
import org.lwjgl.openxr.XrGraphicsBindingOpenGLWin32KHR
import org.lwjgl.openxr.XrGraphicsBindingOpenGLXlibKHR
import org.lwjgl.openxr.XrGraphicsRequirementsOpenGLKHR
import org.lwjgl.openxr.XrInstance
import org.lwjgl.openxr.XrInstanceCreateInfo
import org.lwjgl.openxr.XrPosef
import org.lwjgl.openxr.XrReferenceSpaceCreateInfo
import org.lwjgl.openxr.XrSession
import org.lwjgl.openxr.XrSessionActionSetsAttachInfo
import org.lwjgl.openxr.XrSessionCreateInfo
import org.lwjgl.openxr.XrSpace
import org.lwjgl.openxr.XrSpaceLocation
import org.lwjgl.openxr.XrSwapchain
import org.lwjgl.openxr.XrSwapchainCreateInfo
import org.lwjgl.openxr.XrSwapchainImageAcquireInfo
import org.lwjgl.openxr.XrSwapchainImageBaseHeader
import org.lwjgl.openxr.XrSwapchainImageOpenGLKHR
import org.lwjgl.openxr.XrSwapchainImageReleaseInfo
import org.lwjgl.openxr.XrSwapchainImageWaitInfo
import org.lwjgl.openxr.XrSwapchainSubImage
import org.lwjgl.openxr.XrSystemGetInfo
import org.lwjgl.openxr.XrView
import org.lwjgl.openxr.XrViewConfigurationView
import org.lwjgl.system.windows.User32.GetDC
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.LongBuffer
import kotlin.math.max

abstract class OpenXR(val window: Long) {

    companion object {
        private val LOGGER = LogManager.getLogger(OpenXR::class)
    }

    abstract fun copyToDesktopWindow(framebuffer: Int, w: Int, h: Int)
    abstract fun renderFrame(
        viewIndex: Int, w: Int, h: Int,
        predictedDisplayTime: Long,
        handLocations: XrSpaceLocation.Buffer?,
        framebuffer: Int, colorTexture: Int, depthTexture: Int,
    )

    fun checkExtensions() {
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

    fun collectExtensions(): PointerBuffer {
        checkExtensions()
        val result = PointerBuffer.allocateDirect(4)
        result.put(XR_KHR_OPENGL_ENABLE_EXTENSION_NAME.ptr())
        if (hasHandTracking) result.put(XR_EXT_HAND_TRACKING_EXTENSION_NAME.ptr())
        if (hasDebug) result.put(XR_EXT_DEBUG_UTILS_EXTENSION_NAME.ptr())
        result.flip()
        return result
    }

    fun createInstance(): XrInstance {
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
        val instanceB = PointerBuffer.allocateDirect(1)
        LOGGER.info("Creating instance")
        checkXR(xrCreateInstance(instanceCreateInfo, instanceB))
        LOGGER.info("Created instance")
        return XrInstance(instanceB[0], instanceCreateInfo)
    }

    val instance = createInstance()

    init {
        xrInstance = instance
        setupDebugging(instance)
        printInstanceProperties(instance)
    }

    fun createSystemId(): Long {
        val systemGetInfo = XrSystemGetInfo.calloc()
            .type(XR_TYPE_SYSTEM_GET_INFO).next(0)
            .formFactor(XR_FORM_FACTOR_HEAD_MOUNTED_DISPLAY)
        LOGGER.info("Getting System")
        checkXR(xrGetSystem(instance, systemGetInfo, longPtr))
        val systemId = longPtr[0]
        LOGGER.info("Got XrSystem with HMD id $systemId")
        return systemId
    }

    val systemId = createSystemId()

    init {
        checkHandTrackingAndPrintSystemProperties(instance, systemId)
    }

    private fun findViewCount(): Int {
        checkXR(xrEnumerateViewConfigurationViews(instance, systemId, viewConfigType, intPtr, null))
        return intPtr[0]
    }

    private fun getViewConfigs(systemId: Long): XrViewConfigurationView.Buffer {
        val viewConfigViews = XrViewConfigurationView.calloc(viewCount)
        for (i in 0 until viewCount) {
            viewConfigViews[i].type(XR_TYPE_VIEW_CONFIGURATION_VIEW).next(0)
        }
        checkXR(xrEnumerateViewConfigurationViews(instance, systemId, viewConfigType, intPtr, viewConfigViews))
        return viewConfigViews
    }

    val viewCount = findViewCount()
    val viewConfigViews = getViewConfigs(systemId)

    private fun checkOpenGLRequirements(systemId: Long) {
        val openGLRequirements = XrGraphicsRequirementsOpenGLKHR.calloc()
            .type(XR_TYPE_GRAPHICS_REQUIREMENTS_OPENGL_KHR).next(0)
        checkXR(xrGetOpenGLGraphicsRequirementsKHR(instance, systemId, openGLRequirements))
        LOGGER.info( // 4.3 to 4.6 for Meta Quest 3 in SteamVR, 4.0 to 4.6 in Meta Quest Link
            "Graphics Requirements: " +
                    "${XR_VERSION_MAJOR(openGLRequirements.minApiVersionSupported())}." +
                    "${XR_VERSION_MINOR(openGLRequirements.minApiVersionSupported())} to " +
                    "${XR_VERSION_MAJOR(openGLRequirements.maxApiVersionSupported())}." +
                    "${XR_VERSION_MINOR(openGLRequirements.maxApiVersionSupported())}"
        )
    }

    init {
        LOGGER.info("Views: $viewCount")
        checkOpenGLRequirements(systemId)
    }

    private fun createSession(instance: XrInstance, systemId: Long): XrSession {
        val sessionCreateInfo = XrSessionCreateInfo.calloc()
            .type(XR_TYPE_SESSION_CREATE_INFO)
            .systemId(systemId)

        // https://gitlab.freedesktop.org/monado/demos/openxr-simple-example/-/blob/master/main.c?ref_type=heads
        if (OS.isWindows) {
            val hGLRC = glfwGetWGLContext(window)
            val hDC = GetDC(glfwGetWin32Window((window)))
            LOGGER.info("wglContext: $hGLRC, dc: $hDC")
            val binding = XrGraphicsBindingOpenGLWin32KHR.calloc()
                .type(XR_TYPE_GRAPHICS_BINDING_OPENGL_WIN32_KHR)
                .next(0)
                .hDC(hDC)
                .hGLRC(hGLRC)
            sessionCreateInfo.next(binding)
        } else {
            val display = glfwGetX11Display()
            val window1 = glfwGetX11Window(window)
            val glxContext = glfwGetGLXContext(window)
            val binding = XrGraphicsBindingOpenGLXlibKHR.calloc()
                .type(XR_TYPE_GRAPHICS_BINDING_OPENGL_XLIB_KHR)
                .next(0)
                .xDisplay(display)
                .visualid(-1) // todo fill in
                .glxFBConfig(-1) // todo fill in
                .glxDrawable(window1)
                .glxContext(glxContext)
            sessionCreateInfo.next(binding)
        }

        val sessionPointer = PointerBuffer.allocateDirect(1)
        checkXR(xrCreateSession(instance, sessionCreateInfo, sessionPointer))
        return XrSession(sessionPointer[0], instance)
    }

    val session = createSession(instance, systemId)

    val identityPose: XrPosef = XrPosef.calloc()

    init {
        identityPose.`position$`().set(0f, 0f, 0f)
        identityPose.orientation().set(0f, 0f, 0f, 1f)
    }

    private fun createPlaySpace(session: XrSession): XrSpace {
        val playSpaceCreateInfo = XrReferenceSpaceCreateInfo.calloc()
            .type(XR_TYPE_REFERENCE_SPACE_CREATE_INFO).next(0)
            .referenceSpaceType(XR_REFERENCE_SPACE_TYPE_LOCAL)
            .poseInReferenceSpace(identityPose)
        checkXR(xrCreateReferenceSpace(session, playSpaceCreateInfo, ptr))
        return XrSpace(ptr[0], session)
    }

    val playSpace = createPlaySpace(session)

    fun getSwapchainFormats(session: XrSession): LongBuffer {
        checkXR(xrEnumerateSwapchainFormats(session, intPtr, null))
        val formatCount = intPtr[0]
        val formats = ByteBuffer.allocateDirect(8 * formatCount).order(ByteOrder.nativeOrder()).asLongBuffer()
        checkXR(xrEnumerateSwapchainFormats(session, intPtr, formats))
        LOGGER.info("Available swapchain formats: ${(0 until formats.capacity()).map { GFX.getName(formats[it].toInt()) }}")
        return formats
    }

    val formats = getSwapchainFormats(session)

    private fun chooseSwapchainFormat(formats: LongBuffer, preferredFormats: List<Int>, fallback: Boolean): Long {
        var bestFormat = if (fallback) formats[0] else -1
        var bestValue = preferredFormats.size
        for (i in 0 until formats.capacity()) {
            val format = formats[i]
            val idx = preferredFormats.indexOf(format.toInt())
            if (idx in 0 until bestValue) {
                bestFormat = format
                bestValue = idx
            }
        }
        return bestFormat
    }

    private fun chooseColorFormat(formats: LongBuffer): Long {
        return chooseSwapchainFormat(
            formats, listOf(
                GL_SRGB8_ALPHA8_EXT,
                GL_SRGB8_EXT,
                GL_RGBA8,
                GL_RGB8,
            ), true
        )
    }

    private fun chooseDepthFormat(formats: LongBuffer): Long {
        val format = chooseSwapchainFormat(
            formats, listOf(
                GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT32,
                GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT16,
            ), false
        )
        if (format < 0) {
            hasDepth = false
        }
        return format
    }

    val colorFormat = chooseColorFormat(formats)
    val depthFormat = chooseDepthFormat(formats)

    init {
        LOGGER.info("Chosen format: ${GFX.getName(colorFormat.toInt())}, ${GFX.getName(depthFormat.toInt())}")
    }

    private fun createSwapchain(i: Int, format: Long, usage: Int) {
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
            images[j].type(XR_TYPE_SWAPCHAIN_IMAGE_OPENGL_KHR).next(0)
        }
        // unsafe casting, but works somehow
        val buffer = XrSwapchainImageBaseHeader.Buffer(images.address(), images.capacity())
        checkXR(xrEnumerateSwapchainImages(swapchain, ico, buffer))
        swapchainImages.add(images)
    }

    private fun createSwapchains() {
        for (i in 0 until viewCount) {
            val usage = XR_SWAPCHAIN_USAGE_SAMPLED_BIT or XR_SWAPCHAIN_USAGE_COLOR_ATTACHMENT_BIT
            createSwapchain(i, colorFormat, usage)
        }
        if (hasDepth) {
            for (i in 0 until viewCount) {
                createSwapchain(i, depthFormat, XR_SWAPCHAIN_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT)
            }
        }
    }

    val swapchains = ArrayList<XrSwapchain>(viewCount)
    val swapchainImages = ArrayList<XrSwapchainImageOpenGLKHR.Buffer>(viewCount)

    init {
        createSwapchains()
    }

    fun createViews(viewCount: Int): XrView.Buffer {
        val views = XrView.calloc(viewCount)
        for (i in 0 until viewCount) {
            views[i].type(XR_TYPE_VIEW).next(0)
        }
        return views
    }

    val views = createViews(viewCount)
    val projectionViews: XrCompositionLayerProjectionView.Buffer = XrCompositionLayerProjectionView.calloc(viewCount)

    private fun defineSubImage(
        subImage: XrSwapchainSubImage, swapchain: XrSwapchain,
        viewConfigViews: XrViewConfigurationView
    ) {
        subImage
            .swapchain(swapchain)
            .imageArrayIndex(0)
        val rect = subImage.imageRect()
        rect.offset().set(0, 0)
        rect.extent().set(
            viewConfigViews.recommendedImageRectWidth(),
            viewConfigViews.recommendedImageRectHeight()
        )
    }

    private fun defineColorLayers() {
        for (i in 0 until viewCount) {
            val pv = projectionViews[i].type(XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW).next(0)
            defineSubImage(pv.subImage(), swapchains[i], viewConfigViews[i])
        }
    }

    private fun defineDepthLayers() {
        val infos = XrCompositionLayerDepthInfoKHR.calloc(viewCount)
        for (i in 0 until viewCount) {
            val info = infos[i]
                .type(XR_TYPE_COMPOSITION_LAYER_DEPTH_INFO_KHR)
                .next(0)
                .minDepth(0f)
                .maxDepth(1f)
                .nearZ(nearZ)
                .farZ(farZ)
            defineSubImage(info.subImage(), swapchains[viewCount + i], viewConfigViews[i])
            projectionViews[i].next(info)
        }
    }

    init {
        defineColorLayers()
        if (hasDepth) {
            defineDepthLayers()
        }
    }

    val actions = OpenXRActions(instance, session, identityPose)

    init {
        ptr.put(0, actions.actionSet)
        val actionSetAttachInfo: XrSessionActionSetsAttachInfo = XrSessionActionSetsAttachInfo.calloc()
            .type(XR_TYPE_SESSION_ACTION_SETS_ATTACH_INFO).next(0)
            .actionSets(ptr)
        checkXR(xrAttachSessionActionSets(session, actionSetAttachInfo))
    }

    val viewMatrix = Matrix4f()
    val projectionMatrix = Matrix4f()

    val fs = XrFrameStructs()
    val events = OpenXREvents(instance, session, window)

    private fun updateViews(
        playSpace: XrSpace, session: XrSession,
        views: XrView.Buffer, fs: XrFrameStructs
    ) {
        fs.viewState.type(XR10.XR_TYPE_VIEW_STATE).next(0)
        fs.viewLocateInfo.type(XR10.XR_TYPE_VIEW_LOCATE_INFO).next(0)
            .viewConfigurationType(viewConfigType)
            .displayTime(fs.frameState.predictedDisplayTime())
            .space(playSpace)
        checkXR(xrLocateViews(session, fs.viewLocateInfo, fs.viewState, intPtr, views))
    }

    val position = Vector3f()
    val rotation = Quaternionf()
    open fun beginRenderViews() {
        position.set(0f)
        rotation.set(0f, 0f, 0f, 0f)
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

    fun renderView(viewIndex: Int) {
        val view = views[viewIndex]
        createProjectionFov(projectionMatrix, view.fov(), nearZ, farZ)

        val colorSwapchain = swapchains[viewIndex]
        val depthSwapchain = if (hasDepth) swapchains[viewCount + viewIndex] else null

        val colorAcquiredIndex = acquireSwapchainImage(colorSwapchain, fs.acquireInfo, fs.waitInfo)
        val depthAcquiredIndex = if (depthSwapchain != null) {
            acquireSwapchainImage(depthSwapchain, fs.acquireInfo, fs.waitInfo)
        } else -1

        projectionViews[viewIndex]
            .pose(view.pose())
            .fov(view.fov())

        val viewConfig = viewConfigViews[viewIndex]
        val w = viewConfig.recommendedImageRectWidth()
        val h = viewConfig.recommendedImageRectHeight()

        val framebuffer = framebuffer
        val colorImage = swapchainImages[viewIndex][colorAcquiredIndex].image()
        val depthImage = if (hasDepth) swapchainImages[viewCount + viewIndex][depthAcquiredIndex].image() else -1
        renderFrame(
            viewIndex, w, h, fs.frameState.predictedDisplayTime(),
            actions.handLocations, framebuffer,
            colorImage, depthImage
        )
        if (viewIndex == 0) {
            copyToDesktopWindow(framebuffer, w, h)
        }

        releaseSwapchainImage(colorSwapchain, fs.releaseInfo)
        if (depthSwapchain != null) {
            releaseSwapchainImage(depthSwapchain, fs.releaseInfo)
        }
    }

    fun acquireSwapchainImage(
        swapchain: XrSwapchain,
        acquireInfo: XrSwapchainImageAcquireInfo,
        waitInfo: XrSwapchainImageWaitInfo
    ): Int {
        acquireInfo.type(XR_TYPE_SWAPCHAIN_IMAGE_ACQUIRE_INFO).next(0)
        checkXR(xrAcquireSwapchainImage(swapchain, acquireInfo, intPtr))
        val acquiredIndex = intPtr[0]

        waitInfo.type(XR_TYPE_SWAPCHAIN_IMAGE_WAIT_INFO).next(0)
        checkXR(xrWaitSwapchainImage(swapchain, waitInfo))
        return acquiredIndex
    }

    fun releaseSwapchainImage(swapchain: XrSwapchain, releaseInfo: XrSwapchainImageReleaseInfo) {
        releaseInfo.type(XR_TYPE_SWAPCHAIN_IMAGE_RELEASE_INFO).next(0)
        checkXR(xrReleaseSwapchainImage(swapchain, releaseInfo))
    }

    fun renderFrameMaybe() {
        events.pollEvents()
        if (events.canSkipRendering) {
            return
        }

        fs.waitFrame(session)
        updateViews(playSpace, session, views, fs)
        actions.updateActions(playSpace, fs.frameState)
        fs.beginFrame(session)

        if (fs.frameState.shouldRender()) {
            beginRenderViews()
            for (viewIndex in 0 until viewCount) {
                renderView(viewIndex)
            }
        }

        fs.endFrame(session, playSpace, projectionViews)
    }
}