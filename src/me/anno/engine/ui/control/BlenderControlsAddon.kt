package me.anno.engine.ui.control

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.engine.ui.EditorState
import me.anno.input.Input.isShiftDown
import me.anno.parser.SimpleExpressionParser
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3d
import kotlin.math.pow

// todo test this
class BlenderControlsAddon {

    companion object {
        private val LOGGER = LogManager.getLogger(BlenderControlsAddon::class)
    }

    // todo preview changes

    enum class InputMode {
        NONE,
        MOVE,
        ROTATE,
        SCALE,
    }

    var mode = InputMode.NONE
    var inputString = ""
    var axisMask = 0 // 0 = free mode

    enum class LocalMode {
        LOCAL, GLOBAL, FREE
    }

    var local = LocalMode.LOCAL

    val transformStart = Vector2f()

    fun onCharTyped(x: Float, y: Float, key: Int): Boolean {
        when (val char = key.toChar()) {
            in '0'..'9', in "+-*/()" -> {
                if (mode != InputMode.NONE) {
                    inputString += char
                    // LOGGER.debug("New Input: $inputString")
                } else return false
            }
            'g', 's', 'r' -> {
                val newMode = when (char) {
                    'g' -> InputMode.MOVE
                    's' -> InputMode.SCALE
                    else -> InputMode.ROTATE
                }
                if (newMode == mode) {
                    local = when (local) {
                        LocalMode.LOCAL -> LocalMode.GLOBAL
                        LocalMode.GLOBAL -> LocalMode.FREE
                        LocalMode.FREE -> LocalMode.LOCAL
                    }
                } else {
                    if (mode == InputMode.NONE) {
                        // record mouse position for mouse-based transform
                        transformStart.set(x, y)
                    }
                    mode = newMode
                }
                LOGGER.debug("Changed Mode to: {}/{}", mode, local)
            }
            'x' -> axisMask = if (isShiftDown) 6 else 1
            'y' -> axisMask = if (isShiftDown) 5 else 2
            'z' -> axisMask = if (isShiftDown) 3 else 4
            else -> {
                // LOGGER.debug("Ignored key press $key/$char")
                return false
            }
        }
        return true
    }

    fun resetBlenderInput() {
        // reset blender input state
        mode = InputMode.NONE
        // reset space? let's do it..., can be changed later
        local = LocalMode.LOCAL
        axisMask = 7 // all
        inputString = ""
    }

    fun onEscapeKey(x: Float, y: Float): Boolean {
        return if (mode != InputMode.NONE) {
            resetBlenderInput()
            true
        } else false
    }

    fun onBackSpaceKey(x: Float, y: Float): Boolean {
        return if (mode != InputMode.NONE) {
            if (inputString.isNotEmpty()) {
                inputString = inputString.substring(0, inputString.length - 1)
            }
            true
        } else false
    }

    fun onEnterKey(x: Float, y: Float): Boolean {
        return if (mode != InputMode.NONE) {
            // analyse expression
            // collect all, where the transform needs to be applied to
            for (child in EditorState.selection) {// probably needs sorting
                when (child) {
                    is Entity -> {
                        transform(child.transform, x, y, false)
                        child.invalidateChildTransforms()
                        child.transform.teleportUpdate()
                    }
                    is BlenderCATransformable -> child.transform(this, x, y, false)
                }
            }
            resetBlenderInput()
            true
        } else false
    }


    fun transform(transform: Transform, vec: Vector3d) {
        when (local) {
            LocalMode.LOCAL -> {
                when (mode) {
                    InputMode.MOVE -> transform.localPosition = transform.localPosition
                        .add(vec)
                    InputMode.ROTATE -> transform.localRotation = transform.localRotation
                        .rotateYXZ(vec.y.toFloat(), vec.x.toFloat(), vec.z.toFloat())
                    InputMode.SCALE -> transform.localScale = transform.localScale
                        .mul(vec.x.toFloat(), vec.y.toFloat(), vec.z.toFloat())
                    InputMode.NONE -> {}
                }
            }
            LocalMode.GLOBAL -> {
                when (mode) {
                    InputMode.MOVE -> transform.globalPosition = transform.globalPosition.add(vec)
                    InputMode.ROTATE -> transform.globalRotation = transform.globalRotation
                        .rotateYXZ(vec.y.toFloat(), vec.x.toFloat(), vec.z.toFloat())
                    InputMode.SCALE -> transform.globalScale = transform.globalScale
                        .mul(vec.x.toFloat(), vec.y.toFloat(), vec.z.toFloat())
                    InputMode.NONE -> {}
                }
            }
            LocalMode.FREE -> {
                // todo ... idk... screen space?
                LOGGER.debug("todo: screen space transforms not yet implemented")
            }
        }
    }

    fun preTransform(value: Double?, x: Float, y: Float, vec: Vector3d) {
        // default value
        vec.set(if (mode == InputMode.SCALE) 1.0 else 0.0)
        if (value != null) {
            if (axisMask.and(1) != 0) vec.set(value)
            if (axisMask.and(2) != 0) vec.set(value)
            if (axisMask.and(4) != 0) vec.set(value)
        }
        if (axisMask == 0 || value == null) {
            // todo use mouse delta for transform
            // todo transform rotation/movement/scale into vec...
            when (mode) {
                InputMode.SCALE -> {
                    val dx = x - transformStart.x
                    vec.set(1.01.pow(dx.toDouble()))
                }
                else -> {
                    LOGGER.debug("todo: transform mouse delta into movement / scale")
                }
            }
        }
    }

    fun transform(transform: Transform?, x: Float, y: Float, reset: Boolean) {
        val mode = mode
        val value = SimpleExpressionParser.parseDouble(inputString)
        if (inputString.isBlank2() || value != null) {
            val vec = JomlPools.vec3d.create()
            preTransform(value, x, y, vec)
            if (transform != null) transform(transform, vec)
            LOGGER.info("todo: apply transform $mode x $vec")
            JomlPools.vec3d.sub(1)
            if (reset) resetBlenderInput()
        }
    }
}