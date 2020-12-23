package me.anno.objects.distributions

import me.anno.gpu.buffer.Attribute

class DistAttr(val distribution: Distribution, val attr: Attribute) {
    operator fun component1() = distribution
    operator fun component2() = attr
}