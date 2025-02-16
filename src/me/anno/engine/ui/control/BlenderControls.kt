package me.anno.engine.ui.control

import me.anno.ecs.Transform
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.drawing.DrawTexts
import me.anno.input.Input.isShiftDown
import me.anno.input.Key
import me.anno.maths.Maths.length
import me.anno.parser.SimpleExpressionParser
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.atan2
import kotlin.math.ln
import kotlin.math.pow

/**
 * blender like controls
 * issue: WASD was used for moving in the scene, and that's incompatible with the 's' key
 * temporarily, because moving around in Blender is tedious (not good), we rename 's' to 'b'
 * also we should no longer show the ids, when pressing 'g'
 * in general these things should be placed in menus XD
 * */
class BlenderControls(view: RenderView) : ControlScheme(view) {

    // todo make selected axes thicker

    var mode = Mode.NOTHING

    var axis = 0

    var deltaX = 0f
    var deltaY = 0f

    var isLocking = false

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {

        DrawTexts.drawSimpleTextCharByChar(x, y, 2, number)
        DrawTexts.drawSimpleTextCharByChar(x, y + 20, 2, mode.name)
    }

    fun getAxis(): Vector3f {
        return when (axis) {
            1 -> if (isShiftDown) {
                Vector3f(0f, 1f, 1f)
            } else {
                Vector3f(1f, 0f, 0f)
            }
            2 -> if (isShiftDown) {
                Vector3f(1f, 0f, 1f)
            } else {
                Vector3f(0f, 1f, 0f)
            }
            4 -> if (isShiftDown) {
                Vector3f(1f, 1f, 0f)
            } else {
                Vector3f(0f, 0f, 1f)
            }
            else -> camera.transform!!.globalTransform
                .transformDirection(Vector3f(1f, 0f, 0f)).normalize()
        }
    }

    fun accept() {
        showChange()
        old = emptyList()
        reset()
    }

    fun cancel() {
        reset()
    }

    fun showChange() {

        if (mode == Mode.NOTHING) return

        // parent * local = global
        //  - get global
        //  - apply transform
        //  - set global
        //  - calculate local

        // apply the change, which was made by mouse or by entering a number
        val numberValue = SimpleExpressionParser.parseDouble(number)
        if (numberValue != null) {

            if (!numberValue.isFinite()) return

            // use the number
            val axis = getAxis()
            when (mode) {
                Mode.SCALING -> {
                    val v = ln(numberValue.toFloat()) / ln(2f)
                    if (this.axis == 0) {
                        // scale uniformly
                        axis.set(v)
                    } else {
                        axis.x = v.pow(axis.x)
                        axis.y = v.pow(axis.y)
                        axis.z = v.pow(axis.z)
                    }
                }
                else -> {
                    axis.mul(numberValue.toFloat())
                }
            }
            applyTransform(axis)
        } else {

            val direction = camera.transform!!.globalTransform
                .transformDirection(Vector3f(deltaX, deltaY, 0f))

            if (mode == Mode.SCALING) {
                direction.safeNormalize(length(deltaX, deltaY) / height)
            }

            when (axis) {
                0 -> {
                    if (mode == Mode.SCALING) {
                        direction.set(direction.x + direction.y + direction.z)
                    }
                }
                1 -> {
                    if (isLocking) {
                        direction.x = 0f
                    } else {
                        direction.y = 0f
                        direction.z = 0f
                    }
                }
                2 -> {
                    if (isLocking) {
                        direction.y = 0f
                    } else {
                        direction.x = 0f
                        direction.z = 0f
                    }
                }
                4 -> {
                    if (isLocking) {
                        direction.z = 0f
                    } else {
                        direction.x = 0f
                        direction.y = 0f
                    }
                }
            }

            applyTransform(direction)
        }
    }

    fun applyTransform(direction: Vector3f) {
        if (direction.dot(1.0, 1.0, 1.0).isNaN()) throw RuntimeException("$direction")
        when (mode) {
            Mode.TRANSLATING -> {
                applyTransform { selfGlobal, distance ->
                    selfGlobal.translate(
                        direction.x / distance,
                        direction.y / distance,
                        direction.z / distance
                    )
                }
            }
            Mode.ROTATING -> {
                val angle = atan2(deltaY, deltaX)
                applyTransform { selfGlobal, _ ->
                    selfGlobal.rotate(angle, direction.normalize())
                }
            }
            Mode.SCALING -> {
                applyTransform { selfGlobal, _ ->
                    selfGlobal.scale(
                        2f.pow(direction.x),
                        2f.pow(direction.y),
                        2f.pow(direction.z),
                    )
                }
            }
            else -> {
            }
        }
    }

    fun applyTransform(transformFunction: (Matrix4x3, Double) -> Unit) {
        for ((index, entity) in selectedEntities.withIndex()) {
            val base = old.getOrNull(index) ?: break // todo I feel like old is lost...
            val transform = entity.transform
            val parentTransform = entity.parentEntity?.transform
            val parentGlobal = parentTransform?.globalTransform?.run { Matrix4x3(this) } ?: Matrix4x3()
            transform.checkTransform(parentGlobal)
            val selfGlobal = parentGlobal.mul(base)
            transform.checkTransform(selfGlobal)
            val distance = selfGlobal.transformPosition(Vector3d()).distance(camera.transform!!.localPosition)
            transformFunction(selfGlobal, distance)
            transform.checkTransform(selfGlobal)
            transform.globalTransform.set(selfGlobal)
            transform.setStateAndUpdate(Transform.State.VALID_GLOBAL)
        }
    }

    var number = ""

    var old: List<Matrix4x3> = emptyList()

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        if (mode != Mode.NOTHING) {
            when (button) {
                Key.BUTTON_LEFT -> accept()
                Key.BUTTON_RIGHT -> cancel()
                else -> super.onMouseClicked(x, y, button, long)
            }
        } else super.onMouseClicked(x, y, button, long)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        // camera turning and such, default
        if (mode == Mode.NOTHING) {
            super.onMouseMoved(x, y, dx, dy)
        } else {
            deltaX += dx
            deltaY -= dy
            showChange()
        }
    }

    override fun onEnterKey(x: Float, y: Float) {
        accept()
    }

    override fun onEscapeKey(x: Float, y: Float) {
        if (mode != Mode.NOTHING) {
            cancel()
        } else super.onEscapeKey(x, y)
    }

    fun reset() {
        axis = 0
        number = ""
        deltaX = 0f
        deltaY = 0f
        // set the old transforms
        for ((index, transform) in selectedTransforms.withIndex()) {
            if (index >= old.size) break
            transform.setLocal(old[index])
            transform.teleportUpdate()
        }
        old = emptyList()
        mode = Mode.NOTHING
    }

    override fun onCharTyped(x: Float, y: Float, codepoint: Int) {
        when (codepoint.toChar()) {
            'x' -> {
                axis = 1
                isLocking = isShiftDown
                showChange()
            }
            'y' -> {
                axis = 2
                isLocking = isShiftDown
                showChange()
            }
            'z' -> {
                axis = 4
                isLocking = isShiftDown
                showChange()
            }
            'b' -> {
                if (mode == Mode.NOTHING) reset()
                mode = Mode.TRANSLATING
                old = selectedLocalTransforms()
                showChange()
            }
            'n' -> {
                if (mode == Mode.NOTHING) reset()
                mode = Mode.ROTATING
                old = selectedLocalTransforms()
                showChange()
            }
            'm' -> {
                if (mode == Mode.NOTHING) reset()
                mode = Mode.SCALING
                old = selectedLocalTransforms()
                showChange()
            }
            'h' -> {
                // todo these properties need to be propagated to the prefab as well
                val isShiftDown = isShiftDown
                for (it in selectedEntities) {
                    it.isEnabled = isShiftDown
                }
                showChange()
            }
            // todo instead of supporting expressions, just make - switch the sign?
            in '0'..'9', '.', '*', '/', '(', ')', '+', '-' -> {
                number += codepoint.toChar()
                showChange()
            }
        }
    }

    private fun selectedLocalTransforms(): List<Matrix4x3> {
        return selectedTransforms.map { it.getLocalTransform(Matrix4x3()) }
    }

    override fun onBackSpaceKey(x: Float, y: Float) {
        if (number.isNotEmpty()) {
            number = number.substring(0, number.length - 1)
        }
    }
}