package me.anno.gfx

import me.anno.ecs.components.mesh.TypeValue

interface Shader {
    val bindings: HashMap<String, TypeValue>
    fun destroy()
}

interface GraphicsShader : Shader
interface ComputeShader : Shader