package me.anno.engine.ui.control

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector3d

interface DCDroppable {
    fun drop(
        self: DraggingControls, prefab: Prefab, hovEntity: Entity?, hovComponent: Component?,
        dropPosition: Vector3d, results: MutableCollection<PrefabSaveable>
    )
}