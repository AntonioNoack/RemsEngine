package me.anno.openxr

import me.anno.Time
import me.anno.gpu.GFX
import me.anno.input.ButtonUpdateState
import me.anno.input.Key
import me.anno.engine.ui.vr.VROffset.additionalOffset
import me.anno.engine.ui.vr.VROffset.additionalRotation
import me.anno.maths.Maths.clamp
import me.anno.openxr.OpenXRController.Companion.xrControllers
import me.anno.openxr.OpenXRUtils.checkXR
import me.anno.openxr.OpenXRUtils.longPtr
import me.anno.openxr.OpenXRUtils.ptr
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.pooling.NativeStringPointers.buffer
import me.anno.utils.types.Booleans.toInt
import org.joml.Quaterniond
import org.lwjgl.openxr.XR10.XR_ACTION_TYPE_BOOLEAN_INPUT
import org.lwjgl.openxr.XR10.XR_ACTION_TYPE_FLOAT_INPUT
import org.lwjgl.openxr.XR10.XR_ACTION_TYPE_POSE_INPUT
import org.lwjgl.openxr.XR10.XR_ACTION_TYPE_VECTOR2F_INPUT
import org.lwjgl.openxr.XR10.XR_ACTION_TYPE_VIBRATION_OUTPUT
import org.lwjgl.openxr.XR10.XR_FREQUENCY_UNSPECIFIED
import org.lwjgl.openxr.XR10.XR_MIN_HAPTIC_DURATION
import org.lwjgl.openxr.XR10.XR_NULL_PATH
import org.lwjgl.openxr.XR10.XR_SESSION_NOT_FOCUSED
import org.lwjgl.openxr.XR10.XR_TYPE_ACTIONS_SYNC_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_ACTION_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_ACTION_SET_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_ACTION_SPACE_CREATE_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_ACTION_STATE_BOOLEAN
import org.lwjgl.openxr.XR10.XR_TYPE_ACTION_STATE_FLOAT
import org.lwjgl.openxr.XR10.XR_TYPE_ACTION_STATE_GET_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_ACTION_STATE_POSE
import org.lwjgl.openxr.XR10.XR_TYPE_ACTION_STATE_VECTOR2F
import org.lwjgl.openxr.XR10.XR_TYPE_HAPTIC_ACTION_INFO
import org.lwjgl.openxr.XR10.XR_TYPE_HAPTIC_VIBRATION
import org.lwjgl.openxr.XR10.XR_TYPE_INTERACTION_PROFILE_SUGGESTED_BINDING
import org.lwjgl.openxr.XR10.XR_TYPE_SPACE_LOCATION
import org.lwjgl.openxr.XR10.xrApplyHapticFeedback
import org.lwjgl.openxr.XR10.xrCreateAction
import org.lwjgl.openxr.XR10.xrCreateActionSet
import org.lwjgl.openxr.XR10.xrCreateActionSpace
import org.lwjgl.openxr.XR10.xrGetActionStateBoolean
import org.lwjgl.openxr.XR10.xrGetActionStateFloat
import org.lwjgl.openxr.XR10.xrGetActionStatePose
import org.lwjgl.openxr.XR10.xrGetActionStateVector2f
import org.lwjgl.openxr.XR10.xrLocateSpace
import org.lwjgl.openxr.XR10.xrStringToPath
import org.lwjgl.openxr.XR10.xrSuggestInteractionProfileBindings
import org.lwjgl.openxr.XR10.xrSyncActions
import org.lwjgl.openxr.XrAction
import org.lwjgl.openxr.XrActionCreateInfo
import org.lwjgl.openxr.XrActionSet
import org.lwjgl.openxr.XrActionSetCreateInfo
import org.lwjgl.openxr.XrActionSpaceCreateInfo
import org.lwjgl.openxr.XrActionStateBoolean
import org.lwjgl.openxr.XrActionStateFloat
import org.lwjgl.openxr.XrActionStateGetInfo
import org.lwjgl.openxr.XrActionStatePose
import org.lwjgl.openxr.XrActionStateVector2f
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

    val joystickPath = createPathPair("/user/hand/left/input/thumbstick")
    val gripPosePath = createPathPair("/user/hand/left/input/grip/pose")
    val squeezePath = createPathPair("/user/hand/left/input/squeeze/value")
    val hapticPath = createPathPair("/user/hand/left/output/haptic")

    val tmpActionInfo: XrActionCreateInfo = XrActionCreateInfo.calloc()
    val tmpActionSpaceCreateInfo: XrActionSpaceCreateInfo = XrActionSpaceCreateInfo.calloc()
    val tmpSuggestedBindings: XrInteractionProfileSuggestedBinding = XrInteractionProfileSuggestedBinding.calloc()

    private fun createActionSet(): XrActionSet {
        val actionSetCreateInfo = XrActionSetCreateInfo.calloc()
            .type(XR_TYPE_ACTION_SET_CREATE_INFO).next(0)
            .priority(0)
            .actionSetName("gameplay_actions".buffer())
            .localizedActionSetName("Gameplay Actions".buffer())
        checkXR(xrCreateActionSet(instance, actionSetCreateInfo, ptr))
        actionSetCreateInfo.free()
        return XrActionSet(ptr[0], instance)
    }

    private fun createAction(
        paths: LongBuffer, id: String,
        name: String, actionType: Int
    ): XrAction {
        val actionInfo = tmpActionInfo
            .type(XR_TYPE_ACTION_CREATE_INFO).next(0)
            .actionType(actionType)
            .actionName(id.buffer())
            .localizedActionName(name.buffer())
            .countSubactionPaths(paths.remaining())
            .subactionPaths(paths)
        checkXR(xrCreateAction(actionSet, actionInfo, ptr))
        return XrAction(ptr[0], actionSet)
    }

    val actionSet = createActionSet()
    val handPoseAction = createAction(handPaths, "hand_pose", "Hand Pose", XR_ACTION_TYPE_POSE_INPUT)
    val handPoseSpaces = Array(2) { hand ->
        val actionSpaceInfo = tmpActionSpaceCreateInfo
            .type(XR_TYPE_ACTION_SPACE_CREATE_INFO).next(0)
            .action(handPoseAction)
            .subactionPath(handPaths[hand])
            .poseInActionSpace(identityPose)
        checkXR(xrCreateActionSpace(session, actionSpaceInfo, ptr))
        XrSpace(ptr[0], session)
    }

    // todo thumbstick clicks/touches
    // todo button touches
    // todo trigger/squeeze touches

    val thumbstickAction = createAction(handPaths, "thumbsticks", "Thumbsticks", XR_ACTION_TYPE_VECTOR2F_INPUT)
    val grabAction = createAction(handPaths, "grab_object", "Grab Object", XR_ACTION_TYPE_FLOAT_INPUT)
    val hapticAction = createAction(handPaths, "haptic", "Haptic Vibration", XR_ACTION_TYPE_VIBRATION_OUTPUT)
    val buttonAction0 = createAction(handPaths, "primary_buttons", "X/A-Buttons", XR_ACTION_TYPE_BOOLEAN_INPUT)
    val buttonAction1 = createAction(handPaths, "secondary_buttons", "Y/B-Buttons", XR_ACTION_TYPE_BOOLEAN_INPUT)
    val buttonAction2 = createAction(handPaths, "menu_buttons", "Menu-Buttons", XR_ACTION_TYPE_BOOLEAN_INPUT)
    val squeezeAction = createAction(handPaths, "squeeze", "Squeeze", XR_ACTION_TYPE_FLOAT_INPUT)

    // suggest actions for simple controller
    fun createProfile(
        path: String, grabPath: LongBuffer,
        buttons: String, leftMenu: String, rightMenu: String
    ) {

        val interactionProfilePath = stringToPath(path)
        val isNotSimple = !path.endsWith("simple_controller")
        val bindings = XrActionSuggestedBinding.calloc(8 + isNotSimple.toInt(4) + buttons.length)

        var bi = 0
        fun bind(action: XrAction, paths: Long) {
            bindings[bi++].action(action).binding(paths)
        }

        fun bind(action: XrAction, paths: LongBuffer) {
            bind(action, paths[0])
            bind(action, paths[1])
        }

        // six paths
        bind(handPoseAction, gripPosePath)
        bind(grabAction, grabPath)
        bind(hapticAction, hapticPath)
        // two more
        bind(buttonAction2, stringToPath("/user/hand/left/input/$leftMenu/click"))
        bind(buttonAction2, stringToPath("/user/hand/right/input/$rightMenu/click"))
        // four if not simple controller
        if (isNotSimple) {
            bind(thumbstickAction, joystickPath)
            bind(squeezeAction, squeezePath)
        }
        // buttons for Valve index controller and Oculus touch controller
        if (buttons.length == 4) {
            bind(buttonAction0, stringToPath("/user/hand/left/input/${buttons[0]}/click"))
            bind(buttonAction1, stringToPath("/user/hand/left/input/${buttons[1]}/click"))
            bind(buttonAction0, stringToPath("/user/hand/right/input/${buttons[2]}/click"))
            bind(buttonAction1, stringToPath("/user/hand/right/input/${buttons[3]}/click"))
        }

        val suggestedBindings = tmpSuggestedBindings
            .type(XR_TYPE_INTERACTION_PROFILE_SUGGESTED_BINDING).next(0)
            .interactionProfile(interactionProfilePath)
            .suggestedBindings(bindings)
        checkXR(xrSuggestInteractionProfileBindings(instance, suggestedBindings))
    }

    init {
        val selectClickPath = createPathPair("/user/hand/left/input/select/click")
        val triggerValuePath = createPathPair("/user/hand/left/input/trigger/value")
        // https://docs.unity3d.com/Packages/com.unity.xr.openxr@0.1/manual/features/khrsimplecontrollerprofile.html
        createProfile("/interaction_profiles/khr/simple_controller", selectClickPath, "", "menu", "menu")
        // https://docs.unity3d.com/Packages/com.unity.xr.openxr@0.1/manual/features/valveindexcontrollerprofile.html
        createProfile("/interaction_profiles/valve/index_controller", triggerValuePath, "abab", "system", "system")
        // https://docs.unity3d.com/Packages/com.unity.xr.openxr@0.1/manual/features/oculustouchcontrollerprofile.html
        createProfile("/interaction_profiles/oculus/touch_controller", triggerValuePath, "xyab", "menu", "system")
    }

    // haptic stuff
    val vibrationBuffer =
        ByteBufferPool.allocateDirect(max(XrHapticVibration.SIZEOF, XrHapticBaseHeader.SIZEOF))
    val vibration = XrHapticVibration(vibrationBuffer)
    val vibration0 = XrHapticBaseHeader(vibrationBuffer)
    val hapticActionInfo: XrHapticActionInfo = XrHapticActionInfo.calloc()

    // control structs
    val getInfo: XrActionStateGetInfo = XrActionStateGetInfo.calloc()
    val poseState: XrActionStatePose = XrActionStatePose.calloc()
    val floatState: XrActionStateFloat = XrActionStateFloat.calloc()
    val booleanState: XrActionStateBoolean = XrActionStateBoolean.calloc()
    val vector2fState: XrActionStateVector2f = XrActionStateVector2f.calloc()
    val handLocations: XrSpaceLocation.Buffer = XrSpaceLocation.calloc(2)

    val actionsSyncInfo: XrActionsSyncInfo = XrActionsSyncInfo.calloc()
    val activeActionSets: XrActiveActionSet.Buffer = XrActiveActionSet.calloc(1)

    val engineButtons = listOf(
        Key.CONTROLLER_LEFT_KEY_X, // x
        Key.CONTROLLER_RIGHT_KEY_X, // a
        Key.CONTROLLER_LEFT_KEY_Y, // y
        Key.CONTROLLER_RIGHT_KEY_Y, // b
        // todo these ones don't work :/
        Key.CONTROLLER_LEFT_KEY_MENU, // menu
        Key.CONTROLLER_RIGHT_KEY_MENU, // system
    )

    val buttonsTimers = LongArray(6)
    val buttonActions = listOf(buttonAction0, buttonAction1, buttonAction2)

    val keysByAxis = listOf(
        Key.CONTROLLER_LEFT_THUMBSTICK_LEFT, Key.CONTROLLER_LEFT_THUMBSTICK_RIGHT,
        Key.CONTROLLER_RIGHT_THUMBSTICK_LEFT, Key.CONTROLLER_RIGHT_THUMBSTICK_RIGHT,
        Key.CONTROLLER_LEFT_THUMBSTICK_DOWN, Key.CONTROLLER_LEFT_THUMBSTICK_UP,
        Key.CONTROLLER_RIGHT_THUMBSTICK_DOWN, Key.CONTROLLER_RIGHT_THUMBSTICK_UP,
        Key.KEY_UNKNOWN, Key.CONTROLLER_LEFT_TRIGGER_PRESS,
        Key.KEY_UNKNOWN, Key.CONTROLLER_RIGHT_TRIGGER_PRESS,
        Key.KEY_UNKNOWN, Key.CONTROLLER_LEFT_SQUEEZE_PRESS,
        Key.KEY_UNKNOWN, Key.CONTROLLER_RIGHT_SQUEEZE_PRESS,
    )

    private fun updatePose(
        action: XrAction, subactionPath: Long,
        location: XrSpaceLocation, poseSpace: XrSpace,
        playSpace: XrSpace, displayTime: Long
    ) {
        poseState.type(XR_TYPE_ACTION_STATE_POSE).next(0)
        getInfo.type(XR_TYPE_ACTION_STATE_GET_INFO).next(0)
            .action(action).subactionPath(subactionPath)
        checkXR(xrGetActionStatePose(session, getInfo, poseState))
        location.type(XR_TYPE_SPACE_LOCATION).next(0)
        checkXR(xrLocateSpace(poseSpace, playSpace, displayTime, location))
    }

    fun clearActions(frameState: XrFrameState) {
        val window = GFX.activeWindow!!
        val dt = frameState.predictedDisplayPeriod() * 1e-9f
        for (hand in xrControllers.indices) {
            val controller = xrControllers[hand]
            for (i in 0 until 4) {
                val keyOffset = hand * 2 + i * 4
                controller.setAxisValue(
                    window, i, Float.NaN, dt,
                    keysByAxis[keyOffset], keysByAxis[keyOffset + 1]
                )
            }
        }
        for (i in engineButtons.indices) {
            ButtonUpdateState.callButtonUpdateEvents(window, Time.gameTimeN, false, buttonsTimers, i, engineButtons[i])
        }
    }

    private fun syncActions() {
        activeActionSets[0].actionSet(actionSet).subactionPath(XR_NULL_PATH)
        actionsSyncInfo.type(XR_TYPE_ACTIONS_SYNC_INFO).next(0)
            .activeActionSets(activeActionSets)
        val ret0 = xrSyncActions(session, actionsSyncInfo)
        if (ret0 != XR_SESSION_NOT_FOCUSED) checkXR(ret0)
    }

    private fun setupGetInfo(action: XrAction, subactionPath: Long) {
        getInfo.type(XR_TYPE_ACTION_STATE_GET_INFO).next(0)
            .action(action).subactionPath(subactionPath)
    }

    private fun updateVector2fState(action: XrAction, subactionPath: Long) {
        vector2fState.type(XR_TYPE_ACTION_STATE_VECTOR2F).next(0)
        setupGetInfo(action, subactionPath)
        checkXR(xrGetActionStateVector2f(session, getInfo, vector2fState))
    }

    private fun updateFloatState(action: XrAction, subactionPath: Long) {
        floatState.type(XR_TYPE_ACTION_STATE_FLOAT).next(0)
        setupGetInfo(action, subactionPath)
        checkXR(xrGetActionStateFloat(session, getInfo, floatState))
    }

    private fun updateBooleanState(action: XrAction, subactionPath: Long) {
        booleanState.type(XR_TYPE_ACTION_STATE_BOOLEAN).next(0)
        setupGetInfo(action, subactionPath)
        checkXR(xrGetActionStateBoolean(session, getInfo, booleanState))
    }

    private fun getFloatStateOrNaN(action: XrAction, subactionPath: Long): Float {
        updateFloatState(action, subactionPath)
        return if (floatState.isActive) floatState.currentState() else Float.NaN
    }

    fun updateActions(playSpace: XrSpace, frameState: XrFrameState) {
        syncActions()

        val window = GFX.activeWindow!!
        val dt = frameState.predictedDisplayPeriod() * 1e-9f
        val displayTime = frameState.predictedDisplayTime()
        for (hand in 0 until 2) {
            val handPath = handPaths[hand]
            updatePose(
                handPoseAction, handPath,
                handLocations[hand], handPoseSpaces[hand],
                playSpace, displayTime
            )

            // ^^, not necessarily true, but we want them to always be in the correct order
            val controller = xrControllers[hand]
            controller.isConnected = true

            val pose = handLocations[hand].pose()
            val pos = pose.`position$`()
            val rot = pose.orientation()
            controller.position.set(pos.x(), pos.y(), pos.z()) // play space
                .rotate(Quaterniond(additionalRotation)) // -> scene space
                .add(additionalOffset)
            controller.rotation
                .set(additionalRotation)
                .mul(rot.x(), rot.y(), rot.z(), rot.w())

            val keyOffset = hand * 2
            updateVector2fState(thumbstickAction, handPath)
            if (vector2fState.isActive) {
                val value = vector2fState.currentState()
                controller.setAxisValue(
                    window, AXIS_THUMBSTICK_X, value.x(), dt,
                    keysByAxis[keyOffset + 0], keysByAxis[keyOffset + 1] // 0-3
                )
                controller.setAxisValue(
                    window, AXIS_THUMBSTICK_Y, value.y(), dt,
                    keysByAxis[keyOffset + 4], keysByAxis[keyOffset + 5] // 4-7
                )
            }

            val grab = getFloatStateOrNaN(grabAction, handPath)
            controller.setAxisValue(
                window, AXIS_TRIGGER, grab, dt,
                Key.KEY_UNKNOWN, keysByAxis[keyOffset + 9] // 8-11
            )

            val squeeze = getFloatStateOrNaN(squeezeAction, handPath)
            controller.setAxisValue(
                window, AXIS_SQUEEZE, squeeze, dt,
                Key.KEY_UNKNOWN, keysByAxis[keyOffset + 11] // 12-15
            )

            val rumble = clamp(controller.rumble)
            if (rumble > 0f) {
                vibration.type(XR_TYPE_HAPTIC_VIBRATION).next(0)
                    .amplitude(rumble).duration(XR_MIN_HAPTIC_DURATION)
                    .frequency(XR_FREQUENCY_UNSPECIFIED)
                hapticActionInfo.type(XR_TYPE_HAPTIC_ACTION_INFO).next(0)
                    .action(hapticAction).subactionPath(handPath)
                checkXR(xrApplyHapticFeedback(session, hapticActionInfo, vibration0))
            }

            // rumble needs to be set each frame,
            // to it can be set from multiple sources independently
            controller.rumble = 0f
        }

        // todo booleanState.lastChangedTime contains the correct time, but idk what time-system it is
        //  same as predicated display time, so todo: override time with predicatedDisplayTime-system
        val time = Time.nanoTime
        for (i in 0 until 4) {
            updateBooleanState(buttonActions[i.shr(1)], handPaths[i.and(1)])
            val currState = if (booleanState.isActive) booleanState.currentState() else null
            ButtonUpdateState.callButtonUpdateEvents(
                window, time, currState == true,
                buttonsTimers, i, engineButtons[i]
            )
        }
    }

    companion object {
        const val AXIS_THUMBSTICK_X = 0
        const val AXIS_THUMBSTICK_Y = 1
        const val AXIS_TRIGGER = 2
        const val AXIS_SQUEEZE = 3
    }
}