package me.anno.input

import me.anno.Time
import me.anno.maths.Maths.sq
import me.anno.ui.WindowStack
import org.joml.Vector3f
import speiger.primitivecollections.IntToObjectHashMap
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Stores the current state of which touches are present, where, and for how long;
 * useful for Android, Web on phone, and touch-enabled laptops
 * */
class Touch(var x: Float, var y: Float) {

    val t0 = Time.nanoTime
    val x0 = x
    val y0 = y

    var dx = 0f
    var dy = 0f
    var lastX = x
    var lastY = y
    var lastX2 = x
    var lastY2 = y

    fun update() {
        dx = x - lastX
        dy = y - lastY
        lastX2 = lastX
        lastY2 = lastY
        lastX = x
        lastY = y
    }

    fun update(x: Float, y: Float) {
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
        val touches = IntToObjectHashMap<Touch>()

        @JvmStatic
        fun updateAll() {
            touches.forEach { _, touch ->
                touch.update()
            }
        }

        @JvmStatic
        fun getZoomFactor(): Float {
            val touches = touches.values.toList()
            return if (touches.size == 2) {
                val t0 = touches[0]
                val t1 = touches[1]
                val dist0 = sq(t0.x - t1.x, t0.y - t1.y)
                val dist1 = sq(t0.lastX - t1.lastX, t0.lastY - t1.lastY)
                if (dist0 < 1f || dist1 < 1f) return 1f
                return sqrt(dist1 / dist0)
            } else 1f
        }

        @JvmStatic
        fun sumDeltaX(): Float = touches.values.sumOf { it.dx.toDouble() }.toFloat()

        @JvmStatic
        fun sumDeltaY(): Float = touches.values.sumOf { it.dy.toDouble() }.toFloat()

        @JvmStatic
        fun avgDeltaX(): Float = sumDeltaX() / max(1, touches.size)

        @JvmStatic
        fun avgDeltaY(): Float = sumDeltaY() / max(1, touches.size)

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
        @Suppress("unused")
        fun registerListener(listener: TouchListener) {
            listeners.add(listener)
        }

        @JvmStatic
        @Suppress("unused")
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
            for (i in listeners.indices) {
                listeners[i].onTouchUp(touch)
            }
        }
    }
}