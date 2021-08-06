package me.anno.utils.test

import me.anno.cache.Cache
import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib
import me.anno.gpu.TextureLib
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.texture.Texture2D
import me.anno.ui.editor.files.thumbs.Thumbs.generateAssimpMeshFrame
import me.anno.utils.OS
import me.anno.utils.Sleep
import org.lwjgl.opengl.GL30

fun main() {

    // like Rem's CLI instantiate OpenGL
    HiddenOpenGLContext.setSize(32, 32)
    HiddenOpenGLContext.createOpenGL()
    TextureLib.init()
    ShaderLib.init()
    DefaultConfig.init()

    var isDone = false

    // actual code
    val src = OS.documents.getChild("sphere.obj")
    val dst = OS.desktop.getChild("sphere.png")
    generateAssimpMeshFrame(src, dst, 32) {
        isDone = true
    }

    while (!isDone) {
        Texture2D.destroyTextures()
        GFX.ensureEmptyStack()
        GFX.updateTime()
        Cache.update()
        Texture2D.bindTexture(GL30.GL_TEXTURE_2D, 0)
        GL30.glDisable(GL30.GL_CULL_FACE)
        GL30.glDisable(GL30.GL_ALPHA_TEST)
        GFX.check()
        Frame.reset()
        GFX.workGPUTasks(false)
        GFX.workEventTasks()
        Sleep.sleepABit(true)
    }

}