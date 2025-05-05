package me.anno.tests.gfx.nanite

import me.anno.gpu.buffer.AttributeLayout
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.shader.Shader

data class ComputeShaderKey(
    val shader: Shader,
    val target: DeferredSettings?,
    val meshAttr: AttributeLayout,
    val instAttr: AttributeLayout,
    val indexType: AttributeType?,
    val drawMode: DrawMode
)
