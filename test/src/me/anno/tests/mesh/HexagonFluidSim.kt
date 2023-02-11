package me.anno.tests.mesh

import me.anno.ecs.components.chunks.spherical.HexagonSphere
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.ComputeShader
import me.anno.maths.noise.PerlinNoise
import org.joml.Vector3i

fun main() {

    HiddenOpenGLContext.createOpenGL()

    val hexagons = HexagonSphere.createHexSphere(4)
    val dynamicLayout = listOf(
        Attribute("flow0", AttributeType.FLOAT, 4),
        Attribute("flow1", AttributeType.FLOAT, 2),
        Attribute("height", AttributeType.FLOAT, 1),
        Attribute("pad0", AttributeType.FLOAT, 1),
    )
    val dynamicData0 = ComputeBuffer(hexagons.size, dynamicLayout)
    val dynamicData1 = ComputeBuffer(hexagons.size, dynamicLayout)
    val staticData = ComputeBuffer(
        hexagons.size, listOf(
            Attribute("neighbor0", AttributeType.UINT32, 4),
            Attribute("neighbor1", AttributeType.UINT32, 2),
            Attribute("terrain", AttributeType.FLOAT, 1),
            Attribute("pad0", AttributeType.FLOAT, 1),
        )
    )

    val fluidPerlin = PerlinNoise(1234L, 8, 0.5f, 0f, 10f)
    val terrainPerlin = PerlinNoise(1234L, 8, 0.5f, 0f, 10f)

    // store initial data into compute buffers/textures
    dynamicData0.createNioBuffer()
    staticData.createNioBuffer()
    for (hex in hexagons) {
        val center = hex.center
        dynamicData0.put(0f, 0f, 0f, 0f) // flow0
        dynamicData0.put(0f, 0f) // flow1
        dynamicData0.put(fluidPerlin[center.x, center.y, center.z]) // height
        dynamicData0.put(0f) // padding
        val nei = hex.neighborIds
        for (i in 0 until 6) staticData.putInt(nei[i])
        staticData.put(terrainPerlin[center.x, center.y, center.z])
        staticData.put(0f)
    }

    val updateShader = ComputeShader(
        "fluid", Vector3i(256, 1, 1), "" +
                "struct Static {\n" +
                "   ivec4 neighbor0;\n" +
                "   ivec2 neighbor1;\n" +
                "   float terrain;\n" +
                "   float pad0;\n" +
                "};\n" +
                "struct Dynamic {\n" +
                "   vec4 flow0;\n" +
                "   vec2 flow1;\n" +
                "   float height;\n" +
                "   float pad0;\n" +
                "};\n" +
                // std430 needed? yes, core since 4.3
                "layout(std140, shared, binding = 0) readonly buffer statics { Static statics1[]; };\n" +
                "layout(std140, shared, binding = 1) readonly buffer dynamicSrc { Dynamic dynamicSrc1[]; };\n" +
                "layout(std140, shared, binding = 2)          buffer dynamicDst { Dynamic dynamicDst1[]; };\n" +
                "uniform int totalSize;\n" +
                "void main(){\n" +
                "   int index = int(gl_GlobalInvocationID.x);\n" +
                "   if(index < totalSize){\n" +
                // todo for all neighbors and self, calculate the flow into us
                // todo apply this flow
                "       " +
                "   }\n" +
                "}\n"
    )

    updateShader.use()
    updateShader.bindBuffer(0, staticData)
    updateShader.bindBuffer(1, dynamicData0)
    updateShader.bindBuffer(2, dynamicData1)
    updateShader.v1i("totalSize", hexagons.size)
    updateShader.runBySize(hexagons.size)

    // todo create shader to update fluid
    // todo visual shader for sphere
}