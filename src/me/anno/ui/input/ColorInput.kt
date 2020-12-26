package me.anno.ui.input

import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.objects.Camera
import me.anno.objects.animation.AnimatedProperty
import me.anno.studio.rems.RemsStudio
import me.anno.studio.StudioBase.Companion.shiftSlowdown
import me.anno.studio.rems.Selection.selectedProperty
import me.anno.studio.rems.Selection.selectedTransform
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.color.ColorChooser
import me.anno.ui.input.components.TitlePanel
import me.anno.ui.style.Style
import me.anno.utils.Maths.clamp
import me.anno.utils.Lists.one
import me.anno.utils.Maths.pow
import org.joml.Vector4f
import kotlin.math.max

class ColorInput(
    style: Style, title: String,
    oldValue: Vector4f,
    withAlpha: Boolean,
    private val owningProperty: AnimatedProperty<*>? = null
) : PanelListY(style) {

    private val contentView = ColorChooser(style, withAlpha, owningProperty)
    private val titleView = TitlePanel(title, contentView, style)
    private var mouseIsDown = false

    init {
        this += titleView
        titleView.enableHoverColor = true
        titleView.focusTextColor = titleView.textColor
        titleView.setSimpleClickListener { contentView.toggleVisibility() }
        this += contentView
        contentView.setRGBA(oldValue.x, oldValue.y, oldValue.z, oldValue.w, false)
        contentView.hide()
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        super.onMouseDown(x, y, button)
        mouseIsDown = true
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        super.onMouseUp(x, y, button)
        mouseIsDown = false
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        super.onMouseMoved(x, y, dx, dy)
        if (mouseIsDown) {
            val scale2 = 20f * shiftSlowdown
            val size = scale2 * (if (selectedTransform is Camera) -1f else 1f) / max(GFX.width, GFX.height)
            val dx0 = dx * size
            val dy0 = dy * size
            val delta = dx0 - dy0
            val scaleFactor = 1.10f
            val scale = pow(scaleFactor, delta)
            contentView.apply {
                if (Input.isControlDown) {
                    setHSL(hue, saturation, lightness * scale, opacity, colorSpace, true)
                } else {
                    setHSL(hue, saturation, lightness, clamp(opacity + delta, 0f, 1f), colorSpace, true)
                }
            }
        }
    }

    fun noTitle(): ColorInput {
        titleView.hide()
        contentView.show()
        return this
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused1 = titleView.isInFocus || contentView.listOfAll.one { it.isInFocus }
        if (focused1) isSelectedListener?.invoke()
        if (RemsStudio.hideUnusedProperties) {
            val focused2 = focused1 || (owningProperty == selectedProperty && owningProperty != null)
            contentView.visibility = if (focused2) Visibility.VISIBLE else Visibility.GONE
        }
        super.onDraw(x0, y0, x1, y1)
    }

    fun setChangeListener(listener: (r: Float, g: Float, b: Float, a: Float) -> Unit): ColorInput {
        contentView.setChangeRGBListener(listener)
        return this
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): ColorInput {
        isSelectedListener = listener
        return this
    }

}