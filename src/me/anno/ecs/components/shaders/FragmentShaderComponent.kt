package me.anno.ecs.components.shaders

import me.anno.gpu.shader.Shader

interface FragmentShaderComponent {

    fun getShaderComponent(env: ShaderEnvironment): String

    fun getShaderCodeState(): Any?

    fun bindUniforms(shader: Shader, env: ShaderEnvironment)

}