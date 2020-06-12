package de.javagl.jgltf

val pbr = "" +
        "#version 450\n" +
        "\n" +
        "layout (location = 0) in vec3 inPos;\n" +
        "layout (location = 1) in vec3 inNormal;\n" +
        "layout (location = 2) in vec2 inUV0;\n" +
        "layout (location = 3) in vec2 inUV1;\n" +
        "layout (location = 4) in vec4 inJoint0;\n" +
        "layout (location = 5) in vec4 inWeight0;\n" +
        "\n" +
        "layout (set = 0, binding = 0) uniform UBO \n" +
        "{\n" +
        "    mat4 projection;\n" +
        "    mat4 model;\n" +
        "    mat4 view;\n" +
        "    vec3 camPos;\n" +
        "} ubo;\n" +
        "\n" +
        "#define MAX_NUM_JOINTS 128\n" +
        "\n" +
        "layout (set = 2, binding = 0) uniform UBONode {\n" +
        "    mat4 matrix;\n" +
        "    mat4 jointMatrix[MAX_NUM_JOINTS];\n" +
        "    float jointCount;\n" +
        "} node;\n" +
        "\n" +
        "layout (location = 0) out vec3 outWorldPos;\n" +
        "layout (location = 1) out vec3 outNormal;\n" +
        "layout (location = 2) out vec2 outUV0;\n" +
        "layout (location = 3) out vec2 outUV1;\n" +
        "\n" +
        "out gl_PerVertex\n" +
        "{\n" +
        "    vec4 gl_Position;\n" +
        "};\n" +
        "\n" +
        "void main() \n" +
        "{\n" +
        "    vec4 locPos;\n" +
        "    if (node.jointCount > 0.0) {\n" +
        "        // Mesh is skinned\n" +
        "        mat4 skinMat = \n" +
        "            inWeight0.x * node.jointMatrix[int(inJoint0.x)] +\n" +
        "            inWeight0.y * node.jointMatrix[int(inJoint0.y)] +\n" +
        "            inWeight0.z * node.jointMatrix[int(inJoint0.z)] +\n" +
        "            inWeight0.w * node.jointMatrix[int(inJoint0.w)];\n" +
        "\n" +
        "        locPos = ubo.model * node.matrix * skinMat * vec4(inPos, 1.0);\n" +
        "        outNormal = normalize(transpose(inverse(mat3(ubo.model * node.matrix * skinMat))) * inNormal);\n" +
        "    } else {\n" +
        "        locPos = ubo.model * node.matrix * vec4(inPos, 1.0);\n" +
        "        outNormal = normalize(transpose(inverse(mat3(ubo.model * node.matrix))) * inNormal);\n" +
        "    }\n" +
        "    locPos.y = -locPos.y;\n" +
        "    outWorldPos = locPos.xyz / locPos.w;\n" +
        "    outUV0 = inUV0;\n" +
        "    outUV1 = inUV1;\n" +
        "    gl_Position =  ubo.projection * ubo.view * vec4(outWorldPos, 1.0);\n" +
        "}"