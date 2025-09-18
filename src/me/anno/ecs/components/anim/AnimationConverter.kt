package me.anno.ecs.components.anim

import me.anno.ecs.prefab.Prefab
import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder
import me.anno.maths.MinMax.max
import org.joml.Matrix4x3f
import org.joml.Vector3f

object AnimationConverter {

    fun createImportedPrefab(
        skeletonRef: FileReference, duration: Float,
        skinningMatrices: List<List<Matrix4x3f>>
    ): Prefab {
        val prefab = Prefab("ImportedAnimation")
        prefab["skeleton"] = skeletonRef
        prefab["duration"] = duration
        prefab["frames"] = skinningMatrices
        return prefab
    }

    fun createAnimationPrefabs(
        animFolder: InnerFolder, importedPrefab: Prefab,
        globalTransform: Matrix4x3f?, globalInverseTransform: Matrix4x3f?
    ) {

        val skeletonRef = importedPrefab["skeleton"] as FileReference
        val duration = importedPrefab["duration"] as Float

        @Suppress("unchecked_cast")
        val skinningMatrices = importedPrefab["frames"] as List<List<Matrix4x3f>>
        val numFrames = skinningMatrices.size
        val numBones = skinningMatrices.firstOrNull()?.size ?: 0

        val boneByBonePrefab = createBoneByBonePrefab(
            skeletonRef, duration, skinningMatrices, numBones,
            globalTransform, globalInverseTransform
        )

        animFolder.createPrefabChild("Imported.json", importedPrefab)
        animFolder.createPrefabChild("BoneByBone.json", boneByBonePrefab)

        // create in-place animations (animations without root motion)
        val rootMotion = findRootMotion(skinningMatrices)
        val inplaceSkinning = removeRootMotionImported(skinningMatrices, rootMotion)
        val importedInPlacePrefab = createImportedPrefab(skeletonRef, duration, inplaceSkinning)
        val boneByBoneInPlacePrefab = removeRootMotionBoneByBone(boneByBonePrefab, rootMotion, numFrames, numBones)

        animFolder.createPrefabChild("Imported_InPlace.json", importedInPlacePrefab)
        animFolder.createPrefabChild("BoneByBone_InPlace.json", boneByBoneInPlacePrefab)
    }

    private fun findRootMotion(skinningMatrices: List<List<Matrix4x3f>>): Pair<Vector3f, Vector3f> {
        if (skinningMatrices.isEmpty() || skinningMatrices[0].isEmpty()) {
            return Pair(Vector3f(), Vector3f())
        }
        val boneId = 0
        val offset0 = skinningMatrices.first()[boneId].getTranslation(Vector3f())
        val offset1 = skinningMatrices.last()[boneId].getTranslation(Vector3f())
        return Pair(offset0, offset1)
    }

    private fun removeRootMotionImported(
        skinningMatrices: List<List<Matrix4x3f>>,
        rootMotion: Pair<Vector3f, Vector3f>
    ): List<List<Matrix4x3f>> {
        val offset = Vector3f()
        return skinningMatrices.indices.map { frameIndex ->
            val t = frameIndex.toFloat() / max(skinningMatrices.size - 1, 1)
            rootMotion.first.mix(rootMotion.second, t, offset).negate()
            skinningMatrices[frameIndex].map { boneMatrix ->
                boneMatrix.translateLocal(offset, Matrix4x3f())
            }
        }
    }

    private fun removeRootMotionBoneByBone(
        boneByBonePrefab: Prefab, rootMotion: Pair<Vector3f, Vector3f>,
        numFrames: Int, numBones: Int,
    ): Prefab {
        val prefab = Prefab("BoneByBoneAnimation")
        boneByBonePrefab.sets.forEach { path, key, value -> prefab[path, key] = value }
        val translations = boneByBonePrefab["translations"] as FloatArray
        prefab["translations"] = removeRootMotionFromTranslations(translations, rootMotion, numFrames, numBones)
        return prefab
    }

    private fun removeRootMotionFromTranslations(
        translations: FloatArray, rootMotion: Pair<Vector3f, Vector3f>,
        numFrames: Int, numBones: Int,
    ): FloatArray {
        val dst = translations.copyOf()
        val offset = Vector3f()
        val boneId = 0
        for (frameIndex in 0 until numFrames) {
            val t = frameIndex.toFloat() / max(numFrames - 1, 1)
            rootMotion.first.mix(rootMotion.second, t, offset)
            val i = (frameIndex * numBones + boneId) * 3
            dst[i] -= offset.x
            dst[i + 1] -= offset.y
            dst[i + 2] -= offset.z
        }
        return dst
    }

    private fun createBoneByBonePrefab(
        skeletonRef: FileReference, duration: Float,
        skinningMatrices: List<List<Matrix4x3f>>, numBones: Int,
        globalTransform: Matrix4x3f?, globalInverseTransform: Matrix4x3f?
    ): Prefab {
        val importedInstance = ImportedAnimation()
        importedInstance.skeleton = skeletonRef
        importedInstance.duration = duration
        importedInstance.frames = skinningMatrices
        return createBoneByBone(
            importedInstance, numBones,
            globalTransform, globalInverseTransform
        )
    }

    private fun createBoneByBone(
        imported: ImportedAnimation,
        numBones: Int,
        globalTransform: Matrix4x3f?,
        globalInverseTransform: Matrix4x3f?
    ): Prefab {
        val instance = BoneByBoneAnimation(imported)
        val prefab = Prefab("BoneByBoneAnimation")
        prefab._sampleInstance = instance
        prefab["name"] = imported.name
        prefab["duration"] = imported.duration
        prefab["skeleton"] = imported.skeleton
        prefab["frameCount"] = imported.numFrames
        prefab["boneCount"] = numBones
        prefab["translations"] = instance.translations
        prefab["rotations"] = instance.rotations
        prefab["scales"] = instance.scales
        if (globalTransform != null) prefab["globalTransform"] = globalTransform
        if (globalInverseTransform != null) prefab["globalInvTransform"] = globalInverseTransform
        return prefab
    }
}