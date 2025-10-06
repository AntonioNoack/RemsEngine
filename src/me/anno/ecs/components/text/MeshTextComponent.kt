package me.anno.ecs.components.text

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.TransformMesh.translate
import me.anno.ecs.components.text.TextureTextComponent.Companion.getDx
import me.anno.fonts.Font
import me.anno.fonts.mesh.MeshGlyphLayout
import me.anno.ui.base.components.AxisAlignment
import org.joml.Vector3d

// todo TextMeshSpawner component for long texts?
class MeshTextComponent : TextComponentImpl {

    @Suppress("unused")
    constructor() : super()

    @Suppress("unused")
    constructor(text: String, font: Font, alignmentX: AxisAlignment) : super(text, font, alignmentX)
    constructor(text: String, font: Font, alignmentX: AxisAlignment, alignmentY: TextAlignmentY) :
            super(text, font, alignmentX, alignmentY, 0f)

    override fun generateMesh(mesh: Mesh) {
        val meshGroup = MeshGlyphLayout(
            font, text, relativeWidthLimit, maxNumLines,
            false
        )
        meshGroup.createJoinedMesh(mesh)

        val sx = meshGroup.width * meshGroup.baseScale
        val sy = meshGroup.height * meshGroup.baseScale

        val dx = getDx(sx, alignmentX).toDouble() - sx
        val y0 = getY0(sy, alignmentY).toDouble()

        mesh.translate(Vector3d(dx, y0, 0.0))
    }

    private fun getY0(sy: Float, alignmentY: TextAlignmentY): Float {
        return when (alignmentY) {
            TextAlignmentY.MAX -> sy - 1f
            TextAlignmentY.CENTER -> (sy - 1f - 1f) * 0.5f
            TextAlignmentY.MIN -> -1f
            TextAlignmentY.BASELINE -> 0f
        }
    }

}