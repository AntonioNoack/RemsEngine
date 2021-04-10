package me.anno.objects

import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.ColorInput
import me.anno.ui.style.Style

object ColorGrading {

    fun createInspector(
        t: Transform,
        cgPower: AnimatedProperty<*>,
        cgSaturation: AnimatedProperty<*>,
        cgSlope: AnimatedProperty<*>,
        cgOffset: AnimatedProperty<*>,
        img: (Panel) -> Panel,
        getGroup: (name: String, ttt: String, id: String) -> SettingCategory,
        style: Style
    ) {

        val group = getGroup("Color Grading (ASC CDL)", "", "color-grading")
        group.add(img(TextPanel("" +
                "1. tint by slope\n" +
                "2. add color with offset\n" +
                "3. control the power\n" +
                "4. (de)saturate", style).apply {
            textColor = textColor and 0x77ffffff
            focusTextColor = textColor
        }))

        val power = t.vi("Power", "sRGB, Linear, ...", "cg.power", cgPower, style)
        val slope = t.vi("Slope", "Intensity or Tint", "cg.slope", cgSlope, style)
        val offset = t.vi("Offset", "Can be used to color black objects", "cg.offset", cgOffset, style)

        group.add(img(power))
        group.add(img(slope))
        group.add(img(offset))

        val satDesc = "0 = gray scale, 1 = normal, -1 = inverted colors"
        group.add(img(t.vi("Saturation", satDesc, "cg.saturation", cgSaturation, style)))

    }

}