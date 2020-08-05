package me.anno.objects.effects.modifiers

enum class EffectLocation(val id: Int, val location: Int, val isFixed: Boolean){
    START(0, 0, true), END(1, 1, true),
    // todo merge them?, automatically assign?
    IN_BETWEEN(2, 0, false)
}