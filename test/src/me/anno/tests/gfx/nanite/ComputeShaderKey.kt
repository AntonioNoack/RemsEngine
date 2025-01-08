package me.anno.tests.gfx.nanite

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.shader.Shader

data class ComputeShaderKey(
    val shader: Shader,
    val target: DeferredSettings?,
    val meshAttr: List<Attribute>,
    val instAttr: List<Attribute>,
    val indexType: AttributeType?,
    val drawMode: DrawMode
)
