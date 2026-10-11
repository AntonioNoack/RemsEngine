package me.anno.audio.remfx.sound

import me.anno.audio.remfx.RemFXContext
import me.anno.audio.remfx.effect.Sound

open class Modulator(val sound: Sound) {

    var left = 20e3f
    var right = 20e3f

    var lastLeft = 0f
    var lastRight = 0f

    fun setVolume(left: Float, right: Float = left): Modulator {
        this.left = left
        this.right = right
        return this
    }

    open fun update(context: RemFXContext) {}

}