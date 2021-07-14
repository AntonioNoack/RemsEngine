package me.anno.mesh.assimp

import org.apache.logging.log4j.LogManager

class Animation(val name: String, val frames: Array<AnimationFrame>, val duration: Double) {

    // var currentFrame = 0
    init {
        LOGGER.debug("Loaded animation '$name' with ${frames.size} frames over $duration s")
    }

    override fun toString(): String {
        return "Animation('$name', $duration s):\n${frames.joinToString()}"
    }

    companion object {
        private val LOGGER = LogManager.getLogger(Animation::class)
    }

}
