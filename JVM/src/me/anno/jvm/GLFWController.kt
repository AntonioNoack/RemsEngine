package me.anno.jvm

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX
import me.anno.gpu.GFXBase
import me.anno.gpu.OSWindow
import me.anno.input.ButtonLogic
import me.anno.input.controller.CalibrationProcedure
import me.anno.input.controller.Controller
import me.anno.input.controller.ControllerCalibration
import me.anno.input.controller.ControllerCalibration.Companion.loadCalibration
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.min
import me.anno.ui.base.menu.Menu
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Booleans.hasFlag
import org.apache.logging.log4j.LogManager
import org.lwjgl.glfw.GLFW.GLFW_JOYSTICK_1
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.glfwGetGamepadState
import org.lwjgl.glfw.GLFW.glfwGetJoystickAxes
import org.lwjgl.glfw.GLFW.glfwGetJoystickButtons
import org.lwjgl.glfw.GLFW.glfwGetJoystickGUID
import org.lwjgl.glfw.GLFW.glfwGetJoystickName
import org.lwjgl.glfw.GLFW.glfwJoystickIsGamepad
import org.lwjgl.glfw.GLFW.glfwJoystickPresent
import org.lwjgl.glfw.GLFWGamepadState
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.math.abs

// todo rumble using external library? glfw and lwjgl sadly have no support for it
// https://github.com/williamahartman/Jamepad?
class GLFWController(private val glfwId: Int) : Controller(MAX_NUM_BUTTONS, MAX_NUM_AXES) {

    override var numButtons = 0
        private set
    override var numAxes = 0
        private set

    /**
     * when the controller is a gamepad, then we can use the standard controls with 6 axes;
     * idk what happens, when the controller has more than that... should we support that somehow?
     * */
    var isGamepad = false

    private val gamepadState = GLFWGamepadState.calloc()
    private val gamepadAxes get() = gamepadState.axes()
    private val gamepadButtons get() = gamepadState.buttons()

    private var lastTime = 0L

    private fun onActivate() {
        name = glfwGetJoystickName(glfwId) ?: "Controller[$id]"
        guid = glfwGetJoystickGUID(glfwId) ?: name
        isGamepad = glfwJoystickIsGamepad(glfwId)
        LOGGER.info("Connected to controller '$name', id '$guid', gamepad? $isGamepad")
        calibration = loadCalibration(guid) ?: ControllerCalibration(isGamepad)
        if (!calibration.isCalibrated) {
            LOGGER.warn("No calibration was found for controller '$name', guid '$guid'")
            Menu.ask(GFX.someWindow.windowStack, NameDesc("Calibrate new controller '$name'?")) {
                CalibrationProcedure.start(GFX.someWindow, style)
            }
        }
        buttonDownTime.fill(0)
        axisValues.fill(0f)
        numButtons = 0
        numAxes = 0
        lastTime = Time.nanoTime
    }

    private var lastAskedPresent = -SECONDS_TO_NANOS
    fun pollEvents(window: OSWindow): Boolean {

        val time = Time.nanoTime
        val dt = clamp((time - lastTime) * 1e-9f, 1e-3f, 0.2f)
        lastTime = time

        val isPresent =
            if (abs(time - lastAskedPresent) < 1e9 / 3) isConnected
            else {
                lastAskedPresent = time
                glfwJoystickPresent(glfwId)
            }

        if (isConnected != isPresent) {
            isConnected = isPresent
            if (isPresent) {
                onActivate()
            }
        }

        if (isPresent) {
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
            val time = Time.nanoTime
            numButtons = min(buttons.remaining(), MAX_NUM_BUTTONS)
            for (buttonId in 0 until numButtons) {
                val rawState = buttons.get(buttonId)
                val state = rawState.toInt() // press or release, nothing else
                val eventFlags = ButtonLogic.process(time, state == GLFW_PRESS, buttonDownTime, buttonId)
                if (eventFlags.hasFlag(ButtonLogic.DOWN)) buttonDown(window, buttonId)
                if (eventFlags.hasFlag(ButtonLogic.TYPE)) buttonType(window, buttonId)
                if (eventFlags.hasFlag(ButtonLogic.UP)) buttonUp(window, buttonId)
            }
        }
    }

    private fun updateAxes(window: OSWindow, dt: Float, axes: FloatBuffer? = glfwGetJoystickAxes(glfwId)) {
        if (axes != null) {

            numAxes = min(axes.remaining(), MAX_NUM_AXES)
            for (axisId in 0 until numAxes) {
                val state = axes.get()
                setAxisValue(window, axisId, state, dt)
            }

            // support multiple mice (?); game specific; nice for strategy games
            if (isFirst && enableControllerInputs) {
                updateMouseMovement(window, dt)
                updateMouseScroll(window, dt)
            }
        }
    }

    companion object {

        const val MAX_NUM_BUTTONS = 128
        const val MAX_NUM_AXES = 8

        val glfwControllers = createArrayList(15) {
            GLFWController(GLFW_JOYSTICK_1 + it)
        }

        fun pollControllers(window: OSWindow) {
            // GLFW controllers need to be pulled constantly
            synchronized(GFXBase.glfwLock) {
                for (index in glfwControllers.indices) {
                    glfwControllers[index].pollEvents(window)
                }
            }
        }

        private val LOGGER = LogManager.getLogger(GLFWController::class)
    }
}