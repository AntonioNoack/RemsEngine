package me.anno.tests.audio

import me.anno.ecs.components.audio.AudioComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.zip.InnerTmpFile
import me.anno.maths.Maths.TAUf
import me.anno.studio.StudioBase.Companion.addEvent
import kotlin.math.sin

fun main() {
    val scene = AudioComponent()
    scene.source = object : InnerTmpFile.InnerTmpAudioFile() {

        val mainFrequency = TAUf * 500f
        val modulator = TAUf * 10f

        override fun sample(time: Double, channel: Int): Short {
            val sound = sin(time * mainFrequency) * sin(time * modulator)
            return (sound * 32767).toInt().toShort()
            // return (time * 320 * 65000).toLong().toShort()
        }

    }

    // complex way:
    addEvent { scene.start() }
    testSceneWithUI("Procedural Audio", scene)

}