package me.anno.engine.ui.control

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

interface DCDroppable {
    fun drop(
        self: DraggingControls, prefab: Prefab, hovEntity: Entity?, hovComponent: Component?,
        dropPosition: Vector3d, dropRotation: Quaternionf, dropScale: Vector3f,
        results: MutableCollection<PrefabSaveable>
    )
}