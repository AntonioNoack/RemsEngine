package me.anno.ecs.components.cache

import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.prefab.PrefabByFileCache

object SkeletonCache : PrefabByFileCache<Skeleton>(Skeleton::class)