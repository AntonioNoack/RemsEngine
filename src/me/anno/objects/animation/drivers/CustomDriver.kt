package me.anno.objects.animation.drivers

import me.anno.config.DefaultConfig
import me.anno.io.base.BaseWriter
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.parser.SimpleExpressionParser.parseDouble
import me.anno.parser.SimpleExpressionParser.preparse
import me.anno.ui.base.Panel
import me.anno.ui.input.TextInputML
import me.anno.ui.style.Style
import kotlin.math.PI
import kotlin.math.sin

class CustomDriver: AnimationDriver(){

    var amplitude = AnimatedProperty.float().set(1f)

    // make them animated? no xD
    var formula = "sin(time*360)"
    var formulaParts: List<Any> = preparse(formula)

    // todo a formula field to set all values, depending on index?
    override fun createInspector(transform: Transform, style: Style): List<Panel> {
        return listOf(
            TextInputML("Function f(time)", style, formula)
                .setChangeListener { formula = it; updateFormula() }
                .setIsSelectedListener { show(null) }
                .setTooltip("Example: sin(time*pi)")
            // ,
            // FloatInput("", amplitude, lastLocalTime, style)
        )
    }

    // update by time? would be possible... but still...
    fun updateFormula(){
        formulaParts = preparse(formula)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "amplitude", amplitude)
        writer.writeString("formula", formula)
    }

    override fun getValue(time: Float): Float {
        val t = time.toDouble()
        return amplitude[time] * (parseDouble(
            ArrayList(formulaParts), mapOf(
            "t" to t, "time" to t
        ))?.toFloat() ?: 0f)
    }

    override fun getClassName() = "CustomDriver"
    override fun getDisplayName() = if(formula.length <= maxFormulaDisplayLength) formula else "Custom"

    companion object {
        // could support more, but is useless anyways xD
        val maxFormulaDisplayLength = DefaultConfig["driver.formula.maxLength", 15]
    }

}