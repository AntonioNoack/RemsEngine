package me.anno.gpu

import me.anno.engine.serialization.SerializedProperty

enum class CullMode {

    /**
     * only back side will be visible
     * */
    BACK,

    /**
     * both sides will be visible
     * */
    BOTH,

    /**
     * only front side will be visible
     * */
    FRONT;

    @SerializedProperty
    val id get() = ordinal - 1

    operator fun times(mode: CullMode): CullMode {
        return entries[(ordinal - 1) * (mode.ordinal - 1) + 1]
    }
}