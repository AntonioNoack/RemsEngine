package me.anno.gpu.pipeline

import me.anno.ecs.components.mesh.MeshInstanceData
import me.anno.gpu.GFXState
import me.anno.utils.structures.tuples.LongPair

abstract class DrawableStack(val instanceData: MeshInstanceData) {

    fun draw0(
        pipeline: Pipeline,
        stage: PipelineStage,
        needsLightUpdateForEveryMesh: Boolean,
        time: Long,
        depth: Boolean
    ): LongPair {
        return GFXState.instanceData.use(instanceData) {
            draw1(pipeline, stage, needsLightUpdateForEveryMesh, time, depth)
        }
    }

    /**
     * draws; returns number of triangles drawn
     * */
    abstract fun draw1(
        pipeline: Pipeline,
        stage: PipelineStage,
        needsLightUpdateForEveryMesh: Boolean,
        time: Long,
        depth: Boolean
    ): LongPair

    abstract fun clear()

    abstract fun size1(): Long
}