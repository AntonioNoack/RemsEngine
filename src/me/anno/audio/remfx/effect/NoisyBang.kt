package me.anno.audio.remfx.effect

import me.anno.maths.Maths.clamp
import me.anno.audio.remfx.RemFXContext
import kotlin.random.Random

class NoisyBang : Sound() {

    private val random = Random(1234)

    var w = 0.3f
    var curr = 0f

    override fun add(context: RemFXContext, left: ShortArray, right: ShortArray) {
        var curr = curr
        for (i in context.indices) {
            val value = random.nextFloat() - 0.5f
            curr += value * w
            curr = clamp(curr, -1f, +1f)
            add(context, curr, i, left, right)
        }
        this.curr = curr
    }
}