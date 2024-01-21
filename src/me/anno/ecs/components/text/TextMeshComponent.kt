package me.anno.ecs.components.text

import me.anno.ecs.components.mesh.Mesh
import me.anno.fonts.mesh.TextMesh
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.fonts.Font
import me.anno.ui.base.components.AxisAlignment

// todo MeshSpawner component for long texts?
class TextMeshComponent : TextComponent {

    constructor() : super()

    @Suppress("unused")
    constructor(text: String, font: Font, alignment: AxisAlignment) : super(text, font, alignment)
    constructor(text: String, font: Font, alignment: AxisAlignment, widthLimit: Int) :
            super(text, font, alignment, widthLimit)

    override fun generateMesh(mesh: Mesh) {
        val meshGroup = TextMeshGroup(font, text, 0f, false)
        meshGroup.createJoinedMesh(mesh)
        val bounds = mesh.getBounds()
        val scale = 2f / TextMesh.DEFAULT_LINE_HEIGHT
        val dy = -0.5f
        val dx = -bounds.centerX * scale + when (alignmentX) {
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