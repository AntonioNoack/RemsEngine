package me.anno.ecs.components.mesh.utils

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.utils.structures.lists.Lists.wrap

/**
 * Defines how InstanceStacks apply their data onto drawn instances.
 * */
class MeshInstanceData(
    val transformPosition: List<ShaderStage>,
    val transformNorTan: List<ShaderStage>,
    val transformColors: List<ShaderStage>,
    val transformMotionVec: List<ShaderStage>,
) {

    constructor(
        transformPosition: ShaderStage,
        transformNorTan: ShaderStage,
        transformColors: ShaderStage?,
        transformMotionVec: ShaderStage
    ) : this(
        listOf(transformPosition),
        listOf(transformNorTan),
        transformColors.wrap(),
        listOf(transformMotionVec)
    )

    companion object {

        val DEFAULT = MeshInstanceData(
            ShaderStage(
                "def-pos", listOf(
                    Variable(GLSLType.M4x3, "localTransform"),
                    Variable(GLSLType.V3F, "localPosition"),
                    Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT)
                ), "finalPosition = matMul(localTransform, vec4(localPosition, 1.0));\n"
            ),
            ShaderStage(
                "def-nor", listOf(
                    Variable(GLSLType.V3F, "normal", VariableMode.INOUT),
                    Variable(GLSLType.V4F, "tangent", VariableMode.INOUT),
                    Variable(GLSLType.M4x3, "localTransform"),
                ), "" +
                        "normal = normalize(matMul(localTransform, vec4(normal,0.0)));\n" +
                        "tangent.xyz = normalize(matMul(localTransform, vec4(tangent.xyz,0.0)));\n"
            ),
            null, // colors aren't changed
            ShaderStage(
                "def-mov", listOf(
                    Variable(GLSLType.V3F, "prevLocalPosition"),
                    Variable(GLSLType.M4x3, "prevLocalTransform"),
                    Variable(GLSLType.V4F, "prevPosition", VariableMode.OUT)
                ),
                "prevPosition = vec4(matMul(prevLocalTransform, vec4(prevLocalPosition, 1.0)),1.0);\n"
            )
        )

        val DEFAULT_INSTANCED = MeshInstanceData(
            /*listOf(
                // todo how do we define them?
                Attribute("animWeights", 4),
                Attribute("animIndices", 4),
                Attribute("prevAnimWeights", 4),
                Attribute("prevAnimIndices", 4),
            ),*/
            listOf(
                ShaderStage(
                    "def-pos", listOf(
                        Variable(GLSLType.V4F, "instanceFinalId", VariableMode.ATTR),
                        Variable(GLSLType.V4F, "finalId", VariableMode.OUT).flat(),
                        Variable(GLSLType.V4F, "instanceTrans0", VariableMode.ATTR),
                        Variable(GLSLType.V4F, "instanceTrans1", VariableMode.ATTR),
                        Variable(GLSLType.V4F, "instanceTrans2", VariableMode.ATTR),
                        Variable(GLSLType.M4x3, "localTransform", VariableMode.OUT).flat(),
                        Variable(GLSLType.M4x3, "invLocalTransform", VariableMode.OUT).flat(),
                    ),
                    "" +
                            "finalId = instanceFinalId;\n" +
                            "localTransform = loadMat4x3(instanceTrans0,instanceTrans1,instanceTrans2);\n" +
                            // todo we could optimize this... only needed by a few shaders...
                            "invLocalTransform = inverse4x3(localTransform);\n"
                ).add(ShaderLib.loadMat4x3).add(ShaderLib.inverseMat4x3)
            ) + DEFAULT.transformPosition,
            DEFAULT.transformNorTan,
            emptyList(), // colors aren't changed
            listOf(
                ShaderStage(
                    "def-mov", listOf(
                        Variable(GLSLType.V4F, "instancePrevTrans0", VariableMode.ATTR),
                        Variable(GLSLType.V4F, "instancePrevTrans1", VariableMode.ATTR),
                        Variable(GLSLType.V4F, "instancePrevTrans2", VariableMode.ATTR),
                        Variable(GLSLType.M4x3, "prevLocalTransform", VariableMode.OUT).flat()
                    ),
                    "prevLocalTransform = loadMat4x3(instancePrevTrans0,instancePrevTrans1,instancePrevTrans2);\n"
                ).add(ShaderLib.loadMat4x3)
            ) + DEFAULT.transformMotionVec
        )

        val TRS = MeshInstanceData(
            listOf(
                ShaderStage(
                    "trs-pos",
                    listOf(
                        Variable(GLSLType.V4F, "instancePosSize", VariableMode.ATTR),
                        Variable(GLSLType.V4F, "instanceRot", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "localPosition"),
                        Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT)
                    ),
                    "finalPosition = quatRot(localPosition, instanceRot) * instancePosSize.w + instancePosSize.xyz;\n"
                ).add(ShaderLib.quatRot)
            ),
            listOf(
                ShaderStage(
                    "trs-nor", listOf(
                        Variable(GLSLType.V4F, "instanceRot", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "normal", VariableMode.INOUT),
                        Variable(GLSLType.V4F, "tangent", VariableMode.INOUT)
                    ), "normal = quatRot(normal, instanceRot);\n" +
                            "tangent.xyz = quatRot(tangent.xyz, instanceRot);\n"
                ).add(ShaderLib.quatRot)
            ),
            emptyList(), // colors aren't changed
            listOf(
                ShaderStage(
                    "trs-mov", listOf(
                        Variable(GLSLType.V3F, "finalPosition"),
                        Variable(GLSLType.V4F, "prevPosition", VariableMode.OUT),
                    ), "prevPosition = vec4(finalPosition,1.0);\n"
                )
            )
        )
    }
}