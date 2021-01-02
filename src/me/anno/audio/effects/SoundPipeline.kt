package me.anno.audio.effects

import me.anno.audio.effects.falloff.ExponentialFalloff
import me.anno.audio.effects.falloff.LinearFalloff
import me.anno.audio.effects.falloff.SquareFalloff
import me.anno.audio.effects.impl.*
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.objects.inspectable.Inspectable
import me.anno.studio.rems.RemsStudio
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.stacked.Option
import me.anno.ui.editor.stacked.StackPanel
import me.anno.ui.style.Style
import org.jtransforms.fft.FloatFFT_1D

class SoundPipeline() : Saveable(), Inspectable {

    lateinit var audio: Audio
    lateinit var camera: Camera

    constructor(audio: Audio): this(){
        this.audio = audio
    }

    fun option(generator: () -> SoundEffect): Option {
        val sample = generator()
        sample.audio = audio
        return Option(sample.displayName, sample.description, generator)
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {
        stages.forEach { it.audio = audio }
        val effectsGroup = getGroup("Effects", "effects")
        effectsGroup += object : StackPanel("Effects Stack", "Effects can be added with RMB, are applied one after another", listOf(
            option { EchoEffect(audio) },
            option { AmplitudeEffect(audio) },
            option { EqualizerEffect(audio) },
            option { PitchEffect(audio) },
            option { SquareFalloff(audio) },
            option { LinearFalloff(audio) },
            option { ExponentialFalloff(audio) }
        ), stages, style){
            override fun onAddComponent(component: Inspectable, index: Int) {
                component as SoundEffect
                RemsStudio.largeChange("Add ${component.displayName}"){
                    if(index >= stages.size){
                        stages.add(component)
                    } else {
                        stages.add(index, component)
                    }
                }
            }
            override fun onRemoveComponent(component: Inspectable) {
                component as SoundEffect
                RemsStudio.largeChange("Remove ${component.displayName}"){
                    stages.remove(component)
                }
            }

            override fun getOptionFromInspectable(inspectable: Inspectable): Option? {
                return if(inspectable is SoundEffect){
                    option { inspectable }
                } else null
            }
        }
    }

    val stages = ArrayList<SoundEffect>()

    val fft = FloatFFT_1D(bufferSize.toLong())

    val input = FloatArray(bufferSize)

    fun process(
        data0: FloatArray,
        audio: Audio,
        inputDomain: Domain, outputDomain: Domain,
        time0: Time, time1: Time
    ): FloatArray {

        if (data0.size < bufferSize) {
            throw IllegalArgumentException("Input is too small!")
        } else if (data0.size > bufferSize) {
            val output = FloatArray(data0.size)
            for (offset in data0.indices step bufferSize) {
                System.arraycopy(data0, offset, input, 0, bufferSize)
                val solution = process(input, audio, inputDomain, outputDomain, time0, time1)
                System.arraycopy(solution, 0, output, offset, bufferSize)
            }
            return output
        }

        var data = data0
        var domain = inputDomain
        for (stage in stages) {
            if (stage.inputDomain != domain) {
                when (stage.inputDomain) {
                    Domain.TIME_DOMAIN -> fft.realInverse(data, true)
                    Domain.FREQUENCY_DOMAIN -> fft.realForward(data)
                }
            }
            data = stage.apply(data, audio, camera, time0, time1)
            domain = stage.outputDomain
        }

        if (outputDomain != domain) {
            when (outputDomain) {
                Domain.TIME_DOMAIN -> fft.realInverse(data, true)
                Domain.FREQUENCY_DOMAIN -> fft.realForward(data)
            }
        }

        return data

    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        for (stage in stages) {
            writer.writeObject(this, "stage", stage)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "stage" -> {
                if(value is SoundEffect){
                    stages.add(value)
                }
            }
            else -> super.readObject(name, value)
        }
    }

    override fun getClassName() = "SoundPipeline"
    override fun getApproxSize() = 100
    override fun isDefaultValue() = stages.isEmpty()

    fun clone(): SoundPipeline {
        val copy = SoundPipeline(audio)
        copy.stages.addAll(stages.map { it.clone() })
        return copy
    }

    companion object {
        // 1024 (48Hz .. 48kHz) or 2048? (24Hz .. 48kHz)
        const val bufferSize = 2048
        const val bufferSizeM1 = bufferSize - 1
    }


}