package me.anno.audio.remfx.effect

import me.anno.audio.remfx.RemFXContext
import kotlin.random.Random

class WhiteNoiseSound : Sound() {

    private val random = Random(1234)

    override fun add(context: RemFXContext, left: ShortArray, right: ShortArray) {
        for (i in context.indices) {
            add(context, random.nextFloat() * 2f - 1f, i, left, right)
        }
    }
}