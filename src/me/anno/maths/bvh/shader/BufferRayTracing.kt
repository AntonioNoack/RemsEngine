package me.anno.maths.bvh.shader

import me.anno.maths.bvh.BLASNode

object BufferRayTracing {

    val bufferStructs = "" +
            "struct Vertex {\n" +
            "   vec3 pos;\n" +
            "   uint _pad0;\n" +
            "#if ${BLASNode.PIXELS_PER_VERTEX} > 1\n" +
            "   vec3 nor;\n" +
            "   uint color;\n" +
            "#endif\n" +
            "};\n" +
            "struct BLASNode {\n" +
            "   vec3 min;\n" +
            "   uint v0;\n" +
            "   vec3 max;\n" +
            "   uint v1;\n" +
            "};\n" +
            "struct TLASNode0 {\n" +
            "   vec3    min;\n" +
            "   uint    v0;\n" +
            "   vec3    max;\n" +
            "   uint    v1;\n" +
            "};\n"+
            "struct TLASNode1 {\n" +
            // mat4x3 seems to have a different layout -> it must have 4x4 layout instead of 4x3 🤨
            "   vec4 w2l0, w2l1, w2l2;\n" +
            "   vec4 l2w0, l2w1, l2w2;\n" +
            "};\n"

    // std430 needed? yes, core since 4.3
    val bufferLayouts = "" +
            "layout(std140, shared, binding = 0) readonly buffer triangles  { Vertex vertices[]; };\n" +
            "layout(std140, shared, binding = 1) readonly buffer blasBuffer { BLASNode blasNodes[]; };\n" +
            "layout(std140, shared, binding = 2) readonly buffer tlasBuffer0 { TLASNode0 tlasNodes0[]; };\n" +
            "layout(std140, shared, binding = 3) readonly buffer tlasBuffer1 { TLASNode1 tlasNodes1[]; };\n" +
            "layout(rgba32f, binding = 4) uniform image2D dst;\n"

}