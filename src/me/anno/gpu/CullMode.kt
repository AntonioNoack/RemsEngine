package me.anno.gpu

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

    // for serialization
    // todo are getters supported?
    val id get() = ordinal - 1

    operator fun times(mode: CullMode): CullMode {
        return values[(ordinal - 1) * (mode.ordinal - 1) + 1]
    }

    companion object {
        private val values = values()
    }
}