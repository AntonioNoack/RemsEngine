package me.anno.objects.lists.distrubutions

import me.anno.objects.Transform
import java.util.*

abstract class WeightedDist : ElementDistribution() {

    lateinit var list: FiniteLinearDist
    var random = Random()
    var weights = ArrayList<Float>()

    private var cummulative = FloatArray(0)
    private var cachedSize = 0
    private var weightSum = 1f
    fun invalidate() {
        val size = list.getSize()
        cachedSize = size
        while (weights.size < size) weights.add(1f)
        weightSum = weights.subList(0, size).sum()
        cummulative = FloatArray(size){ weights[it]/weightSum }
        for (i in 1 until size) {
            cummulative[i] += cummulative[i-1]
        }
    }

    override fun generateEntry(index: Int): Transform? {
        if(cachedSize <= 0) return null
        val randValue = random.nextFloat()
        var index2 = cummulative.binarySearch(randValue)
        if(index2 <0) index2 = -index2-1
        if(index2 >= cachedSize) index2 = cachedSize-1 // should rarely happen
        return list.generateEntry(index2)
    }

}