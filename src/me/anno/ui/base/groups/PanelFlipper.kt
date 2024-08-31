package me.anno.ui.base.groups

import me.anno.Time
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.drawing.GFXx2D.transform
import me.anno.input.Input
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.maths.Maths.mix
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Floats.toRadians
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

// like ViewFlipper in Android
open class PanelFlipper(sorter: Comparator<Panel>?, style: Style) : PanelList(sorter, style) {

    constructor(style: Style) : this(null, style)

    enum class TransitionType(val swipeXAxis: Boolean, val swipeYAxis: Boolean) {
        INSTANT(true, true),
        SWIPE_HORIZONTAL(true, false),
        SWIPE_VERTICAL(false, true),
        ROTATE_HORIZONTAL(true, false),
        ROTATE_VERTICAL(false, true)
        // we could add a few more animations
    }

    var transitionType = TransitionType.SWIPE_HORIZONTAL

    var useLeftMouseButton = true
    var useRightMouseButton = true
    var useMouseWheel = false

    @Docs("allows the user to turn farther than possible for a spring like effect, [0,1]")
    @Range(0.0, 0.499)
    var leftBounce = 0f

    @Docs("allows the user to turn farther than possible for a spring like effect, [0,1]")
    @Range(0.0, 0.499)
    var rightBounce = 0f

    var position = 0f
    var targetPosition = 0f

    var swipeSpeed = 1f

    @Range(0.0, Double.POSITIVE_INFINITY)
    var smoothingPerSeconds = 3f

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minW = 0
        minH = 0
        for (child in children) {
            child.calculateSize(w, h)
            minW = max(minW, child.minW)
            minH = max(minH, child.minH)
        }
    }

    fun updatePosition() {
        val oldPosition = position
        val dt = Time.uiDeltaTime.toFloat()
        position = constantLerpTo(position, targetPosition, dt * smoothingPerSeconds)
        if (abs(position - oldPosition) > 1e-4) {
            invalidateDrawing()
        }
        // if not is controller down, then clamp targetPosition smoothly into an integer value
        val correction =
            if ((Input.isLeftDown && useLeftMouseButton) || (Input.isRightDown && useRightMouseButton)) 0f
            else if (useMouseWheel) (abs(Time.nanoTime - lastMouseWheel) * 1e-9f) else 1f
        if (correction > 0f) {
            val int = round(targetPosition)
            targetPosition = constantLerpTo(targetPosition, int, dt * smoothingPerSeconds)
        }
    }

    fun constantLerpTo(a: Float, b: Float, dt: Float): Float {
        val diff = max(abs(b - a), 1e-9f)
        return mix(a, b, min(dt / diff, 1f))
    }

    private fun swipe(deltaPos: Float) {
        if (deltaPos == 0f) return
        val oldPosition = targetPosition.roundToIntOr()
        invalidateDrawing()
        targetPosition = clamp(targetPosition - deltaPos * swipeSpeed, -leftBounce, (children.size - 1) + rightBounce)
        val newPosition = targetPosition.roundToIntOr()
        if (oldPosition != newPosition) {
            invalidateLayout()
        }
    }

    var rotationStrengthRadians = 90f.toRadians()

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        when (transitionType) {
            TransitionType.INSTANT -> {
                val posIndex = position.roundToIntOr()
                for ((index, child) in children.withIndex()) {
                    if (index == posIndex) {
                        placeChild(child, x, y, width, height)
                    } else {
                        child.setPosSize(x, y, 0, 0)
                    }
                }
            }
            TransitionType.SWIPE_HORIZONTAL, TransitionType.ROTATE_HORIZONTAL -> {
                for ((index, child) in children.withIndex()) {
                    val offset = (width * (index - position)).roundToIntOr()
                    placeChild(child, x + offset, y, width, height)
                    child.weight2 = (index - position) * rotationStrengthRadians // unused field abused ^^
                    // todo for rotated children, set their approximate position properly
                }
            }
            TransitionType.SWIPE_VERTICAL, TransitionType.ROTATE_VERTICAL -> {
                for ((index, child) in children.withIndex()) {
                    val offset = (height * (index - position)).roundToIntOr()
                    placeChild(child, x, y + offset, width, height)
                    child.weight2 = (index - position) * rotationStrengthRadians // unused field abused ^^
                }
            }
        }
    }

    private fun placeChild(child: Panel, x: Int, y: Int, width: Int, height: Int) {
        child.setPosSize(
            x + child.alignmentX.getOffset(width, child.minW),
            y + child.alignmentY.getOffset(height, child.minH),
            child.alignmentX.getSize(width, child.minW),
            child.alignmentX.getSize(height, child.minH)
        )
    }

    override fun drawChild(child: Panel, x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        val rotateChildren = transitionType == TransitionType.ROTATE_HORIZONTAL ||
                transitionType == TransitionType.ROTATE_VERTICAL
        return if (rotateChildren) {
            val x02 = max(child.x, x0)
            val y02 = max(child.y, y0)
            val x12 = min(child.x + child.width, x1)
            val y12 = min(child.y + child.height, y1)
            if (x12 > x02 && y12 > y02) {
                // rotate child
                // todo define rotation center properly, currently is somehow at the top center, right center
                val aspect = child.width.toFloat() / child.height
                transform.pushMatrix()
                transform.scale(1f, aspect, 1f)
                transform.rotateZ(child.weight2)
                transform.scale(1f, 1f / aspect, 1f)
                child.draw(x02, y02, x12, y12)
                transform.popMatrix()
                true
            } else false
        } else super.drawChild(child, x0, y0, x1, y1)
    }

    // if they are overlapping, we need to redraw the others as well
    override fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return false // not really, I think...
        // todo ok so?
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        updatePosition()
        setPosition(x, y)
        super.onDraw(x0, y0, x1, y1)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if ((Input.isLeftDown && useLeftMouseButton) || (Input.isRightDown && useRightMouseButton)) {
            val type = transitionType
            val ws = windowStack
            val dt = (if (type.swipeXAxis) dx / min(width, ws.width) else 0f) +
                    (if (type.swipeYAxis) dy / min(height, ws.height) else 0f)
            swipe(dt)
            if (!type.swipeXAxis || !type.swipeYAxis) {
                super.onMouseMoved(x, y, if (type.swipeXAxis) 0f else dx, if (type.swipeYAxis) 0f else dy)
            }
        } else super.onMouseMoved(x, y, dx, dy)
    }

    private var lastMouseWheel = 0L

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        if (useMouseWheel) {
            swipe(dx + dy)
            if (length(dx, dy) > 1e-3f) lastMouseWheel = Time.nanoTime
        } else super.onMouseWheel(x, y, dx, dy, byMouse)
    }

    override fun clone(): PanelFlipper {
        val clone = PanelFlipper(sorter, style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is PanelFlipper) return
        dst.transitionType = transitionType
        dst.useLeftMouseButton = useLeftMouseButton
        dst.useRightMouseButton = useRightMouseButton
        dst.useMouseWheel = useMouseWheel
        dst.position = position
        dst.targetPosition = targetPosition
        dst.smoothingPerSeconds = smoothingPerSeconds
        dst.leftBounce = leftBounce
        dst.rightBounce = rightBounce
    }
}