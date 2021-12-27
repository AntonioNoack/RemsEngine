package me.anno.input

import me.anno.gpu.GFX
import me.anno.ui.utils.WindowStack
import org.joml.Vector3f

class Touch(var x: Float, var y: Float) {

    val t0 = GFX.gameTime
    val x0 = x
    val y0 = y
    var dx = 0f
    var dy = 0f
    var lastX = x
    var lastY = y

    fun update() {
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