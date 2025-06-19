package me.anno.ecs.components.anim

import me.anno.cache.CacheSection
import me.anno.cache.DualCacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.cache.FileCacheSection.getFileEntryWithoutGenerator
import me.anno.ecs.prefab.PrefabByFileCache
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import org.apache.logging.log4j.LogManager

/**
 * caches animations with their specific retargetings
 * */
object AnimationCache : PrefabByFileCache<Animation>(Animation::class, "Animation") {

    private val LOGGER = LogManager.getLogger(AnimationCache::class)

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
        }!!.waitFor()!!
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

    fun getMappedAnimation(animation: Animation, dstSkeleton: Skeleton): BoneByBoneAnimation? {
        val s0 = animation.ref
        val s1 = dstSkeleton.ref
        return mappedAnimCache.getDualEntry(
            s0.getFileKey(), s1.getFileKey(),
            timeoutMillis
        ) { k1, k2, result ->
            val retargeting = Retargetings.getRetargeting(k1.file, k2.file)
            result.value = if (retargeting != null) {
                val bbb = animation as? BoneByBoneAnimation
                    ?: BoneByBoneAnimation(animation as ImportedAnimation)
                retargeting.map(bbb)
            } else {
                LOGGER.warn("Missing retargeting from ${k1.file} to ${k2.file}")
                null
            }
        }.waitFor()
    }
}