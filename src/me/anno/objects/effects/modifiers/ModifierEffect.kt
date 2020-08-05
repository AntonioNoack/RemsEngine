package me.anno.objects.effects.modifiers

import me.anno.io.Saveable
import me.anno.objects.Transform

// todo effect order?
abstract class ModifierEffect<V>: Saveable(){

    abstract fun registerInKeyframes(transform: Transform)
    abstract fun apply(relativeTime: Double, strength: Double, value: V): V

    override fun isDefaultValue() = false
    override fun getApproxSize() = 4

}