package me.anno.ecs.components.anim

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.DualCacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.cache.FileCacheSection.getFileEntryWithoutGenerator
import me.anno.ecs.prefab.PrefabByFileCache
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference

/**
 * caches animations with their specific retargetings
 * */
object AnimationCache : PrefabByFileCache<Animation>(Animation::class, "Animation") {

    var timeout = 10_000L
    private val animTexCache = CacheSection<FileKey, AnimTexture>("AnimTextures")
    private val mappedAnimCache = DualCacheSection<FileKey, FileKey, BoneByBoneAnimation>("MappedAnimations")

    operator fun get(skeleton: Skeleton) = getTexture(skeleton)
    fun getTexture(skeleton: Skeleton): AnimTexture {
        return animTexCache.getFileEntry(
            skeleton.prefab!!.sourceFile, true,
            timeoutMillis
        ) { skeletonFile, result ->
            result.value = AnimTexture(skeleton)
        }.waitFor()!!
    }

    fun invalidate(animation: Animation, skeleton: Skeleton) {
        animTexCache.getFileEntryWithoutGenerator(
            skeleton.prefab!!.sourceFile,
            timeoutMillis
        )?.value?.invalidate(animation)
    }

    fun invalidate(animation: Animation, skeleton: FileReference = animation.skeleton) {
        invalidate(animation, SkeletonCache[skeleton] ?: return)
    }

    fun getMappedAnimation(animation: Animation, dstSkeleton: Skeleton): AsyncCacheData<BoneByBoneAnimation> {
        val s0 = animation.ref
        val s1 = dstSkeleton.ref
        return mappedAnimCache.getDualEntry(
            s0.getFileKey(), s1.getFileKey(),
            timeoutMillis
        ) { k1, k2, result ->
            Retargetings.getRetargeting(k1.file, k2.file)
                .mapResult(result) { retargeting ->
                    val bbb = animation as? BoneByBoneAnimation
                        ?: BoneByBoneAnimation(animation as ImportedAnimation)
                    retargeting.map(bbb)
                }
        }
    }
}