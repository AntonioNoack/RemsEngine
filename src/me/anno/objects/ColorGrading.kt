package me.anno.objects

import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.Panel
import me.anno.ui.editor.SettingCategory
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

        val color = getGroup("Color Grading (ASC CDL)", "", "color-grading")
        color.add(img(t.vi("Power", "sRGB, Linear, ...", "cg.power", cgPower, style)))
        val satDesc = "0 = gray scale, 1 = normal, -1 = inverted colors"
        color.add(img(t.vi("Saturation", satDesc, "cg.saturation", cgSaturation, style)))
        color.add(img(t.vi("Slope", "Intensity or Tint", "cg.slope", cgSlope, style)))
        color.add(img(t.vi("Offset", "Can be used to color black objects", "cg.offset", cgOffset, style)))

    }

}