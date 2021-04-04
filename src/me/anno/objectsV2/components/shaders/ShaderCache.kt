package me.anno.objectsV2.components.shaders

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.gpu.ShaderLib.createShader
import me.anno.gpu.shader.Shader

object ShaderCache: CacheSection("Shader") {

    class ShaderData(shader: Shader): CacheData<Shader>(shader) {
        override fun destroy() {
            value.destroy()
        }
    }

    fun getShader(name: String, vertex: String, varying: String, fragment: String, textures: List<String>): ShaderData {
        return getEntry(vertex to fragment, timeout, false){
            ShaderData(createShader(name, vertex, varying, fragment, textures))
        } as ShaderData
    }

    private val timeout = 30_000L

}