package me.anno.mesh

import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Texture2D
import me.anno.utils.AnyToFloat.get
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f

class Model(val name: String, val meshes: List<Mesh>) {

    val localTranslation = Vector3f()
    val localRotation = Vector3f()
    val localScale = Vector3f(1f)
    val pivot = Vector3f()

    private val bounds = AABBf()
    fun getBounds(): AABBf {
        if (bounds.maxX < bounds.minX){
            for(mesh in meshes){
                bounds.union(mesh.getBounds())
            }
        }
        return bounds
    }

    var texture: Texture2D? = null
    var materialOverrides = HashMap<String, Vector4f>()

    fun flipV() = meshes.forEach { it.flipV() }

    fun switchYZ() {
        meshes.forEach { it.switchYZ() }
        localTranslation.set(localTranslation.x, localTranslation.z, -localTranslation.y)
        localRotation.set(localRotation.x, localRotation.z, -localRotation.y)
        localScale.set(localScale.x, localScale.z, -localScale.y)
    }

    fun switchXZ() {
        meshes.forEach { it.switchXZ() }
        localTranslation.set(localTranslation.z, localTranslation.y, -localTranslation.x)
        localRotation.set(localRotation.z, localRotation.y, -localRotation.x)
        localScale.set(localScale.z, localScale.y, -localScale.x)
    }

    fun draw(shader: Shader, alpha: Float, materialOverride: Map<String, Vector4f>) {
        for (mesh in meshes) mesh.draw(shader, alpha, materialOverride)
    }

    fun scale(scale: Float) {
        meshes.forEach { it.scale(scale) }
        localTranslation.mul(scale)
        // rotation doesn't need scale
        // scale the scale???
    }

    override fun toString() = "$name: $meshes"
}