package me.anno.tests.engine.text

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.TypeValue
import me.anno.fonts.FontManager
import me.anno.fonts.mesh.TextMesh
import me.anno.fonts.signeddistfields.TextSDFGroup
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.shader.GLSLType
import me.anno.mesh.Shapes.flat11
import me.anno.ui.base.Font
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.utils.types.Arrays.resize
import org.joml.AABBd
import org.joml.Matrix4x3d
import kotlin.math.sign

class SDFTextureComponent(val text: String, val font: Font, alignment: AxisAlignment) : MeshSpawner() {

    val meshGroup = TextSDFGroup(FontManager.getFont(font), text, 0f)
    val materials = ArrayList<Material>()

    val size = FontManager.getSize(font, text, -1, -1)
    val dx = getSizeX(size).toDouble() / getSizeY(size)
    val alignmentOffset = when (alignment) {
        AxisAlignment.MIN -> -2f
        AxisAlignment.MAX -> 0f
        else -> -1f
    } * dx

    val mesh = Mesh()

    init {
        mesh.indices = flat11.indices
        val pos = mesh.positions.resize(flat11.positions.size)
        val uvs = mesh.uvs.resize(pos.size / 3 * 2)
        var j = 0
        for (i in pos.indices step 3) {
            pos[i] = sign(flat11.positions[i])
            pos[i + 1] = sign(flat11.positions[i + 1])
            uvs[j++] = +sign(flat11.positions[i]) * .5f + .5f
            uvs[j++] = -sign(flat11.positions[i + 1]) * .5f + .5f
        }
        mesh.positions = pos
        mesh.uvs = uvs
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

    override fun forEachMesh(run: (Mesh, Material?, Transform) -> Unit) {
        var i = 0
        val extraScale = 2f / TextMesh.DEFAULT_LINE_HEIGHT
        val baseScale = meshGroup.baseScale * extraScale
        meshGroup.draw { _, sdfTexture, offset ->
            val texture = sdfTexture?.texture
            if (texture != null && texture.isCreated) {

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
}