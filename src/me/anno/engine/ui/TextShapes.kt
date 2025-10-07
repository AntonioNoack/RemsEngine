package me.anno.engine.ui

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.render.MovingGrid
import me.anno.fonts.mesh.MeshGlyphLayout
import me.anno.gpu.FinalRendering.isFinalRendering
import me.anno.gpu.FinalRendering.onMissingResource
import me.anno.gpu.pipeline.Pipeline
import org.joml.Matrix4x3
import org.joml.Quaterniond
import org.joml.Vector3d

object TextShapes : CacheSection<String, Mesh>("TextShapes") {

    fun drawTextMesh(
        pipeline: Pipeline,
        text: String,
        position: Vector3d,
        rotation: Quaterniond?,
        scale: Double,
        transform: Matrix4x3?
    ) {
        val mesh = getEntry(text, 10000, meshGenerator).value
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

    private val font = lazy { DefaultConfig.defaultFont }
    private val meshGenerator = { text: String, result: AsyncCacheData<Mesh> ->
        result.value = MeshGlyphLayout(
            font.value, text,
            0f, Int.MAX_VALUE,
            null
        ).getOrCreateMesh()
    }
}