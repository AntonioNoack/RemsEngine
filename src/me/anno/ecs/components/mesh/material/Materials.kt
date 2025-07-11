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

    @Deprecated("Please use cacheMaterials instead of materials")
    fun getMaterial(
        materialOverrides: List<FileReference>?,
        materials: List<FileReference>, index: Int
    ): Material = getMaterialOrNull(materialOverrides, materials, index) ?: defaultMaterial

    @Deprecated("Please use cacheMaterials instead of materials")
    fun getMaterialOrNull(
        materialOverrides: List<FileReference>?,
        materials: List<FileReference>, index: Int
    ): Material? {
        val ref = getMaterialRef(materialOverrides, materials, index)
        return if (FinalRendering.isFinalRendering) {
            MaterialCache.getEntry(ref).waitFor()
        } else {
            MaterialCache[ref]
        }
    }

    @Deprecated("Please use cacheMaterials instead of materials")
    fun getMaterial(
        materialOverride: Material?,
        materialOverrides: List<FileReference>?,
        materials: List<FileReference>,
        index: Int
    ): Material {
        val mat1 = getMaterial(materialOverrides, materials, index)
        return getMaterial(materialOverride, mat1)
    }

    fun getMaterial(
        materialOverrides: FileCacheList<Material>?,
        materials: FileCacheList<Material>, index: Int
    ): Material = getMaterialOrNull(materialOverrides, materials, index) ?: defaultMaterial

    fun getMaterialOrNull(
        materialOverrides: FileCacheList<Material>?,
        materials: FileCacheList<Material>, index: Int
    ): Material? {
        return when {
            FinalRendering.isFinalRendering -> {
                val ref = getMaterialRef(materialOverrides, materials, index)
                MaterialCache.getEntry(ref).waitFor()
            }
            materialOverrides != null && index in materialOverrides.indices -> {
                materialOverrides.getValue(index)
            }
            index in materials.indices -> {
                materials.getValue(index)
            }
            else -> null
        }
    }

    fun getMaterial(materials: FileCacheList<Material>, index: Int): Material =
        getMaterialOrNull(materials, index) ?: defaultMaterial

    fun getMaterialOrNull(materials: FileCacheList<Material>, index: Int): Material? {
        return when {
            FinalRendering.isFinalRendering -> {
                val ref = getMaterialRef(materials, index)
                MaterialCache.getEntry(ref).waitFor()
            }
            index in materials.indices -> {
                materials.getValue(index)
            }
            else -> null
        }
    }

    fun getMaterial(
        materialOverride: Material?,
        materialOverrides: FileCacheList<Material>?,
        materials: FileCacheList<Material>,
        index: Int
    ): Material {
        val mat1 = getMaterial(materialOverrides, materials, index)
        return getMaterial(materialOverride, mat1)
    }

    fun getMaterial(
        materialOverride: Material?,
        material: Material,
    ): Material {
        return if (materialOverride != null && material.shader == null) materialOverride
        else material
    }

    fun getMaterialRef(
        materialOverrides: List<FileReference>?,
        materials: List<FileReference>,
        index: Int
    ): FileReference {
        val m0 = materialOverrides?.getOrNull(index)?.nullIfUndefined()
        return m0 ?: getMaterialRef(materials, index)
    }

    fun getMaterialRef(materials: List<FileReference>, index: Int): FileReference {
        return materials.getOrNull(index)?.nullIfUndefined() ?: InvalidRef
    }
}