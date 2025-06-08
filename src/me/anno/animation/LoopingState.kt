package me.anno.animation

import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.posMod

enum class LoopingState(val id: Int, val nameDesc: NameDesc) {
    PLAY_ONCE(0, NameDesc("Once")) {
        override fun get(time: Double, duration: Double): Double {
            return clamp(time, 0.0, duration)
        }

        override fun get(time: Long, duration: Long): Long {
            return clamp(time, 0, duration)
        }
    },

    /**
     * this plays on repeat
     * */
    PLAY_LOOP(1, NameDesc("Looping")) {
        override fun get(time: Double, duration: Double): Double {
            if (duration == 0.0) return time
            return posMod(time, duration)
        }

        override fun get(time: Long, duration: Long): Long {
            if (duration == 0L) return time
            return posMod(time, duration)
        }
    },

    /**
     * This plays forward, then backward, then repeat.
     * It's not supported everywhere, e.g., getting videos to play backwards isn't that common/straight forward.
     * */
    PLAY_REVERSING_LOOP(2, NameDesc("Reversing")) {
        override fun get(time: Double, duration: Double): Double {
            val doubleDuration = 2.0 * duration
            val time0 = posMod(time, doubleDuration)
            return if (time0 >= duration) {
                // reverse
                doubleDuration - time0
            } else {
                // play usually
                time0
            }
        }

        override fun get(time: Long, duration: Long): Long {
            val doubleDuration = 2 * duration
            val time1 = posMod(time, doubleDuration)
            return if (time1 >= duration) {
                // reverse
                doubleDuration - time1
            } else {
                // play usually
                time1
            }
        }
    };

    abstract operator fun get(time: Double, duration: Double): Double
    abstract operator fun get(time: Long, duration: Long): Long

    operator fun get(time: Float, duration: Float): Float {
        return get(time.toDouble(), duration.toDouble()).toFloat()
    }

    operator fun get(time: Int, duration: Int): Int {
        return get(time.toLong(), duration.toLong()).toInt()
    }

    companion object {
        @JvmStatic
        fun getById(id: Int) = entries.firstOrNull { it.id == id } ?: PLAY_ONCE
    }
}