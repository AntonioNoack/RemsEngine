package me.anno.ui.input

import me.anno.config.DefaultStyle.iconGray
import me.anno.fonts.FontStats
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.Input
import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.maths.MinMax.max
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.unmix
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.components.TitlePanel
import me.anno.utils.types.AnyToDouble
import me.anno.utils.types.Floats.roundToIntOr
import kotlin.math.round

// todo show exact value?
// todo possibility to set exact value -> maybe via double click, and an ask-menu
class SliderInput(
    var minValue: Double, var maxValue: Double, var step: Double,
    override var value: Double, nameDesc: NameDesc, val visibilityKey: String, style: Style
) : PanelListY(style), InputPanel<Double> {

    inner class SliderDrawPanel : Panel(style.getChild("deep")) {

        private val targetHeight = style.getSize("text.fontSize", FontStats.getDefaultFontSize())

        override fun calculateSize(w: Int, h: Int) {
            super.calculateSize(w, h)
            minW = 5 * targetHeight
            minH = targetHeight
        }

        override var isEnabled: Boolean
            get() = InputVisibility[visibilityKey]
            set(_) {}

        override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
            super.draw(x0, y0, x1, y1)
            val x = clamp(x + (width * unmix(minValue, maxValue, value)).roundToIntOr(), x0, x1)
            drawRect(x0, y0, x - x0, y1 - y0, sliderColor)
        }

        private fun setFromMousePos(x: Float) {
            val rx = clamp((x - this.x) / max(this.width, 1)).toDouble()
            setValue(mix(minValue, maxValue, rx), true)
        }

        override fun onKeyDown(x: Float, y: Float, key: Key) {
            if (key == Key.BUTTON_LEFT) {
                setFromMousePos(x)
            } else super.onKeyDown(x, y, key)
        }

        override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
            if (Input.isLeftDown) {
                setFromMousePos(x)
            } else super.onMouseMoved(x, y, dx, dy)
        }
    }

    var titleView = TitlePanel(nameDesc, this, style)

    var title: String
        get() = titleView.text
        set(value) {
            titleView.text = value
        }

    val slider = SliderDrawPanel()

    init {
        add(titleView)
        add(slider)
        titleView.enableHoverColor = true
        titleView.disableFocusColors()
        titleView.addLeftClickListener {
            InputVisibility.toggle(visibilityKey)
        }
        tooltip = nameDesc.desc
    }

    var sliderColor = style.getColor("textColor", iconGray)

    private var changeListener: ((value: Double) -> Unit)? = null
    private var isSelectedListener: (() -> Unit)? = null
    private var resetListener: (() -> Double)? = null
    override var isInputAllowed: Boolean = true

    override fun setValue(newValue: Double, mask: Int, notify: Boolean): Panel {
        val v0 = clamp(newValue, minValue, maxValue)
        val v1 = if (step == 0.0) v0 else round((v0 - minValue) / step) * step + minValue
        if (v1 != value) {
            value = v1
            if (notify) changeListener?.invoke(value)
        }
        return this
    }

    val averageValue: Double get() = (minValue + maxValue) * 0.5
    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        val value = AnyToDouble.getDouble(data, averageValue)
        setValue(value, true)
    }

    override fun onCopyRequested(x: Float, y: Float): String = value.toString()

    fun setChangeListener(listener: (value: Double) -> Unit): SliderInput {
        changeListener = listener
        return this
    }

    fun setIsSelectedListener(listener: () -> Unit): SliderInput {
        isSelectedListener = listener
        return this
    }

    fun setResetListener(listener: () -> Double): SliderInput {
        resetListener = listener
        return this
    }

    override fun onEmpty(x: Float, y: Float) {
        setValue(resetListener?.invoke() ?: averageValue, true)
    }

    fun keyStep(dir: Double) {
        var step = step
        if (step == 0.0) step = (maxValue - minValue) / 16
        setValue(step * dir, true)
    }

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
        when (key) {
            Key.KEY_ARROW_LEFT, Key.KEY_ARROW_DOWN -> keyStep(+1.0)
            Key.KEY_ARROW_RIGHT, Key.KEY_ARROW_UP -> keyStep(-1.0)
            else -> super.onKeyTyped(x, y, key)
        }
    }
}