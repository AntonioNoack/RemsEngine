package me.anno.ui.input

import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.objects.Camera
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.TextPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.color.ColorChooser
import me.anno.ui.style.Style
import me.anno.utils.clamp
import me.anno.utils.pow
import org.joml.Vector4f
import kotlin.math.max

class ColorInput(style: Style, title: String,
                 oldValue: Vector4f,
                 private val owningProperty: AnimatedProperty<*>? = null): PanelListY(style){

    private val titleView = object: TextPanel(title, style){
        override fun onCopyRequested(x: Float, y: Float) = parent?.onCopyRequested(x,y)
        override fun onMouseDown(x: Float, y: Float, button: Int) {
            super.onMouseDown(x, y, button)
            mouseIsDown = true
        }
        var mouseIsDown = false
        override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
            super.onMouseMoved(x, y, dx, dy)
            if(mouseIsDown){
                val size = (if(Input.isShiftDown) 4f else 20f) * (if(GFX.selectedTransform is Camera) -1f else 1f) / max(GFX.width,GFX.height)
                val dx0 = dx*size
                val dy0 = dy*size
                val delta = dx0-dy0
                val scaleFactor = 1.10f
                val scale = pow(scaleFactor, delta)
                contentView.apply {
                    if(Input.isControlDown){
                        setHSL(hue, saturation, lightness * scale, opacity, colorSpace)
                    } else {
                        setHSL(hue, saturation, lightness, clamp(opacity + delta, 0f, 1f), colorSpace)
                    }
                }
            }
        }
        override fun onMouseUp(x: Float, y: Float, button: Int) {
            super.onMouseUp(x, y, button)
            mouseIsDown = false
        }
    }
    private val contentView = ColorChooser(style, true)

    init {
        this += titleView
        this += contentView
        contentView.setRGBA(oldValue.x, oldValue.y, oldValue.z, oldValue.w)
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused1 = titleView.isInFocus || contentView.listOfAll.count { it.isInFocus } > 0
        if(focused1) isSelectedListener?.invoke()
        val focused2 = focused1 || owningProperty == GFX.selectedProperty
        contentView.visibility = if(focused2) Visibility.VISIBLE else Visibility.GONE
        super.draw(x0, y0, x1, y1)
    }

    fun setChangeListener(listener: (x: Float, y: Float, z: Float, w: Float) -> Unit): ColorInput {
        contentView.setChangeRGBListener(listener)
        return this
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): ColorInput {
        isSelectedListener = listener
        return this
    }

}