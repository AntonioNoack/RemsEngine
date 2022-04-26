package me.anno.ui.base.groups

import me.anno.Engine
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.input.Input
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.maths.Maths.mix
import me.anno.ui.Panel
import me.anno.ui.style.Style
import kotlin.math.*

// todo test it

// like ViewFlipper in Android
open class PanelFlipper(sorter: Comparator<Panel>?, style: Style) : PanelList(sorter, style) {

    constructor(style: Style) : this(null, style)

    enum class TransitionType(val swipeXAxis: Boolean, val swipeYAxis: Boolean) {
        INSTANT(true, true),
        SWIPE_HORIZONTAL(true, false),
        SWIPE_VERTICAL(false, true),
        ROTATE_HORIZONTAL(true, false), // todo requires perspective/skewed drawing of UI, needs to be implemented
        ROTATE_VERTICAL(false, true)
        // we could add a few more animations
        // todo implement them all
    }

    var transitionType = TransitionType.SWIPE_HORIZONTAL

    var useLeftMouseButton = true
    var useRightMouseButton = true
    var useMouseWheel = false

    // allows the user to turn farther than possible for a spring like effect, [0,1]
    @Range(0.0, 1.0)
    var leftBounce = 0f

    @Range(0.0, 1.0)
    var rightBounce = 0f

    var position = 0f
    var targetPosition = 0f

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
        position = mix(position, targetPosition, clamp(Engine.deltaTime * smoothingPerSeconds))
        if (abs(position - oldPosition) > 1e-4) {
            invalidateDrawing()
        }
        // if not is controller down, then clamp targetPosition smoothly into an integer value
        val correction =
            if ((Input.isLeftDown && useLeftMouseButton) || (Input.isRightDown && useRightMouseButton)) 0f
            else if (useMouseWheel) (abs(Engine.gameTime - lastMouseWheel) * 1e-9f) else 1f
        if (correction > 0f) {
            val int = round(targetPosition)
            targetPosition = mix(targetPosition, int, Engine.deltaTime * smoothingPerSeconds)
        }
    }

    private fun swipe(dt: Float) {
        if (dt == 0f) return
        val oldPosition = targetPosition.roundToInt()

        invalidateDrawing()
        val newPosition = targetPosition.roundToInt()
        if (oldPosition != newPosition) {
            invalidateLayout()
        }
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        when (transitionType) {
            TransitionType.INSTANT -> {
                val posIndex = position.roundToInt()
                for ((index, child) in children.withIndex()) {
                    if (index == posIndex) {
                        child.setPosSize(x, y, w, h)
                    } else {
                        child.setPosSize(x, y, 0, 0)
                    }
                }
            }
            TransitionType.SWIPE_HORIZONTAL, TransitionType.ROTATE_HORIZONTAL -> {
                for ((index, child) in children.withIndex()) {
                    val offset = (w * (position - index)).roundToInt()
                    child.setPosSize(x + offset, y, w, h)
                }
            }
            TransitionType.SWIPE_VERTICAL, TransitionType.ROTATE_VERTICAL -> {
                for ((index, child) in children.withIndex()) {
                    val offset = (h * (position - index)).roundToInt()
                    child.setPosSize(x, y + offset, w, h)
                }
            }
        }
    }

    // if they are overlapping, we need to redraw the others as well
    override fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return false // not really, I think...
        // todo ok so?
        // return children.count { it.visibility == Visibility.VISIBLE } > 1
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
            val dt = (if (type.swipeXAxis) dx / min(w, ws.width) else 0f) +
                    (if (type.swipeYAxis) dy / min(h, ws.height) else 0f)
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
            if (length(dx, dy) > 1e-3f) lastMouseWheel = Engine.gameTime
        } else super.onMouseWheel(x, y, dx, dy, byMouse)
    }

    override fun clone(): PanelFlipper {
        val clone = PanelFlipper(sorter, style)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as PanelFlipper
        clone.transitionType = transitionType
        clone.useLeftMouseButton = useLeftMouseButton
        clone.useRightMouseButton = useRightMouseButton
        clone.useMouseWheel = useMouseWheel
        clone.position = position
        clone.targetPosition = targetPosition
        clone.smoothingPerSeconds = smoothingPerSeconds
        clone.leftBounce = leftBounce
        clone.rightBounce = rightBounce
    }

    override val className: String = "PanelFlipper"

}