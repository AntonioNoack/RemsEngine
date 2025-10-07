package me.anno.ecs.components.text

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.TransformMesh.translate
import me.anno.fonts.Font
import me.anno.fonts.mesh.MeshGlyphLayout
import me.anno.ui.base.components.AxisAlignment

// todo TextMeshSpawner component for long texts?
class MeshTextComponent : TextComponentImpl {

    @Suppress("unused")
    constructor() : super()

    @Suppress("unused")
    constructor(text: String, font: Font, alignmentX: AxisAlignment) : super(text, font, alignmentX)
    constructor(text: String, font: Font, alignmentX: AxisAlignment, alignmentY: TextAlignmentY) :
            super(text, font, alignmentX, alignmentY, 0f)

    fun createBaseMesh(mesh: Mesh): MeshGlyphLayout {
        val meshGroup = MeshGlyphLayout(
            font, text, relativeWidthLimit, maxNumLines,
            null
        )
        meshGroup.createJoinedMesh(mesh)
        return meshGroup
    }

    override fun generateMesh(mesh: Mesh) {
        val meshGroup = createBaseMesh(mesh)

        val sx = meshGroup.width * meshGroup.baseScale
        val sy = meshGroup.height * meshGroup.baseScale

        val dx = getX0(sx, alignmentX).toDouble()
        val dy = getY0(sy, alignmentY).toDouble()

        mesh.translate(dx, dy, 0.0)
    }

    companion object {

        fun getX0(sx: Float, alignmentX: AxisAlignment): Float {
            return TextureTextComponent.getX0(sx, alignmentX) - sx
        }

        fun getY0(sy: Float, alignmentY: TextAlignmentY): Float {
            return when (alignmentY) {
                TextAlignmentY.MAX -> sy - 1f
                TextAlignmentY.CENTER -> (sy - 2f) * 0.5f
                TextAlignmentY.MIN -> -1f
                TextAlignmentY.BASELINE -> 0f
            }
        }

    }
}