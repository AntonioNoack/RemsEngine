package me.anno.tests.audio

import me.anno.Build
import me.anno.audio.remfx.Note
import me.anno.audio.remfx.RemFXPlayer
import me.anno.audio.remfx.effect.BirdChirps
import me.anno.audio.remfx.effect.NoisyBang
import me.anno.audio.remfx.effect.NoteSound
import me.anno.audio.remfx.effect.ReverbEffect
import me.anno.audio.remfx.effect.WhiteNoiseSound
import me.anno.audio.remfx.effect.WoodpeckerDrum
import me.anno.audio.remfx.sound.PerlinModulator
import me.anno.audio.remfx.sound.PlayedModulator
import me.anno.audio.remfx.sound.SpacialModulator
import me.anno.audio.streams.AudioStreamRaw.Companion.bufferSize
import me.anno.ecs.Entity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.input.Key

/**
 * creates a RemFX player with some effects bound to keys
 * */
fun main() {

    bufferSize = 512
    Build.isDebug = true

    val scene = Entity()
    scene.add(RemFXPlayer().apply {
        sounds.add(PlayedModulator(Key.KEY_M, WhiteNoiseSound())) // like NoisyBang, just much brighter
        if (false) sounds.add(SpacialModulator(NoteSound(Note.C))) // spatial test
        if (false) sounds.add(PerlinModulator(WhiteNoiseSound()).apply { maxVolume = 0.1f }) // wind test
        sounds.add(PlayedModulator(Key.KEY_F, NoteSound(Note.C)))
        sounds.add(PlayedModulator(Key.KEY_G, NoteSound(Note.D)))
        sounds.add(PlayedModulator(Key.KEY_H, NoteSound(Note.E)))
        sounds.add(PlayedModulator(Key.KEY_J, NoteSound(Note.F)))
        sounds.add(PlayedModulator(Key.KEY_K, NoteSound(Note.G)))
        sounds.add(PlayedModulator(Key.KEY_L, NoteSound(Note.A)))
        sounds.add(PlayedModulator(Key.KEY_O, NoisyBang()))

        // Short, upward “tweet”
        val c0 = BirdChirps(
            startHz = 1800f,
            endHz = 3000f,
            durationSeconds = 0.09f,
            sweepPower = 0.7f
        )
        sounds.add(PlayedModulator(Key.KEY_N, c0))

        // Downward, slightly longer call
        val c1 = BirdChirps(
            startHz = 2800f,
            endHz = 1500f,
            durationSeconds = 0.22f,
            sweepPower = 1.4f
        )
        sounds.add(PlayedModulator(Key.KEY_B, c1))

        // Nearly flat whistle
        val c2 = BirdChirps(
            startHz = 2200f,
            endHz = 2250f,
            durationSeconds = 0.35f,
            spacing = 0f,
            runs = 1,
        )
        sounds.add(PlayedModulator(Key.KEY_V, c2))

        val c3 = WoodpeckerDrum()
        sounds.add(PlayedModulator(Key.KEY_C, c3))

        sounds.add(
            PlayedModulator(
                Key.KEY_P,
                ReverbEffect(NoteSound(Note.A)).apply {
                    numReverbs = 3
                    timeOffset = 0.1f
                })
        )
    })
    testSceneWithUI("Procedural Audio", scene)
}