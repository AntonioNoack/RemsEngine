package me.anno.tests.audio

import me.anno.ecs.components.audio.AudioComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.inner.temporary.InnerTmpAudioFile
import me.anno.maths.Maths.TAUf
import kotlin.math.sin

/**
 * creates an AudioComponent using a InnerTmpAudioFile,
 * where you can define your logic to create sound
 * */
fun main() {
    // todo how is it soo loud at the start, and then nearly silent??
    val scene = AudioComponent()
    scene.source = object : InnerTmpAudioFile() {

        val mainFrequency = TAUf * 500f
        val modulator = TAUf * 10f

        override fun sample(time: Double, channel: Int): Short {
            if(Math.random() < 0.01) println(time)
            val sound = sin(time * mainFrequency) * sin(time * modulator)
            return (sound * 32767).toInt().toShort()
        }
    }
    scene.autoStart = true
    testSceneWithUI("Procedural Audio", scene)
}