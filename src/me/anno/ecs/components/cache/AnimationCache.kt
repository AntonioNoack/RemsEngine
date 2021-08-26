package me.anno.ecs.components.cache

import me.anno.ecs.components.anim.Animation
import me.anno.ecs.prefab.PrefabByFileCache

object AnimationCache : PrefabByFileCache<Animation>(Animation::class)