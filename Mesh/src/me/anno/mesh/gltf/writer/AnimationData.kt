package me.anno.mesh.gltf.writer

import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.anim.Skeleton

data class AnimationData(
    val skeleton: Skeleton,
    val animation: Animation,
    val baseId: Int,
)