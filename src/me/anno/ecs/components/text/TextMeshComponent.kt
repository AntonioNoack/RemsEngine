package me.anno.ecs.components.text

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.TransformMesh.transformMesh
import me.anno.fonts.Font
import me.anno.fonts.mesh.TextMesh
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.ui.base.components.AxisAlignment
import org.joml.Matrix4x3d

// todo MeshSpawner component for long texts?
class TextMeshComponent : TextComponent {

    @Suppress("unused")
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
        val dy = -0.5
        val dx = -bounds.centerX * scale + when (alignmentX) {
            AxisAlignment.MIN -> -1
            AxisAlignment.MAX -> +1
            else -> 0
        }.toDouble() * bounds.deltaX / bounds.deltaY
        transformMesh(
            mesh, Matrix4x3d()
                .translate(dx, dy, 0.0)
                .scale(scale.toDouble())
        )
    }
}