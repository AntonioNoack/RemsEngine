package me.anno.engine.ui

import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig
import me.anno.engine.ui.render.MovingGrid
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.gpu.FinalRendering.isFinalRendering
import me.anno.gpu.FinalRendering.onMissingResource
import me.anno.gpu.pipeline.Pipeline
import org.joml.Matrix4x3
import org.joml.Quaterniond
import org.joml.Vector3d

object TextShapes : CacheSection("TextShapes") {

    private val font by lazy { DefaultConfig.defaultFont }

    fun drawTextMesh(
        pipeline: Pipeline,
        text: String,
        position: Vector3d,
        rotation: Quaterniond?,
        scale: Double,
        transform: Matrix4x3?
    ) {
        val mesh = getEntry(text, 10000, true) {
            TextMeshGroup(font, text, 0f, false)
                .getOrCreateMesh()
        }
        if (mesh != null) {
            val matrix = MovingGrid.init()
            if (transform != null) matrix.mul(transform)
            matrix.translate(position)
            if (rotation != null) matrix.rotate(rotation)
            matrix.scale(scale)
            MovingGrid.drawMesh(pipeline, mesh)
        } else if (isFinalRendering) {
            onMissingResource("TextMesh", text)
        }
    }
}