package me.anno.openxr

import me.anno.Time
import me.anno.gpu.GLNames
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.openxr.OpenXR.Companion.VIEW_CONFIG_TYPE
import me.anno.openxr.OpenXR.Companion.farZ
import me.anno.openxr.OpenXR.Companion.nearZ
import me.anno.openxr.OpenXRUtils.checkHandTrackingAndPrintSystemProperties
import me.anno.openxr.OpenXRUtils.checkOpenGLRequirements
import me.anno.openxr.OpenXRUtils.checkXR
import me.anno.openxr.OpenXRUtils.intPtr
import me.anno.openxr.OpenXRUtils.ptr
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import org.lwjgl.glfw.GLFWNativeGLX.glfwGetGLXContext
import org.lwjgl.glfw.GLFWNativeWGL.glfwGetWGLContext
import org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Display
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Window
import org.lwjgl.opengl.EXTTextureSRGB.GL_SRGB8_ALPHA8_EXT
import org.lwjgl.opengl.EXTTextureSRGB.GL_SRGB8_EXT
import org.lwjgl.opengl.GL11C.GL_RGB10_A2
import org.lwjgl.opengl.GL11C.GL_RGBA12
import org.lwjgl.opengl.GL11C.GL_RGBA16
import org.lwjgl.opengl.GL30C.GL_RGBA16F
import org.lwjgl.opengl.GL46C.GL_DEPTH_COMPONENT16
import org.lwjgl.opengl.GL46C.GL_DEPTH_COMPONENT24
import org.lwjgl.opengl.GL46C.GL_DEPTH_COMPONENT32
import org.lwjgl.opengl.GL46C.GL_DEPTH_COMPONENT32F
import org.lwjgl.opengl.GL46C.GL_RGB8
import org.lwjgl.opengl.GL46C.GL_RGBA8
import org.lwjgl.openxr.KHRCompositionLayerDepth.XR_TYPE_COMPOSITION_LAYER_DEPTH_INFO_KHR
import org.lwjgl.openxr.KHROpenGLEnable.XR_TYPE_GRAPHICS_BINDING_OPENGL_WIN32_KHR
import org.lwjgl.openxr.KHROpenGLEnable.XR_TYPE_GRAPHICS_BINDING_OPENGL_XLIB_KHR
import org.lwjgl.openxr.KHROpenGLEnable.XR_TYPE_SWAPCHAIN_IMAGE_OPENGL_KHR
import org.lwjgl.openxr.XR10
import org.lwjgl.openxr.XR10.XR_REFERENCE_SPACE_TYPE_LOCAL
import org.lwjgl.openxr.XR10.XR_SWAPCHAIN_USAGE_COLOR_ATTACHMENT_BIT
import org.lwjgl.openxr.XR10.XR_SWAPCHAIN_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT
import org.lwjgl.openxr.XR10.XR_SWAPCHAIN_USAGE_SAMPLED_BIT
import org.lwjgl.openxr.XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW
import org.lwjgl.openxr.XR10.XR_TYPE_REFERENCE_SPACE_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SESSION_ACTION_SETS_ATTACH_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SESSION_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SWAPCHAIN_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SWAPCHAIN_IMAGE_ACQUIRE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SWAPCHAIN_IMAGE_RELEASE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_SWAPCHAIN_IMAGE_WAIT_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_VIEW
import org.lwjgl.openxr.XR10.XR_TYPE_VIEW_CONFIGURATION_VIEW
import org.lwjgl.openxr.XR10.xrAcquireSwapchainImage
import org.lwjgl.openxr.XR10.xrAttachSessionActionSets
import org.lwjgl.openxr.XR10.xrCreateReferenceSpace
import org.lwjgl.openxr.XR10.xrCreateSession
import org.lwjgl.openxr.XR10.xrCreateSwapchain
import org.lwjgl.openxr.XR10.xrEnumerateSwapchainFormats
import org.lwjgl.openxr.XR10.xrEnumerateSwapchainImages
import org.lwjgl.openxr.XR10.xrEnumerateViewConfigurationViews
import org.lwjgl.openxr.XR10.xrLocateViews
import org.lwjgl.openxr.XR10.xrReleaseSwapchainImage
import org.lwjgl.openxr.XR10.xrWaitSwapchainImage
import org.lwjgl.openxr.XrCompositionLayerDepthInfoKHR
import org.lwjgl.openxr.XrCompositionLayerProjectionView
import org.lwjgl.openxr.XrGraphicsBindingOpenGLWin32KHR
import org.lwjgl.openxr.XrGraphicsBindingOpenGLXlibKHR
import org.lwjgl.openxr.XrInstance
import org.lwjgl.openxr.XrPosef
import org.lwjgl.openxr.XrReferenceSpaceCreateInfo
import org.lwjgl.openxr.XrSession
import org.lwjgl.openxr.XrSessionActionSetsAttachInfo
import org.lwjgl.openxr.XrSessionCreateInfo
import org.lwjgl.openxr.XrSpace
import org.lwjgl.openxr.XrSwapchain
import org.lwjgl.openxr.XrSwapchainCreateInfo
import org.lwjgl.openxr.XrSwapchainImageAcquireInfo
import org.lwjgl.openxr.XrSwapchainImageBaseHeader
import org.lwjgl.openxr.XrSwapchainImageOpenGLKHR
import org.lwjgl.openxr.XrSwapchainImageReleaseInfo
import org.lwjgl.openxr.XrSwapchainImageWaitInfo
import org.lwjgl.openxr.XrSwapchainSubImage
import org.lwjgl.openxr.XrView
import org.lwjgl.openxr.XrViewConfigurationView
import org.lwjgl.system.windows.User32.GetDC
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.LongBuffer

class OpenXRSession(val window: Long, val system: OpenXRSystem) {

    companion object {
        private val LOGGER = LogManager.getLogger(OpenXRSession::class)
    }

    val instance get() = system.instance
    val systemId get() = system.systemId

    private fun findViewCount(): Int {
        checkXR(xrEnumerateViewConfigurationViews(instance, systemId, VIEW_CONFIG_TYPE, intPtr, null))
        return intPtr[0]
    }

    private fun getViewConfigs(systemId: Long): XrViewConfigurationView.Buffer {
        val viewConfigViews = XrViewConfigurationView.calloc(viewCount)
        for (i in 0 until viewCount) {
            viewConfigViews[i].type(XR_TYPE_VIEW_CONFIGURATION_VIEW).next(0)
        }
        checkXR(xrEnumerateViewConfigurationViews(instance, systemId, VIEW_CONFIG_TYPE, intPtr, viewConfigViews))
        return viewConfigViews
    }

    private fun createSession(instance: XrInstance, systemId: Long): XrSession {
        val sessionCreateInfo = XrSessionCreateInfo.calloc()
            .type(XR_TYPE_SESSION_CREATE_INFO)
            .systemId(systemId)

        // https://gitlab.freedesktop.org/monado/demos/openxr-simple-example/-/blob/master/main.c?ref_type=heads
        val bindingFreeable = if (OS.isWindows) {
            val hGLRC = glfwGetWGLContext(window)
            val hDC = GetDC(glfwGetWin32Window((window)))
            LOGGER.info("wglContext: $hGLRC, dc: $hDC")
            val binding = XrGraphicsBindingOpenGLWin32KHR.calloc()
                .type(XR_TYPE_GRAPHICS_BINDING_OPENGL_WIN32_KHR)
                .next(0)
                .hDC(hDC)
                .hGLRC(hGLRC)
            sessionCreateInfo.next(binding)
            binding
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
            binding
        }

        checkXR(xrCreateSession(instance, sessionCreateInfo, ptr))
        sessionCreateInfo.free()
        bindingFreeable.free()
        return XrSession(ptr[0], instance)
    }

    private fun createSpace(session: XrSession): XrSpace {
        val spaceCreateInfo = XrReferenceSpaceCreateInfo.calloc()
            .type(XR_TYPE_REFERENCE_SPACE_CREATE_INFO).next(0)
            .referenceSpaceType(XR_REFERENCE_SPACE_TYPE_LOCAL)
            .poseInReferenceSpace(identityPose)
        checkXR(xrCreateReferenceSpace(session, spaceCreateInfo, ptr))
        spaceCreateInfo.free()
        return XrSpace(ptr[0], session)
    }

    private fun createIdentityPose(): XrPosef {
        val identityPose: XrPosef = XrPosef.calloc()
        identityPose.`position$`().set(0f, 0f, 0f)
        identityPose.orientation().set(0f, 0f, 0f, 1f)
        return identityPose
    }

    fun getSwapchainFormats(session: XrSession): LongBuffer {
        checkXR(xrEnumerateSwapchainFormats(session, intPtr, null))
        val formatCount = intPtr[0]
        val formats = ByteBuffer.allocateDirect(8 * formatCount).order(ByteOrder.nativeOrder()).asLongBuffer()
        checkXR(xrEnumerateSwapchainFormats(session, intPtr, formats))
        LOGGER.info("Available swapchain formats: ${(0 until formats.capacity()).map { GLNames.getName(formats[it].toInt()) }}")
        return formats
    }

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
                GL_RGBA16F,
                GL_RGBA16,
                GL_RGBA12,
                GL_RGB10_A2,
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
            system.hasDepth = false
        }
        return format
    }

    private fun createSwapchain(i: Int, format: Long, usage: Int) {
        val swapchainCreateInfo = XrSwapchainCreateInfo.calloc()
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
        checkXR(xrCreateSwapchain(session, swapchainCreateInfo, ptr))
        swapchainCreateInfo.free()
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
        if (system.hasDepth) {
            for (i in 0 until viewCount) {
                createSwapchain(i, depthFormat, XR_SWAPCHAIN_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT)
            }
        }
    }

    fun createViews(viewCount: Int): XrView.Buffer {
        val views = XrView.calloc(viewCount)
        for (i in 0 until viewCount) {
            views[i].type(XR_TYPE_VIEW).next(0)
        }
        return views
    }

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
                .minDepth(0f) // todo set to -1 if depth-clipping isn't supported???
                .maxDepth(1f)
                .nearZ(nearZ) // todo fetch these from a RenderView???
                .farZ(farZ)
            defineSubImage(info.subImage(), swapchains[viewCount + i], viewConfigViews[i])
            projectionViews[i].next(info)
        }
    }

    private fun attachActionSet() {
        ptr.put(0, actions.actionSet)
        val actionSetAttachInfo = XrSessionActionSetsAttachInfo.calloc()
            .type(XR_TYPE_SESSION_ACTION_SETS_ATTACH_INFO).next(0)
            .actionSets(ptr)
        checkXR(xrAttachSessionActionSets(session, actionSetAttachInfo))
        actionSetAttachInfo.free()
    }

    init {
        checkHandTrackingAndPrintSystemProperties(system, instance, systemId)
    }

    val viewCount = findViewCount() // will most likely be two
    val viewConfigViews = getViewConfigs(systemId)

    init {
        LOGGER.info("Views: $viewCount")
        checkOpenGLRequirements(systemId, instance)
    }

    val session = createSession(instance, systemId)
    val identityPose = createIdentityPose()
    val space = createSpace(session)
    val formats = getSwapchainFormats(session)
    val colorFormat = chooseColorFormat(formats)
    val depthFormat = chooseDepthFormat(formats)

    init {
        LOGGER.info("Chosen format: ${GLNames.getName(colorFormat.toInt())}, ${GLNames.getName(depthFormat.toInt())}")
    }

    val swapchains = ArrayList<XrSwapchain>(viewCount)
    val swapchainImages = ArrayList<XrSwapchainImageOpenGLKHR.Buffer>(viewCount)

    init {
        createSwapchains()
    }

    val views = createViews(viewCount)
    val projectionViews: XrCompositionLayerProjectionView.Buffer = XrCompositionLayerProjectionView.calloc(viewCount)

    init {
        defineColorLayers()
        if (system.hasDepth) {
            defineDepthLayers()
        }
    }

    val actions = OpenXRActions(instance, session, identityPose)

    init {
        attachActionSet()
    }

    val fs = OpenXRFrameManager()
    val events = OpenXREvents(instance, session, window)

    private fun updateViews(
        playSpace: XrSpace, session: XrSession,
        views: XrView.Buffer, fs: OpenXRFrameManager
    ) {
        fs.viewState.type(XR10.XR_TYPE_VIEW_STATE).next(0)
        fs.viewLocateInfo.type(XR10.XR_TYPE_VIEW_LOCATE_INFO).next(0)
            .viewConfigurationType(VIEW_CONFIG_TYPE)
            .displayTime(fs.frameState.predictedDisplayTime())
            .space(playSpace)
        checkXR(xrLocateViews(session, fs.viewLocateInfo, fs.viewState, intPtr, views))
    }

    fun renderView(xr: OpenXR, viewIndex: Int) {
        val view = views[viewIndex]

        val colorSwapchain = swapchains[viewIndex]
        val depthSwapchain = if (xr.system.hasDepth) swapchains[viewCount + viewIndex] else null

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

        val colorImage = swapchainImages[viewIndex][colorAcquiredIndex].image()
        val depthImage = if (xr.system.hasDepth) {
            swapchainImages[viewCount + viewIndex][depthAcquiredIndex].image()
        } else -1
        xr.renderFrame(
            viewIndex, w, h, fs.frameState.predictedDisplayTime(),
            actions.handLocations,
            colorImage, depthImage
        )
        if (viewIndex == viewCount - 1) {
            xr.copyToDesktopWindow(w, h)
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

    private var lastTime = Time.nanoTime - 60 * MILLIS_TO_NANOS
    fun updateEngineTime() {
        // between GFXBase->Time.updateTime and this, the time will be jumpy...
        // predicted system time was 60ms ahead of my CPU time
        val predictedSystemTime = fs.frameState.predictedDisplayTime()
        val engineTime = predictedSystemTime - Time.startTimeN
        Time.updateTime(engineTime, lastTime)
        lastTime = engineTime
    }

    fun renderFrameMaybe(xr: OpenXR): Boolean {
        events.pollEvents(xr)
        if (events.canSkipRendering) {
            return false
        }

        fs.waitFrame(session)
        updateEngineTime()
        updateViews(space, session, views, fs)
        if (events.sessionFocussed) {
            actions.updateActions(space, fs.frameState)
        } else {
            actions.clearActions(fs.frameState)
        }
        fs.beginFrame(session)

        val rendered = fs.frameState.shouldRender()
        if (rendered) {
            xr.beginRenderViews()
            for (viewIndex in 0 until viewCount) {
                renderView(xr, viewIndex)
            }
        }

        fs.endFrame(session, space, projectionViews)
        return rendered
    }
}