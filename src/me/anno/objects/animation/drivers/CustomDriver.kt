package me.anno.objects.animation.drivers

import me.anno.config.DefaultConfig
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.parser.SimpleExpressionParser.parseDouble
import me.anno.parser.SimpleExpressionParser.preparse
import me.anno.ui.base.Panel
import me.anno.ui.input.TextInputML
import me.anno.ui.style.Style

class CustomDriver: AnimationDriver(){

    // make them animated? no xD
    var formula = "sin(time*360)"
    var formulaParts: List<Any> = preparse(formula)

    // todo a formula field to set all values, depending on index?
    override fun createInspector(list: MutableList<Panel>, transform: Transform, style: Style) {
        super.createInspector(list, transform, style)
        list += TextInputML("Function f(time)", style, formula)
            .setChangeListener { formula = it; updateFormula() }
            .setIsSelectedListener { show(null) }
            .setTooltip("Example: sin(time*pi)")
    }

    // update by time? would be possible... but still...
    fun updateFormula(){
        formulaParts = preparse(formula)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("formula", formula)
    }

    override fun readString(name: String, value: String) {
        when(name){
            "formula" -> {
                formula = value
                updateFormula()
            }
            else -> super.readString(name, value)
        }
    }

    override fun getValue0(time: Double): Double {
        return parseDouble(
            ArrayList(formulaParts), mapOf(
            "t" to time, "time" to time
        )) ?: 0.0
    }

    override fun getClassName() = "CustomDriver"
    override fun getDisplayName() = if(formula.length <= maxFormulaDisplayLength) formula else "Custom"

    companion object {
        // could support more, but is useless anyways xD
        val maxFormulaDisplayLength = DefaultConfig["driver.formula.maxLength", 15]
    }

}