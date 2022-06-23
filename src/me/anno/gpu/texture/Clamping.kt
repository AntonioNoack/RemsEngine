package me.anno.gpu.texture

import me.anno.language.translation.NameDesc
import org.lwjgl.opengl.GL14C.*

enum class Clamping(val id: Int, val naming: NameDesc, val mode: Int) {
    CLAMP(0, NameDesc("Clamp"), GL_CLAMP_TO_EDGE),
    REPEAT(1, NameDesc("Repeat"), GL_REPEAT),
    MIRRORED_REPEAT(2, NameDesc("Mirrored"), GL_MIRRORED_REPEAT)
}