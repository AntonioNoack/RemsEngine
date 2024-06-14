package me.anno.openxr

import me.anno.openxr.OpenXRUtils.checkXR
import me.anno.openxr.OpenXRUtils.longPtr
import me.anno.openxr.OpenXRUtils.ptr
import me.anno.openxr.OpenXRUtils.ptr1
import me.anno.utils.pooling.ByteBufferPool
import org.lwjgl.openxr.XR10.XR_ACTION_TYPE_FLOAT_INPUT
import org.lwjgl.openxr.XR10.XR_ACTION_TYPE_POSE_INPUT
import org.lwjgl.openxr.XR10.XR_ACTION_TYPE_VIBRATION_OUTPUT
import org.lwjgl.openxr.XR10.XR_FREQUENCY_UNSPECIFIED
import org.lwjgl.openxr.XR10.XR_MIN_HAPTIC_DURATION
import org.lwjgl.openxr.XR10.XR_NULL_PATH
import org.lwjgl.openxr.XR10.XR_SESSION_NOT_FOCUSED
import org.lwjgl.openxr.XR10.XR_TYPE_ACTIONS_SYNC_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_ACTION_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_ACTION_SET_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_ACTION_SPACE_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_ACTION_STATE_FLOAT
import org.lwjgl.openxr.XR10.XR_TYPE_ACTION_STATE_GET_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_ACTION_STATE_POSE
import org.lwjgl.openxr.XR10.XR_TYPE_HAPTIC_ACTION_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_HAPTIC_VIBRATION
import org.lwjgl.openxr.XR10.XR_TYPE_INTERACTION_PROFILE_SUGGESTED_BINDING
import org.lwjgl.openxr.XR10.XR_TYPE_SPACE_LOCATION
import org.lwjgl.openxr.XR10.xrApplyHapticFeedback
import org.lwjgl.openxr.XR10.xrCreateAction
import org.lwjgl.openxr.XR10.xrCreateActionSet
import org.lwjgl.openxr.XR10.xrCreateActionSpace
import org.lwjgl.openxr.XR10.xrGetActionStateFloat
import org.lwjgl.openxr.XR10.xrGetActionStatePose
import org.lwjgl.openxr.XR10.xrLocateSpace
import org.lwjgl.openxr.XR10.xrStringToPath
import org.lwjgl.openxr.XR10.xrSuggestInteractionProfileBindings
import org.lwjgl.openxr.XR10.xrSyncActions
import org.lwjgl.openxr.XrAction
import org.lwjgl.openxr.XrActionCreateInfo
import org.lwjgl.openxr.XrActionSet
import org.lwjgl.openxr.XrActionSetCreateInfo
import org.lwjgl.openxr.XrActionSpaceCreateInfo
import org.lwjgl.openxr.XrActionStateFloat
import org.lwjgl.openxr.XrActionStateGetInfo
import org.lwjgl.openxr.XrActionStatePose
import org.lwjgl.openxr.XrActionSuggestedBinding
import org.lwjgl.openxr.XrActionsSyncInfo
import org.lwjgl.openxr.XrActiveActionSet
import org.lwjgl.openxr.XrFrameState
import org.lwjgl.openxr.XrHapticActionInfo
import org.lwjgl.openxr.XrHapticBaseHeader
import org.lwjgl.openxr.XrHapticVibration
import org.lwjgl.openxr.XrInstance
import org.lwjgl.openxr.XrInteractionProfileSuggestedBinding
import org.lwjgl.openxr.XrPosef
import org.lwjgl.openxr.XrSession
import org.lwjgl.openxr.XrSpace
import org.lwjgl.openxr.XrSpaceLocation
import java.nio.LongBuffer
import kotlin.math.max

class OpenXRActions(val instance: XrInstance, val session: XrSession, identityPose: XrPosef) {

    private fun stringToPath(string: String): Long {
        checkXR(xrStringToPath(instance, string, longPtr))
        return longPtr[0]
    }

    private fun createPathPair(pattern: String): LongBuffer {
        val buffer = ByteBufferPool.allocateDirect(8 * 2).asLongBuffer()
        buffer.put(0, stringToPath(pattern))
        buffer.put(1, stringToPath(pattern.replace("left", "right")))
        return buffer
    }

    val handPaths = createPathPair("/user/hand/left")
    val selectClickPath = createPathPair("/user/hand/left/input/select/click")
    val triggerValuePath = createPathPair("/user/hand/left/input/trigger/value")

    // val thumbStickYPath = createPathPair("/user/hand/left/input/thumbstick/y") // todo how do we query this?
    val gripPosePath = createPathPair("/user/hand/left/input/grip/pose")
    val hapticPath = createPathPair("/user/hand/left/output/haptic")

    init {
        val actionSetCreateInfo = XrActionSetCreateInfo.calloc()
            .type(XR_TYPE_ACTION_SET_CREATE_INFO).next(0)
            .priority(0)
            .actionSetName("gameplay_actionset".ptr1())
            .localizedActionSetName("Gameplay Actions".ptr1())
        checkXR(xrCreateActionSet(instance, actionSetCreateInfo, ptr))
    }

    private fun createAction(
        actionSet: XrActionSet, paths: LongBuffer,
        id: String, name: String, actionType: Int
    ): XrAction {
        val actionInfo = XrActionCreateInfo.calloc()
            .type(XR_TYPE_ACTION_CREATE_INFO).next(0)
            .actionType(actionType)
            .actionName(id.ptr1())
            .localizedActionName(name.ptr1())
            .countSubactionPaths(2)
            .subactionPaths(paths)
        checkXR(xrCreateAction(actionSet, actionInfo, ptr))
        return XrAction(ptr[0], actionSet)
    }

    val actionSet = XrActionSet(ptr[0], instance)
    val handPoseAction = createAction(actionSet, handPaths, "handpose", "Hand Pose", XR_ACTION_TYPE_POSE_INPUT)
    val handPoseSpaces = Array(2) { hand ->
        val actionSpaceInfo = XrActionSpaceCreateInfo.calloc()
            .type(XR_TYPE_ACTION_SPACE_CREATE_INFO).next(0)
            .action(handPoseAction)
            .poseInActionSpace(identityPose)
            .subactionPath(handPaths[hand])
        checkXR(xrCreateActionSpace(session, actionSpaceInfo, ptr))
        XrSpace(ptr[0], session)
    }

    val grabActionFloat =
        createAction(actionSet, handPaths, "grabobjectfloat", "Grab Object", XR_ACTION_TYPE_FLOAT_INPUT)
    val hapticAction = createAction(actionSet, handPaths, "haptic", "Haptic Vibration", XR_ACTION_TYPE_VIBRATION_OUTPUT)

    // suggest actions for simple controller
    fun createProfile(path: String, grabPath: LongBuffer) {

        val interactionProfilePath = stringToPath(path)
        val bindings = XrActionSuggestedBinding.calloc(6)

        fun bind(i: Int, action: XrAction, paths: Long) {
            bindings[i].action(action).binding(paths)
        }

        fun bind(i: Int, action: XrAction, paths: LongBuffer) {
            bind(i, action, paths[0])
            bind(i + 1, action, paths[1])
        }

        bind(0, handPoseAction, gripPosePath)
        bind(2, grabActionFloat, grabPath)
        bind(4, hapticAction, hapticPath)

        val suggestedBindings = XrInteractionProfileSuggestedBinding.calloc()
            .type(XR_TYPE_INTERACTION_PROFILE_SUGGESTED_BINDING).next(0)
            .interactionProfile(interactionProfilePath)
            .suggestedBindings(bindings)
        checkXR(xrSuggestInteractionProfileBindings(instance, suggestedBindings))
    }

    init {
        createProfile("/interaction_profiles/khr/simple_controller", selectClickPath)
        createProfile("/interaction_profiles/valve/index_controller", triggerValuePath)
        // todo do we need a Meta Quest profile?
    }

    // haptic stuff
    val vibrationBuffer =
        ByteBufferPool.allocateDirect(max(XrHapticVibration.SIZEOF, XrHapticBaseHeader.SIZEOF))
    val vibration = XrHapticVibration(vibrationBuffer)
    val vibration0 = XrHapticBaseHeader(vibrationBuffer)
    val hapticActionInfo: XrHapticActionInfo = XrHapticActionInfo.calloc()

    // control structs
    val handPoseState: XrActionStatePose = XrActionStatePose.calloc()
    val getInfo: XrActionStateGetInfo = XrActionStateGetInfo.calloc()
    val grabValue: XrActionStateFloat.Buffer = XrActionStateFloat.calloc(2)
    val handLocations: XrSpaceLocation.Buffer = XrSpaceLocation.calloc(2)

    val actionsSyncInfo: XrActionsSyncInfo = XrActionsSyncInfo.calloc()
    val activeActionSets: XrActiveActionSet.Buffer = XrActiveActionSet.calloc(1)

    fun updateActions(playSpace: XrSpace, frameState: XrFrameState) {
        activeActionSets[0].actionSet(actionSet).subactionPath(XR_NULL_PATH)
        actionsSyncInfo.type(XR_TYPE_ACTIONS_SYNC_INFO).next(0)
            .activeActionSets(activeActionSets)
        val ret0 = xrSyncActions(session, actionsSyncInfo)
        if (ret0 != XR_SESSION_NOT_FOCUSED) checkXR(ret0)

        for (hand in 0 until 2) {
            handPoseState.type(XR_TYPE_ACTION_STATE_POSE).next(0)
            getInfo.type(XR_TYPE_ACTION_STATE_GET_INFO).next(0)
                .action(handPoseAction)
                .subactionPath(handPaths[hand])
            checkXR(xrGetActionStatePose(session, getInfo, handPoseState))
            handLocations[hand].type(XR_TYPE_SPACE_LOCATION).next(0)
            checkXR(
                xrLocateSpace(
                    handPoseSpaces[hand], playSpace, frameState.predictedDisplayTime(),
                    handLocations[hand]
                )
            )

            grabValue[hand].type(XR_TYPE_ACTION_STATE_FLOAT).next(0)
            getInfo.type(XR_TYPE_ACTION_STATE_GET_INFO).next(0)
                .action(grabActionFloat).subactionPath(handPaths[hand])
            checkXR(xrGetActionStateFloat(session, getInfo, grabValue[hand]))

            if (grabValue[hand].isActive && grabValue[hand].currentState() > 0.75f) {
                vibration.type(XR_TYPE_HAPTIC_VIBRATION).next(0)
                    .amplitude(0.5f).duration(XR_MIN_HAPTIC_DURATION)
                    .frequency(XR_FREQUENCY_UNSPECIFIED)
                hapticActionInfo.type(XR_TYPE_HAPTIC_ACTION_INFO).next(0)
                    .action(hapticAction).subactionPath(handPaths[hand])
                checkXR(xrApplyHapticFeedback(session, hapticActionInfo, vibration0))
            }
        }
    }
}