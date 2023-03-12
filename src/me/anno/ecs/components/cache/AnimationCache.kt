package me.anno.ecs.components.cache

import me.anno.cache.CacheSection
import me.anno.ecs.components.anim.AnimTexture
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.anim.Skeleton
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
}