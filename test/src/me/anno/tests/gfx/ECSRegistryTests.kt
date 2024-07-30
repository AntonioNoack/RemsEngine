package me.anno.tests.gfx

import me.anno.engine.ECSRegistry
import me.anno.jvm.HiddenOpenGLContext
import me.anno.gpu.shader.ShaderLib

fun initWithGFX(w: Int = 512, h: Int = w) {
    HiddenOpenGLContext.createOpenGL(w, h)
    ECSRegistry.init()
}