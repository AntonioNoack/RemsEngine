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
    // todo UI gets stuck???
    val scene = AudioComponent()
    scene.source = object : InnerTmpAudioFile() {

        val mainFrequency = TAUf * 500f
        val modulator = TAUf * 10f

        override fun sample(time: Double, channel: Int): Short {
            val sound = sin(time * mainFrequency) * sin(time * modulator)
            return (sound * 32767).toInt().toShort()
            // return (time * 320 * 65000).toLong().toShort()
        }
    }
    scene.autoStart = true
    testSceneWithUI("Procedural Audio", scene)
}