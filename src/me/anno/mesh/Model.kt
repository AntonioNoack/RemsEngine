package me.anno.mesh

import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Texture2D
import org.joml.Vector4f

class Model(val name: String, val meshes: List<Mesh>){
    var texture: Texture2D? = null
    var materialOverrides = HashMap<String, Vector4f>()
    fun flipV() = meshes.forEach { it.flipV() }
    fun draw(shader: Shader, alpha: Float, materialOverride: Map<String, Vector4f>){
        for(mesh in meshes) mesh.draw(shader, alpha, materialOverride)
    }
}