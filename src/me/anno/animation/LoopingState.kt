package me.anno.animation

import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp

enum class LoopingState(val id: Int, val naming: NameDesc) {
    PLAY_ONCE(0, NameDesc("Once")) {
        override fun get(time: Float, duration: Float): Float {
            return clamp(time, 0f, duration)
        }

        override fun get(time: Double, duration: Double): Double {
            return clamp(time, 0.0, duration)
        }

        override fun get(time: Int, duration: Int): Int {
            return clamp(time, 0, duration)
        }

        override fun get(time: Long, duration: Long): Long {
            return clamp(time, 0, duration)
        }
    },
    PLAY_LOOP(1, NameDesc("Looping")) {

        override fun get(time: Float, duration: Float): Float {
            val t = time % duration
            return if (t < 0f) t + duration else t
        }

        override fun get(time: Double, duration: Double): Double {
            val t = time % duration
            return if (t < 0.0) t + duration else t
        }

        override fun get(time: Int, duration: Int): Int {
            val t = time % duration
            return if (t < 0) t + duration else t
        }

        override fun get(time: Long, duration: Long): Long {
            val t = time % duration
            return if (t < 0) t + duration else t
        }
    },
    PLAY_REVERSING_LOOP(2, NameDesc("Reversing")) {

        override fun get(time: Float, duration: Float): Float {
            val doubleDuration = 2f * duration
            val time0 = time % doubleDuration
            return if (time0 >= duration) {
                // reverse
                doubleDuration - time0
            } else {
                // play usually
                time0
            }
        }

        override fun get(time: Double, duration: Double): Double {
            val doubleDuration = 2.0 * duration
            val time0 = time % doubleDuration
            return if (time0 >= duration) {
                // reverse
                doubleDuration - time0
            } else {
                // play usually
                time0
            }
        }

        override fun get(time: Int, duration: Int): Int {
            val doubleDuration = 2 * duration
            val time1 = time % doubleDuration
            return if (time1 >= duration) {
                // reverse
                doubleDuration - time1
            } else {
                // play usually
                time1
            }
        }

        override fun get(time: Long, duration: Long): Long {
            val doubleDuration = 2 * duration
            val time1 = time % doubleDuration
            return if (time1 >= duration) {
                // reverse
                doubleDuration - time1
            } else {
                // play usually
                time1
            }
        }
    };

    abstract operator fun get(time: Float, duration: Float): Float
    abstract operator fun get(time: Double, duration: Double): Double
    abstract operator fun get(time: Int, duration: Int): Int
    abstract operator fun get(time: Long, duration: Long): Long

    companion object {
        fun getState(id: Int) = values().firstOrNull { it.id == id } ?: PLAY_ONCE
    }

}