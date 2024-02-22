package me.anno.ecs.components.text

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.components.text.TextComponent.Companion.defaultFont
import me.anno.engine.serialization.SerializedProperty
import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.fonts.mesh.TextMesh
import me.anno.fonts.signeddistfields.TextSDFGroup
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.shader.GLSLType
import me.anno.mesh.Shapes
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.types.Arrays.resize
import org.joml.AABBd
import org.joml.Matrix4x3d
import kotlin.math.sign

class SDFTextComponent(text: String, font: Font, alignmentX: AxisAlignment) : MeshSpawner() {

    constructor() : this("Text", defaultFont, AxisAlignment.CENTER)

    @SerializedProperty
    var text = text
        set(value) {
            if (field != value) {
                field = value
                onTextFontChange()
            }
        }

    @SerializedProperty
    var font = font
        set(value) {
            field = value
            onTextFontChange()
        }

    @SerializedProperty
    var alignmentX = alignmentX
        set(value) {
            if (field != value) {
                field = value
                onAlignmentChange()
            }
        }

    var meshGroup: TextSDFGroup? = null
    private var size = 0
    private var dx = 0.0
    private var alignmentOffset = 0.0

    private val materials = ArrayList<Material>()

    fun onTextFontChange() {
        meshGroup = null
    }

    fun onAlignmentChange() {
        size = FontManager.getSize(font, text, -1, -1, false)
        dx = getSizeX(size).toDouble() / getSizeY(size)
        alignmentOffset = when (alignmentX) {
            AxisAlignment.MIN -> -2f
            AxisAlignment.CENTER -> -1f
            else -> 0f
        } * dx
    }

    init {
        onTextFontChange()
        onAlignmentChange()
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {

        // calculate local aabb
        val local = localAABB
        val x0 = alignmentOffset
        // 2.05, because the real result is a little wider, why ever
        local.setMin(x0, -1.0, 0.0)
        local.setMax(x0 + 2.05 * dx, +1.0, 0.0)

        // calculate global aabb
        val global = globalAABB
        local.transform(globalTransform, global)

        // add the result to the output
        aabb.union(global)

        // yes, we calculated stuff
        return true
    }

    override fun forEachMesh(run: (IMesh, Material?, Transform) -> Unit) {
        var i = 0
        val extraScale = 2f / TextMesh.DEFAULT_LINE_HEIGHT
        val meshGroup = meshGroup ?: TextSDFGroup(font, text, 0.0)
        this.meshGroup = meshGroup
        val baseScale = meshGroup.baseScale * extraScale
        meshGroup.draw { _, sdfTexture, offset ->
            val texture = sdfTexture?.texture
            if (texture != null && texture.wasCreated) {

                val transform = getTransform(i)
                if (i >= materials.size) materials.add(Material())

                val material = materials[i]
                material.diffuseMap = texture.ref
                material.shader = SDFShader
                material.shaderOverrides["invertSDF"] = TypeValue(GLSLType.V1B, false)

                val scaleX = 0.5f * texture.width * baseScale
                val scaleY = 0.5f * texture.height * baseScale

                val offsetX = alignmentOffset + offset * extraScale + sdfTexture.offset.x * scaleX
                val offsetY = sdfTexture.offset.y * scaleY - 0.6 // 0.0 = perfect baseline

                transform.localPosition = transform.localPosition.set(offsetX, offsetY, 0.0)
                transform.localScale = transform.localScale.set(scaleX, scaleY, 1.0)

                run(mesh, material, transform)
                i++
            }
        }
    }

    companion object {
        private val mesh = Mesh().apply {
            val srcMesh = Shapes.flat11
            indices = srcMesh.indices
            val srcPos = srcMesh.positions
            val dstPos = positions.resize(srcPos.size)
            val dstUVs = uvs.resize(srcPos.size / 3 * 2)
            var j = 0
            for (i in dstPos.indices step 3) {
                dstPos[i] = sign(srcPos[i])
                dstPos[i + 1] = sign(srcPos[i + 1])
                dstUVs[j++] = +sign(srcPos[i]) * .5f + .5f
                dstUVs[j++] = -sign(srcPos[i + 1]) * .5f + .5f
            }
            positions = dstPos
            uvs = dstUVs
        }
    }
}