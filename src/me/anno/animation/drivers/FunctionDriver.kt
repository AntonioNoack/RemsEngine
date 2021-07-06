package me.anno.animation.drivers

import me.anno.config.DefaultConfig
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.utils.structures.lists.CountingList
import me.anno.parser.SimpleExpressionParser.parseDouble
import me.anno.parser.SimpleExpressionParser.preparse
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.TextInputML
import me.anno.ui.style.Style

class FunctionDriver : AnimationDriver() {

    // make them animated? no xD
    var formula = "sin(time*360)"
    var formulaParts: CountingList? = preparse(formula)

    override fun createInspector(
        list: PanelListY, style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        list += TextInputML(Dict["Function f(time)", "driver.function"], style, formula)
            .setChangeListener { formula = it; updateFormula() }
            .setIsSelectedListener { show(null) }
            .setTooltip(Dict["Example: sin(time*pi)", "driver.function.desc"])
    }

    // update by time? would be possible... but still...
    fun updateFormula() {
        formulaParts = preparse(formula)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("formula", formula)
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "formula" -> {
                formula = value
                updateFormula()
            }
            else -> super.readString(name, value)
        }
    }

    override fun getValue0(time: Double, keyframeValue: Double, index: Int): Double {
        val formulaParts = formulaParts ?: return 0.0
        return parseDouble(
            CountingList(formulaParts), mapOf(
                "t" to time, "time" to time,
                "v" to keyframeValue, "value" to keyframeValue
            )
        ) ?: 0.0
    }

    override val className get() = "FunctionDriver"
    override fun getDisplayName() =
        if (formula.length <= maxFormulaDisplayLength) formula
        else Dict["Function f(time)", "driver.function"]

    companion object {
        // could support more, but is useless anyways xD
        val maxFormulaDisplayLength = DefaultConfig["driver.formula.maxLength", 15]
    }

}