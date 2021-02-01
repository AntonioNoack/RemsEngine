package me.anno.audio.effects.impl

import me.anno.audio.effects.Domain
import me.anno.audio.effects.SoundEffect
import me.anno.audio.effects.SoundPipeline.Companion.bufferSize
import me.anno.audio.effects.Time
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.Maths.mix
import me.anno.utils.hpc.HeavyProcessing.processBalanced

// todo different distance based effects <3 :D
// todo velocity-based effects
// todo normalize amplitude effect
// todo limit amplitude effect (straight cut-off; smooth-cut-off)

class AmplitudeEffect() : SoundEffect(Domain.TIME_DOMAIN, Domain.TIME_DOMAIN) {

    override fun apply(data: FloatArray, source: Audio, destination: Camera, time0: Time, time1: Time): FloatArray {

        val amplitude = source.amplitude
        if (!amplitude.isAnimated && amplitude.drivers[0] == null) {

            val singleMultiplier = amplitude[time0.localTime]
            for (i in 0 until bufferSize) data[i] *= singleMultiplier

        } else {

            val t0 = time0.localTime
            val t1 = time1.localTime
            processBalanced(0, bufferSize, false) { i0, i1 ->
                for (i in i0 until i1) {
                    data[i] *= amplitude[mix(t0, t1, i.toDouble() / bufferSize)]
                }
            }

        }
        return data

    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        list += SpacePanel(0, 0, style) // nothing, but it must not be empty, soo...
    }

    override fun clone(): SoundEffect {
        return AmplitudeEffect()
    }

    override val displayName: String = "Amplitude"
    override val description: String = "Changes the volume"
    override fun getClassName() = "AmplitudeEffect"

}