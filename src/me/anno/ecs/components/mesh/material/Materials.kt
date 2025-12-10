package me.anno.ecs.components.mesh.material

import me.anno.cache.FileCacheList
import me.anno.ecs.components.mesh.material.Material.Companion.defaultMaterial
import me.anno.gpu.FinalRendering
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import org.joml.Vector2f

object Materials {

    var lodBias = 0f
    val jitterInPixels = Vector2f()

    fun reset() {
        lodBias = 0f
        jitterInPixels.set(0f)
    }

    fun getMaterial(
        materialOverrides: List<FileReference>?,
        materials: List<FileReference>, index: Int
    ): Material = getMaterialOrNull(materialOverrides, materials, index) ?: defaultMaterial

    fun getMaterial(
        materials: List<FileReference>, index: Int
    ): Material = getMaterialOrNull(materials, index) ?: defaultMaterial

    fun getMaterialOrNull(
        materialOverrides: List<FileReference>?,
        materials: List<FileReference>, index: Int
    ): Material? {
        return getMaterialOrNull(materialOverrides, index)
            ?: getMaterialOrNull(materials, index)
    }

    fun getMaterialOrNull(materials: List<FileReference>?, index: Int): Material? {
        val ref = materials?.getOrNull(index) ?: return null
        return when {
            index !in materials.indices -> null
            materials is FileCacheList<*> -> materials.getValue(index) as? Material
            FinalRendering.isFinalRendering -> MaterialCache.getEntry(ref).waitFor()
            else -> MaterialCache[ref]
        }
    }

    fun getMaterial(
        superMaterialIfShaderNull: Material?,
        materialOverrides: List<FileReference>?,
        materials: List<FileReference>,
        index: Int
    ): Material {
        val mat1 = getMaterial(materialOverrides, materials, index)
        return getMaterial(superMaterialIfShaderNull, mat1)
    }

    fun getMaterial(
        superMaterialIfShaderNull: Material?,
        material: Material,
    ): Material {
        return if (superMaterialIfShaderNull != null && material.shader == null) superMaterialIfShaderNull
        else material
    }

    fun getMaterialRef(
        materialOverrides: List<FileReference>?,
        materials: List<FileReference>,
        index: Int
    ): FileReference {
        return getMaterialRef(materialOverrides, index).nullIfUndefined()
            ?: getMaterialRef(materials, index)
    }

    fun getMaterialRef(materials: List<FileReference>?, index: Int): FileReference {
        return materials?.getOrNull(index)?.nullIfUndefined() ?: InvalidRef
    }
}