package me.anno.openxr

import me.anno.openxr.OpenXRUtils.checkXR
import me.anno.openxr.OpenXRUtils.ptr
import me.anno.utils.types.Booleans.hasFlag
import org.lwjgl.openxr.XR10.XR_ENVIRONMENT_BLEND_MODE_OPAQUE
import org.lwjgl.openxr.XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION
import org.lwjgl.openxr.XR10.XR_TYPE_FRAME_BEGIN_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_FRAME_END_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_FRAME_STATE
import org.lwjgl.openxr.XR10.XR_TYPE_FRAME_WAIT_INFO
import org.lwjgl.openxr.XR10.XR_VIEW_STATE_ORIENTATION_VALID_BIT
import org.lwjgl.openxr.XR10.xrBeginFrame
import org.lwjgl.openxr.XR10.xrEndFrame
import org.lwjgl.openxr.XR10.xrWaitFrame
import org.lwjgl.openxr.XrCompositionLayerBaseHeader
import org.lwjgl.openxr.XrCompositionLayerProjection
import org.lwjgl.openxr.XrCompositionLayerProjectionView
import org.lwjgl.openxr.XrFrameBeginInfo
import org.lwjgl.openxr.XrFrameEndInfo
import org.lwjgl.openxr.XrFrameState
import org.lwjgl.openxr.XrFrameWaitInfo
import org.lwjgl.openxr.XrSession
import org.lwjgl.openxr.XrSpace
import org.lwjgl.openxr.XrSwapchainImageAcquireInfo
import org.lwjgl.openxr.XrSwapchainImageReleaseInfo
import org.lwjgl.openxr.XrSwapchainImageWaitInfo
import org.lwjgl.openxr.XrViewLocateInfo
import org.lwjgl.openxr.XrViewState

class XrFrameStructs {

    // frame structs
    val frameState: XrFrameState = XrFrameState.calloc()
    val frameWaitInfo: XrFrameWaitInfo = XrFrameWaitInfo.calloc()
    val viewLocateInfo: XrViewLocateInfo = XrViewLocateInfo.calloc()
    val waitInfo: XrSwapchainImageWaitInfo = XrSwapchainImageWaitInfo.calloc()
    val acquireInfo: XrSwapchainImageAcquireInfo = XrSwapchainImageAcquireInfo.create()
    val frameBeginInfo: XrFrameBeginInfo = XrFrameBeginInfo.create()
    val frameEndInfo: XrFrameEndInfo = XrFrameEndInfo.calloc()
    val releaseInfo: XrSwapchainImageReleaseInfo = XrSwapchainImageReleaseInfo.calloc()

    // other stuff
    val projectionLayer: XrCompositionLayerProjection = XrCompositionLayerProjection.calloc()
    val viewState: XrViewState = XrViewState.calloc()
    val submittedLayers: XrCompositionLayerBaseHeader = XrCompositionLayerBaseHeader
        .create(projectionLayer.address()) // seems to be correct

    fun waitFrame(session: XrSession) {
        // wait for headset-vsync to catch up
        frameState.type(XR_TYPE_FRAME_STATE).next(0)
        frameWaitInfo.type(XR_TYPE_FRAME_WAIT_INFO).next(0)
        checkXR(xrWaitFrame(session, frameWaitInfo, frameState))
    }

    fun beginFrame(session: XrSession) {
        frameBeginInfo.type(XR_TYPE_FRAME_BEGIN_INFO).next(0)
        checkXR(xrBeginFrame(session, frameBeginInfo))
    }

    fun endFrame(
        session: XrSession, playSpace: XrSpace,
        projectionViews: XrCompositionLayerProjectionView.Buffer
    ) {
        // not useless!!, submittedLayer === projectionLayer
        projectionLayer
            .type(XR_TYPE_COMPOSITION_LAYER_PROJECTION)
            .next(0)
            .layerFlags(0)
            .space(playSpace)
            .views(projectionViews)

        ptr.put(0, submittedLayers)
        frameEndInfo
            .type(XR_TYPE_FRAME_END_INFO)
            .next(0)
            .displayTime(frameState.predictedDisplayTime())
            .layerCount(findSubmittedLayerCount(viewState, frameState))
            .layers(ptr)
            // real AR glasses only support additive blending
            // todo why is alpha-blending unsupported in SteamVR?
            .environmentBlendMode(XR_ENVIRONMENT_BLEND_MODE_OPAQUE)
        checkXR(xrEndFrame(session, frameEndInfo))
    }

    private fun findSubmittedLayerCount(viewState: XrViewState, frameState: XrFrameState): Int {
        var submittedLayerCount = 1
        if (!viewState.viewStateFlags().hasFlag(XR_VIEW_STATE_ORIENTATION_VALID_BIT.toLong())) {
            println("Submitting no layers, because orientation is invalid")
            submittedLayerCount = 0
        }

        if (!frameState.shouldRender()) {
            println("Submitting no layers, because shouldRender is false")
            submittedLayerCount = 0
        }
        return submittedLayerCount
    }
}