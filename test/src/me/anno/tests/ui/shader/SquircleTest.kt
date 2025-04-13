package me.anno.tests.ui.shader

import me.anno.config.DefaultConfig.style
import me.anno.gpu.drawing.DrawRounded.drawRoundedRect
import me.anno.gpu.drawing.DrawSquircle.drawSquircle
import me.anno.language.translation.NameDesc
import me.anno.ui.UIColors
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestDrawPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.FloatInput
import me.anno.ui.input.NumberType

fun main() {

    var powerX = 10f
    var powerY = 10f
    var smoothness = 1f
    var outline = 2f
    var squircle = true

    val ui = PanelListY(style)
    ui.add(TestDrawPanel {
        if (squircle) {
            drawSquircle(
                it.x, it.y, it.width, it.height,
                powerX, powerY, outline,
                UIColors.axisWColor, UIColors.darkOrange, it.backgroundColor,
                smoothness
            )
        } else {
            val radius = (it.width / powerX + it.height / powerY) * 0.5f
            drawRoundedRect(
                it.x, it.y, it.width, it.height,
                radius, radius, radius, radius, outline,
                UIColors.axisWColor, UIColors.darkOrange, it.backgroundColor,
                smoothness
            )
        }
    }.fill(1f))

    val type = NumberType.FLOAT_PLUS
    ui.add(BooleanInput(NameDesc("Squircle"), squircle, true, style).setChangeListener { squircle = it })
    ui.add(FloatInput(NameDesc("PowerX"), powerX, type, style).setChangeListener { powerX = it.toFloat() })
    ui.add(FloatInput(NameDesc("PowerY"), powerY, type, style).setChangeListener { powerY = it.toFloat() })
    ui.add(FloatInput(NameDesc("Smoothness"), smoothness, type, style).setChangeListener { smoothness = it.toFloat() })
    ui.add(FloatInput(NameDesc("Outline"), outline, type, style).setChangeListener { outline = it.toFloat() })

    testUI3("Squircle", ui)
}