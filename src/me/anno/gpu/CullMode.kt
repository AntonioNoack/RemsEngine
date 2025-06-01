package me.anno.gpu

/**
 * Defines which sides of triangles shall be rendered: front / back / both.
 * You can use the times-function/operator to combine cull-modes
 * */
enum class CullMode(val id: Int) {

    /**
     * only back side will be visible
     * */
    BACK(-1),

    /**
     * both sides will be visible
     * */
    BOTH(0),

    /**
     * only front side will be visible
     * */
    FRONT(1);

    /**
     * Combines with a second cullMode.
     * BOTH always wins; null always loses; BACK flips the other value.
     * */
    operator fun times(mode: CullMode?): CullMode {
        if (mode == null) return this
        return entries[(ordinal - 1) * (mode.ordinal - 1) + 1]
    }
}