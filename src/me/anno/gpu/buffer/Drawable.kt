package me.anno.gpu.buffer

import me.anno.gpu.shader.Shader

interface Drawable {
    fun draw(shader: Shader)
    fun drawInstanced(shader: Shader, instanceData: Buffer)
    fun drawInstanced(shader: Shader, instanceCount: Int)
}