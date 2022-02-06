package me.anno.remsstudio.objects.modes

import me.anno.language.translation.NameDesc

enum class LoopingState(val id: Int, val naming: NameDesc){
    PLAY_ONCE(0, NameDesc("Once")),
    PLAY_LOOP(1, NameDesc("Looping")),
    PLAY_REVERSING_LOOP(2, NameDesc("Reversing"));

    operator fun get(time: Double, duration: Double) = when(this){
        PLAY_ONCE -> time
        PLAY_LOOP -> time % duration
        PLAY_REVERSING_LOOP -> {
            val time0 = time % (2 * duration)
            if(time0 >= duration){
                // reverse
                2 * duration - time0
            } else {
                // play usually
                time0
            }
        }
    }

    operator fun get(index: Long, maxSampleIndex: Long) = when(this){
        PLAY_ONCE -> index
        PLAY_LOOP -> index % maxSampleIndex
        PLAY_REVERSING_LOOP -> {
            val index0 = index % (2 * maxSampleIndex)
            if(index0 >= maxSampleIndex){
                // reverse
                2 * maxSampleIndex - index0
            } else {
                // play usually
                index0
            }
        }
    }

    companion object {
        fun getState(id: Int) = values().firstOrNull { it.id == id } ?: PLAY_ONCE
    }

}