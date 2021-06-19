package me.anno.objects

import me.anno.animation.AnimatedProperty
import me.anno.ui.base.Panel
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style

object ColorGrading {

    fun createInspector(
        t: Transform,
        cgPower: AnimatedProperty<*>,
        cgSaturation: AnimatedProperty<*>,
        cgSlope: AnimatedProperty<*>,
        cgOffsetAdd: AnimatedProperty<*>,
        cgOffsetSub: AnimatedProperty<*>,
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
        val offset1 = t.vi("Plus Offset", "Can be used to color black objects", "cg.offset", cgOffsetAdd, style)
        val offset2 = t.vi("Minus Offset", "Can be used to color white objects", "cg.offset.sub", cgOffsetSub, style)

        group.add(img(power))
        group.add(img(slope))
        group.add(img(offset1))
        group.add(img(offset2))

        val satDesc = "0 = gray scale, 1 = normal, -1 = inverted colors"
        group.add(img(t.vi("Saturation", satDesc, "cg.saturation", cgSaturation, style)))

    }

}