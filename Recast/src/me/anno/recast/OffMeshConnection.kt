package me.anno.recast

import me.anno.ecs.Component
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.ui.LineShapes
import me.anno.gpu.pipeline.Pipeline
import org.joml.Vector3d

class OffMeshConnection : Component(), OnDrawGUI {

    @Docs("Start position in local space")
    var from = Vector3d()

    @Docs("End position in local space")
    var to = Vector3d()

    var radius = 0.3f
    var areaId = 0
    var connectionFlags = 0 // user-defined flags
    var isBidirectional = true

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        val entity = entity
        val radius = radius.toDouble()
        LineShapes.drawCircle(entity, radius, 0, 2, 0.0, from)
        LineShapes.drawCircle(entity, radius, 0, 2, 0.0, to)
        LineShapes.drawLine(entity, from, to)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is OffMeshConnection) return
        dst.from.set(from)
        dst.to.set(to)
        dst.radius = radius
        dst.connectionFlags = connectionFlags
        dst.areaId = areaId
        dst.isBidirectional = isBidirectional
    }
}