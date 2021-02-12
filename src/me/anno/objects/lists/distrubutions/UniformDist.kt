package me.anno.objects.lists.distrubutions

import me.anno.objects.Transform
import java.util.*

abstract class UniformDist: ElementDistribution(){

    var random = Random()
    lateinit var list: FiniteLinearDist

    override fun generateEntry(index: Int): Transform? {
        return list.generateEntry(random.nextInt(list.getSize()))
    }

}