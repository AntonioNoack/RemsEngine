package me.anno.remsstudio.ui.input

import me.anno.remsstudio.RemsStudio
import me.anno.remsstudio.Selection
import me.anno.remsstudio.animation.AnimatedProperty
import me.anno.ui.base.Visibility
import me.anno.ui.input.ColorInput
import me.anno.ui.style.Style
import org.joml.Vector4f

class ColorInputV2(
    style: Style, title: String, visibilityKey: String,
    oldValue: Vector4f, withAlpha: Boolean, val property: AnimatedProperty<*>
) : ColorInput(style, title, visibilityKey, oldValue, withAlpha, ColorChooserV2(style, withAlpha, property)) {

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        if (RemsStudio.hideUnusedProperties) {
            val focused1 = titleView.isInFocus || contentView.listOfAll.any { it.isInFocus }
            val focused2 = focused1 || (property == Selection.selectedProperty)
            contentView.visibility = Visibility[focused2]
        }
        super.onDraw(x0, y0, x1, y1)
    }

}