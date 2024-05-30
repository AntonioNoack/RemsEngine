package me.anno.mesh.gltf.writer

import me.anno.ecs.components.anim.Skeleton

data class SkinData(
    val skeleton: Skeleton,
    val nodes: IntRange
)