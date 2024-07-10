package me.anno.openxr

import me.anno.openxr.OpenXR.Companion.VIEW_CONFIG_TYPE
import me.anno.openxr.OpenXRUtils.checkXR
import me.anno.utils.pooling.ByteBufferPool
import org.apache.logging.log4j.LogManager
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_EXITING
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_FOCUSED
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_IDLE
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_LOSS_PENDING
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_READY
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_STOPPING
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_SYNCHRONIZED
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_UNKNOWN
import org.lwjgl.openxr.XR10.XR_SESSION_STATE_VISIBLE
import org.lwjgl.openxr.XR10.XR_TYPE_EVENT_DATA_BUFFER
import org.lwjgl.openxr.XR10.XR_TYPE_EVENT_DATA_INSTANCE_LOSS_PENDING
import org.lwjgl.openxr.XR10.XR_TYPE_EVENT_DATA_INTERACTION_PROFILE_CHANGED
import org.lwjgl.openxr.XR10.XR_TYPE_EVENT_DATA_REFERENCE_SPACE_CHANGE_PENDING
import org.lwjgl.openxr.XR10.XR_TYPE_EVENT_DATA_SESSION_STATE_CHANGED
import org.lwjgl.openxr.XR10.XR_TYPE_SESSION_BEGIN_INFO
import org.lwjgl.openxr.XR10.xrBeginSession
import org.lwjgl.openxr.XR10.xrDestroySession
import org.lwjgl.openxr.XR10.xrEndSession
import org.lwjgl.openxr.XR10.xrPollEvent
import org.lwjgl.openxr.XrEventDataBuffer
import org.lwjgl.openxr.XrEventDataSessionStateChanged
import org.lwjgl.openxr.XrInstance
import org.lwjgl.openxr.XrSession
import org.lwjgl.openxr.XrSessionBeginInfo
import kotlin.math.max

class OpenXREvents(val instance: XrInstance, val session: XrSession, val window: Long) {

    companion object {
        private val LOGGER = LogManager.getLogger(OpenXREvents::class)
    }

    var canSkipRendering = true
    var sessionRunning = false
    var instanceAlive = true
    var sessionFocussed = false

    // event structs
    val eventBuffer =
        ByteBufferPool.allocateDirect(max(XrEventDataBuffer.SIZEOF, XrEventDataSessionStateChanged.SIZEOF))
    val stateEvent = XrEventDataSessionStateChanged(eventBuffer)
    val runtimeEvent = XrEventDataBuffer(eventBuffer)

    fun pollEvents(xr: OpenXR) {
        while (true) {
            runtimeEvent
                .type(XR_TYPE_EVENT_DATA_BUFFER)
                .next(0)
            if (checkXR(xrPollEvent(instance, runtimeEvent))) break
            handleEvent(xr)
        }
    }

    private fun getStateName(state: Int): String {
        return when (state) {
            XR_SESSION_STATE_UNKNOWN -> "Unknown"
            XR_SESSION_STATE_IDLE -> "Idle"
            XR_SESSION_STATE_READY -> "Ready"
            XR_SESSION_STATE_SYNCHRONIZED -> "Synchronized"
            XR_SESSION_STATE_VISIBLE -> "Visible"
            XR_SESSION_STATE_FOCUSED -> "Focused"
            XR_SESSION_STATE_STOPPING -> "Stopping"
            XR_SESSION_STATE_LOSS_PENDING -> "Loss Pending"
            XR_SESSION_STATE_EXITING -> "Exiting"
            else -> state.toString()
        }
    }

    private fun handleEvent(xr: OpenXR) {
        when (runtimeEvent.type()) {
            // todo monitor state changes, e.g. for whether we should render
            XR_TYPE_EVENT_DATA_INSTANCE_LOSS_PENDING -> {}
            XR_TYPE_EVENT_DATA_SESSION_STATE_CHANGED -> {
                val newState = stateEvent.state()
                LOGGER.info("State changed: ${getStateName(newState)}")
                xr.state = newState
                sessionFocussed = newState == XR_SESSION_STATE_FOCUSED
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
                            LOGGER.info("Starting session")
                            val beginInfo = XrSessionBeginInfo.calloc()
                                .type(XR_TYPE_SESSION_BEGIN_INFO).next(0)
                                .primaryViewConfigurationType(VIEW_CONFIG_TYPE)
                            checkXR(xrBeginSession(session, beginInfo))
                            beginInfo.free() // no longer needed
                            LOGGER.info("Session started")
                            sessionRunning = true
                        }
                        canSkipRendering = false
                    }
                    XR_SESSION_STATE_STOPPING -> {
                        if (sessionRunning) {
                            checkXR(xrEndSession(session))
                            LOGGER.info("Session ended")
                            sessionRunning = false
                        }
                        canSkipRendering = true
                    }
                    XR_SESSION_STATE_LOSS_PENDING,
                    XR_SESSION_STATE_EXITING -> {
                        checkXR(xrDestroySession(session))
                        instanceAlive = false
                        canSkipRendering = true
                        LOGGER.info("Destroying session")
                    }
                }
            }
            XR_TYPE_EVENT_DATA_INTERACTION_PROFILE_CHANGED -> {
                // not doing any work except printing
                /*val state = XrInteractionProfileState.calloc()
                    .type(XR_TYPE_INTERACTION_PROFILE_STATE).next(0)
                for(hand in 0 until 2) {
                    checkXR(xrGetCurrentInteractionProfile(session, handPaths[hand], state))
                    val prof = state.interactionProfile()

                }*/
            }
            XR_TYPE_EVENT_DATA_BUFFER -> {}
            XR_TYPE_EVENT_DATA_REFERENCE_SPACE_CHANGE_PENDING -> {}
            else -> {
                LOGGER.info("Unhandled event: ${runtimeEvent.type()}")
            }
        }
    }
}