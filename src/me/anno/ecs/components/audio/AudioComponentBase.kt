package me.anno.ecs.components.audio

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.prefab.PrefabSaveable

// todo audio system
// todo some kind of event system for when music changed

// todo there need to be multiple types: procedural & loaded
// todo define distance function

// todo effects maybe? mmh... procedural audio!
// todo what's the best way to compute distance to the main listener? world position

// todo what's the best way to combine audio for local multiplayer? we could use stereo a little for that :)
// (should be disable-able)

// todo implement things

abstract class AudioComponentBase : Component() {

    var playMode = PlayMode.ONCE
    var attenuation = Attenuation.CONSTANT

    var isRunning = false
    var isFinished = false

    var startTime = 0L

    enum class PlayMode {
        ONCE,
        LOOP
    }

    enum class Attenuation {
        LINEAR, EXPONENTIAL,
        CONSTANT,
    }

    @DebugAction
    fun start() {
        stop()
        startTime = Engine.nanoTime
        // todo start audio here
    }

    @DebugAction
    fun stop() {
        // todo stop audio here
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as AudioComponentBase
        clone.isRunning = isRunning
        clone.isFinished = isFinished
        clone.playMode = playMode
        clone.attenuation = attenuation
    }

    companion object {

    }

}