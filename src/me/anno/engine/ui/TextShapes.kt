package me.anno.engine.ui

import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.render.MovingGrid
import me.anno.fonts.FontManager
import me.anno.fonts.mesh.TextMeshGroup
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Vector3d

object TextShapes {

    private val textCache = CacheSection("TextMeshes")

    // draw bone names where they are
    fun drawTextMesh(
        text: String,
        position: Vector3d,
        rotation: Quaterniond?,
        scale: Double,
        transform: Matrix4x3d?
    ) {
        val mesh = textCache.getEntry(text, 10000, false) {
            val font = FontManager.getFont(DefaultConfig.defaultFont)
            TextMeshGroup(font, text, 0f, false, debugPieces = false)
                .getOrCreateMesh()
        } as Mesh
        val matrix = MovingGrid.init()
        if (transform != null) matrix.mul(transform)
        matrix.translate(position)
        if (rotation != null) matrix.rotate(rotation)
        matrix.scale(scale)
        MovingGrid.drawMesh(mesh)
    }
}