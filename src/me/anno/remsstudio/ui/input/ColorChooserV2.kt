package me.anno.remsstudio.ui.input

import me.anno.io.serialization.NotSerializedProperty
import me.anno.remsstudio.RemsStudio
import me.anno.remsstudio.RemsStudio.project
import me.anno.remsstudio.animation.AnimatedProperty
import me.anno.ui.editor.color.ColorChooser
import me.anno.ui.input.components.ColorPalette
import me.anno.ui.style.Style
import org.joml.Vector3f
import org.joml.Vector4f

class ColorChooserV2(style: Style, withAlpha: Boolean, val property: AnimatedProperty<*>) :
    ColorChooser(style, withAlpha, ColorPaletteV2(style)) {

    class ColorPaletteV2(style: Style) : ColorPalette(8, 4, style) {

        override fun getColor(x: Int, y: Int) = project?.config?.get("color.$x.$y", 0) ?: 0
        override fun setColor(x: Int, y: Int, color: Int) {
            project?.config?.set("color.$x.$y", color)
        }

        override fun clone(): ColorPaletteV2 {
            val clone = ColorPaletteV2(style)
            copy(clone)
            return clone
        }

        override val className: String = "ColorPaletteV2"

    }

    @NotSerializedProperty
    private var lastTime = RemsStudio.editorTime

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        if (lastTime != RemsStudio.editorTime) {
            lastTime = RemsStudio.editorTime
            when (val c = property[RemsStudio.editorTime]) {
                is Vector3f -> setRGBA(c.x, c.y, c.z, 1f, false)
                is Vector4f -> setRGBA(c, false)
                else -> throw RuntimeException()
            }
        }
        super.onDraw(x0, y0, x1, y1)
    }

    override fun clone(): ColorChooserV2 {
        val clone = ColorChooserV2(style, withAlpha, property)
        copy(clone)
        return clone
    }

    override val className: String = "ColorChooserV2"

}