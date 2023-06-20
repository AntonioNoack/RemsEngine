package me.anno.gpu

import org.lwjgl.opengl.GL11C.GL_BACK
import org.lwjgl.opengl.GL11C.GL_FRONT

enum class CullMode(val id: Int, val opengl: Int) {

    /**
     * only back side will be visible
     * */
    BACK(-1, GL_BACK),

    /**
     * both sides will be visible
     * */
    BOTH(0, 0),

    /**
     * only front side will be visible
     * */
    FRONT(1, GL_FRONT);

    operator fun times(mode: CullMode): CullMode {
        return values[id * mode.id + 1]
    }

    companion object {
        private val values = values()
    }
}