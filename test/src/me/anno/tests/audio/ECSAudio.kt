package me.anno.tests.audio

import me.anno.ecs.Entity
import me.anno.ecs.components.audio.AudioComponent
import me.anno.ecs.components.audio.AudioComponentBase
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.extensions.ExtensionLoader
import me.anno.utils.OS.music

fun main() {

    OfficialExtensions.register()
    ExtensionLoader.load()
    ECSRegistry.init()

    // test 2d and 3d audio in a scene:
    // - local speaker
    // - 2d background music
    val scene = Entity()
    scene.add(AudioComponent().apply {
        // todo audio seems to break with very high distances??? -> clamp them
        name = "Local"
        rollOffFactor = 1f
        referenceDistance = 1f
        maxDistance = 10f
        source = music.getChild("Nightcore - Story Of Love.mp3")
        playMode = AudioComponentBase.PlayMode.LOOP
        start()
    })
    scene.add(AudioComponent().apply {
        name = "Global"
        volume = 0.5f
        rollOffFactor = 0f
        source = music.getChild("Sabrina Carpenter - Thumbs.mp3")
        playMode = AudioComponentBase.PlayMode.LOOP
        start()
    })
    testSceneWithUI("ECSAudio", scene)
}