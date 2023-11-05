package me.anno.gpu.pipeline

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Material

class DrawRequest(
    var mesh: IMesh,
    var component: Component, // light component or renderer
    var entity: Entity, // entity for transform
    var material: Material, // appearance
    var materialIndex: Int // used for grabbing helper meshes, when a mesh has multiple materials
)