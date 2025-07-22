package me.anno.ecs.components.text

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.components.text.TextComponent.Companion.defaultFont
import me.anno.ecs.components.text.TextureTextComponent.Companion.getDx
import me.anno.ecs.components.text.TextureTextComponent.Companion.getSx
import me.anno.ecs.components.text.TextureTextComponent.Companion.getSy
import me.anno.ecs.components.text.TextureTextComponent.Companion.getY0
import me.anno.ecs.components.text.TextureTextComponent.Companion.getY1
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.fonts.signeddistfields.TextSDFGroup
import me.anno.gpu.FinalRendering.isFinalRendering
import me.anno.gpu.FinalRendering.onMissingResource
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.texture.Texture2D
import me.anno.mesh.Shapes
import me.anno.ui.base.components.AxisAlignment
import org.joml.AABBd
import org.joml.Matrix4x3

class SDFTextComponent(
    text: String, font: Font,
    override var alignmentX: AxisAlignment,
    override var alignmentY: TextAlignmentY
) : MeshSpawner(), TextComponent {

    @Suppress("unused")
    constructor() : this("Text", defaultFont, AxisAlignment.CENTER)

    constructor(text: String, font: Font, alignmentX: AxisAlignment) :
            this(text, font, alignmentX, TextAlignmentY.CENTER)

    @Suppress("unused", "unused_parameter")
    constructor(text: String, font: Font, alignmentX: AxisAlignment, alignmentY: TextAlignmentY, widthLimit: Float) :
            this(text, font, alignmentX, alignmentY)

    @SerializedProperty
    override var text = text
        set(value) {
            if (field != value) {
                field = value
                onTextOrFontChange()
            }
        }

    @SerializedProperty
    override var font = font
        set(value) {
            field = value
            onTextOrFontChange()
        }

    // todo support this (?)
    override var widthLimit: Float = -1f

    private var meshGroup: TextSDFGroup? = null

    override fun onTextOrFontChange() {
        meshGroup = null
    }

    private val materials = ArrayList<Material>()

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {

        // effectively just the same code as TextureTextComponent
        val size = FontManager.getSize(font, text, -1, -1).waitFor() ?: 0
        val baselineY = FontManager.getBaselineY(font)
        val sx = getSx(getSizeX(size), baselineY)
        val sy = getSy(getSizeY(size), baselineY)
        val dx = getDx(sx, alignmentX)
        val y0 = getY0(sy, alignmentY)
        val y1 = getY1(y0, sy)

        val local = localAABB
            .setMin((dx - sx).toDouble(), y0.toDouble(), 0.0)
            .setMax((dx + sx).toDouble(), y1.toDouble(), 0.0)

        // calculate global aabb
        val global = globalAABB
        local.transform(globalTransform, global)
        // add the result to the output
        dstUnion.union(global)
    }

    private fun getOrCreateMeshGroup(): TextSDFGroup {
        val meshGroup = meshGroup ?: TextSDFGroup(font, text, 0.0)
        this.meshGroup = meshGroup
        return meshGroup
    }

    private fun getOrCreateMaterial(i: Int): Material {
        while (i >= materials.size) {
            materials.add(Material().apply {
                shader = SDFShader
                shaderOverrides["invertSDF"] = TypeValue(GLSLType.V1B, false)
            })
        }
        return materials[i]
    }

    override fun forEachMesh(pipeline: Pipeline?, callback: (IMesh, Material?, Transform) -> Boolean) {

        var i = 0
        val group = getOrCreateMeshGroup()
        val lineHeight = FontManager.getLineHeight(font)
        val baselineY = FontManager.getBaselineY(font)
        val mesh = Shapes.flat11.front

        val dx0 = when (alignmentX) {
            AxisAlignment.MIN -> -1f
            AxisAlignment.CENTER -> -0.5f
            else -> 0f
        } * group.offsets.last().toFloat() / lineHeight
        val dy0 = baselineY / lineHeight - 1f

        group.draw { _, sdfTexture, offset ->
            val texture = sdfTexture?.texture
            if (texture is Texture2D && texture.wasCreated) {

                val material = getOrCreateMaterial(i)
                material.diffuseMap = texture.ref

                // println("char[$offset]: ${texture.width} x ${texture.height}")

                val sx = getSx(texture.width, baselineY)
                val sy = getSy(texture.height, baselineY) * 0.5f

                // todo why is that extra offset needed???
                val dx1 = if (alignmentX == AxisAlignment.MAX) 0f else -0.33f

                // todo this correction is needed, because dy0=baseline-correction isn't correct
                val dy1 = if (alignmentY == TextAlignmentY.BASELINE) 0f else -0.08f

                val dx = dx0 + dx1 + offset + sdfTexture.offset.x * sx
                val dy = sdfTexture.offset.y * sy + when (alignmentY) {
                    TextAlignmentY.MIN -> dy0 - 0.5f
                    TextAlignmentY.CENTER -> dy0
                    TextAlignmentY.MAX -> dy0 + 0.5f
                    TextAlignmentY.BASELINE -> 0f
                } + dy1

                val transform = getTransform(i++)
                    .setLocalPosition(dx.toDouble(), dy.toDouble(), 0.0)
                    .setLocalScale(sx.toDouble(), sy.toDouble(), 1.0)

                callback(mesh, material, transform)
            } else if (texture is Texture2D && isFinalRendering) {
                onMissingResource(className, offset)
                true // quit loop
            } else false
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is TextComponent) return
        dst.text = text
        dst.font = font
        dst.alignmentX = alignmentX
        dst.alignmentY = alignmentY
        dst.widthLimit = widthLimit
    }
}