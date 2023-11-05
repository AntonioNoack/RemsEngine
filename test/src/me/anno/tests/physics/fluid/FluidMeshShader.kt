package me.anno.tests.physics.fluid

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.TypeValueV2
import me.anno.ecs.components.mesh.terrain.TerrainUtils
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.types.Arrays.resize

object FluidMeshShader : ECSMeshShader("fluid") {
    override fun createVertexStages(key: ShaderKey): List<ShaderStage> {
        val variables = createAnimVariables(key)
        val fluidYAndNormalStage = ShaderStage(
            "vertex",
            variables + listOf(
                Variable(GLSLType.S2D, "heightTex"),
                Variable(GLSLType.V1F, "waveHeight"),
                Variable(GLSLType.V2F, "uvs"),
                Variable(GLSLType.V3F, "localPosition", VariableMode.INOUT),
                Variable(GLSLType.V3F, "normal", VariableMode.OUT),
                Variable(GLSLType.V4F, "tangent", VariableMode.OUT),
                Variable(GLSLType.V4F, "tangents")
            ), "" +
                    "localPosition.y += waveHeight * texture(heightTex,uvs).x;\n" + // is output, so no declaration needed
                    "#ifdef COLORS\n" +
                    "   vec2 texSize = textureSize(heightTex,0);\n" +
                    "   vec2 du = vec2(1.0/texSize.x,0.0), dv = vec2(0.0,1.0/texSize.y);\n" +
                    "   float dx = texture(heightTex,uvs+du).x - texture(heightTex,uvs-du).x;\n" +
                    "   float dz = texture(heightTex,uvs+dv).x - texture(heightTex,uvs-dv).x;\n" +
                    "   normal = normalize(vec3(dx*waveHeight, 1.0, dz*waveHeight));\n" +
                    "   tangent = tangents;\n" +
                    "#endif\n"
        )
        return createDefines(key) +
                loadVertex(key) +
                fluidYAndNormalStage +
                transformVertex(key) +
                finishVertex(key)
    }

    fun createFluidMesh(sim: FluidSimulation, waveHeight: Float): Mesh {

        // todo use procedural mesh instead?

        val w = sim.width
        val h = sim.height
        val mesh = Mesh()
        TerrainUtils.generateRegularQuadHeightMesh(w, h, false, cellSize, mesh)
        // generate UVs
        val pos = mesh.positions!!
        val uvs = mesh.uvs.resize(pos.size / 3 * 2)
        val invU = 1f / (w * cellSize)
        val invV = 1f / (h * cellSize)
        for (i in uvs.indices step 2) {
            uvs[i] = 0.5f + pos[i / 2 * 3] * invU
            uvs[i + 1] = 0.5f - pos[i / 2 * 3 + 2] * invV
        }
        mesh.uvs = uvs
        mesh.invalidateGeometry()

        val fluidMaterial = Material()
        fluidMaterial.shader = FluidMeshShader
        fluidMaterial.shaderOverrides["heightTex"] = TypeValueV2(GLSLType.S2D) { sim.pressure.read }
        fluidMaterial.shaderOverrides["waveHeight"] = TypeValue(GLSLType.V1F, waveHeight)
        fluidMaterial.pipelineStage = PipelineStage.TRANSPARENT_PASS
        fluidMaterial.metallicMinMax.set(1f)
        fluidMaterial.roughnessMinMax.set(0f)
        fluidMaterial.diffuseBase.w = 1f
        fluidMaterial.indexOfRefraction = 1.33f // water
        mesh.materials = listOf(fluidMaterial.ref)
        return mesh
    }
}