package me.anno.ecs.components.text

import me.anno.ecs.Transform
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.text.TextComponent.Companion.defaultFont
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.fonts.Codepoints
import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.fonts.GlyphLayout
import me.anno.fonts.signeddistfields.SDFGlyphLayout
import me.anno.fonts.signeddistfields.TextSDF
import me.anno.gpu.FinalRendering.isFinalRendering
import me.anno.gpu.FinalRendering.onMissingResource
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Texture2D
import me.anno.mesh.Shapes
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color.toVecRGBA
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3f

class SDFTextComponent(
    text: String, font: Font,
    override var blockAlignmentX: AxisAlignment,
    override var blockAlignmentY: TextAlignmentY,
    override var lineAlignmentX: Float,
    override var relativeWidthLimit: Float
) : MeshSpawner(), TextComponent {

    companion object {

        fun getSX(group: GlyphLayout, textSDF: TextSDF): Float {
            val bounds = textSDF.bounds
            return 0.5f * bounds.deltaX * group.baseScale
        }

        fun getSY(group: GlyphLayout, textSDF: TextSDF): Float {
            val bounds = textSDF.bounds
            return 0.5f * bounds.deltaY * group.baseScale
        }

        fun getX0(group: GlyphLayout, textSDF: TextSDF, alignmentX: AxisAlignment): Float {
            return when (alignmentX) {
                AxisAlignment.MIN -> -1f
                AxisAlignment.CENTER -> -0.5f
                else -> 0f
            } * group.width * group.baseScale + textSDF.bounds.centerX * group.baseScale
        }

        fun getY0(group: GlyphLayout, textSDF: TextSDF, alignmentY: TextAlignmentY): Float {
            val sy = group.height * group.baseScale
            return when (alignmentY) {
                TextAlignmentY.BASELINE -> 0f
                TextAlignmentY.MIN -> -1f
                TextAlignmentY.CENTER -> (sy - 2f) * 0.5f
                TextAlignmentY.MAX -> sy - 1f
            } - textSDF.bounds.centerY * group.baseScale
        }
    }

    @Suppress("unused")
    constructor() : this("Text", defaultFont, AxisAlignment.CENTER)

    constructor(text: String, font: Font, blockAlignmentX: AxisAlignment) :
            this(text, font, blockAlignmentX, TextAlignmentY.CENTER)

    constructor(text: String, font: Font, blockAlignmentX: AxisAlignment, blockAlignmentY: TextAlignmentY) :
            this(text, font, blockAlignmentX, blockAlignmentY, 0f, 0f)

    // support writing emissive color, too?
    @Type("Color3")
    var color = Vector3f(1f)
        set(value) {
            field.set(value)
        }

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

    override var maxNumLines: Int = Int.MAX_VALUE

    private var meshGroup: SDFGlyphLayout? = null

    override fun onTextOrFontChange() {
        meshGroup = null
    }

    private val materials = ArrayList<Material>()

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {

        // effectively just the same code as TextureTextComponent
        val size = FontManager.getSize(font, text, -1, -1)
        val baselineY = FontManager.getBaselineY(font)

        val scale = baselineY / font.size
        val yCorrection = scale - 1f

        val sx = TextureTextComponent.getSx(getSizeX(size), baselineY) * scale
        val sy = TextureTextComponent.getSy(getSizeY(size), baselineY) * scale
        val dx = TextureTextComponent.getX0(sx, blockAlignmentX)
        val y0 = TextureTextComponent.getY0(sy, blockAlignmentY) + yCorrection
        val y1 = TextureTextComponent.getY1(y0, sy)

        val local = localAABB
            .setMin((dx - sx).toDouble(), y0.toDouble(), 0.0)
            .setMax((dx + sx).toDouble(), y1.toDouble(), 0.0)

        // calculate global aabb
        val global = globalAABB
        local.transform(globalTransform, global)
        // add the result to the output
        dstUnion.union(global)
    }

    private fun getOrCreateMeshGroup(): SDFGlyphLayout {
        val meshGroup = meshGroup
            ?: SDFGlyphLayout(font, text, relativeWidthLimit, maxNumLines)
        this.meshGroup = meshGroup
        return meshGroup
    }

    private fun getOrCreateMaterial(i: Int): Material {
        while (i >= materials.size) {
            materials.add(Material().apply {
                shader = SDFShader
                clamping = Clamping.CLAMP
            })
        }
        return materials[i]
    }

    override fun forEachMesh(pipeline: Pipeline?, callback: (IMesh, Material?, Transform) -> Boolean) {

        var partIndex = 0
        val group = getOrCreateMeshGroup()
        val mesh = Shapes.flat11.front

        val totalWidth = group.width * group.baseScale
        group.draw(0, group.size) { textSDF, offsetX0, _, offsetY, lineWidth, _ ->
            val texture = textSDF.texture
            if (texture is Texture2D && texture.wasCreated) {

                val material = getOrCreateMaterial(partIndex)
                configureMaterial(material, texture, textSDF)

                val sx = getSX(group, textSDF)
                val sy = getSY(group, textSDF)

                val x0 = getX0(group, textSDF, blockAlignmentX)
                val y0 = getY0(group, textSDF, blockAlignmentY)

                val dx = x0 + offsetX0 + lineAlignmentX * (totalWidth - lineWidth)
                val dy = y0 - offsetY

                val dz = textSDF.z.toDouble()

                val transform = getTransform(partIndex++)
                    .setLocalPosition(dx.toDouble(), dy.toDouble(), dz)
                    .setLocalScale(sx.toDouble(), sy.toDouble(), 1.0)

                callback(mesh, material, transform)
            } else if (texture is Texture2D && isFinalRendering) {
                onMissingResource(className, offsetX0)
                true // quit loop
            } else false
        }
    }

    fun configureMaterial(material: Material, texture: Texture2D, textSDF: TextSDF) {
        material.diffuseMap = texture.ref
        if (Codepoints.isEmoji(textSDF.codepoint)) {
            // emojis are colored themselves, coloring them twice doesn't make sense
            textSDF.color.toVecRGBA(material.diffuseBase)
        } else {
            material.diffuseBase.set(color, 1f)
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is TextComponent) return
        dst.text = text
        dst.font = font
        dst.blockAlignmentX = blockAlignmentX
        dst.blockAlignmentY = blockAlignmentY
        dst.relativeWidthLimit = relativeWidthLimit
        dst.maxNumLines = maxNumLines
        if (dst !is SDFTextComponent) return
        dst.color.set(color)
    }
}