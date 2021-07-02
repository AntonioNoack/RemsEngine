package me.anno.mesh.assimp

class Animation(val name: String, val frames: Array<AnimatedFrame>, val duration: Double) {

    // var currentFrame = 0
    init {
        println("animation $name: ${frames.size} frames over $duration s")
    }

    override fun toString(): String {
        return "Animation('$name', $duration s):\n${frames.joinToString()}"
    }
}
