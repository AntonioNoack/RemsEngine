package me.anno.input

import me.anno.Engine
import me.anno.maths.Maths.length
import me.anno.ui.utils.WindowStack
import me.anno.utils.bugs.SumOf
import org.joml.Vector3f
import kotlin.math.max

class Touch(var x: Float, var y: Float) {

    val t0 = Engine.gameTime
    val x0 = x
    val y0 = y
    var dx = 0f
    var dy = 0f
    var lastX = x
    var lastY = y
    var lastX2 = x
    var lastY2 = y

    fun update() {
        lastX2 = lastX
        lastY2 = lastY
        lastX = x
        lastY = y
    }

    fun update(x: Float, y: Float) {
        dx = x - this.x
        dy = y - this.y
        this.x = x
        this.y = y
    }

    interface TouchListener {
        fun onTouchDown(touch: Touch)
        fun onTouchUp(touch: Touch)
        fun onTouchMove(touch: Touch)
    }

    companion object {

        val touches = HashMap<Int, Touch>()

        fun getNumTouches() = touches.size
        fun getTouchIds() = touches.keys

        fun update() {
            for (touch in touches.values) {
                touch.update()
            }
        }

        fun getZoomFactor(): Float {
            return when (val size = touches.size) {
                0, 1 -> 1f
                else -> {
                    val cx = SumOf.sumOf(touches.values) { it.x } / size
                    val cy = SumOf.sumOf(touches.values) { it.y } / size
                    val clx = SumOf.sumOf(touches.values) { it.lastX } / size
                    val cly = SumOf.sumOf(touches.values) { it.lastY } / size
                    val d0 = SumOf.sumOf(touches.values) { length(it.x - cx, it.y - cy) } / size
                    val d1 = SumOf.sumOf(touches.values) { length(it.lastX - clx, it.lastY - cly) } / size
                    if (d0 > 0f && d1 > 0f)
                        d0 / d1
                    else 0f
                }
            }
        }

        fun sumDeltaX(): Float = SumOf.sumOf(touches.values) { it.x - it.lastX }
        fun sumDeltaY(): Float = SumOf.sumOf(touches.values) { it.y - it.lastY }

        fun avgDeltaX(): Float = SumOf.sumOf(touches.values) { it.x - it.lastX } / max(1, touches.size)
        fun avgDeltaY(): Float = SumOf.sumOf(touches.values) { it.y - it.lastY } / max(1, touches.size)

        fun getTouchPosition(ws: WindowStack, touchId: Int, dst: Vector3f = Vector3f()): Vector3f {
            val touch = touches[touchId] ?: return dst
            ws.viewTransform.transformProject(dst.set(touch.x, touch.y, 0f), dst)
            return dst
        }

        fun getLastTouchPosition(ws: WindowStack, touchId: Int, dst: Vector3f = Vector3f()): Vector3f {
            val touch = touches[touchId] ?: return dst
            ws.viewTransform.transformProject(dst.set(touch.lastX, touch.lastY, 0f), dst)
            return dst
        }

        fun getTouchDelta(ws: WindowStack, touchId: Int, dst: Vector3f = Vector3f()): Vector3f {
            val touch = touches[touchId] ?: return dst
            // transform delta properly?
            ws.viewTransform.transformDirection(dst.set(touch.dx, touch.dy, 0f), dst)
            return dst
        }

        private val listeners = ArrayList<TouchListener>()

        fun registerListener(listener: TouchListener) {
            listeners.add(listener)
        }

        fun unregisterListener(listener: TouchListener) {
            listeners.remove(listener)
        }

        fun onTouchDown(touchId: Int, x: Float, y: Float) {
            val touch = Touch(x, y)
            touches[touchId] = touch
            for (listener in listeners) {
                listener.onTouchDown(touch)
            }
        }

        fun onTouchMove(touchId: Int, x: Float, y: Float) {
            val touch = touches[touchId] ?: return
            touch.update(x, y)
            for (listener in listeners) {
                listener.onTouchMove(touch)
            }
        }

        fun onTouchUp(touchId: Int, x: Float, y: Float) {
            val touch = touches.remove(touchId) ?: return
            touch.update(x, y)
            for (listener in listeners) {
                listener.onTouchUp(touch)
            }
        }

    }

}