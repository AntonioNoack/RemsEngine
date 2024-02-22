package me.anno.ecs.components.mesh.material.shaders

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

object FurShader : ECSMeshShader("Fur") {
    override fun createVertexStages(key: ShaderKey): List<ShaderStage> {
        // return super.createVertexStages(flags)
        // extrude by hair length along normals
        val variables = createAnimVariables(key) + listOf(
            Variable(GLSLType.V1F, "hairLength"),
            Variable(GLSLType.V1F, "relativeHairLength"),
            Variable(GLSLType.V3F, "hairGravity"),
            Variable(GLSLType.V3F, "normal"),
            Variable(GLSLType.V1I, "instanceId", VariableMode.OUT),
            Variable(GLSLType.V3F, "localPosition", VariableMode.INOUT),
            //Variable(GLSLType.V3F, "localPositionDIndex", VariableMode.OUT),
            Variable(GLSLType.V3F, "seedBase", VariableMode.OUT),
        )
        val hullStage = ShaderStage(
            "vertex",
            variables, "" +
                    "instanceId = gl_InstanceID;\n" +
                    "float instanceIdF = float(instanceId);\n" +
                    // normalization is better to keep the hair length unaffected by gravity
                    // this square dependency on the relative height makes nice curves
                    "seedBase = localPosition;\n" +
                    "localPosition += normalize(normal + hairGravity * (instanceIdF * relativeHairLength)) * instanceIdF * hairLength;\n"
        )
        return createDefines(key) +
                loadVertex(key) +
                hullStage +
                animateVertex(key) +
                transformVertex(key) +
                finishVertex(key)
    }

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        // discards pixels that don't belong to the currently processed hair
        val furDiscardStage = ShaderStage(
            "furStage", listOf(
                Variable(GLSLType.V1F, "hairDensity"),
                Variable(GLSLType.V1F, "relativeHairLength"),
                Variable(GLSLType.V1F, "hairSharpness"),
                Variable(GLSLType.V3F, "seedBase"),
                Variable(GLSLType.V3F, "normal", VariableMode.INOUT),
                Variable(GLSLType.V1I, "instanceId", VariableMode.INOUT),
            ), "" +
                    // discard if not hair
                    "if(instanceId > 0) {\n" +
                    "   vec3 hairSeed0 = seedBase * hairDensity;\n" +
                    "   vec3 hairSeed = round(hairSeed0);\n" +
                    "   vec3 hairSeedFract = (hairSeed0 - hairSeed) * hairSharpness;\n" +
                    "   vec3 normal1 = gl_FrontFacing ? normal : -normal;\n" +
                    // orthogonalize to normal
                    "   hairSeedFract -= dot(normalize(normal1), hairSeedFract);\n" +
                    "   float hairRandom = fract(sin(dot(hairSeed, vec3(1.29898, 0.41414, 0.95153))) * 43758.5453);\n" +
                    "   if(hairRandom * (1.0 - length(hairSeedFract)) < float(instanceId) * relativeHairLength) discard;\n" +
                    "}\n"
        )
        // increase natural occlusion at the bottom
        val furOcclusionStage = ShaderStage(
            "furOcclusion", listOf(
                Variable(GLSLType.V1F, "relativeHairLength"),
                Variable(GLSLType.V1F, "hairSharpness"),
                Variable(GLSLType.V1I, "instanceId"),
                Variable(GLSLType.V1F, "finalTranslucency"),
                Variable(GLSLType.V1F, "finalOcclusion", VariableMode.OUT)
            ), "" +
                    "float hairHeight = float(instanceId) * relativeHairLength;\n" +
                    "finalOcclusion = (1.0 - finalTranslucency) * (1.0 - hairHeight) / (1.2 + 0.7 * hairSharpness);\n"
        )
        return listOf(furDiscardStage) + super.createFragmentStages(key) + furOcclusionStage
    }
}