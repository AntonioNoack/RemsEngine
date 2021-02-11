package me.anno.objects.animation.drivers

import me.anno.config.DefaultConfig
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.objects.Transform
import me.anno.utils.structures.lists.CountingList
import me.anno.parser.SimpleExpressionParser.parseDouble
import me.anno.parser.SimpleExpressionParser.preparse
import me.anno.ui.base.Panel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import kotlin.math.PI
import kotlin.math.sin

class HarmonicDriver: AnimationDriver(){

    // use drivers to generate sound? rather not xD
    // maybe in debug mode

    // make them animated? no xD
    var harmonicsFormula = "1/n"
    val harmonics = FloatArray(maxHarmonics){
        1f/(it+1f)
    }

    override fun createInspector(list: MutableList<Panel>, transform: Transform, style: Style, getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory) {
        super.createInspector(list, transform, style, getGroup)
        list += TextInput(getDisplayName(), style.getChild("deep"), harmonicsFormula)
            .setChangeListener { harmonicsFormula = it; updateHarmonics() }
            .setIsSelectedListener { show(null) }
            .setTooltip(Dict["Default value is 1/n, try [2,0,1][n-1]", "driver.harmonic.desc"])
    }

    // update by time? would be possible... but still...
    private fun updateHarmonics(){
        val prepared = preparse(harmonicsFormula)
        if(prepared != null){
            for(i in 0 until maxHarmonics){
                val n = i + 1.0
                harmonics[i] = parseDouble(
                    CountingList(prepared), mapOf(
                        "n" to n, "i" to n
                    ))?.toFloat() ?: harmonics[i]
            }
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("harmonics", harmonicsFormula)
    }

    override fun readString(name: String, value: String) {
        when(name){
            "harmonics" -> {
                harmonicsFormula = value
                updateHarmonics()
            }
            else -> super.readString(name, value)
        }
    }

    override fun getValue0(time: Double, keyframeValue: Double): Double {
        val w0 = time * 2.0 * PI
        return harmonics.withIndex().sumByDouble { (index, it) -> it * sin((index + 1f) * w0) }
    }

    override fun getClassName() = "HarmonicDriver"
    override fun getDisplayName() = Dict["Harmonics h(n)", "driver.harmonic"]

    companion object {
        // could support more, but is useless anyways xD
        val maxHarmonics = DefaultConfig["driver.harmonics.max", 32]
    }

}