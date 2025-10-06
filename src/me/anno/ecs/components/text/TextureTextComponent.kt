package me.anno.ecs.components.text

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.systems.OnUpdate
import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.io.files.InvalidRef
import me.anno.mesh.Shapes
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Floats.toIntOr
import kotlin.math.sign

class TextureTextComponent : TextComponentImpl, OnUpdate {

    @Suppress("unused")
    constructor() : super()

    constructor(text: String, font: Font, alignmentX: AxisAlignment) : super(text, font, alignmentX)
    constructor(text: String, font: Font, alignmentX: AxisAlignment, alignmentY: TextAlignmentY, widthLimit: Float) :
            super(text, font, alignmentX, alignmentY, widthLimit)

    companion object {

        fun getSx(sizeX: Int, baselineY: Float): Float {
            return 0.5f * sizeX / baselineY
        }

        fun getSy(sizeY: Int, baselineY: Float): Float {
            return sizeY / baselineY
        }

        fun getDx(sx: Float, alignmentX: AxisAlignment): Float {
            return when (alignmentX) {
                AxisAlignment.MIN -> -sx
                AxisAlignment.MAX -> +sx
                else -> 0f
            }
        }

        fun getY0(sy: Float, alignmentY: TextAlignmentY): Float {
            return when (alignmentY) {
                TextAlignmentY.MAX -> 0f
                TextAlignmentY.CENTER -> -0.5f * sy
                TextAlignmentY.MIN -> -sy
                TextAlignmentY.BASELINE -> 1f - sy
            }
        }

        fun getY1(y0: Float, sy: Float): Float {
            return y0 + sy
        }
    }

    private val material = Material()
    private var key = TextCacheKey.getTextCacheKey(font, text, relativeWidthLimit.toIntOr(-1), -1, true)

    override fun onTextOrFontChange() {
        super.onTextOrFontChange()
        key = TextCacheKey.getTextCacheKey(font, text, relativeWidthLimit.toIntOr(-1), -1, true)
        material.diffuseMap = InvalidRef // invalidate texture
    }

    init {
        material.shader = SDFAvgShader
        material.shaderOverrides["invertSDF"] = TypeValue(GLSLType.V1B, true)
    }

    override fun generateMesh(mesh: Mesh) {
        val size = FontManager.getSize(key).waitFor() ?: 0
        mesh.indices = Shapes.flat11.indices
        val pos = mesh.positions.resize(Shapes.flat11.positions.size)
        val baselineY = FontManager.getBaselineY(font)

        // todo scale is correct, but the following y-offset calculation is not
        val scale = baselineY / font.size

        val sx = getSx(getSizeX(size), baselineY) * scale
        val sy = getSy(getSizeY(size), baselineY) * scale
        val dx = getDx(sx, alignmentX)
        val y0 = getY0(sy, alignmentY)
        val y1 = getY1(y0, sy)
        val flat11 = Shapes.flat11.positions
        val uvs = mesh.uvs.resize(pos.size / 3 * 2)
        var j = 0
        for (i in pos.indices step 3) {
            pos[i] = sign(flat11[i]) * sx + dx
            pos[i + 1] = if (flat11[i + 1] > 0f) y1 else y0
            uvs[j++] = sign(flat11[i]) * 0.5f + 0.5f
            uvs[j++] = sign(flat11[i + 1]) * 0.5f + 0.5f
        }
        mesh.positions = pos
        mesh.materials = listOf(material.ref)
        mesh.uvs = uvs
    }

    override fun onUpdate() {
        getTexture() // ensure it is being loaded; asynchronously
    }

    fun getTexture(): ITexture2D? {
        val srcTexture = FontManager.getTexture(key).value // keep it loaded
        val texture = TextureCache[material.diffuseMap].value?.createdOrNull()
        if (texture == null && srcTexture is Texture2D && srcTexture.isCreated()) {
            material.diffuseMap = srcTexture.ref
            material.clamping = Clamping.CLAMP
        }
        return texture
    }
}