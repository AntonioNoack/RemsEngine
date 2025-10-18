package me.anno.ecs.components.text

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.TransformMesh.translate
import me.anno.fonts.Font
import me.anno.fonts.mesh.MeshGlyphLayout
import me.anno.ui.base.components.AxisAlignment

// todo TextMeshSpawner component for long texts?
/**
 * This is effectively TextMeshPro from Unity.
 * */
class MeshTextComponent : TextComponentImpl {

    @Suppress("unused")
    constructor() : super()

    constructor(text: String, font: Font, blockAlignmentX: AxisAlignment) : super(text, font, blockAlignmentX)
    constructor(text: String, font: Font, blockAlignmentX: AxisAlignment, blockAlignmentY: TextAlignmentY) :
            super(text, font, blockAlignmentX, blockAlignmentY)

    fun createBaseMesh(mesh: Mesh): MeshGlyphLayout {
        val meshGroup = MeshGlyphLayout(
            font, text, relativeWidthLimit, maxNumLines
        )
        meshGroup.createJoinedMesh(mesh, lineAlignmentX)
        return meshGroup
    }

    override fun generateMesh(mesh: Mesh) {
        val meshGroup = createBaseMesh(mesh)

        val sx = meshGroup.width * meshGroup.baseScale
        val sy = meshGroup.height * meshGroup.baseScale

        val dx = getX0(sx, blockAlignmentX) * 0.5
        val dy = getY0(sy, blockAlignmentY).toDouble()

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