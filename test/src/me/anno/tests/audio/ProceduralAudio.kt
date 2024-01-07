package me.anno.tests.audio

import me.anno.ecs.Component
import me.anno.ecs.Entity
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
    // how is it soo loud at the start, and then nearly silent??
    // - because it is placed at 0,0,0, like the camera is before the first frame;
    // - then the camera gets placed properly, and the distance to it gets much larger

    // it's fun to play with the audio speed (pitch), try it xD
    val mainFrequency = TAUf * 500f
    val modulator = TAUf * 10f
    testProcedural { time ->
        sin(time * mainFrequency) * sin(time * modulator)
    }
}

fun testProcedural(audio: (Double) -> Double) {
    return testProcedural(null, audio)
}

fun testProcedural(component: Component?, generator: (time: Double) -> Double) {
    val scene = Entity()
    val audio = AudioComponent()
    audio.source = object : InnerTmpAudioFile() {
        override fun sample(time: Double, channel: Int): Short {
            return (generator(time) * 32767).toInt().toShort()
        }
    }
    audio.rollOffFactor = 0f // global
    audio.autoStart = true
    scene.add(audio)
    if (component != null) scene.add(component)
    testSceneWithUI("Procedural Audio", scene)
}