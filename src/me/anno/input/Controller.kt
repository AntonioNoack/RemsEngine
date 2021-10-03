package me.anno.input

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.maths.Maths.min
import me.anno.utils.maths.Maths.sq
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
class Controller(val id: Int) {

    private val glfwId = GLFW_JOYSTICK_1 + id

    var isConnected = false
    val baseKey = BASE_KEY * (id + 1)
    val baseAxis = baseKey + (BASE_AXIS - BASE_KEY)

    private val buttonDownTime = LongArray(MAX_NUM_BUTTONS)

    private var ticksOnline = 0

    private val axisValues = FloatArray(MAX_NUM_AXES)
    private val axisSpeeds = FloatArray(MAX_NUM_AXES)

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
     * 0,1 = x,y of left wheel,
     * 2,3 = x,y of right wheel,
     * 4,5 = left,right trigger
     * */
    fun getAxis(axis: Int): Float {
        return if (axis in 0 until numAxes) axisValues[axis] else 0f
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
            // todo controller calibration procedure
            LOGGER.warn("TODO: Request a calibration from the user for controller '$name', '$guid'")
        }
        buttonDownTime.fill(0)
        axisValues.fill(0f)
        ticksOnline = 0
        numButtons = 0
        numAxes = 0
        isActiveMaybe = 0f
        lastTime = GFX.gameTime
    }

    private fun isMouseInWindow(): Boolean {
        return mousePosX in 0f..GFX.width - 1f && mousePosY in 0f..GFX.height - 1f
    }

    private fun mouseButtonDown(key: Int) {
        if (isMouseInWindow() && GFX.isInFocus) {
            Input.onMousePress(key)
        } else {
            GFX.robot.mousePress(
                when (key) {
                    0 -> InputEvent.BUTTON1_MASK
                    1 -> InputEvent.BUTTON2_MASK
                    else -> InputEvent.BUTTON3_MASK
                }
            )
        }
    }

    private fun mouseButtonUp(key: Int){
        if (isMouseInWindow() && GFX.isInFocus) {
            Input.onMouseRelease(key)
        } else {
            GFX.robot.mouseRelease(when (key) {
                0 -> InputEvent.BUTTON1_MASK
                1 -> InputEvent.BUTTON2_MASK
                else -> InputEvent.BUTTON3_MASK
            })
        }
    }

    private fun buttonDown(key: Int) {
        if (isFirst) {
            when (key) {
                DefaultConfig["ui.controller.leftMouseButton", 0] -> mouseButtonDown(0)
                DefaultConfig["ui.controller.rightMouseButton", 1] -> mouseButtonDown(1)
                // 9 = click on right wheel
                DefaultConfig["ui.controller.middleMouseButton", 9] -> mouseButtonDown(2)
            }
        }
        ActionManager.onKeyDown(baseKey + key)
        isActiveMaybe = 1f
    }

    private fun buttonType(key: Int) {
        ActionManager.onKeyTyped(baseKey + key)
        isActiveMaybe = 1f
    }

    private fun buttonUp(key: Int) {
        if (isFirst) {
            when (key) {
                DefaultConfig["ui.controller.leftMouseButton", 0] -> mouseButtonUp(0)
                DefaultConfig["ui.controller.rightMouseButton", 1] -> mouseButtonUp(1)
                // 9 = click on right wheel
                DefaultConfig["ui.controller.middleMouseButton", 9] -> mouseButtonUp(2)
            }
        }
        ActionManager.onKeyTyped(baseKey + key)
        isActiveMaybe = 1f
    }

    fun pollEvents(isFirst: Boolean): Boolean {

        val time = GFX.gameTime
        val dt = clamp((time - lastTime) * 1e-9f, 1e-3f, 0.2f)
        lastTime = time
        isActiveMaybe *= (1f - dt)

        val isPresent = glfwJoystickPresent(glfwId)
        this.isFirst = isPresent && isFirst

        if (isConnected != isPresent) {
            // todo show that the controller has been enabled / deactivated
            LOGGER.info("Controller ${this.id} has been ${if (isPresent) "activated" else "deactivated"}")
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
                updateButtons(gamepadButtons)
                updateAxes(dt, gamepadAxes)

            } else {

                updateButtons()
                updateAxes(dt)

            }
        }

        return isPresent

    }

    private fun updateButtons(buttons: ByteBuffer? = glfwGetJoystickButtons(glfwId)) {
        if (buttons != null) {
            val time = GFX.gameTime
            numButtons = min(buttons.remaining(), MAX_NUM_BUTTONS)
            for (buttonId in 0 until numButtons) {
                val rawState = buttons.get()
                val state = rawState.toInt() // press or release, nothing else
                if (state == GLFW_PRESS) {
                    if (buttonDownTime[buttonId] == 0L) {
                        buttonDownTime[buttonId] = time
                        buttonDown(buttonId)
                        buttonType(buttonId)
                    }
                    val timeSinceDown = time - buttonDownTime[buttonId]
                    if (timeSinceDown > initialTypeDelay) {
                        buttonDownTime[buttonId] = max(
                            buttonDownTime[buttonId] + typeDelay, // reset
                            time - initialTypeDelay - typeDelay * 2 // & we must not collect too many,
                            // when the window is not active
                        )
                        buttonType(buttonId)
                    }
                } else {
                    if (buttonDownTime[buttonId] != 0L) {
                        buttonDownTime[buttonId] = 0L
                        buttonUp(buttonId)
                    }
                }
            }
        }
    }

    private fun updateAxes(dt: Float, axes: FloatBuffer? = glfwGetJoystickAxes(glfwId)) {
        if (axes != null) {

            val time = GFX.gameTime
            numAxes = min(axes.remaining(), MAX_NUM_AXES)
            for (axisId in 0 until numAxes) {

                val state = axes.get()

                val lastValue = axisValues[axisId]
                val value = calibration.getValue(state, axisId)
                axisValues[axisId] = value
                axisSpeeds[axisId] = (value - lastValue) / dt
                isActiveMaybe = clamp(isActiveMaybe + abs(lastValue - value) * 5f, 0f, 1f)

                // trigger for actions
                val wasDown = lastValue < -axisKeyTriggerPoint
                val wasUp = lastValue > +axisKeyTriggerPoint
                val isDown = value < -axisKeyTriggerPoint
                val isUp = value > axisKeyTriggerPoint
                if (isDown != wasDown) {
                    isActiveMaybe = 1f
                    if (isDown) {
                        ActionManager.onKeyDown(baseAxis + axisId * 2)
                    } else {
                        ActionManager.onKeyUp(baseAxis + axisId * 2)
                    }
                }
                if (isUp != wasUp) {
                    isActiveMaybe = 1f
                    if (isUp) {
                        ActionManager.onKeyDown(baseAxis + axisId * 2 + 1)
                    } else {
                        ActionManager.onKeyUp(baseAxis + axisId * 2 + 1)
                    }
                }

            }

            // support multiple mice (?); game specific; nice for strategy games
            if (isFirst && isActive) {

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

                    ActionManager.onMouseMoved(dx, dy)
                    if (!GFX.isMouseTrapped) {
                        // only works well, if we have a single player
                        // it we have a mouse user and a controller user, the controller user will win here ...
                        // todo grand theft waifu should be playable on keyboard/mouse + controller at the same time <3
                        // reset the mouse position, if we used the original mouse again
                        // todo update mouseX/Y outside the main window, and then remove this condition
                        // don't reset, if we're outside the window, because Input.mouseX/Y is not updated outside
                        if (abs(time - lastMousePos) > 1e9 && isMouseInWindow()) {// 1s delay to switch back to mouse
                            mousePosX = Input.mouseX
                            mousePosY = Input.mouseY
                        }
                        lastMousePos = time
                        mousePosX += dx
                        mousePosY += dy
                        GFX.moveMouseTo(mousePosX, mousePosY)
                    }
                }

                // if buttons = 2,3 (right wheel), then scroll
                val scrollButtons = DefaultConfig["ui.controller.mouseWheelAxis0", 2]
                if (scrollButtons in 0 until MAX_NUM_AXES - 1 &&
                    (axisValues[scrollButtons] != 0f || axisValues[scrollButtons + 1] != 0f)
                ) {

                    var dx = axisValues[scrollButtons]
                    var dy = axisValues[scrollButtons + 1]

                    // non linear curve for better control, and fast-scrolling
                    val speed = DefaultConfig["ui.controller.mouseWheelSpeed", 50f] * dt
                    if (DefaultConfig["ui.controller.mouseWheelEnableAcceleration", true]) {
                        val f = sq(dx, dy)
                        dx *= f
                        dy *= f
                    }

                    dx *= speed
                    dy *= speed

                    if (GFX.isInFocus && isMouseInWindow()) {
                        // why -y? mmh...
                        if (DefaultConfig["ui.controller.mouseWheelIsSingleAxis", false]) {
                            Input.onMouseWheel(0f, -dy, true)
                        } else {
                            Input.onMouseWheel(dx, -dy, false)
                        }
                        mouseWheelFract = 0f
                    } else {
                        mouseWheelFract += dy
                        val mwf = mouseWheelFract.toInt() // round towards zero
                        if (mwf != 0) {
                            GFX.robot.mouseWheel(+mwf)
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

        fun loadCalibration(guid: String): ControllerCalibration? {
            val file = getReference(ConfigBasics.configFolder, "controller/${formatGuid(guid)}.json")
            if (!file.exists || file.isDirectory) return null
            return TextReader.read(file).firstOrNull() as? ControllerCalibration
        }

        fun saveCalibration(guid: String, calibration: ControllerCalibration) {
            if (!calibration.isCalibrated) throw IllegalArgumentException(
                "You should not save a controller calibration, " +
                        "that has not actually been calibrated"
            )
            val file = getReference(ConfigBasics.configFolder, "controller/${formatGuid(guid)}.json")
            file.getParent()?.mkdirs()
            TextWriter.save(calibration, file)
        }

        // first controller: 1000,1200;
        // second controller: 2000,2200;
        // ...
        const val BASE_KEY = 1000
        const val BASE_AXIS = 1200

        const val MAX_NUM_BUTTONS = 32
        const val MAX_NUM_AXES = 32

        // todo make these configurable
        // should get a place in the config
        private val initialTypeDelay = 1_000_000_000L
        private val typeDelay = 100_000_000L

        // should probably get a place in the config as well
        private val axisKeyTriggerPoint = 0.25f

        // private val minActivationPoint = 0.05f
        // private val maxActivationPoint = 0.95f
        private val LOGGER = LogManager.getLogger(Controller::class)

    }
}