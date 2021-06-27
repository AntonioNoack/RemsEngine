package me.anno.mesh.assimp

class Animation(val name: String, val frames: List<AnimatedFrame>, val duration: Double) {
    // var currentFrame = 0
    init {
        println("animation $name: ${frames.size} frames over $duration s")
    }
}
