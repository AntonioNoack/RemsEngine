package me.anno.gpu.pipeline

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
    ): Long

    fun clear()

}