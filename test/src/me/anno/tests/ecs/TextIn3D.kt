package me.anno.tests.ecs

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.*
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.fonts.FontManager
import me.anno.fonts.keys.TextCacheKey
import me.anno.fonts.mesh.TextMesh
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.fonts.signeddistfields.TextSDFGroup
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Texture2D
import me.anno.io.files.InvalidRef
import me.anno.mesh.Shapes.flat11
import me.anno.ui.base.Font
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.utils.types.Arrays.resize
import org.joml.AABBd
import org.joml.Matrix4x3d
import kotlin.math.sign

/**
 * Discusses different ways to draw text in 3d
 *
 * TextTextureComponent is much cheaper to calculate than SDFTextureComponent, but also a bit lower quality.
 * TextMeshComponent has the highest quality, and has medium effort to calculate. The downside is triangles, which may become expensive,
 * if there is tons of text.
 * */

class TextTextureComponent(text: String, font: Font, val alignment: AxisAlignment, widthLimit: Int = -1) :
    ProceduralMesh() {

    val material = Material()
    val key = TextCacheKey.getKey(font, text, widthLimit, -1, true)

    init {
        material.shader = sdfAvgShader
        material.shaderOverrides["invertSDF"] = TypeValue(GLSLType.V1B, true)
    }

    override fun generateMesh(mesh: Mesh) {
        val size = FontManager.getSize(key)
        mesh.indices = flat11.indices
        val pos = mesh.positions.resize(flat11.positions.size)
        val dx = getSizeX(size).toFloat() / getSizeY(size)
        val uvs = mesh.uvs.resize(pos.size / 3 * 2)
        var j = 0
        val x0f = when (alignment) {
            AxisAlignment.MIN -> -dx
            AxisAlignment.MAX -> +dx
            else -> 0f
        }
        for (i in pos.indices step 3) {
            pos[i] = sign(flat11.positions[i]) * dx + x0f
            pos[i + 1] = sign(flat11.positions[i + 1])
            uvs[j++] = sign(flat11.positions[i]) * .5f + .5f
            uvs[j++] = sign(flat11.positions[i + 1]) * .5f + .5f
        }
        mesh.positions = pos
        mesh.material = material.ref
        mesh.uvs = uvs
    }

    override fun onUpdate(): Int {
        return if (material.diffuseMap == InvalidRef) {
            val texture = FontManager.getTexture(key)
            if (texture is Texture2D && texture.isCreated) {
                material.diffuseMap = texture.createImage(flipY = false, withAlpha = false).ref
                material.clamping = Clamping.CLAMP
                -1 // done
            } else 1 // wait
        } else -1 // done
    }
}

// to do MeshSpawner component for long texts?
class TextMeshComponent(val text: String, val font: Font, val alignment: AxisAlignment) : ProceduralMesh() {
    override fun generateMesh(mesh: Mesh) {
        val font = FontManager.getFont(this.font)
        val meshGroup = TextMeshGroup(font, text, 0f, false, debugPieces = false)
        meshGroup.createMesh(mesh)
        val bounds = mesh.getBounds()
        val scale = 2f / TextMesh.DEFAULT_LINE_HEIGHT
        val dy = -0.5f
        val dx = -bounds.avgX() * scale + when (alignment) {
            AxisAlignment.MIN -> -1
            AxisAlignment.MAX -> +1
            else -> 0
        } * bounds.deltaX() / bounds.deltaY()
        val pos = mesh.positions!!
        for (i in pos.indices step 3) {
            pos[i] = pos[i] * scale + dx
            pos[i + 1] = pos[i + 1] * scale + dy
        }
        mesh.invalidateGeometry()
    }
}

val sdfShader = object : ECSMeshShader("SDF") {
    override fun createFragmentStages(flags: Int): List<ShaderStage> {
        return listOf(
            ShaderStage(
                "material",
                createFragmentVariables(flags) +
                        listOf(
                            Variable(GLSLType.V4F, "cameraRotation"),
                            Variable(GLSLType.V1B, "invertSDF")
                        ),
                createDefines(flags).toString() +
                        discardByCullingPlane +
                        // step by step define all material properties
                        // to do smoothstep (? would need transparency, and that's an issue...)
                        // to do smoothstep for non-deferred mode?
                        "finalAlpha = step(texture(diffuseMap,uv).x,0.5);\n" +
                        "if(invertSDF) finalAlpha = 1.0 - finalAlpha;\n" +
                        "if(finalAlpha < 0.5) discard;\n" +
                        "finalColor = vertexColor0.rgb * diffuseBase.rgb;\n" +
                        normalTanBitanCalculation +
                        normalMapCalculation +
                        emissiveCalculation +
                        occlusionCalculation +
                        metallicCalculation +
                        roughnessCalculation +
                        v0 + sheenCalculation +
                        clearCoatCalculation +
                        reflectionPlaneCalculation +
                        reflectionMapCalculation +
                        (if (motionVectors) finalMotionCalculation else "")
            ).add(ShaderLib.quatRot).add(ShaderLib.parallaxMapping)
        )
    }
}

// averaging filter to get better results?
// kills small details and corners, but the edges look better :)
val sdfAvgShader = object : ECSMeshShader("SDF-AVG") {
    override fun createFragmentStages(flags: Int): List<ShaderStage> {
        return listOf(
            ShaderStage(
                "material",
                createFragmentVariables(flags) +
                        listOf(
                            Variable(GLSLType.V4F, "cameraRotation"),
                            Variable(GLSLType.V1B, "invertSDF")
                        ),
                createDefines(flags).toString() +
                        discardByCullingPlane +
                        // step by step define all material properties
                        "vec2 duv = 0.5/textureSize(diffuseMap,0), duv1 = vec2(duv.x,-duv.y);\n" +
                        "float sdf = texture(diffuseMap,uv+duv).x + texture(diffuseMap,uv+duv1).x +\n" +
                        "            texture(diffuseMap,uv-duv).x + texture(diffuseMap,uv-duv1).x;\n" +
                        "finalAlpha = step(sdf,2.0);\n" +
                        "if(invertSDF) finalAlpha = 1.0 - finalAlpha;\n" +
                        "if(finalAlpha < 0.5) discard;\n" +
                        "finalColor = vertexColor0.rgb * diffuseBase.rgb;\n" +
                        normalTanBitanCalculation +
                        normalMapCalculation +
                        emissiveCalculation +
                        occlusionCalculation +
                        metallicCalculation +
                        roughnessCalculation +
                        v0 + sheenCalculation +
                        clearCoatCalculation +
                        reflectionPlaneCalculation +
                        reflectionMapCalculation +
                        (if (motionVectors) finalMotionCalculation else "")
            ).add(ShaderLib.quatRot).add(ShaderLib.parallaxMapping)
        )
    }
}

class SDFTextureComponent(val text: String, val font: Font, val alignment: AxisAlignment) : MeshSpawner() {

    val meshGroup = TextSDFGroup(FontManager.getFont(this.font), text, 0f)
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
                material.shader = sdfShader
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

fun main() {

    val scene = Entity("Scene")
    fun place(comp: Component, pos: Double) {
        val ent = Entity(scene)
        ent.add(comp)
        ent.position = ent.position.set(0.0, pos, 0.0)
    }

    val font = Font("Verdana", 40f)
    place(TextTextureComponent("Texture Text g", font, AxisAlignment.MIN), 0.0)
    place(SDFTextureComponent("SDF Text g", font, AxisAlignment.MAX), 0.0)
    place(TextMeshComponent("Mesh Text g", font, AxisAlignment.CENTER), -2.0)

    testSceneWithUI("Text in 3d", scene)
}