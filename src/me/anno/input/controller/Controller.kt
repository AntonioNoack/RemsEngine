package me.anno.input.controller

import me.anno.Time
import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.Range
import me.anno.gpu.GFX
import me.anno.gpu.OSWindow
import me.anno.input.ActionManager
import me.anno.input.ButtonUpdateState
import me.anno.input.Input
import me.anno.input.Input.isMouseLocked
import me.anno.input.Key
import me.anno.input.Output
import me.anno.maths.Maths.sq
import me.anno.utils.InternalAPI
import me.anno.utils.structures.Collections.setContains
import me.anno.utils.structures.lists.Lists.any2
import org.apache.logging.log4j.LogManager
import org.joml.Quaternionf
import org.joml.Vector3d
import kotlin.math.abs

@Suppress("unused")
abstract class Controller(maxNumButtons: Int, maxNumAxes: Int) {

    @InternalAPI
    var id = 0

    /**
     * not supported by all types, currently only in VR (via OpenXR);
     * in most cases, add to this value instead of setting it, so multiple effects are accumulated
     * */
    @Range(0.0, 1.0)
    var rumble = 0f

    // only set for VR controllers
    val position = Vector3d()
    val rotation = Quaternionf()

    val axisValues = FloatArray(maxNumAxes)
    val axisSpeeds = FloatArray(maxNumAxes)
    val buttonDownTime = LongArray(maxNumButtons)
    val axesDownTime = LongArray(maxNumAxes * 2)

    abstract val type: ControllerType

    @InternalAPI // used for calibration
    val rawAxisValues = FloatArray(maxNumAxes)

    fun getRawAxis(axis: Int): Float {
        return if (axis in 0 until numAxes) rawAxisValues[axis] else 0f
    }

    var calibration = ControllerCalibration()
    open val baseKey: Int get() = Key.CONTROLLER_0_KEY_0.id + 32 * id

    var name = ""
    var guid = ""

    var isConnected = false
        set(value) {
            if (field != value) {
                field = value
                onConnectionChanged(value)
            }
        }

    private fun onConnectionChanged(value: Boolean) {
        Input.controllers.setContains(this, value)
        if (!value) {
            LOGGER.info("Controller #$id has been deactivated")
        }
        id = Input.controllers.indexOf(this)
        if (value) {
            LOGGER.info("Controller #$id has been activated")
        }
        for (listener in onControllerChanged) {
            try {
                listener(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    abstract val numButtons: Int
    abstract val numAxes: Int

    /**
     * get whether that button is pressed
     * A = 0, B = 1, X = 2, Y = 3,
     * left wheel = 8, right wheel = 9,
     * cross: 10 = up, 11 = right, 12 = down, 13 = left;
     * left menu = 6, right menu = 7,
     * left shoulder = 4, right shoulder = 5,
     * */
    fun isButtonDown(button: Int): Boolean {
        return button in 0 until numButtons && buttonDownTime[button] != 0L
    }

    /**
     * get the calibrated values for the axes
     * 0/1 = x/y of left wheel,
     * 2/3 = x/y of right wheel,
     * 4/5 = left/right trigger
     * */
    fun getAxis(axis: Int): Float {
        return if (axis in 0 until numAxes) axisValues[axis] else 0f
    }

    val isFirst: Boolean get() = id == 0

    fun setAxisValue(window: OSWindow, axisId: Int, state: Float, dt: Float) {
        val baseAxis = baseKey + numButtons
        val negativeKey = if (numButtons + axisId * 2 < MAX_NUM_INPUTS) {
            Key.byId(baseAxis + axisId * 2)
        } else Key.KEY_UNKNOWN
        val positiveKey = if (numButtons + axisId * 2 < MAX_NUM_INPUTS) {
            Key.byId(baseAxis + axisId * 2 + 1)
        } else Key.KEY_UNKNOWN
        setAxisValue(window, axisId, state, dt, negativeKey, positiveKey)
    }

    fun setAxisValue(
        window: OSWindow, axisId: Int, state: Float, dt: Float,
        negativeKey: Key, positiveKey: Key
    ) {
        setAxisValue(
            window, axisId, state, dt, negativeKey, positiveKey,
            axesDownTime, axisId * 2
        )
    }

    fun setAxisValue(
        window: OSWindow, axisId: Int, state: Float, dt: Float,
        negativeKey: Key, positiveKey: Key,
        buttonDownTime: LongArray, buttonId01: Int
    ) {
        val lastValue = axisValues[axisId]
        val value = calibration.getValue(state, axisId)
        axisValues[axisId] = value
        axisSpeeds[axisId] = (value - lastValue) / dt

        // trigger for actions
        // can we find out if an axis can be down? triggers cannot do that
        //  -> not really, we can just assume
        val axisKeyTriggerPoint = axisKeyTriggerPoint
        ButtonUpdateState.callButtonUpdateEvents(
            window, Time.frameTimeNanos, value < -axisKeyTriggerPoint,
            buttonDownTime, buttonId01, negativeKey
        )
        ButtonUpdateState.callButtonUpdateEvents(
            window, Time.frameTimeNanos, value > axisKeyTriggerPoint,
            buttonDownTime, buttonId01 + 1, positiveKey
        )
    }

    fun updateMouseScroll(window: OSWindow, dt: Float) {
        // if buttons = 2,3 (right wheel), then scroll
        val scrollButtons = DefaultConfig["ui.controller.mouseWheelAxis0", 2]
        if (scrollButtons in 0 until numAxes - 1 &&
            (axisValues[scrollButtons] != 0f || axisValues[scrollButtons + 1] != 0f)
        ) {

            var dx = axisValues[scrollButtons]
            var dy = axisValues[scrollButtons + 1]

            // non-linear curve for better control, and fast-scrolling
            val speed = DefaultConfig["ui.controller.mouseWheelSpeed", 50f] * dt
            if (DefaultConfig["ui.controller.mouseWheelEnableAcceleration", true]) {
                val f = sq(dx, dy)
                dx *= f
                dy *= f
            }

            dx *= speed
            dy *= speed

            if (GFX.windows.any2 { it.isInFocus } && isMouseInWindow()) {
                if (DefaultConfig["ui.controller.mouseWheelIsSingleAxis", false]) {
                    Input.onMouseWheel(window, 0f, -dy, true)
                } else {
                    Input.onMouseWheel(window, dx, -dy, false)
                }
                mouseWheelFract = 0f
            } else {
                mouseWheelFract += dy
                val mwf = mouseWheelFract.toInt() // round towards zero
                if (mwf != 0) {
                    Output.systemMouseWheel(mwf)
                    mouseWheelFract -= mwf
                }
            }
        }
    }

    fun updateMouseMovement(window: OSWindow, dt: Float) {
        // if buttons = 0,1 (left wheel), then move the mouse
        val moveButtons = DefaultConfig["ui.controller.mouseMoveAxis0", 0]
        if (moveButtons in 0 until numAxes - 1 &&
            (axisValues[moveButtons] != 0f || axisValues[moveButtons + 1] != 0f)
        ) {

            val speed = DefaultConfig["ui.controller.mouseMoveSpeed", 1000f] * dt
            var dx = axisValues[moveButtons]
            var dy = axisValues[moveButtons + 1]

            if (DefaultConfig["ui.controller.mouseMoveEnableAcceleration", true]) {
                val f = sq(dx, dy)
                dx *= f
                dy *= f
            }

            dx *= speed
            dy *= speed

            ActionManager.onMouseMoved(window, dx, dy)
            if (!isMouseLocked) {
                val time = Time.nanoTime
                // only works well, if we have a single player
                // if we have a mouse user and a controller user, the controller user will win here ...
                // reset the mouse position, if we used the original mouse again
                // to do update mouseX/Y outside the main window, and then remove this condition
                // don't reset, if we're outside the window, because Input.mouseX/Y is not updated outside
                val mainWindow = GFX.windows.first()
                if (abs(time - lastMousePos) > 1e9 && isMouseInWindow()) {// 1s delay to switch back to mouse
                    mousePosX = mainWindow.mouseX
                    mousePosY = mainWindow.mouseY
                }
                lastMousePos = time
                mousePosX += dx
                mousePosY += dy
                mainWindow.moveMouseTo(mousePosX, mousePosY)
            }
        }
    }

    fun buttonDown(window: OSWindow, key: Int) {
        if (isFirst) {
            val mouseKey = getMouseKey(key)
            if (mouseKey != null) {
                mouseButtonDown(window, mouseKey)
            }
        }
        if (key < MAX_NUM_INPUTS) {
            ActionManager.onKeyDown(window, Key.byId(baseKey + key))
        }
    }

    fun buttonType(window: OSWindow, key: Int) {
        if (key < MAX_NUM_INPUTS) {
            ActionManager.onKeyTyped(window, Key.byId(baseKey + key))
        }
    }

    fun buttonUp(window: OSWindow, key: Int) {
        if (isFirst) {
            val mouseKey = getMouseKey(key)
            if (mouseKey != null) {
                mouseButtonUp(window, mouseKey)
            }
        }
        if (key < MAX_NUM_INPUTS) {
            ActionManager.onKeyTyped(window, Key.byId(baseKey + key))
        }
    }

    companion object {

        private fun getMouseKey(key: Int): Key? {
            return when (key) {
                DefaultConfig["ui.controller.leftMouseButton", 0] -> Key.BUTTON_LEFT
                DefaultConfig["ui.controller.rightMouseButton", 1] -> Key.BUTTON_RIGHT
                DefaultConfig["ui.controller.middleMouseButton", 9] -> Key.BUTTON_MIDDLE
                else -> null
            }
        }

        var lastMousePos = 0L
        var mousePosX = 0f
        var mousePosY = 0f
        var mouseWheelFract = 0f

        var enableControllerInputs = true

        var onControllerChanged = ArrayList<(Controller) -> Unit>()

        /**
         * use this function to get notified about controller changes
         * */
        fun addControllerChangeListener(listener: (Controller) -> Unit) {
            onControllerChanged.add(listener)
        }

        /**
         * max supported inputs per controller for UI;
         * could be increased by adding more enum values to enum class Key
         * */
        val MAX_NUM_INPUTS = Key.CONTROLLER_1_KEY_0.id - Key.CONTROLLER_0_KEY_0.id

        /**
         * max supported number of controllers for UI;
         * could be increased by adding more enum values to enum class Key
         * */
        const val MAX_NUM_CONTROLLERS = 4

        fun isMouseInWindow(): Boolean {
            return GFX.windows.any2 {
                mousePosX in 0f..it.width - 1f && mousePosY in 0f..it.height - 1f
            }
        }

        private fun mouseButtonDown(window: OSWindow, key: Key) {
            if (isMouseInWindow() && GFX.windows.any2 { it.isInFocus }) {
                Input.onMouseDown(window, key, Time.nanoTime)
            } else {
                Output.systemMousePress(key)
            }
        }

        private fun mouseButtonUp(window: OSWindow, key: Key) {
            if (isMouseInWindow() && GFX.windows.any2 { it.isInFocus }) {
                Input.onMouseUp(window, key, Time.nanoTime)
            } else {
                Output.systemMouseRelease(key)
            }
        }

        // should probably get a place in the config as well
        val axisKeyTriggerPoint get() = DefaultConfig["controller.axisKeyTriggerPoint", 0.25f]

        // private val minActivationPoint = 0.05f
        // private val maxActivationPoint = 0.95f
        private val LOGGER = LogManager.getLogger(Controller::class)
    }
}