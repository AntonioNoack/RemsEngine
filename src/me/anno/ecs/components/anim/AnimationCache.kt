package me.anno.ecs.components.anim

import me.anno.cache.CacheSection
import me.anno.ecs.prefab.PrefabByFileCache
import me.anno.io.files.FileReference

/**
 * caches animations with their specific retargetings
 * */
object AnimationCache : PrefabByFileCache<Animation>(Animation::class, "Animation") {

    var timeout = 10_000L
    val animTexCache = CacheSection("AnimTextures")

    operator fun get(skeleton: Skeleton) = getTexture(skeleton)
    fun getTexture(skeleton: Skeleton): AnimTexture {
        return animTexCache.getEntry(skeleton.prefab!!.source, timeoutMillis, false) { _ ->
            AnimTexture(skeleton)
        } as AnimTexture
    }

    fun invalidate(animation: Animation, skeleton: Skeleton) {
        (animTexCache.getEntryWithoutGenerator(
            skeleton.prefab!!.source,
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
                ?: throw NullPointerException("Missing retargeting from ${animation.skeleton} to ${dstSkeleton.ref}")
            val bbb = if (animation is BoneByBoneAnimation) animation
            else BoneByBoneAnimation(animation as ImportedAnimation)
            retargeting.map(bbb)
        } as? BoneByBoneAnimation
    }
}