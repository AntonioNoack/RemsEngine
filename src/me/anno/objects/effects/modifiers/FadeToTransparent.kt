package me.anno.objects.effects.modifiers

import me.anno.objects.Transform
import me.anno.utils.Color

/**
 * fade to color can be done with background plane and color
 * todo easier way than that??
 *
 * todo complex effects that change the visuals by using objects???
 * todo e.g. a pixelation effect could be applied using a modifier...
 *
 * todo this would need to modify the draw calls in the easiest case
 * just make shortcuts for most effects, to add them to existing object structures?
 * for fading, this won't work well, because we need to be able to still cut the video, and let remain the effect
 * todo we need shortcuts for complex effects, which change the structure, and simple effects,
 * todo which can be applied directly, and moved with cuts without issue
 *
 * */
class FadeToTransparent: ModifierEffect<Color>(){

    override fun registerInKeyframes(transform: Transform) {
        TODO("Not yet implemented")
    }

    override fun apply(relativeTime: Double, strength: Double, value: Color): Color {
        value.w *= strength.toFloat()
        return value
    }

    override fun getClassName() = "FadeOutEffect"

}