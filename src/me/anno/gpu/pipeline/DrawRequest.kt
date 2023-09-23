package me.anno.gpu.pipeline

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh

class DrawRequest(
    var mesh: Mesh,
    var component: Component, // light component or renderer
    var entity: Entity,
    var materialIndex: Int
)