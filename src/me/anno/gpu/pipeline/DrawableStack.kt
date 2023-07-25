package me.anno.gpu.pipeline

import me.anno.utils.structures.tuples.LongPair

interface DrawableStack {

    /**
     * draws; returns number of triangles drawn
     * */
    fun draw(
        pipeline: Pipeline,
        stage: PipelineStage,
        needsLightUpdateForEveryMesh: Boolean,
        time: Long,
        depth: Boolean
    ): LongPair

    fun clear()

    fun size1(): Long

}