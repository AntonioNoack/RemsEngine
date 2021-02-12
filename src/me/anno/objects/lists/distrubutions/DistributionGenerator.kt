package me.anno.objects.lists.distrubutions

import me.anno.objects.Transform
import me.anno.objects.lists.ElementGenerator

abstract class DistributionGenerator: ElementGenerator() {

    lateinit var distribution: ElementDistribution
    override fun generateEntry(index: Int): Transform? = distribution.generateEntry(index)

}