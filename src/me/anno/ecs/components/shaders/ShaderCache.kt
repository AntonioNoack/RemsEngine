package me.anno.ecs.components.shaders

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.ShaderLib.createShader
import me.anno.gpu.shader.builder.Variable

object ShaderCache : CacheSection("Shader") {

    data class ShaderKey(val vertex: String, val fragment: String, val textures: List<String>)

    class ShaderData(shader: BaseShader) : CacheData<BaseShader>(shader) {
        override fun destroy() {
            value.destroy()
        }
    }

    fun getShader(
        name: String,
        vertex: String,
        varying: List<Variable>,
        fragment: String,
        textures: List<String>
    ): ShaderData {
        return getEntry(ShaderKey(vertex, fragment, textures), timeout, false) {
            ShaderData(createShader(name, vertex, varying, fragment, textures))
        } as ShaderData
    }

    private val timeout = 30_000L

}