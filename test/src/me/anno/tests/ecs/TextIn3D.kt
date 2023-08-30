package me.anno.tests.ecs

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.*
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.fonts.FontManager
import me.anno.fonts.keys.TextCacheKey
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.fonts.signeddistfields.TextSDFGroup
import me.anno.gpu.GFX
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.io.files.InvalidRef
import me.anno.mesh.Shapes.flat11
import me.anno.ui.base.Font
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.utils.types.Arrays.resize
import kotlin.math.sign

/**
 * Discusses different ways to draw text in 3d
 * */

class TextTextureComponent(text: String, font: Font, val alignment: AxisAlignment, widthLimit: Int = -1) :
    ProceduralMesh() {

    val material = Material()
    val key = TextCacheKey.getKey(font, text, widthLimit, -1)

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
        val scale = 2f / bounds.deltaY()
        val dy = -bounds.avgY() * scale
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
                createFragmentVariables(flags) + listOf(Variable(GLSLType.V4F, "cameraRotation")),
                createDefines(flags).toString() +
                        discardByCullingPlane +
                        // step by step define all material properties
                        // to do smoothstep (? would need transparency, and that's an issue...)
                        // to do smoothstep for non-deferred mode?
                        "finalAlpha = step(texture(diffuseMap,uv).x,0.5);\n" +
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

class SDFTextureComponent(val text: String, val font: Font, alignment: AxisAlignment) : MeshSpawner() {

    val meshGroup = TextSDFGroup(FontManager.getFont(this.font), text, 0f)
    val materials = ArrayList<Material>()

    val size = FontManager.getSize(font, text, -1, -1)
    val alignmentOffset = when (alignment) {
        AxisAlignment.MIN -> -2f
        AxisAlignment.MAX -> 0f
        else -> -1f
    } * getSizeX(size) / getSizeY(size)

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

    override fun forEachMesh(run: (Mesh, Material?, Transform) -> Unit) {
        var i = 0
        val extraScale = 10f // todo where is this size defined?, what does it depend on?
        val baseScale = meshGroup.baseScale * extraScale
        meshGroup.draw { _, sdfTexture, offset ->
            val texture = sdfTexture?.texture
            if (texture != null) {

                val transform = getTransform(i)
                if (i >= materials.size) materials.add(Material())

                if (texture.filtering != GPUFiltering.TRULY_LINEAR && GFX.isGFXThread()) {
                    val idx = sdfShader.value.getTextureIndex("diffuseMap")
                    if (idx >= 0) {
                        texture.bind(idx) // dangerous!!!
                        texture.ensureFilterAndClamping(GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
                    }
                }

                val material = materials[i]
                material.diffuseMap = texture.ref
                material.shader = sdfShader

                val scaleX = 0.5f * texture.width * baseScale
                val scaleY = 0.5f * texture.height * baseScale

                val offsetX = alignmentOffset + offset * extraScale + sdfTexture.offset.x * scaleX
                val offsetY = sdfTexture.offset.y * scaleY - 1.0

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

    val font = Font("Verdana", 50f)
    val align = AxisAlignment.MIN
    place(TextTextureComponent("Texture Text", font, align), 0.0)
    place(TextMeshComponent("Mesh Text", font, align), -2.0)
    place(SDFTextureComponent("SDF Text", font, align), -4.0)

    testSceneWithUI("Text in 3d", scene)
}