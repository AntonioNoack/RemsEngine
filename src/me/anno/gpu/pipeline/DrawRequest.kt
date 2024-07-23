package me.anno.gpu.pipeline

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.material.Material

class DrawRequest(
    var mesh: IMesh,
    var component: Component, // light component or renderer; e.g., used for animations
    var transform: Transform,
    var material: Material, // appearance
    var materialIndex: Int // used for grabbing helper meshes, when a mesh has multiple materials
)