package me.anno.tests.engine.text

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.fonts.FontManager
import me.anno.fonts.mesh.TextMesh
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.ui.base.Font
import me.anno.ui.base.constraints.AxisAlignment

// to do MeshSpawner component for long texts?
class TextMeshComponent(val text: String, val font: Font, val alignment: AxisAlignment) : ProceduralMesh() {
    override fun generateMesh(mesh: Mesh) {
        val font = FontManager.getFont(this.font)
        val meshGroup = TextMeshGroup(font, text, 0f, false, debugPieces = false)
        meshGroup.createJoinedMesh(mesh)
        val bounds = mesh.getBounds()
        val scale = 2f / TextMesh.DEFAULT_LINE_HEIGHT
        val dy = -0.5f
        val dx = -bounds.centerX * scale + when (alignment) {
            AxisAlignment.MIN -> -1
            AxisAlignment.MAX -> +1
            else -> 0
        } * bounds.deltaX / bounds.deltaY
        val pos = mesh.positions!!
        for (i in pos.indices step 3) {
            pos[i] = pos[i] * scale + dx
            pos[i + 1] = pos[i + 1] * scale + dy
        }
        mesh.invalidateGeometry()
    }
}