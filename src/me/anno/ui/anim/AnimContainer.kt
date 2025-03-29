package me.anno.ui.anim

import me.anno.Time.uiDeltaTime
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.input.Input
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import kotlin.math.abs

/**
 * a container for simple transform animations (scale / translate)
 *
 * to do a container class with rendered framebuffer for more complex transforms like warping or waves
 * */
class AnimContainer(base: Panel, space: Padding, style: Style) : PanelContainer(base, space, style) {

    constructor(style: Style) : this(Panel(style), Padding.Zero, style)

    @NotSerializedProperty
    var hover = 0f

    @NotSerializedProperty
    var lastHover = 0f

    @NotSerializedProperty
    var touch = 0f

    @NotSerializedProperty
    var lastTouch = 0f

    var speed = 3f

    @Type("List<UIAnimation>")
    @SerializedProperty
    var animations = ArrayList<UIAnimation>()

    fun move(x: Float, deltaSign: Boolean, delta: Float): Float {
        // mix(x, if (deltaSign) 1f else 0f, delta)
        return clamp(if (deltaSign) x + delta else x - delta)
    }

    override fun onUpdate() {
        super.onUpdate()
        var needsUpdate = false
        val dtx = min(uiDeltaTime.toFloat() * speed, 0.5f)
        val minDelta = 0.1f / max(1, max(padding.width, padding.height))
        hover = move(hover, isHovered, dtx)
        if (abs(hover - lastHover) > minDelta) {
            lastHover = hover
            needsUpdate = true
        }
        val window = window
        if (window != null) {
            touch = move(
                touch, (Input.isLeftDown || Input.isRightDown) &&
                        contains(window.mouseDownX, window.mouseDownY), dtx
            )
            if (abs(touch - lastTouch) > minDelta) {
                lastTouch = touch
                needsUpdate = true
            }
        }
        if (needsUpdate) {
            updateAnimations()
        }
    }

    fun updateAnimations() {
        val child = child
        child.x = x + padding.left
        child.y = y + padding.top
        child.width = width - padding.width
        child.height = height - padding.height
        for (index in animations.indices) {
            val anim = animations[index]
            val strength = when (anim.eventType) {
                EventType.HOVER -> hover
                EventType.TOUCH -> touch
            }
            val lastStrength = when (anim.eventType) {
                EventType.HOVER -> lastHover
                EventType.TOUCH -> lastTouch
            }
            val interpolation = if (lastStrength < strength)
                anim.inInterpolation else
                anim.outInterpolation
            val interpolated = interpolation.getIn(strength)
            anim.apply(this, child, interpolated)
        }
        child.minW = child.width
        child.minH = child.height
        child.setPosSize(child.x, child.y, child.width, child.height)
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        updateAnimations()
    }

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        updateAnimations()
    }

    fun add(animation: UIAnimation) {
        animations.add(animation)
    }

    operator fun plusAssign(animation: UIAnimation) {
        add(animation)
    }

    override fun clone(): AnimContainer {
        val clone = AnimContainer(style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is AnimContainer) return
        dst.speed = speed
        dst.animations.addAll(animations.map { it.clone() })
    }
}