package de.javagl.jgltf

val pbrVert = "" +
        "#version 450\n" +
        
        "in vec3 inPos;\n" +
        "in vec3 inNormal;\n" +
        "in vec2 inUV0;\n" +
        "in vec2 inUV1;\n" +
        "in vec4 inJoint0;\n" +
        "in vec4 inWeight0;\n" +
        
        "uniform UBO {\n" +
        "    mat4 projection;\n" +
        "    mat4 model;\n" +
        "    mat4 view;\n" +
        "    vec3 camPos;\n" +
        "} ubo;\n" +
        
        "#define MAX_NUM_JOINTS 128\n" +
        
        "uniform UBONode {\n" +
        "    mat4 matrix;\n" +
        "    mat4 jointMatrix[MAX_NUM_JOINTS];\n" +
        "    float jointCount;\n" +
        "} node;\n" +
        
        "out vec3 outWorldPos;\n" +
        "out vec3 outNormal;\n" +
        "out vec2 outUV0;\n" +
        "out vec2 outUV1;\n" +
        
        "out gl_PerVertex {\n" +
        "    vec4 gl_Position;\n" +
        "};\n" +
        
        "void main(){\n" +
        "    vec4 locPos;\n" +
        "    if (node.jointCount > 0.0) {\n" +
        "        // Mesh is skinned\n" +
        "        mat4 skinMat = \n" +
        "            inWeight0.x * node.jointMatrix[int(inJoint0.x)] +\n" +
        "            inWeight0.y * node.jointMatrix[int(inJoint0.y)] +\n" +
        "            inWeight0.z * node.jointMatrix[int(inJoint0.z)] +\n" +
        "            inWeight0.w * node.jointMatrix[int(inJoint0.w)];\n" +
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

val pbrFrag = "" +
        // PBR shader based on the Khronos WebGL PBR implementation\n" +
        // See https://github.com/KhronosGroup/glTF-WebGL-PBR\n" +
        // Supports both metallic roughness and specular glossiness inputs\n" +
        
        "#version 450\n" +
        
        "layout (location = 0) in vec3 inWorldPos;\n" +
        "layout (location = 1) in vec3 inNormal;\n" +
        "layout (location = 2) in vec2 inUV0;\n" +
        "layout (location = 3) in vec2 inUV1;\n" +
        
        // Scene bindings\n" +
        
        "uniform UBO {\n" +
        "  mat4 projection;\n" +
        "  mat4 model;\n" +
        "  mat4 view;\n" +
        "  vec3 camPos;\n" +
        "} ubo;\n" +
        
        "uniform UBOParams {\n" +
        "  vec4 lightDir;\n" +
        "  float exposure;\n" +
        "  float gamma;\n" +
        "  float prefilteredCubeMipLevels;\n" +
        "  float scaleIBLAmbient;\n" +
        "  float debugViewInputs;\n" +
        "  float debugViewEquation;\n" +
        "} uboParams;\n" +

        // todo do better than constant assignments
        // todo be sure, that jgltf binds them correctly
        "uniform samplerCube samplerIrradiance;\n" +
        "uniform samplerCube prefilteredMap;\n" +
        "uniform sampler2D samplerBRDFLUT;\n" +
        
        // Material bindings\n" +
        
        "uniform sampler2D colorMap;\n" +
        "uniform sampler2D physicalDescriptorMap;\n" +
        "uniform sampler2D normalMap;\n" +
        "uniform sampler2D aoMap;\n" +
        "uniform sampler2D emissiveMap;\n" +
        
        "uniform Material {\n" +
        "  vec4 baseColorFactor;\n" +
        "  vec4 emissiveFactor;\n" +
        "  vec4 diffuseFactor;\n" +
        "  vec4 specularFactor;\n" +
        "  float workflow;\n" +
        "  int baseColorTextureSet;\n" +
        "  int physicalDescriptorTextureSet;\n" +
        "  int normalTextureSet;  \n" +
        "  int occlusionTextureSet;\n" +
        "  int emissiveTextureSet;\n" +
        "  float metallicFactor;  \n" +
        "  float roughnessFactor;  \n" +
        "  float alphaMask;  \n" +
        "  float alphaMaskCutoff;\n" +
        "} material;\n" +
        
        "out vec4 outColor;\n" +
        
        // Encapsulate the various inputs used by the various functions in the shading equation\n" +
        // We store values in this struct to simplify the integration of alternative implementations\n" +
        // of the shading terms, outlined in the Readme.MD Appendix.\n" +
        "struct PBRInfo {\n" +
        "  float NdotL;                  // cos angle between normal and light direction\n" +
        "  float NdotV;                  // cos angle between normal and view direction\n" +
        "  float NdotH;                  // cos angle between normal and half vector\n" +
        "  float LdotH;                  // cos angle between light direction and half vector\n" +
        "  float VdotH;                  // cos angle between view direction and half vector\n" +
        "  float perceptualRoughness;    // roughness value, as authored by the model creator (input to shader)\n" +
        "  float metalness;              // metallic value at the surface\n" +
        "  vec3 reflectance0;            // full reflectance color (normal incidence angle)\n" +
        "  vec3 reflectance90;           // reflectance color at grazing angle\n" +
        "  float alphaRoughness;         // roughness mapped to a more linear change in the roughness (proposed by [2])\n" +
        "  vec3 diffuseColor;            // color contribution from diffuse lighting\n" +
        "  vec3 specularColor;           // color contribution from specular lighting\n" +
        "};\n" +
        
        "const float M_PI = 3.141592653589793;\n" +
        "const float c_MinRoughness = 0.04;\n" +
        
        "const float PBR_WORKFLOW_METALLIC_ROUGHNESS = 0.0;\n" +
        "const float PBR_WORKFLOW_SPECULAR_GLOSINESS = 1.0f;\n" +
        
        "#define MANUAL_SRGB 1\n" +
        
        "vec3 Uncharted2Tonemap(vec3 color){\n" +
        "  float A = 0.15;\n" +
        "  float B = 0.50;\n" +
        "  float C = 0.10;\n" +
        "  float D = 0.20;\n" +
        "  float E = 0.02;\n" +
        "  float F = 0.30;\n" +
        "  float W = 11.2;\n" +
        "  return ((color*(A*color+C*B)+D*E)/(color*(A*color+B)+D*F))-E/F;\n" +
        "}\n" +
        
        "vec4 tonemap(vec4 color){\n" +
        "  vec3 outcol = Uncharted2Tonemap(color.rgb * uboParams.exposure);\n" +
        "  outcol = outcol * (1.0f / Uncharted2Tonemap(vec3(11.2f)));  \n" +
        "  return vec4(pow(outcol, vec3(1.0f / uboParams.gamma)), color.a);\n" +
        "}\n" +
        
        "vec4 SRGBtoLINEAR(vec4 srgbIn){\n" +
        "  #ifdef MANUAL_SRGB\n" +
        "  #ifdef SRGB_FAST_APPROXIMATION\n" +
        "  vec3 linOut = pow(srgbIn.xyz,vec3(2.2));\n" +
        "  #else //SRGB_FAST_APPROXIMATION\n" +
        "  vec3 bLess = step(vec3(0.04045),srgbIn.xyz);\n" +
        "  vec3 linOut = mix( srgbIn.xyz/vec3(12.92), pow((srgbIn.xyz+vec3(0.055))/vec3(1.055),vec3(2.4)), bLess );\n" +
        "  #endif //SRGB_FAST_APPROXIMATION\n" +
        "  return vec4(linOut,srgbIn.w);;\n" +
        "  #else //MANUAL_SRGB\n" +
        "  return srgbIn;\n" +
        "  #endif //MANUAL_SRGB\n" +
        "}\n" +
        
        // Find the normal for this fragment, pulling either from a predefined normal map\n" +
        // or from the interpolated mesh normal and tangent attributes.\n" +
        "vec3 getNormal(){\n" +

        // Perturb normal, see http://www.thetenthplanet.de/archives/1180\n" +
        "  vec3 tangentNormal = texture(normalMap, material.normalTextureSet == 0 ? inUV0 : inUV1).xyz * 2.0 - 1.0;\n" +
        
        "  vec3 q1 = dFdx(inWorldPos);\n" +
        "  vec3 q2 = dFdy(inWorldPos);\n" +
        "  vec2 st1 = dFdx(inUV0);\n" +
        "  vec2 st2 = dFdy(inUV0);\n" +
        
        "  vec3 N = normalize(inNormal);\n" +
        "  vec3 T = normalize(q1 * st2.t - q2 * st1.t);\n" +
        "  vec3 B = -normalize(cross(N, T));\n" +
        "  mat3 TBN = mat3(T, B, N);\n" +
        
        "  return normalize(TBN * tangentNormal);\n" +
        "}\n" +
        
        // Calculation of the lighting contribution from an optional Image Based Light source.\n" +
        // Precomputed Environment Maps are required uniform inputs and are computed as outlined in [1].\n" +
        // See our README.md on Environment Maps [3] for additional discussion.\n" +
        "vec3 getIBLContribution(PBRInfo pbrInputs, vec3 n, vec3 reflection){\n" +

        "  float lod = (pbrInputs.perceptualRoughness * uboParams.prefilteredCubeMipLevels);\n" +
        // retrieve a scale and bias to F0. See [1], Figure 3\n" +
        "  vec3 brdf = (texture(samplerBRDFLUT, vec2(pbrInputs.NdotV, 1.0 - pbrInputs.perceptualRoughness))).rgb;\n" +
        "  vec3 diffuseLight = SRGBtoLINEAR(tonemap(texture(samplerIrradiance, n))).rgb;\n" +
        
        "  vec3 specularLight = SRGBtoLINEAR(tonemap(textureLod(prefilteredMap, reflection, lod))).rgb;\n" +
        
        "  vec3 diffuse = diffuseLight * pbrInputs.diffuseColor;\n" +
        "  vec3 specular = specularLight * (pbrInputs.specularColor * brdf.x + brdf.y);\n" +
        
        "  return (diffuse + specular) * uboParams.scaleIBLAmbient;\n" +

        "}\n" +
        
        // Basic Lambertian diffuse\n" +
        // Implementation from Lambert's Photometria https://archive.org/details/lambertsphotome00lambgoog\n" +
        // See also [1], Equation 1\n" +
        "vec3 diffuse(PBRInfo pbrInputs){\n" +
        "  return pbrInputs.diffuseColor / M_PI;\n" +
        "}\n" +
        
        // The following equation models the Fresnel reflectance term of the spec equation (aka F())\n" +
        // Implementation of fresnel from [4], Equation 15\n" +
        "vec3 specularReflection(PBRInfo pbrInputs){\n" +
        "  return pbrInputs.reflectance0 + (pbrInputs.reflectance90 - pbrInputs.reflectance0) * pow(clamp(1.0 - pbrInputs.VdotH, 0.0, 1.0), 5.0);\n" +
        "}\n" +
        
        // This calculates the specular geometric attenuation (aka G()),\n" +
        // where rougher material will reflect less light back to the viewer.\n" +
        // This implementation is based on [1] Equation 4, and we adopt their modifications to\n" +
        // alphaRoughness as input as originally proposed in [2].\n" +
        "float geometricOcclusion(PBRInfo pbrInputs){\n" +

        "  float NdotL = pbrInputs.NdotL;\n" +
        "  float NdotV = pbrInputs.NdotV;\n" +
        "  float r = pbrInputs.alphaRoughness;\n" +
        
        "  float attenuationL = 2.0 * NdotL / (NdotL + sqrt(r * r + (1.0 - r * r) * (NdotL * NdotL)));\n" +
        "  float attenuationV = 2.0 * NdotV / (NdotV + sqrt(r * r + (1.0 - r * r) * (NdotV * NdotV)));\n" +
        "  return attenuationL * attenuationV;\n" +
        "}\n" +
        
        // The following equation(s) model the distribution of microfacet normals across the area being drawn (aka D())\n" +
        // Implementation from \"Average Irregularity Representation of a Roughened Surface for Ray Reflection\" by T. S. Trowbridge, and K. P. Reitz\n" +
        // Follows the distribution function recommended in the SIGGRAPH 2013 course notes from EPIC Games [1], Equation 3.\n" +
        "float microfacetDistribution(PBRInfo pbrInputs){\n" +
        "  float roughnessSq = pbrInputs.alphaRoughness * pbrInputs.alphaRoughness;\n" +
        "  float f = (pbrInputs.NdotH * roughnessSq - pbrInputs.NdotH) * pbrInputs.NdotH + 1.0;\n" +
        "  return roughnessSq / (M_PI * f * f);\n" +
        "}\n" +
        
        // Gets metallic factor from specular glossiness workflow inputs \n" +
        "float convertMetallic(vec3 diffuse, vec3 specular, float maxSpecular) {\n" +
        "  float perceivedDiffuse = sqrt(0.299 * diffuse.r * diffuse.r + 0.587 * diffuse.g * diffuse.g + 0.114 * diffuse.b * diffuse.b);\n" +
        "  float perceivedSpecular = sqrt(0.299 * specular.r * specular.r + 0.587 * specular.g * specular.g + 0.114 * specular.b * specular.b);\n" +
        "  if (perceivedSpecular < c_MinRoughness) {\n" +
        "    return 0.0;\n" +
        "  }\n" +
        "  float a = c_MinRoughness;\n" +
        "  float b = perceivedDiffuse * (1.0 - maxSpecular) / (1.0 - c_MinRoughness) + perceivedSpecular - 2.0 * c_MinRoughness;\n" +
        "  float c = c_MinRoughness - perceivedSpecular;\n" +
        "  float D = max(b * b - 4.0 * a * c, 0.0);\n" +
        "  return clamp((-b + sqrt(D)) / (2.0 * a), 0.0, 1.0);\n" +
        "}\n" +
        
        "void main(){\n" +

        "  float perceptualRoughness;\n" +
        "  float metallic;\n" +
        "  vec3 diffuseColor;\n" +
        "  vec4 baseColor;\n" +
        
        "  vec3 f0 = vec3(0.04);\n" +
        
        "  if (material.alphaMask == 1.0f) {\n" +
        "    if (material.baseColorTextureSet > -1) {\n" +
        "      baseColor = SRGBtoLINEAR(texture(colorMap, material.baseColorTextureSet == 0 ? inUV0 : inUV1)) * material.baseColorFactor;\n" +
        "    } else {\n" +
        "      baseColor = material.baseColorFactor;\n" +
        "    }\n" +
        "    if (baseColor.a < material.alphaMaskCutoff) {\n" +
        "      discard;\n" +
        "    }\n" +
        "  }\n" +
        
        "  if (material.workflow == PBR_WORKFLOW_METALLIC_ROUGHNESS) {\n" +
        "    // Metallic and Roughness material properties are packed together\n" +
        "    // In glTF, these factors can be specified by fixed scalar values\n" +
        "    // or from a metallic-roughness map\n" +
        "    perceptualRoughness = material.roughnessFactor;\n" +
        "    metallic = material.metallicFactor;\n" +
        "    if (material.physicalDescriptorTextureSet > -1) {\n" +
        "      // Roughness is stored in the 'g' channel, metallic is stored in the 'b' channel.\n" +
        "      // This layout intentionally reserves the 'r' channel for (optional) occlusion map data\n" +
        "      vec4 mrSample = texture(physicalDescriptorMap, material.physicalDescriptorTextureSet == 0 ? inUV0 : inUV1);\n" +
        "      perceptualRoughness = mrSample.g * perceptualRoughness;\n" +
        "      metallic = mrSample.b * metallic;\n" +
        "    } else {\n" +
        "      perceptualRoughness = clamp(perceptualRoughness, c_MinRoughness, 1.0);\n" +
        "      metallic = clamp(metallic, 0.0, 1.0);\n" +
        "    }\n" +
        // Roughness is authored as perceptual roughness; as is convention,\n" +
         // convert to material roughness by squaring the perceptual roughness [2].\n" +
        
        // The albedo may be defined from a base texture or a flat color\n" +
        "    if (material.baseColorTextureSet > -1) {\n" +
        "      baseColor = SRGBtoLINEAR(texture(colorMap, material.baseColorTextureSet == 0 ? inUV0 : inUV1)) * material.baseColorFactor;\n" +
        "    } else {\n" +
        "      baseColor = material.baseColorFactor;\n" +
        "    }\n" +
        "  }\n" +
        
        "  if (material.workflow == PBR_WORKFLOW_SPECULAR_GLOSINESS) {\n" +
        // Values from specular glossiness workflow are converted to metallic roughness\n" +
        "    if (material.physicalDescriptorTextureSet > -1) {\n" +
        "      perceptualRoughness = 1.0 - texture(physicalDescriptorMap, material.physicalDescriptorTextureSet == 0 ? inUV0 : inUV1).a;\n" +
        "    } else {\n" +
        "      perceptualRoughness = 0.0;\n" +
        "    }\n" +
        
        "    const float epsilon = 1e-6;\n" +
        
        "    vec4 diffuse = SRGBtoLINEAR(texture(colorMap, inUV0));\n" +
        "    vec3 specular = SRGBtoLINEAR(texture(physicalDescriptorMap, inUV0)).rgb;\n" +
        
        "    float maxSpecular = max(max(specular.r, specular.g), specular.b);\n" +
        
        // Convert metallic value from specular glossiness inputs\n" +
        "    metallic = convertMetallic(diffuse.rgb, specular, maxSpecular);\n" +
        
        "    vec3 baseColorDiffusePart = diffuse.rgb * ((1.0 - maxSpecular) / (1 - c_MinRoughness) / max(1 - metallic, epsilon)) * material.diffuseFactor.rgb;\n" +
        "    vec3 baseColorSpecularPart = specular - (vec3(c_MinRoughness) * (1 - metallic) * (1 / max(metallic, epsilon))) * material.specularFactor.rgb;\n" +
        "    baseColor = vec4(mix(baseColorDiffusePart, baseColorSpecularPart, metallic * metallic), diffuse.a);\n" +
        
        "  }\n" +
        
        "  diffuseColor = baseColor.rgb * (vec3(1.0) - f0);\n" +
        "  diffuseColor *= 1.0 - metallic;\n" +
        "    \n" +
        "  float alphaRoughness = perceptualRoughness * perceptualRoughness;\n" +
        
        "  vec3 specularColor = mix(f0, baseColor.rgb, metallic);\n" +
        
        "  // Compute reflectance.\n" +
        "  float reflectance = max(max(specularColor.r, specularColor.g), specularColor.b);\n" +
        
        "  // For typical incident reflectance range (between 4% to 100%) set the grazing reflectance to 100% for typical fresnel effect.\n" +
        "  // For very low reflectance range on highly diffuse objects (below 4%), incrementally reduce grazing reflecance to 0%.\n" +
        "  float reflectance90 = clamp(reflectance * 25.0, 0.0, 1.0);\n" +
        "  vec3 specularEnvironmentR0 = specularColor.rgb;\n" +
        "  vec3 specularEnvironmentR90 = vec3(1.0, 1.0, 1.0) * reflectance90;\n" +
        
        "  vec3 n = (material.normalTextureSet > -1) ? getNormal() : normalize(inNormal);\n" +
        "  vec3 v = normalize(ubo.camPos - inWorldPos);\n" + // Vector from surface point to camera
        "  vec3 l = normalize(uboParams.lightDir.xyz);\n" + // Vector from surface point to light
        "  vec3 h = normalize(l+v);\n" + // Half vector between both l and v
        "  vec3 reflection = -normalize(reflect(v, n));\n" +
        "  reflection.y *= -1.0f;\n" +
        
        "  float NdotL = clamp(dot(n, l), 0.001, 1.0);\n" +
        "  float NdotV = clamp(abs(dot(n, v)), 0.001, 1.0);\n" +
        "  float NdotH = clamp(dot(n, h), 0.0, 1.0);\n" +
        "  float LdotH = clamp(dot(l, h), 0.0, 1.0);\n" +
        "  float VdotH = clamp(dot(v, h), 0.0, 1.0);\n" +
        
        "  PBRInfo pbrInputs = PBRInfo(\n" +
        "    NdotL,\n" +
        "    NdotV,\n" +
        "    NdotH,\n" +
        "    LdotH,\n" +
        "    VdotH,\n" +
        "    perceptualRoughness,\n" +
        "    metallic,\n" +
        "    specularEnvironmentR0,\n" +
        "    specularEnvironmentR90,\n" +
        "    alphaRoughness,\n" +
        "    diffuseColor,\n" +
        "    specularColor\n" +
        "  );\n" +
        
        "  // Calculate the shading terms for the microfacet specular shading model\n" +
        "  vec3 F = specularReflection(pbrInputs);\n" +
        "  float G = geometricOcclusion(pbrInputs);\n" +
        "  float D = microfacetDistribution(pbrInputs);\n" +
        
        "  const vec3 u_LightColor = vec3(1.0);\n" +
        
           // Calculation of analytical lighting contribution\n" +
        "  vec3 diffuseContrib = (1.0 - F) * diffuse(pbrInputs);\n" +
        "  vec3 specContrib = F * G * D / (4.0 * NdotL * NdotV);\n" +
        "  // Obtain final intensity as reflectance (BRDF) scaled by the energy of the light (cosine law)\n" +
        "  vec3 color = NdotL * u_LightColor * (diffuseContrib + specContrib);\n" +
        
           // Calculate lighting contribution from image based lighting source (IBL)\n" +
        "  color += getIBLContribution(pbrInputs, n, reflection);\n" +
        
        "  const float u_OcclusionStrength = 1.0f;\n" +
           // Apply optional PBR terms for additional (optional) shading\n" +
        "  if (material.occlusionTextureSet > -1) {\n" +
        "    float ao = texture(aoMap, (material.occlusionTextureSet == 0 ? inUV0 : inUV1)).r;\n" +
        "    color = mix(color, color * ao, u_OcclusionStrength);\n" +
        "  }\n" +
        
        "  const float u_EmissiveFactor = 1.0f;\n" +
        "  if (material.emissiveTextureSet > -1) {\n" +
        "    vec3 emissive = SRGBtoLINEAR(texture(emissiveMap, material.emissiveTextureSet == 0 ? inUV0 : inUV1)).rgb * u_EmissiveFactor;\n" +
        "    color += emissive;\n" +
        "  }\n" +
        "  \n" +
        "  outColor = vec4(color, baseColor.a);\n" +
        
        // Shader inputs debug visualization\n" +
        "  if (uboParams.debugViewInputs > 0.0) {\n" +
        "    int index = int(uboParams.debugViewInputs);\n" +
        "    switch (index) {\n" +
        "      case 1:\n" +
        "        outColor.rgba = material.baseColorTextureSet > -1 ? texture(colorMap, material.baseColorTextureSet == 0 ? inUV0 : inUV1) : vec4(1.0f);\n" +
        "        break;\n" +
        "      case 2:\n" +
        "        outColor.rgb = (material.normalTextureSet > -1) ? texture(normalMap, material.normalTextureSet == 0 ? inUV0 : inUV1).rgb : normalize(inNormal);\n" +
        "        break;\n" +
        "      case 3:\n" +
        "        outColor.rgb = (material.occlusionTextureSet > -1) ? texture(aoMap, material.occlusionTextureSet == 0 ? inUV0 : inUV1).rrr : vec3(0.0f);\n" +
        "        break;\n" +
        "      case 4:\n" +
        "        outColor.rgb = (material.emissiveTextureSet > -1) ? texture(emissiveMap, material.emissiveTextureSet == 0 ? inUV0 : inUV1).rgb : vec3(0.0f);\n" +
        "        break;\n" +
        "      case 5:\n" +
        "        outColor.rgb = texture(physicalDescriptorMap, inUV0).bbb;\n" +
        "        break;\n" +
        "      case 6:\n" +
        "        outColor.rgb = texture(physicalDescriptorMap, inUV0).ggg;\n" +
        "        break;\n" +
        "    }\n" +
        "    outColor = SRGBtoLINEAR(outColor);\n" +
        "  }\n" +
        
        // PBR equation debug visualization\n" +
        // \"none\", \"Diff (l,n)\", \"F (l,h)\", \"G (l,v,h)\", \"D (h)\", \"Specular\
        "  if (uboParams.debugViewEquation > 0.0) {\n" +
        "    int index = int(uboParams.debugViewEquation);\n" +
        "    switch (index) {\n" +
        "      case 1:\n" +
        "        outColor.rgb = diffuseContrib;\n" +
        "        break;\n" +
        "      case 2:\n" +
        "        outColor.rgb = F;\n" +
        "        break;\n" +
        "      case 3:\n" +
        "        outColor.rgb = vec3(G);\n" +
        "        break;\n" +
        "      case 4: \n" +
        "        outColor.rgb = vec3(D);\n" +
        "        break;\n" +
        "      case 5:\n" +
        "        outColor.rgb = specContrib;\n" +
        "        break;        \n" +
        "    }\n" +
        "  }\n" +
        
        "}"
