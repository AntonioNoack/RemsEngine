package me.anno.objects.lists.distrubutions

import me.anno.objects.Transform
import me.anno.objects.particles.Particle

abstract class ElementDistribution: Transform() {
    abstract fun generateEntry(index: Int): Transform?
}