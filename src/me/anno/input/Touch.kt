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

        @JvmField
        val touches = HashMap<Int, Touch>()

        @JvmStatic
        fun updateAll() {
            for (touch in touches.values) {
                touch.update()
            }
        }

        @JvmStatic
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

        @JvmStatic
        fun sumDeltaX(): Float = SumOf.sumOf(touches.values) { it.x - it.lastX }

        @JvmStatic
        fun sumDeltaY(): Float = SumOf.sumOf(touches.values) { it.y - it.lastY }

        @JvmStatic
        fun avgDeltaX(): Float = SumOf.sumOf(touches.values) { it.x - it.lastX } / max(1, touches.size)

        @JvmStatic
        fun avgDeltaY(): Float = SumOf.sumOf(touches.values) { it.y - it.lastY } / max(1, touches.size)

        @JvmStatic
        fun getTouchPosition(ws: WindowStack, touchId: Int, dst: Vector3f = Vector3f()): Vector3f {
            val touch = touches[touchId] ?: return dst
            ws.viewTransform.transformProject(dst.set(touch.x, touch.y, 0f), dst)
            return dst
        }

        @JvmStatic
        fun getLastTouchPosition(ws: WindowStack, touchId: Int, dst: Vector3f = Vector3f()): Vector3f {
            val touch = touches[touchId] ?: return dst
            ws.viewTransform.transformProject(dst.set(touch.lastX, touch.lastY, 0f), dst)
            return dst
        }

        @JvmStatic
        fun getTouchDelta(ws: WindowStack, touchId: Int, dst: Vector3f = Vector3f()): Vector3f {
            val touch = touches[touchId] ?: return dst
            // transform delta properly?
            ws.viewTransform.transformDirection(dst.set(touch.dx, touch.dy, 0f), dst)
            return dst
        }

        @JvmStatic
        private val listeners = ArrayList<TouchListener>()

        @JvmStatic
        fun registerListener(listener: TouchListener) {
            listeners.add(listener)
        }

        @JvmStatic
        fun unregisterListener(listener: TouchListener) {
            listeners.remove(listener)
        }

        @JvmStatic
        fun onTouchDown(touchId: Int, x: Float, y: Float) {
            val touch = Touch(x, y)
            touches[touchId] = touch
            for (listener in listeners) {
                listener.onTouchDown(touch)
            }
        }

        @JvmStatic
        fun onTouchMove(touchId: Int, x: Float, y: Float) {
            val touch = touches[touchId] ?: return
            touch.update(x, y)
            for (listener in listeners) {
                listener.onTouchMove(touch)
            }
        }

        @JvmStatic
        fun onTouchUp(touchId: Int, x: Float, y: Float) {
            val touch = touches.remove(touchId) ?: return
            touch.update(x, y)
            for (listener in listeners) {
                listener.onTouchUp(touch)
            }
        }

    }

}