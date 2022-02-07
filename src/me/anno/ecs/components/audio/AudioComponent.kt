package me.anno.ecs.components.audio

// todo audio system
// todo some kind of event system for when music changed

// todo there need to be multiple types: procedural & loaded
// todo define distance function

// todo effects maybe? mmh... procedural audio!
// todo what's the best way to compute distance to the main listener? world position

// todo what's the best way to combine audio for local multiplayer? we could use stereo a little for that :)
// (should be disable-able)

// todo implement things

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