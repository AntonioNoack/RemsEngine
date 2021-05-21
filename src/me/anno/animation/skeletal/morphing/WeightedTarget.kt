package me.anno.animation.skeletal.morphing

data class WeightedTarget(val target: MorphTarget, val weight: Float) {

    val isUseless = weight <= 1e-3f || target.maxIndex == 0
    var nextIndex = 0

    operator fun times(multiplier: Float) = WeightedTarget(target, weight * multiplier)

}