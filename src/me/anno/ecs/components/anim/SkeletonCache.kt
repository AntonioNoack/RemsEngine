package me.anno.ecs.components.anim

import me.anno.ecs.prefab.PrefabByFileCache

/**
 * cache for loaded skeletons
 * */
object SkeletonCache : PrefabByFileCache<Skeleton>(Skeleton::class)