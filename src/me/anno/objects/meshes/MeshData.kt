package me.anno.objects.meshes

import me.anno.gpu.GFX
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.ShaderLib.shaderObjMtl
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.gpu.texture.ClampMode
import me.anno.gpu.texture.FilteringMode
import me.anno.gpu.texture.Texture2D
import me.anno.objects.cache.Cache
import me.anno.objects.cache.CacheData
import me.anno.objects.meshes.obj.Material
import me.anno.video.MissingFrameException
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import java.io.File

class MeshData: CacheData {

    lateinit var toDraw: Map<Material, StaticFloatBuffer>

    fun getTexture(file: File?, defaultTexture: Texture2D): Texture2D {
        if(file == null) return defaultTexture
        val tex = Cache.getImage(file, 1000, true)
        if(tex == null && isFinalRendering) throw MissingFrameException(file)
        return tex ?: defaultTexture
    }

    fun draw(stack: Matrix4fArrayList, time: Double, color: Vector4f){
        for((material, buffer) in toDraw){
            val shader = shaderObjMtl.shader
            GFX.shader3DUniforms(shader, stack, 1, 1, color, null, FilteringMode.NEAREST, null)
            getTexture(material.diffuseTexture, whiteTexture).bind(0, false, ClampMode.CLAMP)
            buffer.draw(shader)
            GFX.check()
        }
    }

    override fun destroy() {
        toDraw.entries.forEach {
            it.value.destroy()
        }
    }

}