package me.anno.ui.base.groups

import me.anno.maths.Maths
import me.anno.ui.style.Style
import org.joml.Vector2d

/** basis for simple 2d map controls with zoom-in/out and drag to move */
abstract class MapPanel(style: Style) : PanelList(style) {

    val target = Vector2d()
    val center = Vector2d()

    open var scale = 1.0

    val centerX get() = x + w / 2
    val centerY get() = y + h / 2

    var minScale = 0.001
    var maxScale = 2.0

    fun windowToCoordsDirX(wx: Double) = wx / scale
    fun windowToCoordsDirY(wy: Double) = wy / scale

    fun coordsToWindowDirX(cx: Double) = cx * scale
    fun coordsToWindowDirY(cy: Double) = cy * scale

    fun windowToCoordsX(wx: Double) = (wx - centerX) / scale + center.x
    fun windowToCoordsY(wy: Double) = (wy - centerY) / scale + center.y

    fun coordsToWindowX(cx: Double) = (cx - center.x) * scale + centerX
    fun coordsToWindowY(cy: Double) = (cy - center.y) * scale + centerY

    abstract fun moveMap(): Boolean

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (moveMap()) {
            // moving around
            center.sub(dx / scale, dy / scale)
            target.set(center)
            invalidateLayout()
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        val oldX = windowToCoordsX(x.toDouble())
        val oldY = windowToCoordsY(y.toDouble())
        val multiplier = Maths.pow(1.05, dy.toDouble())
        scale = Maths.clamp(scale * multiplier, minScale, maxScale)
        val newX = windowToCoordsX(x.toDouble())
        val newY = windowToCoordsY(y.toDouble())
        // zoom in on the mouse pointer
        center.add(oldX - newX, oldY - newY)
        target.add(oldX - newX, oldY - newY)
        invalidateLayout()
    }

}