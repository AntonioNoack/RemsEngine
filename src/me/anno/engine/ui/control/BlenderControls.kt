package me.anno.engine.ui.control

import me.anno.Engine
import me.anno.ecs.Transform
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.drawing.DrawTexts
import me.anno.input.Input.isShiftDown
import me.anno.input.MouseButton
import me.anno.maths.Maths.length
import me.anno.parser.SimpleExpressionParser
import me.anno.utils.types.Vectors.safeNormalize
import org.joml.Matrix4x3d
import org.joml.Vector3d
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

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        DrawTexts.drawSimpleTextCharByChar(x, y, 2, number)
        DrawTexts.drawSimpleTextCharByChar(x, y + 20, 2, mode.name)

    }

    fun getAxis(): Vector3d {
        return when (axis) {
            1 -> if (isShiftDown) {
                Vector3d(0.0, 1.0, 1.0)
            } else {
                Vector3d(1.0, 0.0, 0.0)
            }
            2 -> if (isShiftDown) {
                Vector3d(1.0, 0.0, 1.0)
            } else {
                Vector3d(0.0, 1.0, 0.0)
            }
            4 -> if (isShiftDown) {
                Vector3d(1.0, 1.0, 0.0)
            } else {
                Vector3d(0.0, 0.0, 1.0)
            }
            else -> cameraNode.transform.globalTransform.transformDirection(Vector3d(1.0, 0.0, 0.0)).normalize()
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
                    val v = ln(numberValue) / ln(2.0)
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
                    axis.mul(numberValue)
                }
            }
            applyTransform(axis)

        } else {

            val direction = cameraNode.transform.globalTransform.transformDirection(
                Vector3d(
                    deltaX.toDouble(),
                    deltaY.toDouble(),
                    0.0
                )
            )

            if (mode == Mode.SCALING) {
                direction.safeNormalize(length(deltaX, deltaY).toDouble() / h)
            }

            when (axis) {
                0 -> {
                    if (mode == Mode.SCALING) {
                        direction.set(direction.x + direction.y + direction.z)
                    }
                }
                1 -> {
                    if (isLocking) {
                        direction.x = 0.0
                    } else {
                        direction.y = 0.0
                        direction.z = 0.0
                    }
                }
                2 -> {
                    if (isLocking) {
                        direction.y = 0.0
                    } else {
                        direction.x = 0.0
                        direction.z = 0.0
                    }
                }
                4 -> {
                    if (isLocking) {
                        direction.z = 0.0
                    } else {
                        direction.x = 0.0
                        direction.y = 0.0
                    }
                }
            }

            applyTransform(direction)

        }
    }

    fun applyTransform(direction: Vector3d) {
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
                val angle = atan2(deltaY, deltaX).toDouble()
                applyTransform { selfGlobal, _ ->
                    selfGlobal.rotate(angle, direction.normalize())
                }
            }
            Mode.SCALING -> {
                applyTransform { selfGlobal, _ ->
                    selfGlobal.scale(
                        2.0.pow(direction.x),
                        2.0.pow(direction.y),
                        2.0.pow(direction.z),
                    )
                }
            }
            else -> {
            }
        }
    }

    fun applyTransform(transformFunction: (Matrix4x3d, Double) -> Unit) {
        for ((index, entity) in selectedEntities.withIndex()) {
            val base = old.getOrNull(index) ?: break // todo I feel like old is lost...
            val transform = entity.transform
            val parentTransform = entity.parentEntity?.transform
            val parentGlobal = parentTransform?.globalTransform?.run { Matrix4x3d(this) } ?: Matrix4x3d()
            transform.checkTransform(parentGlobal)
            val selfGlobal = parentGlobal.mul(base)
            transform.checkTransform(selfGlobal)
            val distance = selfGlobal.transformPosition(Vector3d()).distance(cameraNode.position)
            transformFunction(selfGlobal, distance)
            transform.checkTransform(selfGlobal)
            transform.globalTransform.set(selfGlobal)
            transform.setStateAndUpdate(Transform.State.VALID_GLOBAL)
        }
    }

    var number = ""

    var old: List<Matrix4x3d> = emptyList()

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        if (mode != Mode.NOTHING) {
            when {
                button.isLeft -> accept()
                button.isRight -> cancel()
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
            transform.teleportUpdate(Engine.gameTime)
        }
        old = emptyList()
        mode = Mode.NOTHING
    }

    override fun onCharTyped(x: Float, y: Float, key: Int) {
        when (key.toChar()) {
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
                old = selectedTransforms
                    .map { Matrix4x3d(it.localTransform) }
                showChange()
            }
            'n' -> {
                if (mode == Mode.NOTHING) reset()
                mode = Mode.ROTATING
                old = selectedTransforms
                    .map { Matrix4x3d(it.localTransform) }
                showChange()
            }
            'm' -> {
                if (mode == Mode.NOTHING) reset()
                mode = Mode.SCALING
                old = selectedTransforms
                    .map { Matrix4x3d(it.localTransform) }
                showChange()
            }
            'h' -> {
                // todo these properties need to be propagated to the prefab as well
                if (isShiftDown) {
                    selectedEntities.forEach {
                        it.isEnabled = true
                    }
                } else {
                    selectedEntities.forEach {
                        it.isEnabled = false
                    }
                }
                showChange()
            }
            // todo instead of supporting expressions, just make - switch the sign?
            in '0'..'9', '.', '*', '/', '(', ')', '+', '-' -> {
                number += key.toChar()
                showChange()
            }
        }
    }

    override fun onBackSpaceKey(x: Float, y: Float) {
        if (number.isNotEmpty()) {
            number = number.substring(0, number.length - 1)
        }
    }

}