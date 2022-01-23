package me.anno.ecs.components.audio

// todo audio system
// todo some kind of event system for when music changed

class AudioComponent {

    enum class PlayMode {
        ONCE,
        LOOP
    }

    enum class Attenuation {
        LINEAR, EXPONENTIAL,
        GLOBAL,
    }

}