package me.anno.ecs.components.anim

import me.anno.cache.CacheSection
import me.anno.ecs.prefab.PrefabByFileCache
import me.anno.io.files.FileReference

object AnimationCache : PrefabByFileCache<Animation>(Animation::class) {

    var timeout = 10_000L
    val animTexCache = CacheSection("AnimTextures")

    operator fun get(skeleton: Skeleton) = getTexture(skeleton)
    fun getTexture(skeleton: Skeleton): AnimTexture {
        return animTexCache.getEntry(skeleton.prefab!!.source, timeout, false) { _ ->
            AnimTexture(skeleton)
        } as AnimTexture
    }

    fun invalidate(animation: Animation, skeleton: Skeleton) {
        (animTexCache.getEntryWithoutGenerator(
            skeleton.prefab!!.source,
            timeout
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
            timeout, false
        ) {
            val retargeting = Retargeting.getRetargeting(animation.skeleton, dstSkeleton.ref)
                ?: throw NullPointerException("Missing retargeting from ${animation.skeleton} to ${dstSkeleton.ref}")
            val bbb = if (animation is BoneByBoneAnimation) animation
            else BoneByBoneAnimation(animation as ImportedAnimation)
            retargeting.map(bbb)
        } as? BoneByBoneAnimation
    }
}