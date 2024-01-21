package me.anno.ecs.components.text

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.TypeValue
import me.anno.fonts.FontManager
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.texture.Clamping
import me.anno.io.files.InvalidRef
import me.anno.mesh.Shapes
import me.anno.fonts.Font
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.types.Arrays.resize
import kotlin.math.sign

class TextTextureComponent : TextComponent {

    constructor() : super()
    @Suppress("unused")
    constructor(text: String, font: Font, alignment: AxisAlignment) : super(text, font, alignment)
    constructor(text: String, font: Font, alignment: AxisAlignment, widthLimit: Int) :
            super(text, font, alignment, widthLimit)

    private val material = Material()
    private var key = TextCacheKey.getKey(font, text, widthLimit, -1, true)

    override fun invalidate() {
        super.invalidate()
        key = TextCacheKey.getKey(font, text, widthLimit, -1, true)
    }

    init {
        material.shader = SDFAvgShader
        material.shaderOverrides["invertSDF"] = TypeValue(GLSLType.V1B, true)
    }

    override fun generateMesh(mesh: Mesh) {
        val size = FontManager.getSize(key, false)
        mesh.indices = Shapes.flat11.indices
        val pos = mesh.positions.resize(Shapes.flat11.positions.size)
        val dx = GFXx2D.getSizeX(size).toFloat() / GFXx2D.getSizeY(size)
        val uvs = mesh.uvs.resize(pos.size / 3 * 2)
        var j = 0
        val x0f = when (alignmentX) {
            AxisAlignment.MIN -> -dx
            AxisAlignment.MAX -> +dx
            else -> 0f
        }
        for (i in pos.indices step 3) {
            pos[i] = sign(Shapes.flat11.positions[i]) * dx + x0f
            pos[i + 1] = sign(Shapes.flat11.positions[i + 1])
            uvs[j++] = sign(Shapes.flat11.positions[i]) * .5f + .5f
            uvs[j++] = sign(Shapes.flat11.positions[i + 1]) * .5f + .5f
        }
        mesh.positions = pos
        mesh.material = material.ref
        mesh.uvs = uvs
    }

    override fun onUpdate(): Int {
        return if (material.diffuseMap == InvalidRef) {
            val texture = FontManager.getTexture(key, true)
            if (texture != null && texture.isCreated()) {
                material.diffuseMap = texture.createImage(flipY = false, withAlpha = false).ref
                material.clamping = Clamping.CLAMP
                -1 // done
            } else 1 // wait
        } else -1 // done
    }
}