package me.anno.objects.animation.drivers

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.Panel
import me.anno.ui.input.FloatInput
import me.anno.ui.input.IntInput
import me.anno.ui.input.LongInput
import me.anno.ui.style.Style
import me.anno.utils.clamp
import org.kdotjpg.OpenSimplexNoise
import kotlin.math.max
import kotlin.math.min

class PerlinNoiseDriver: AnimationDriver(){

    var falloff = AnimatedProperty.float01().set(0.5f)
    var octaves = 5

    var seed = 0L

    var baseValue = AnimatedProperty.float()
    var amplitude = AnimatedProperty.float().set(1f)
    var frequency = 1.0 // unit of time -> double

    private var noiseInstance = OpenSimplexNoise(seed)
    fun getNoise(): OpenSimplexNoise {
        if(noiseInstance.seed != seed) noiseInstance = OpenSimplexNoise(seed)
        return noiseInstance
    }

    override fun getValue(time: Double): Double {
        val falloff = falloff[time]
        val octaves = clamp(octaves, 0, 16)
        val relativeValue = getValue((time * frequency), getNoise(), falloff.toDouble(), octaves) / getMaxValue(falloff, min(octaves, 10))
        return baseValue[time] + max(amplitude[time], 0f) * relativeValue
    }

    // recursion isn't the best... but whatever...
    fun getMaxValue(falloff: Float, octaves: Int): Float = if(octaves >= 0) 1f else 1f + falloff * getMaxValue(falloff,octaves-1)

    fun getValue(time: Double, noise: OpenSimplexNoise, falloff: Double, step: Int): Double {
        var value0 = noise.eval(time, step.toDouble())
        if(step > 0) value0 += falloff * getValue(2.0 * time, noise, falloff, step-1)
        return value0
    }

    override fun getClassName() = "PerlinNoiseDriver"
    override fun getDisplayName() = "Noise"

    override fun createInspector(transform: Transform, style: Style): List<Panel> {
        val components = ArrayList<Panel>()
        components += IntInput("Octaves", octaves.toFloat(), style)
            .setChangeListener { octaves = it.toInt() }
            .setIsSelectedListener { show(null) }
        components += LongInput("Seed", seed.toFloat(), style)
            .setChangeListener { octaves = it.toInt() }
            .setIsSelectedListener { show(null) }
        components += transform.VI("Falloff", "Changes high-frequency weight", falloff, style)
        components += transform.VI("Value", "The base value", baseValue, style)
        components += transform.VI("Amplitude", "The scale of this effect", amplitude, style)
        components += FloatInput("Frequency", frequency, style)
            .setIsSelectedListener { show(null) }
            .setChangeListener { frequency = it }
        return components
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeLong("seed", seed, true)
        writer.writeInt("octaves", octaves, true)
        writer.writeObject(this,"falloff", falloff)
        writer.writeObject(this, "minValue", baseValue)
        writer.writeObject(this, "maxValue", amplitude)
        writer.writeDouble("frequency", frequency)
    }

    override fun readInt(name: String, value: Int) {
        when(name){
            "octaves" -> octaves = clamp(value, 0, MAX_OCTAVES)
            else -> super.readInt(name, value)
        }
    }

    override fun readLong(name: String, value: Long) {
        when(name){
            "seed" -> seed = value
            else -> super.readLong(name, value)
        }
    }

    override fun readDouble(name: String, value: Double) {
        when(name){
            "frequency" -> frequency = value
            else -> super.readDouble(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "falloff" -> falloff.copyFrom(value)
            "minValue" -> this.baseValue.copyFrom(value)
            "maxValue" -> amplitude.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    companion object {
        val MAX_OCTAVES = 32
    }

}