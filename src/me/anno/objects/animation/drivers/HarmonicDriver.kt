package me.anno.objects.animation.drivers

import me.anno.config.DefaultConfig
import me.anno.io.base.BaseWriter
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.parser.SimpleExpressionParser.parseDouble
import me.anno.parser.SimpleExpressionParser.preparse
import me.anno.ui.base.Panel
import me.anno.ui.input.TextInput
import me.anno.ui.input.TextInputML
import me.anno.ui.style.Style
import me.anno.utils.sumByFloat
import kotlin.math.PI
import kotlin.math.sin

class HarmonicDriver: AnimationDriver(){

    // use drivers to generate sound? rather not xD
    // maybe in debug mode

    // dangerous when animated?
    var baseFrequency = AnimatedProperty.float().set(1f)

    var amplitude = AnimatedProperty.float().set(1f)

    // make them animated? no xD
    var harmonicsFormula = "1/n"
    val harmonics = FloatArray(maxHarmonics){
        1f/(it+1f)
    }

    override fun createInspector(transform: Transform, style: Style): List<Panel> {
        return listOf(
            TextInput("Harmonics h(n)", style.getChild("deep"), harmonicsFormula)
                .setChangeListener { harmonicsFormula = it; updateHarmonics() }
                .setIsSelectedListener { show(null) }
                .setTooltip("Default value is 1/n, try [2,0,1][n-1]"),
            transform.VI("Amplitude", "Driver Strength", amplitude, style),
            transform.VI("Base Frequency 1/s", "How fast it's oscillating", baseFrequency, style)
            // FloatInput("", amplitude, lastLocalTime, style)
        )
    }

    // update by time? would be possible... but still...
    fun updateHarmonics(){
        val prepared = preparse(harmonicsFormula)
        for(i in 0 until maxHarmonics){
            val n = i + 1.0
            harmonics[i] = parseDouble(ArrayList(prepared), mapOf(
                "n" to n, "i" to n
            ))?.toFloat() ?: harmonics[i]
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "amplitude", amplitude)
        writer.writeObject(this, "frequency", baseFrequency)
        writer.writeString("harmonics", harmonicsFormula)
        // writer.writeFloatArray("harmonics", harmonics)
    }

    override fun getValue(time: Float): Float {
        val w0 = (time * baseFrequency[time] * 2.0 * PI).toFloat()
        return amplitude[time] *
                harmonics.withIndex().sumByFloat { (index, it) -> it * sin((index + 1f) * w0) }
    }

    override fun getClassName() = "HarmonicDriver"
    override fun getDisplayName() = "Harmonic"

    companion object {
        // could support more, but is useless anyways xD
        val maxHarmonics = DefaultConfig["driver.harmonics.max", 32]
    }

}