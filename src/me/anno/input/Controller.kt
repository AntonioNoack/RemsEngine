package me.anno.input

import me.anno.Engine
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX
import me.anno.gpu.GFXBase
import me.anno.gpu.OSWindow
import me.anno.input.Input.isMouseTrapped
import me.anno.input.controller.CalibrationProcedure
import me.anno.input.controller.ControllerCalibration
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.min
import me.anno.maths.Maths.sq
import me.anno.studio.StudioBase
import me.anno.ui.base.menu.Menu
import me.anno.utils.structures.lists.Lists.any2
import org.apache.logging.log4j.LogManager
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWGamepadState
import java.awt.event.InputEvent
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.max

// todo rumble using external library? glfw and lwjgl sadly have no support for it
// https://github.com/williamahartman/Jamepad?
@Suppress("unused")
class Controller(val id: Int) {

    private val glfwId = GLFW_JOYSTICK_1 + id

    var isConnected = false
    val baseKey = BASE_KEY + 32 * id

    private var ticksOnline = 0

    val rawAxisValues = FloatArray(MAX_NUM_AXES)
    val axisValues = FloatArray(MAX_NUM_AXES)
    val axisSpeeds = FloatArray(MAX_NUM_AXES)
    val buttonDownTime = LongArray(MAX_NUM_BUTTONS)

    var calibration = ControllerCalibration()

    var name = ""
    var guid = ""

    private var isActiveMaybe = 0f
    val isActive get() = isActiveMaybe > 0.1f

    var numButtons = 0
        private set
    var numAxes = 0
        private set

    /**
     * when the controller is a gamepad, then we can use the standard controls with 6 axes
     * idk what happens, when the controller has more than that... should we support that somehow?
     * */
    var isGamepad = false

    /**
     * get whether that button is pressed
     * A = 0, B = 1, X = 2, Y = 3,
     * left wheel = 8, right wheel = 9,
     * cross: 10 = up, 11 = right, 12 = down, 13 = left
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

    fun getRawAxis(axis: Int): Float {
        return if (axis in 0 until numAxes) rawAxisValues[axis] else 0f
    }

    private val gamepadState = GLFWGamepadState.calloc()
    private val gamepadAxes get() = gamepadState.axes()
    private val gamepadButtons get() = gamepadState.buttons()

    private var lastTime = 0L

    var isFirst = false

    private fun resetState() {
        name = glfwGetJoystickName(glfwId) ?: "Controller[$id]"
        guid = glfwGetJoystickGUID(glfwId) ?: name
        isGamepad = glfwJoystickIsGamepad(glfwId)
        LOGGER.info("Connected to controller '$name', id '$guid', gamepad? $isGamepad")
        calibration = loadCalibration(guid) ?: ControllerCalibration(isGamepad)
        if (!calibration.isCalibrated) {
            LOGGER.warn("No calibration was found for controller '$name', guid '$guid'")
            Menu.ask(GFX.someWindow!!.windowStack, NameDesc("Calibrate new controller '$name'?")) {
                CalibrationProcedure.start(GFX.someWindow!!, style)
            }
        }
        buttonDownTime.fill(0)
        axisValues.fill(0f)
        ticksOnline = 0
        numButtons = 0
        numAxes = 0
        isActiveMaybe = 0f
        lastTime = Engine.gameTime
    }

    private fun isMouseInWindow(): Boolean {
        return GFX.windows.any {
            mousePosX in 0f..it.width - 1f && mousePosY in 0f..it.height - 1f
        }
    }

    private fun mouseButtonDown(window: OSWindow, key: Key) {
        if (isMouseInWindow() && GFX.windows.any2 { it.isInFocus }) {
            Input.onMousePress(window, key)
        } else {
            GFXBase.robot?.mousePress(
                when (key) {
                    Key.BUTTON_LEFT -> InputEvent.BUTTON1_MASK
                    Key.BUTTON_RIGHT -> InputEvent.BUTTON2_MASK
                    else -> InputEvent.BUTTON3_MASK
                }
            )
        }
    }

    private fun mouseButtonUp(window: OSWindow, key: Key) {
        if (isMouseInWindow() && GFX.windows.any2 { it.isInFocus }) {
            Input.onMouseRelease(window, key)
        } else {
            GFXBase.robot?.mouseRelease(
                when (key) {
                    Key.BUTTON_LEFT -> InputEvent.BUTTON1_MASK
                    Key.BUTTON_RIGHT -> InputEvent.BUTTON2_MASK
                    else -> InputEvent.BUTTON3_MASK
                }
            )
        }
    }

    private fun buttonDown(window: OSWindow, key: Int) {
        if (isFirst) {
            when (key) {
                DefaultConfig["ui.controller.leftMouseButton", 0] -> mouseButtonDown(window, Key.BUTTON_LEFT)
                DefaultConfig["ui.controller.rightMouseButton", 1] -> mouseButtonDown(window, Key.BUTTON_RIGHT)
                // 9 = click on right wheel
                DefaultConfig["ui.controller.middleMouseButton", 9] -> mouseButtonDown(window, Key.BUTTON_MIDDLE)
            }
        }
        if (key < MAX_NUM_INPUTS && id < MAX_NUM_CONTROLLERS) {
            ActionManager.onKeyDown(window, Key.byId(baseKey + key))
        }
        isActiveMaybe = 1f
    }

    private fun buttonType(window: OSWindow, key: Int) {
        if (key < MAX_NUM_INPUTS && id < MAX_NUM_CONTROLLERS) {
            ActionManager.onKeyTyped(window, Key.byId(baseKey + key))
        }
        isActiveMaybe = 1f
    }

    private fun buttonUp(window: OSWindow, key: Int) {
        if (isFirst) {
            when (key) {
                DefaultConfig["ui.controller.leftMouseButton", 0] -> mouseButtonUp(window, Key.BUTTON_LEFT)
                DefaultConfig["ui.controller.rightMouseButton", 1] -> mouseButtonUp(window, Key.BUTTON_RIGHT)
                // 9 = click on right wheel
                DefaultConfig["ui.controller.middleMouseButton", 9] -> mouseButtonUp(window, Key.BUTTON_MIDDLE)
            }
        }
        if (key < MAX_NUM_INPUTS && id < MAX_NUM_CONTROLLERS) {
            ActionManager.onKeyTyped(window, Key.byId(baseKey + key))
        }
        isActiveMaybe = 1f
    }

    fun pollEvents(window: OSWindow, isFirst: Boolean): Boolean {

        val time = Engine.nanoTime
        val dt = clamp((time - lastTime) * 1e-9f, 1e-3f, 0.2f)
        lastTime = time
        isActiveMaybe *= (1f - dt)

        val isPresent = glfwJoystickPresent(glfwId)
        this.isFirst = isPresent && isFirst

        if (isConnected != isPresent) {
            LOGGER.info("Controller ${this.id} has been ${if (isPresent) "activated" else "deactivated"}")
            for (listener in onControllerChanged) {
                try {
                    listener(this)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            isConnected = isPresent
            if (isPresent) {
                resetState()
            }
        }
        if (isPresent) {

            ticksOnline = max(1, ticksOnline + 1)

            if (isGamepad) {

                // update other axes as well? mmh
                glfwGetGamepadState(glfwId, gamepadState)
                updateButtons(window, gamepadButtons)
                updateAxes(window, dt, gamepadAxes)
            } else {

                updateButtons(window)
                updateAxes(window, dt)
            }
        }

        return isPresent
    }

    private fun updateButtons(window: OSWindow, buttons: ByteBuffer? = glfwGetJoystickButtons(glfwId)) {
        if (buttons != null) {
            val time = Engine.gameTime
            numButtons = min(buttons.remaining(), MAX_NUM_BUTTONS)
            for (buttonId in 0 until numButtons) {
                val rawState = buttons.get()
                val state = rawState.toInt() // press or release, nothing else
                if (state == GLFW_PRESS) {
                    if (buttonDownTime[buttonId] == 0L) {
                        buttonDownTime[buttonId] = time
                        buttonDown(window, buttonId)
                        buttonType(window, buttonId)
                    }
                    val timeSinceDown = time - buttonDownTime[buttonId]
                    if (timeSinceDown > initialTypeDelayNanos) {
                        buttonDownTime[buttonId] = max(
                            buttonDownTime[buttonId] + typeDelayNanos, // reset
                            time - initialTypeDelayNanos - typeDelayNanos * 2 // & we must not collect too many,
                            // when the window is not active
                        )
                        buttonType(window, buttonId)
                    }
                } else {
                    if (buttonDownTime[buttonId] != 0L) {
                        buttonDownTime[buttonId] = 0L
                        buttonUp(window, buttonId)
                    }
                }
            }
        }
    }

    private fun updateAxes(window: OSWindow, dt: Float, axes: FloatBuffer? = glfwGetJoystickAxes(glfwId)) {
        if (axes != null) {

            val time = Engine.gameTime
            numAxes = min(axes.remaining(), MAX_NUM_AXES)
            for (axisId in 0 until numAxes) {

                val state = axes.get()

                val lastValue = axisValues[axisId]
                val value = calibration.getValue(state, axisId)
                rawAxisValues[axisId] = state
                axisValues[axisId] = value
                axisSpeeds[axisId] = (value - lastValue) / dt
                isActiveMaybe = clamp(isActiveMaybe + abs(lastValue - value) * 5f, 0f, 1f)

                // trigger for actions
                val wasDown = lastValue < -axisKeyTriggerPoint
                val wasUp = lastValue > +axisKeyTriggerPoint
                val isDown = value < -axisKeyTriggerPoint
                val isUp = value > axisKeyTriggerPoint
                val baseAxis = baseKey + numButtons
                if (isDown != wasDown) {
                    isActiveMaybe = 1f
                    if (numButtons + axisId * 2 < MAX_NUM_INPUTS && id < MAX_NUM_CONTROLLERS) {
                        val key = Key.byId(baseAxis + axisId * 2)
                        if (isDown) {
                            ActionManager.onKeyDown(window, key)
                        } else {
                            ActionManager.onKeyUp(window, key)
                        }
                    }
                }
                if (isUp != wasUp) {
                    isActiveMaybe = 1f
                    if (numButtons + axisId * 2 < MAX_NUM_INPUTS && id < MAX_NUM_CONTROLLERS) {
                        val key = Key.byId(baseAxis + axisId * 2 + 1)
                        if (isUp) {
                            ActionManager.onKeyDown(window, key)
                        } else {
                            ActionManager.onKeyUp(window, key)
                        }
                    }
                }
            }

            // support multiple mice (?); game specific; nice for strategy games
            if (isFirst && isActive && enableControllerInputs) {

                // if buttons = 0,1 (left wheel), then move the mouse
                val moveButtons = DefaultConfig["ui.controller.mouseMoveAxis0", 0]
                if (moveButtons in 0 until MAX_NUM_AXES - 1 &&
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
                    if (!isMouseTrapped) {
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

                // if buttons = 2,3 (right wheel), then scroll
                val scrollButtons = DefaultConfig["ui.controller.mouseWheelAxis0", 2]
                if (scrollButtons in 0 until MAX_NUM_AXES - 1 &&
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
                        // why -y? mmh...
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
                            GFXBase.robot?.mouseWheel(+mwf)
                            mouseWheelFract -= mwf
                        }
                    }
                }
            }
        }
    }

    companion object {

        private var lastMousePos = 0L
        private var mousePosX = 0f
        private var mousePosY = 0f
        private var mouseWheelFract = 0f

        var enableControllerInputs = true

        private var onControllerChanged = ArrayList<(Controller) -> Unit>()

        /**
         * use this function to get notified about controller changes
         * */
        fun addControllerChangeListener(listener: (Controller) -> Unit) {
            onControllerChanged.add(listener)
        }

        fun formatGuid(guid: String): String {
            var str = guid.trim()
            if (str.startsWith('0')) {
                val index = str.indexOfFirst { it != '0' }
                if (index < 0) return "0"
                str = "${index}x${str.substring(index)}"
            }
            if (str.endsWith('0')) {
                var index = str.indexOfLast { it != '0' }
                if (index < 0) index = 0
                str = "${str.substring(0, index + 1)}x${str.length - index - 1}"
            }
            return str
        }

        private fun getCaliFile(guid: String) =
            getReference(ConfigBasics.configFolder, "controller/${formatGuid(guid)}.json")

        fun loadCalibration(guid: String): ControllerCalibration? {
            val file = getCaliFile(guid)
            if (!file.exists || file.isDirectory) return null
            return TextReader.readFirstOrNull<ControllerCalibration>(file, StudioBase.workspace)
        }

        fun saveCalibration(guid: String, calibration: ControllerCalibration) {
            if (!calibration.isCalibrated) throw IllegalArgumentException(
                "You should not save a controller calibration, " +
                        "that has not actually been calibrated"
            )
            val file = getCaliFile(guid)
            file.getParent()?.tryMkdirs()
            TextWriter.save(calibration, file, StudioBase.workspace)
        }

        const val MAX_NUM_BUTTONS = 128
        const val MAX_NUM_AXES = 8

        /**
         * value of first enum
         * */
        val BASE_KEY = Key.CONTROLLER_0_KEY_0.id

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

        // should get a place in the config
        private val initialTypeDelayNanos get() = DefaultConfig["controller.initialTypeDelayMillis", 1000] * 1_000_000L
        private val typeDelayNanos get() = DefaultConfig["controller.typeDelayMillis", 100] * 1_000_000L

        // should probably get a place in the config as well
        private val axisKeyTriggerPoint get() = DefaultConfig["controller.axisKeyTriggerPoint", 0.25f]

        // private val minActivationPoint = 0.05f
        // private val maxActivationPoint = 0.95f
        private val LOGGER = LogManager.getLogger(Controller::class)
    }
}