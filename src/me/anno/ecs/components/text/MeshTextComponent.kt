package me.anno.ecs.components.text

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.TransformMesh.transformMesh
import me.anno.ecs.components.text.TextureTextComponent.Companion.getDx
import me.anno.ecs.components.text.TextureTextComponent.Companion.getSx
import me.anno.ecs.components.text.TextureTextComponent.Companion.getSy
import me.anno.ecs.components.text.TextureTextComponent.Companion.getY0
import me.anno.ecs.components.text.TextureTextComponent.Companion.getY1
import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.ui.base.components.AxisAlignment
import org.joml.Matrix4x3d

// todo TextMeshSpawner component for long texts?
class MeshTextComponent : TextComponentImpl {

    @Suppress("unused")
    constructor() : super()

    @Suppress("unused")
    constructor(text: String, font: Font, alignmentX: AxisAlignment) : super(text, font, alignmentX)
    constructor(text: String, font: Font, alignmentX: AxisAlignment, alignmentY: TextAlignmentY) :
            super(text, font, alignmentX, alignmentY, -1f)

    override fun generateMesh(mesh: Mesh) {
        val meshGroup = TextMeshGroup(font, text, 0f, false)
        meshGroup.createJoinedMesh(mesh)

        val size = FontManager.getSize(font, text, -1, -1, false)
        val baselineY = FontManager.getBaselineY(font)
        val sx = getSx(getSizeX(size), baselineY)
        val sy = getSy(getSizeY(size), baselineY)

        val dx = getDx(sx, alignmentX).toDouble() - sx
        val y0 = getY0(sy, alignmentY)
        val y1 = getY1(y0, sy)
        // this formula is pretty weird...
        val dy = (y0 + y1) - baselineY / FontManager.getLineHeight(font)

        transformMesh(mesh, Matrix4x3d().translation(dx, 0.5 * dy, 0.0))
    }
}