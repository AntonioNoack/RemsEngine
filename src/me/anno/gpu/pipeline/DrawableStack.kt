package me.anno.gpu.pipeline

import me.anno.ecs.components.mesh.MeshInstanceData
import me.anno.gpu.GFXState
import me.anno.utils.structures.tuples.LongTriple

abstract class DrawableStack(val instanceData: MeshInstanceData) {

    fun draw0(
        pipeline: Pipeline,
        stage: PipelineStageImpl,
        needsLightUpdateForEveryMesh: Boolean,
        time: Long,
        depth: Boolean
    ): LongTriple {
        return GFXState.instanceData.use(instanceData) {
            draw1(pipeline, stage, needsLightUpdateForEveryMesh, time, depth)
        }
    }

    /**
     * draws; returns (number of triangles drawn, number of instances drawn, number of draw calls)
     * */
    abstract fun draw1(
        pipeline: Pipeline,
        stage: PipelineStageImpl,
        needsLightUpdateForEveryMesh: Boolean,
        time: Long,
        depth: Boolean
    ): LongTriple

    abstract fun clear()
    abstract fun isEmpty(): Boolean
}