package me.anno.ecs.components.anim

import me.anno.cache.CacheSection
import me.anno.ecs.prefab.PrefabByFileCache
import me.anno.io.files.FileReference
import org.apache.logging.log4j.LogManager

/**
 * caches animations with their specific retargetings
 * */
object AnimationCache : PrefabByFileCache<Animation>(Animation::class, "Animation") {

    private val LOGGER = LogManager.getLogger(AnimationCache::class)

    var timeout = 10_000L
    val animTexCache = CacheSection("AnimTextures")

    operator fun get(skeleton: Skeleton) = getTexture(skeleton)
    fun getTexture(skeleton: Skeleton): AnimTexture {
        return animTexCache.getEntry(skeleton.prefab!!.sourceFile, timeoutMillis, false) { _ ->
            AnimTexture(skeleton)
        } as AnimTexture
    }

    fun invalidate(animation: Animation, skeleton: Skeleton) {
        (animTexCache.getEntryWithoutGenerator(
            skeleton.prefab!!.sourceFile,
            timeoutMillis
        ) as? AnimTexture)?.invalidate(animation)
    }

    fun invalidate(animation: Animation, skeleton: FileReference = animation.skeleton) {
        invalidate(animation, SkeletonCache[skeleton] ?: return)
    }

    fun getMappedAnimation(animation: Animation, dstSkeleton: Skeleton): BoneByBoneAnimation? {
        val s0 = animation.ref
        val s1 = dstSkeleton.ref
        return animTexCache.getEntry(
            DualFileKey(s0, s1),
            timeoutMillis, false
        ) {
            val retargeting = Retargetings.getRetargeting(animation.skeleton, dstSkeleton.ref)
            if (retargeting != null) {
                val bbb = if (animation is BoneByBoneAnimation) animation
                else BoneByBoneAnimation(animation as ImportedAnimation)
                retargeting.map(bbb)
            } else {
                LOGGER.warn("Missing retargeting from ${animation.skeleton} to ${dstSkeleton.ref}")
                null
            }
        }
    }
}