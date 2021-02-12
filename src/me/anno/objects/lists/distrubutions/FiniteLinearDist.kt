package me.anno.objects.lists.distrubutions

import me.anno.objects.Transform

abstract class FiniteLinearDist (): ElementDistribution(){

    abstract fun getSize(): Int
    abstract fun getEntry(index: Int): Transform?

    override fun generateEntry(index: Int): Transform? {
        return if(index in 0 until getSize()) getEntry(index) else null
    }

}