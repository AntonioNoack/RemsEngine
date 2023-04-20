package me.anno.gpu.shader

import me.anno.config.DefaultConfig
import me.anno.ecs.components.anim.AnimTexture.Companion.useAnimTextures
import me.anno.engine.ui.render.ECSMeshShader.Companion.getAnimMatrix
import me.anno.gpu.drawing.UVProjection
import me.anno.gpu.shader.ShaderFuncLib.noiseFunc
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Filtering
import me.anno.mesh.assimp.AnimGameItem
import me.anno.utils.pooling.ByteBufferPool
import org.joml.Matrix4x3f
import org.joml.Vector3i
import org.joml.Vector4f
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sqrt

@Suppress("MemberVisibilityCanBePrivate", "unused")
object ShaderLib {

    val y = Vector4f(0.299f, 0.587f, 0.114f, 0f)
    val u = Vector4f(-0.169f, -0.331f, 0.500f, 0.5f)
    val v = Vector4f(0.500f, -0.419f, -0.081f, 0.5f)
    val m = Matrix4x3f(
        y.x, u.x, v.x,
        y.y, u.y, v.y,
        y.z, u.z, v.z,
        y.w, u.w, v.w,
    ).invert()

    lateinit var shader3DSVG: BaseShader
    lateinit var shaderAssimp: BaseShader
    lateinit var monochromeModelShader: BaseShader

    /**
     * our code only uses 3, I think
     * */
    const val maxOutlineColors = 6

    val coordsList = listOf(Variable(GLSLType.V2F, "coords", VariableMode.ATTR))
    const val coordsVShader = "" +
            "void main(){\n" +
            "   gl_Position = vec4(coords*2.0-1.0,0.5,1.0);\n" +
            "   uv = coords;\n" +
            "}"

    const val simplestVertexShader = "" +
            "void main(){\n" +
            "   gl_Position = vec4(coords*2.0-1.0,0.5,1.0);\n" +
            "}"

    val uvList = listOf(Variable(GLSLType.V2F, "uv"))
    const val simpleVertexShader = "" +
            "uniform vec4 posSize;\n" +
            "uniform vec4 tiling;\n" +
            "uniform mat4 transform;\n" +
            "void main(){\n" +
            "   gl_Position = transform * vec4((posSize.xy + coords * posSize.zw)*2.0-1.0, 0.5, 1.0);\n" +
            "   uv = (coords-0.5) * tiling.xy + 0.5 + tiling.zw;\n" +
            "}"

    val dither2x2 = "" +
            "bool dither2x2(float brightness, vec2 uvf) {\n" +
            "  ivec2 uvi = ivec2(floor(uvf)) & ivec2(1);\n" +
            "  int index = (uvi.x + uvi.y * 2 + gl_SampleID) & 3;\n" +
            "  float limit = 0.20;\n" +
            "  if (index == 1) limit = 0.60;\n" +
            "  if (index == 2) limit = 0.80;\n" +
            "  if (index == 3) limit = 0.40;\n" +
            "  return brightness < limit;\n" +
            "}\n" +
            "bool dither2x2(float brightness) { return dither2x2(brightness, gl_FragCoord.xy); }\n"

    val simpleVertexShaderV2List = listOf(
        Variable(GLSLType.V2F, "coords", VariableMode.ATTR),
        Variable(GLSLType.V4F, "posSize"),
        Variable(GLSLType.V4F, "tiling"),
        Variable(GLSLType.M4x4, "transform")
    )

    const val simpleVertexShaderV2 = "" +
            "void main(){\n" +
            "   gl_Position = transform * vec4((posSize.xy + coords * posSize.zw)*2.0-1.0, 0.5, 1.0);\n" +
            "   uv = (coords-0.5) * tiling.xy + 0.5 + tiling.zw;\n" +
            "}"

    val brightness = "" +
            "float brightness(vec3 color){\n" +
            "   return sqrt(${y.x}*color.r*color.r + ${y.y}*color.g*color.g + ${y.z}*color.b*color.b);\n" +
            "}\n" +
            "float brightness(vec4 color){\n" +
            "   return sqrt(${y.x}*color.r*color.r + ${y.y}*color.g*color.g + ${y.z}*color.b*color.b);\n" +
            "}\n"

    fun brightness(r: Float, g: Float, b: Float) = sqrt(y.dot(r * r, g * g, b * b, 1f))

    // https://community.khronos.org/t/quaternion-functions-for-glsl/50140/3
    const val quatRot = "" +
            "mat2 rot(float angle){\n" +
            "   float c = cos(angle), s = sin(angle);\n" +
            "   return mat2(c,-s,+s,c);\n" +
            "}\n" +
            "vec3 quatRot(vec3 v, vec4 q){\n" +
            "   return v + 2.0 * cross(q.xyz, cross(q.xyz, v) + q.w * v);\n" +
            "}\n" +
            "vec3 quatRotInv(vec3 v, vec4 q){\n" +
            "   return v - 2.0 * cross(q.xyz, q.w * v - cross(q.xyz, v));\n" +
            "}\n"

    // from http://www.java-gaming.org/index.php?topic=35123.0
    // https://stackoverflow.com/questions/13501081/efficient-bicubic-filtering-code-in-glsl
    private const val foreignBicubicInterpolation = "" +
            "vec4 cubic(float v){\n" +
            "    vec4 n = vec4(1.0, 2.0, 3.0, 4.0) - v;\n" +
            "    vec4 s = n * n * n;\n" +
            "    float x = s.x;\n" +
            "    float y = s.y - 4.0 * s.x;\n" +
            "    float z = s.z - 4.0 * s.y + 6.0 * s.x;\n" +
            "    float w = 6.0 - x - y - z;\n" +
            "    return vec4(x, y, z, w) * (1.0/6.0);\n" +
            "}\n" +
            "vec4 textureBicubic(sampler2D sampler, vec2 texCoords){\n" +

            "   vec2 texSize = vec2(textureSize(sampler, 0));\n" +
            "   vec2 invTexSize = 1.0 / texSize;\n" +

            "   texCoords = texCoords * texSize - 0.5;\n" +

            "    vec2 fxy = fract(texCoords);\n" +
            "    texCoords -= fxy;\n" +

            "    vec4 xCubic = cubic(fxy.x);\n" +
            "    vec4 yCubic = cubic(fxy.y);\n" +

            "    vec4 c = texCoords.xxyy + vec2(-0.5, +1.5).xyxy;\n" +

            "    vec4 s = vec4(xCubic.xz + xCubic.yw, yCubic.xz + yCubic.yw);\n" +
            "    vec4 offset = c + vec4(xCubic.yw, yCubic.yw) / s;\n" +

            "    offset *= invTexSize.xxyy;\n" +

            "    vec4 sample0 = texture(sampler, offset.xz);\n" +
            "    vec4 sample1 = texture(sampler, offset.yz);\n" +
            "    vec4 sample2 = texture(sampler, offset.xw);\n" +
            "    vec4 sample3 = texture(sampler, offset.yw);\n" +
            "    float sx = s.x / (s.x + s.y);\n" +
            "    float sy = s.z / (s.z + s.w);\n" +
            "    return mix(mix(sample3, sample2, sx), mix(sample1, sample0, sx), sy);\n" +
            "}"

    const val bicubicInterpolation = "" +
            // no more artifacts, but much smoother
            foreignBicubicInterpolation +
            "vec4 bicubicInterpolation(sampler2D tex, vec2 uv, vec2 duv){\n" +
            "   return textureBicubic(tex, uv);\n" +
            "}\n"

    // https://en.wikipedia.org/wiki/ASC_CDL
    // color grading with asc cdl standard
    const val ascColorDecisionList = "" +
            "uniform vec3 cgSlope, cgOffset, cgPower;\n" +
            "uniform float cgSaturation;\n" +
            "vec3 colorGrading(vec3 raw){\n" +
            "   vec3 color = pow(max(vec3(0.0), raw * cgSlope + cgOffset), cgPower);\n" +
            "   float gray = brightness(color);\n" +
            "   return mix(vec3(gray), color, cgSaturation);\n" +
            "}\n"

    val rgb2yuv = "" +
            "vec3 rgb2yuv(vec3 rgb){\n" +
            "   vec4 rgba = vec4(rgb,1);\n" +
            "   return vec3(\n" +
            "       dot(rgb,  vec3(${y.x}, ${y.y}, ${y.z})),\n" +
            "       dot(rgba, vec4(${u.x}, ${u.y}, ${u.z}, 0.5)),\n" +
            "       dot(rgba, vec4(${v.x}, ${v.y}, ${v.z}, 0.5))\n" +
            "   );\n" +
            "}\n"

    val rgb2uv = "" +
            "vec2 RGBtoUV(vec3 rgb){\n" +
            "   vec4 rgba = vec4(rgb,1.0);\n" +
            "   return vec2(\n" +
            "       dot(rgba, vec4(${u.x}, ${u.y}, ${u.z}, 0.5)),\n" +
            "       dot(rgba, vec4(${v.x}, ${v.y}, ${v.z}, 0.5))\n" +
            "   );\n" +
            "}\n"

    val yuv2rgb = "" +
            "vec3 yuv2rgb(vec3 yuv){" +
            /*"   yuv -= vec3(${16f / 255f}, 0.5, 0.5);\n" +
            "   return vec3(" +
            "       dot(yuv, vec3( 1.164,  0.000,  1.596))," +
            "       dot(yuv, vec3( 1.164, -0.392, -0.813))," +
            "       dot(yuv, vec3( 1.164,  2.017,  0.000)));\n" +*/
            "   return vec3(" +
            "       dot(yuv, vec3(${m.m00}, ${m.m10}, ${m.m20}))+${m.m30},\n" +
            "       dot(yuv, vec3(${m.m01}, ${m.m11}, ${m.m21}))+${m.m31},\n" +
            "       dot(yuv, vec3(${m.m02}, ${m.m12}, ${m.m22}))+${m.m32}\n" +
            "   );\n" +
            "}\n"

    const val anisotropic16 = "" +
            // anisotropic filtering from https://www.shadertoy.com/view/4lXfzn
            "vec4 textureAnisotropic(sampler2D T, vec2 p, mat2 J) {\n" +
            // "    mat2 J = inverse(mat2(dFdx(p),dFdy(p))); // dFdxy: pixel footprint in texture space\n" +
            "   J = transpose(J)*J;\n" + // quadratic form
            "   vec2 R = textureSize(T,0);\n" +
            "   float d = determinant(J), t = J[0][0]+J[1][1],\n" + // find ellipse: eigenvalues, max eigenvector
            "         D = sqrt(abs(t*t-4.*d)),\n" + // abs() fix a bug: in weird view angles 0 can be slightly negative
            "         V = (t-D)/2., v = (t+D)/2.,\n" + // eigenvalues
            "         M = inversesqrt(V), m = inversesqrt(v), l = log2(m*R.y);\n" + // = 1./radii^2
            "  // if (M/m>16.) l = log2(M/16.*R.y);\n" + // optional
            "    vec2 A = M * normalize(vec2(-J[0][1] , J[0][0]-V));\n" + // max eigenvector = main axis
            "    vec4 O = vec4(0);\n" +
            "    for (float i = -7.5; i<8.; i++) \n" + // sample x16 along main axis at LOD min-radius
            "        O += textureLod(T, p+(i/16.)*A, l);\n" +
            "    return O/16.;\n" +
            "}\n" +
            "vec4 textureAnisotropic(sampler2D T, vec2 p, vec2 u) {\n" +
            "   return textureAnisotropic(T, p, inverse(mat2(dFdx(u),dFdy(u))));\n" +
            "}\n" +
            "vec4 textureAnisotropic(sampler2D T, vec2 p) { return textureAnisotropic(T, p, p); }\n"

    val maxColorForceFields = DefaultConfig["objects.attractors.color.maxCount", 12]
    val getColorForceFieldLib = "" +
// additional weights?...
            "uniform int forceFieldColorCount;\n" +
            "uniform vec4 forceFieldBaseColor;\n" +
            "uniform vec4[$maxColorForceFields] forceFieldColors;\n" +
            "uniform vec4[$maxColorForceFields] forceFieldPositionsNWeights;\n" +
            "uniform vec4[$maxColorForceFields] forceFieldColorPowerSizes;\n" +
            "vec4 getForceFieldColor(vec3 finalPosition){\n" +
            "   float sumWeight = 0.25;\n" +
            "   vec4 sumColor = sumWeight * forceFieldBaseColor;\n" +
            "   for(int i=0;i<forceFieldColorCount;i++){\n" +
            "       vec4 positionNWeight = forceFieldPositionsNWeights[i];\n" +
            "       vec3 positionDelta = finalPosition - positionNWeight.xyz;\n" +
            "       vec4 powerSize = forceFieldColorPowerSizes[i];\n" +
            "       float weight = positionNWeight.w / (1.0 + pow(dot(powerSize.xyz * positionDelta, positionDelta), powerSize.w));\n" +
            "       sumWeight += weight;\n" +
            "       vec4 localColor = forceFieldColors[i];\n" +
            "       sumColor += weight * localColor * localColor;\n" +
            "   }\n" +
            "   return sqrt(sumColor / sumWeight);\n" +
            "}\n"

    val colorForceFieldBuffer: FloatBuffer = ByteBufferPool
        .allocateDirect(4 * maxColorForceFields)
        .asFloatBuffer()

    val maxUVForceFields = DefaultConfig["objects.attractors.scale.maxCount", 12]
    val getUVForceFieldLib = "" +
            "uniform int forceFieldUVCount;\n" +
            "uniform vec3[$maxUVForceFields] forceFieldUVs;\n" + // xyz
            "uniform vec4[$maxUVForceFields] forceFieldUVSpecs;\n" + // size, power
            "vec3 getForceFieldUVs(vec3 uvw){\n" +
            "   vec3 sumUVs = uvw;\n" +
            "   for(int i=0;i<forceFieldUVCount;i++){\n" +
            "       vec3 position = forceFieldUVs[i];\n" +
            "       vec4 sizePower = forceFieldUVSpecs[i];\n" +
            "       vec3 positionDelta = uvw - position;\n" +
            "       float weight = sizePower.x / (1.0 + pow(sizePower.z * dot(positionDelta, positionDelta), sizePower.w));\n" +
            "       sumUVs += weight * positionDelta;\n" +
            "   }\n" +
            "   return sumUVs;\n" +
            "}\n" +
            "vec2 getForceFieldUVs(vec2 uv){\n" +
            "   vec2 sumUVs = uv;\n" +
            "   for(int i=0;i<forceFieldUVCount;i++){\n" +
            "       vec3 position = forceFieldUVs[i];\n" +
            "       vec4 sizePower = forceFieldUVSpecs[i];\n" +
            "       vec2 positionDelta = (uv - position.xy) * sizePower.xy;\n" +
            "       float weight = 1.0 / (1.0 + pow(sizePower.z * dot(positionDelta, positionDelta), sizePower.w));\n" +
            "       sumUVs += weight * positionDelta;\n" +
            "   }\n" +
            "   return sumUVs;\n" +
            "}\n"

    val uvForceFieldBuffer: FloatBuffer = ByteBufferPool
        .allocateDirect(3 * maxUVForceFields)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    const val hasForceFieldColor = "(forceFieldColorCount > 0)"
    const val hasForceFieldUVs = "(forceFieldUVCount > 0)"

    val getTextureLib = "" +
            bicubicInterpolation +
            getUVForceFieldLib +
            // the uvs correspond to the used mesh
            // used meshes are flat01 and cubemapBuffer
            "uniform vec2 textureDeltaUV;\n" +
            "uniform int filtering, uvProjection;\n" +
            /*"vec2 getProjectedUVs(vec2 uv){\n" +
            "   switch(uvProjection){\n" +
            "       case ${UVProjection.TiledCubemap.id}:\n" +
            "           return uv;\n" + // correct???
            "       default:\n" +
            "           return uv;\n" +
            "   }\n" +
            "}\n" +*/
            "vec2 getProjectedUVs(vec2 uv){ return uv; }\n" +
            "vec2 getProjectedUVs(vec3 uvw){\n" +
            //"   switch(uvProjection){\n" +
            //"       case ${UVProjection.Equirectangular.id}:\n" +
            //"       default:\n" +
            "           float u = atan(uvw.z, uvw.x)*${0.5 / PI}+0.5;\n " +
            "           float v = atan(uvw.y, length(uvw.xz))*${1.0 / PI}+0.5;\n" +
            "           return vec2(u, v);\n" +
            //"   }\n" +
            "}\n" +
            "vec2 getProjectedUVs(vec2 uv, vec3 uvw){\n" +
            "   return uvProjection == ${UVProjection.Equirectangular.id} ?\n" +
            "       ($hasForceFieldUVs ? getProjectedUVs(getForceFieldUVs(uvw)) : getProjectedUVs(uvw)) :\n" +
            "       ($hasForceFieldUVs ? getProjectedUVs(getForceFieldUVs(uv))  : getProjectedUVs(uv));\n" +
            "}\n" +
            "#define dot2(a) dot(a,a)\n" +
            "vec4 getTexture(sampler2D tex, vec2 uv, vec2 duv){\n" +
            "   if(filtering == ${Filtering.LINEAR.id}) return texture(tex, uv);\n" +
            "   if(filtering == ${Filtering.NEAREST.id}) {\n" +
            // edge smoothing, when zooming in far; not perfect, but quite good :)
            // todo if is axis-aligned, and zoom is integer, don't interpolate
            // (to prevent smoothed edges, where they are not necessary)
            // zoom-round(zoom)>0.02 && dFdx(uv).isSingleAxis && dFdy(uv).isSingleAxis
            "       float zoom = dot2(duv) / dot2(vec4(dFdx(uv),dFdy(uv)));\n" + // guess on zoom level
            "       if(zoom > 4.0) {\n" +
            "           zoom = 0.5 * sqrt(zoom);\n" +
            "           vec2 uvi = uv/duv, uvf = fract(uvi), uvf2 = uvf-0.5;\n" +
            "           float d = -zoom+1.0, a = zoom*2.0-1.0;\n" +
            "           float m = clamp(d+max(abs(uvf2.x),abs(uvf2.y))*a, 0.0, 0.5);\n" +
            "           return mix(texture(tex,uv), texture(tex,uv+(uvf2)*duv/zoom), m);\n" +
            "       }\n" +
            "       return texture(tex, uv);\n" +
            "   } else return bicubicInterpolation(tex, uv, duv);\n" +
            "}\n" +
            "vec4 getTexture(sampler2D tex, vec2 uv){ return getTexture(tex, uv, textureDeltaUV); }\n"


    const val positionPostProcessing = "" +
            "   zDistance = gl_Position.w;\n"

    const val flatNormal = "" +
            "   normal = vec3(0.0, 0.0, 1.0);\n"

    val v3Dl = listOf(
        Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
        Variable(GLSLType.V2F, "attr1", VariableMode.ATTR),
        Variable(GLSLType.M4x4, "transform"),
        Variable(GLSLType.V4F, "tiling")
    )

    val v2Dl = listOf(
        Variable(GLSLType.V2F, "coords", VariableMode.ATTR)
    )

    const val v2D = "" +
            "void main(){\n" +
            "   gl_Position = vec4(coords*2.0-1.0, 0.0, 1.0);\n" +
            "   uv = coords;\n" +
            "}"

    const val v3D = "" +
            "void main(){\n" +
            "   finalPosition = coords;\n" +
            "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
            positionPostProcessing +
            "   uv = (attr1-0.5) * tiling.xy + 0.5 + tiling.zw;\n" +
            "   uvw = coords;\n" +
            flatNormal +
            "}"

    val y2D = uvList

    val y3D = listOf(
        Variable(GLSLType.V2F, "uv"),
        Variable(GLSLType.V3F, "uvw"),
        Variable(GLSLType.V3F, "finalPosition"),
        Variable(GLSLType.V1F, "zDistance"),
        Variable(GLSLType.V3F, "normal")
    )

    val f3Dl = listOf(
        Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
        Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
        Variable(GLSLType.S2D, "tex")
    )

    val f3D = "" +
            getTextureLib +
            getColorForceFieldLib +
            "void main(){\n" +
            "   vec4 color = getTexture(tex, getProjectedUVs(uv, uvw));\n" +
            "   if($hasForceFieldColor) color *= getForceFieldColor(finalPosition);\n" +
            "   finalColor = color.rgb;\n" +
            "   finalAlpha = color.a;\n" +
            "}"

    val v3DlMasked = listOf(
        Variable(GLSLType.M4x4, "transform"),
        Variable(GLSLType.V2F, "coords", VariableMode.ATTR),
    )

    val v3DMasked = "" +
            "void main(){\n" +
            "   finalPosition = vec3(coords*2.0-1.0, 0.0);\n" +
            "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
            "   uv = gl_Position.xyw;\n" +
            positionPostProcessing +
            "}"

    val y3DMasked = listOf(
        Variable(GLSLType.V3F, "uv"),
        Variable(GLSLType.V3F, "finalPosition"),
        Variable(GLSLType.V1F, "zDistance")
    )

    // make this customizable?
    val blacklist = listOf(
        "cgSlope", "cgOffset", "cgPower", "cgSaturation",
        "forceFieldUVCount", "forceFieldColorCount"
    )

    val v3DlPolygon = listOf(
        Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
        Variable(GLSLType.V2F, "attr1", VariableMode.ATTR),
        Variable(GLSLType.V1F, "inset"),
        Variable(GLSLType.M4x4, "transform")
    )

    val v3DPolygon = "" +
            "void main(){\n" +
            "   vec2 betterUV = coords.xy;\n" +
            "   betterUV *= mix(1.0, attr1.r, inset);\n" +
            "   finalPosition = vec3(betterUV, coords.z);\n" +
            "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
            flatNormal +
            positionPostProcessing +
            "   uv = attr1.yx;\n" +
            "}"

    // https://knarkowicz.wordpress.com/2014/04/16/octahedron-normal-vector-encoding/
    const val octNormalPacking = "" +
            "vec2 PackNormal(vec3 n) {\n" +
            "   n /= max(1e-7, abs(n.x)+abs(n.y)+abs(n.z));\n" +
            "   if(n.z < 0.0) n.xy = (1.0-abs(n.yx)) * vec2(n.x >= 0.0 ? 1.0 : -1.0, n.y >= 0.0 ? 1.0 : -1.0);\n" +
            "   return n.xy * 0.5 + 0.5;\n" +
            "}\n" +
            "vec3 UnpackNormal(vec2 f) {\n" +
            "   f = f * 2.0 - 1.0;\n" +
            // https://twitter.com/Stubbesaurus/status/937994790553227264
            "   vec3 n = vec3(f.xy, 1.0-abs(f.x)-abs(f.y));\n" +
            "   n.xy -= sign(n.xy) * max(-n.z,0.0);\n" +
            "   return normalize(n);\n" +
            "}\n"

    fun createSwizzleShader(swizzle: String): BaseShader {
        return createShader(
            "3d${swizzle.ifEmpty { ".rgba" }}", v3Dl, v3D, y3D, listOf(
                Variable(GLSLType.S2D, "tex"),
                Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
            ), "" +
                    getTextureLib +
                    getColorForceFieldLib +
                    brightness +
                    ascColorDecisionList +
                    "void main(){\n" +
                    "   vec4 color = getTexture(tex, getProjectedUVs(uv, uvw))$swizzle;\n" +
                    "   color.rgb = colorGrading(color.rgb);\n" +
                    "   if($hasForceFieldColor) color *= getForceFieldColor(finalPosition);\n" +
                    "   finalColor = color.rgb;\n" +
                    "   finalAlpha = color.a;\n" +
                    "}", listOf("tex")
        )
    }

    fun createSwizzleShader2D(swizzle: String): Shader {
        return Shader(
            "2d${swizzle.ifEmpty { ".rgba" }}", v2Dl, v2D, y2D, listOf(
                Variable(GLSLType.S2D, "tex"),
                Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
            ), "" +
                    "void main(){\n" +
                    "   vec4 color = texture(tex, uv)$swizzle;\n" +
                    "   finalColor = color.rgb;\n" +
                    "   finalAlpha = color.a;\n" +
                    "}"
        )
    }

    fun init() {

        // with texture
        // somehow becomes dark for large |steps|-values

        val vSVGl = listOf(
            Variable(GLSLType.V3F, "aLocalPosition", VariableMode.ATTR),
            Variable(GLSLType.V2F, "aLocalPos2", VariableMode.ATTR),
            Variable(GLSLType.V4F, "aFormula0", VariableMode.ATTR),
            Variable(GLSLType.V1F, "aFormula1", VariableMode.ATTR),
            Variable(GLSLType.V4F, "aColor0", VariableMode.ATTR),
            Variable(GLSLType.V4F, "aColor1", VariableMode.ATTR),
            Variable(GLSLType.V4F, "aColor2", VariableMode.ATTR),
            Variable(GLSLType.V4F, "aColor3", VariableMode.ATTR),
            Variable(GLSLType.V4F, "aStops", VariableMode.ATTR),
            Variable(GLSLType.V1F, "aPadding", VariableMode.ATTR),
            Variable(GLSLType.M4x4, "transform")
        )

        val vSVG = "" +
                "void main(){\n" +
                "   finalPosition = aLocalPosition;\n" +
                "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                flatNormal +
                positionPostProcessing +
                "   color0 = aColor0;\n" +
                "   color1 = aColor1;\n" +
                "   color2 = aColor2;\n" +
                "   color3 = aColor3;\n" +
                "   stops = aStops;\n" +
                "   padding = aPadding;\n" +
                "   localPos2 = aLocalPos2;\n" +
                "   formula0 = aFormula0;\n" +
                "   formula1 = aFormula1;\n" +
                "}"

        val ySVG = y3D + listOf(
            Variable(GLSLType.V4F, "color0"),
            Variable(GLSLType.V4F, "color1"),
            Variable(GLSLType.V4F, "color2"),
            Variable(GLSLType.V4F, "color3"),
            Variable(GLSLType.V4F, "stops"),
            Variable(GLSLType.V4F, "formula0"), // pos, dir
            Variable(GLSLType.V1F, "formula1"), // is circle
            Variable(GLSLType.V1F, "padding"), // spread method / repetition type
            Variable(GLSLType.V2F, "localPos2"), // position for gradient
        )

        val fSVGl = listOf(
            Variable(GLSLType.V4F, "uvLimits"),
            Variable(GLSLType.S2D, "tex"),
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT)
        )

        val fSVG = "" +
                getTextureLib +
                getColorForceFieldLib +
                brightness +
                ascColorDecisionList +
                "bool isInLimits(float value, vec2 minMax){\n" +
                "   return value >= minMax.x && value <= minMax.y;\n" +
                "}\n" + // sqrt and ² for better color mixing
                "vec4 mix2(vec4 a, vec4 b, float stop, vec2 stops){\n" +
                "   float f = clamp((stop-stops.x)/(stops.y-stops.x), 0.0, 1.0);\n" +
                "   return vec4(sqrt(mix(a.rgb*a.rgb, b.rgb*b.rgb, f)), mix(a.a, b.a, f));\n" +
                "}\n" +
                "void main(){\n" +
                // apply the formula; polynomial of 2nd degree
                "   vec2 delta = localPos2 - formula0.xy;\n" +
                "   vec2 dir = formula0.zw;\n" +
                "   float stopValue = formula1 > 0.5 ? length(delta * dir) : dot(dir, delta);\n" +

                "   if(padding < 0.5){\n" + // clamp
                "       stopValue = clamp(stopValue, 0.0, 1.0);\n" +
                "   } else if(padding < 1.5){\n" + // repeat mirrored, and yes, it looks like magic xD
                "       stopValue = 1.0 - abs(fract(stopValue*0.5)*2.0-1.0);\n" +
                "   } else {\n" + // repeat
                "       stopValue = fract(stopValue);\n" +
                "   }\n" +

                // find the correct color
                "   vec4 color = \n" +
                "       stopValue <= stops.x ? color0:\n" +
                "       stopValue >= stops.w ? color3:\n" +
                "       stopValue <  stops.y ? mix2(color0, color1, stopValue, stops.xy):\n" +
                "       stopValue <  stops.z ? mix2(color1, color2, stopValue, stops.yz):\n" +
                "                              mix2(color2, color3, stopValue, stops.zw);\n" +
                // "   color.rgb = fract(vec3(stopValue));\n" +
                "   color.rgb = colorGrading(color.rgb);\n" +
                "   if($hasForceFieldColor) color *= getForceFieldColor(finalPosition);\n" +
                "   if(isInLimits(uv.x, uvLimits.xz) && isInLimits(uv.y, uvLimits.yw)){" +
                "       vec4 color2 = color * getTexture(tex, uv * 0.5 + 0.5);\n" +
                "       finalColor = color2.rgb;\n" +
                "       finalAlpha = color2.a;\n" +
                "   } else {" +
                "       finalColor = vec3(0);\n" +
                "       finalAlpha = 0.0;\n" +
                "   }" +
                "}"

        shader3DSVG = createShader("3d-svg", vSVGl, vSVG, ySVG, fSVGl, fSVG, listOf("tex"))

        // create the obj+mtl shader

        val maxBones = AnimGameItem.maxBones
        val assimpVertexList = listOf(
            Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
            Variable(GLSLType.V2F, "uvs", VariableMode.ATTR),
            Variable(GLSLType.V3F, "normals", VariableMode.ATTR),
            Variable(GLSLType.V4F, "tangents", VariableMode.ATTR),
            Variable(GLSLType.V4F, "colors", VariableMode.ATTR),
            Variable(GLSLType.V4F, "weights", VariableMode.ATTR),
            Variable(GLSLType.V4I, "indices", VariableMode.ATTR),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.M4x3, "localTransform"),
            Variable(GLSLType.V1B, "hasAnimation"),
        )
        val assimpVertex = "" +
                (if (useAnimTextures) "" +
                        "uniform sampler2D animTexture;\n" +
                        "uniform vec4 animWeights, animIndices;\n" +
                        getAnimMatrix else "" +
                        "uniform mat4x3 jointTransforms[${min(128, maxBones)}];\n" +
                        "") +
                "void main(){\n" +
                (if (useAnimTextures) "" +
                        "   if(hasAnimation && textureSize(animTexture,0).x > 1){\n" +
                        "       mat4x3 jointMat;\n" +
                        "jointMat  = getAnimMatrix(indices.x) * weights.x;\n" +
                        "jointMat += getAnimMatrix(indices.y) * weights.y;\n" +
                        "jointMat += getAnimMatrix(indices.z) * weights.z;\n" +
                        "jointMat += getAnimMatrix(indices.w) * weights.w;\n"
                else "" +
                        "   if(hasAnimation){\n" +
                        "       mat4x3 jointMat;\n" +
                        "jointMat  = jointTransforms[indices.x] * weights.x;\n" +
                        "jointMat += jointTransforms[indices.y] * weights.y;\n" +
                        "jointMat += jointTransforms[indices.z] * weights.z;\n" +
                        "jointMat += jointTransforms[indices.w] * weights.w;\n"
                        ) +
                "       finalPosition = jointMat * vec4(coords, 1.0);\n" +
                "       normal = mat3x3(jointMat) * normals;\n" +
                "       tangent = vec4(mat3x3(jointMat) * tangents.xyz, tangents.w);\n" +
                "   } else {\n" +
                "       finalPosition = coords;\n" +
                "       normal = normals;\n" +
                "       tangent = tangents;\n" +
                "   }\n" +
                "   normal = mat3x3(localTransform) * normal;\n" +
                "   tangent.xyz = mat3x3(localTransform) * tangent.xyz;\n" +
                "   finalPosition = localTransform * vec4(finalPosition, 1.0);\n" +
                // normal only needs to be normalized, if we show the normal
                "   normal = normalize(normal);\n" + // here? nah ^^
                "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                "   uv = uvs;\n" +
                // "   weight = weights;\n" +
                "   vertexColor0 = colors;\n" +
                positionPostProcessing +
                "}"

        val assimpVarying = y3D + listOf(
            Variable(GLSLType.V4F, "tangent"),
            // Variable(GLSLType.V4F, "weight"),
            Variable(GLSLType.V4F, "vertexColor0"),
        )

        shaderAssimp = createShader(
            "assimp", assimpVertexList,
            assimpVertex, assimpVarying, listOf(
                Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalNormal", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalPosition"),
                Variable(GLSLType.S2D, "albedoTex"),
                Variable(GLSLType.V4F, "diffuseBase")
            ), "" +
                    getTextureLib +
                    getColorForceFieldLib +
                    "void main(){\n" +
                    "   vec4 color = vec4(vertexColor0.rgb, 1.0) * diffuseBase * getTexture(albedoTex, uv);\n" +
                    "   color.rgb *= 0.6 + 0.4 * dot(vec3(-1.0, 0.0, 0.0), normal);\n" +
                    "   if($hasForceFieldColor) color *= getForceFieldColor(finalPosition);\n" +
                    "   finalColor = color.rgb;\n" +
                    "   finalAlpha = color.a;\n" +
                    "   finalPosition = finalPosition;\n" +
                    "   finalNormal = normal;\n" +
                    "}", listOf("albedoTex", "animTexture")
        )
        shaderAssimp.glslVersion = 330

        monochromeModelShader = createShader(
            "monochrome-model", assimpVertexList,
            assimpVertex, assimpVarying, listOf(
                Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalNormal", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalEmissive", VariableMode.OUT),
                Variable(GLSLType.V1F, "finalRoughness", VariableMode.OUT),
                Variable(GLSLType.V1F, "finalMetallic", VariableMode.OUT),
                Variable(GLSLType.S2D, "tex"),
                Variable(GLSLType.V4F, "tint"),
            ), "" +
                    "void main(){\n" +
                    "   vec4 color = texture(tex, uv);\n" +
                    "   finalColor = color.rgb;\n" +
                    "   finalAlpha = color.a;\n" +
                    "   finalPosition = finalPosition;\n" +
                    "   finalNormal = normal;\n" +
                    "   finalEmissive = tint.rgb;\n" +
                    "   finalRoughness = 1.0;" +
                    "   finalMetallic = 0.0;\n" +
                    "}", listOf("tex", "animTexture")
        )
        monochromeModelShader.glslVersion = 330
        monochromeModelShader.ignoreNameWarnings("worldScale")

        // create the fbx shader
        // shaderFBX = FBXShader.getShader(v3DBase, positionPostProcessing, y3D, getTextureLib)

    }

    val shader3D = createShader(
        "3d", v3Dl, v3D, y3D, f3Dl, f3D, listOf("tex"),
        "cgSlope", "cgOffset", "cgPower", "cgSaturation",
        "normals", "uvs", "tangents", "colors", "drawMode", "tint",
        "finalNormal", "finalEmissive"
    )

    val shader3DPolygon =
        createShader(
            "3d-polygon",
            v3DlPolygon, v3DPolygon, y3D, f3Dl, f3D,
            listOf("tex"),
            "tiling",
            "forceFieldUVCount"
        )
    val shader3DRGBA = createSwizzleShader("")
    val shader3DARGB = createSwizzleShader(".gbar")
    val shader3DBGRA = createSwizzleShader(".bgra")

    val shader2DRGBA = createSwizzleShader2D("")
    val shader2DARGB = createSwizzleShader2D(".gbar")
    val shader2DBGRA = createSwizzleShader2D(".bgra")

    val shader3DYUV = createShader(
        "3d-yuv",
        v3Dl, v3D, y3D, listOf(
            Variable(GLSLType.V2F, "uvCorrection"),
            Variable(GLSLType.S2D, "texY"),
            Variable(GLSLType.S2D, "texUV"),
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT)
        ), "" +
                getTextureLib +
                getColorForceFieldLib +
                brightness +
                ascColorDecisionList +
                yuv2rgb +
                "void main(){\n" +
                "   vec2 uv2 = getProjectedUVs(uv, uvw);\n" +
                "   vec2 correctedUV = uv2*uvCorrection;\n" +
                "   vec2 correctedDUV = textureDeltaUV*uvCorrection;\n" +
                "   vec3 yuv = vec3(" +
                "       getTexture(texY, uv2).r, " +
                "       getTexture(texUV, correctedUV, correctedDUV).rg);\n" +
                "   vec4 color = vec4(yuv2rgb(yuv), 1.0);\n" +
                "   color.rgb = colorGrading(color.rgb);\n" +
                "   if($hasForceFieldColor) color *= getForceFieldColor(finalPosition);\n" +
                "   finalColor = color.rgb;\n" +
                "   finalAlpha = color.a;\n" +
                "}", listOf("texY", "texUV")
    )

    val shader2DYUV = Shader(
        "2d-yuv",
        v2Dl, v2D, y2D, listOf(
            Variable(GLSLType.V2F, "uvCorrection"),
            Variable(GLSLType.S2D, "texY"),
            Variable(GLSLType.S2D, "texUV"),
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT)
        ), "" +
                yuv2rgb +
                "void main(){\n" +
                "   vec2 correctedUV = uv*uvCorrection;\n" +
                "   vec2 correctedDUV = textureDeltaUV*uvCorrection;\n" +
                "   vec3 yuv = vec3(" +
                "       texture(texY, uv).r, " +
                "       texture(texUV, correctedUV, correctedDUV).rg);\n" +
                "   finalColor = yuv2rgb(yuv);\n" +
                "   finalAlpha = 1.0;\n" +
                "}"
    ).apply { setTextureIndices("texY", "texUV") }

    val lineShader3D = BaseShader(
        "3d-lines", listOf(Variable(GLSLType.V3F, "coords", VariableMode.ATTR)),
        "uniform mat4 transform;\n" +
                "void main(){" +
                "   gl_Position = transform * vec4(coords, 1.0);\n" +
                positionPostProcessing +
                "}", listOf(Variable(GLSLType.V1F, "zDistance")),
        listOf(
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT)
        ), "" +
                "void main(){\n" +
                "   finalColor = color.rgb;\n" +
                "   finalAlpha = color.a;\n" +
                "}"
    )

    val shaderSDFText = createShader(
        "3d-text-withOutline", listOf(
            Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
            Variable(GLSLType.V2F, "attr1", VariableMode.ATTR),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.V2F, "offset"),
            Variable(GLSLType.V2F, "scale"),
        ),
        getUVForceFieldLib +
                "void main(){\n" +
                "   uv = coords.xy * 0.5 + 0.5;\n" +
                "   vec2 localPos0 = coords.xy * scale + offset;\n" +
                "   vec2 pseudoUV2 = getForceFieldUVs(localPos0*.5+.5);\n" +
                "   finalPosition = vec3($hasForceFieldUVs ? pseudoUV2*2.0-1.0 : localPos0, 0);\n" +
                "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                positionPostProcessing +
                "}", y3D, listOf(
            Variable(GLSLType.S2D, "tex"),
            Variable(GLSLType.V4F, "colors", maxOutlineColors),
            Variable(GLSLType.V2F, "distSmoothness", maxOutlineColors),
            Variable(GLSLType.V1F, "depth"),
            Variable(GLSLType.V1I, "colorCount"),
            Variable(GLSLType.V4F, "tint"),
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
        ), "" +
                noiseFunc +
                getTextureLib +
                getColorForceFieldLib +
                "float smoothSign(float f){ return clamp(f,-1.0,1.0); }\n" +
                "void main(){\n" +
                "   float distance = texture(tex, uv).r;\n" +
                "   float distDx = dFdx(distance);\n" +
                "   float distDy = dFdy(distance);\n" +
                "   float gradient = length(vec2(distDx, distDy));\n" +
                "#define IS_TINTED\n" +
                "   vec4 color = tint;\n" +
                "   for(int i=0;i<colorCount;i++){" +
                "       vec4 colorHere = colors[i];\n" +
                "       vec2 distSmooth = distSmoothness[i];\n" +
                "       float offset = distSmooth.x;\n" +
                "       float smoothness = distSmooth.y;\n" +
                "       float appliedGradient = max(smoothness, gradient);\n" +
                "       float mixingFactor0 = (distance-offset)*0.5/appliedGradient;\n" +
                "       float mixingFactor = clamp(mixingFactor0, 0.0, 1.0);\n" +
                "       color = mix(color, colorHere, mixingFactor);\n" +
                "   }\n" +
                "   gl_FragDepth = gl_FragCoord.z * (1.0 + distance * depth);\n" +
                "   if(color.a <= 0.001) discard;\n" +
                "   if($hasForceFieldColor) color *= getForceFieldColor(finalPosition);\n" +
                "   finalColor = color.rgb;\n" +
                "   finalAlpha = color.a;\n" +
                "}", listOf("tex"),
        "tiling",
        "filtering",
        "uvProjection",
        "forceFieldUVCount",
        "textureDeltaUV",
        "attr1"
    )

    val textShader = BaseShader(
        "textShader", coordsList,
        "" +
                "uniform vec4 posSize;\n" +
                "uniform mat4 transform;\n" + // not really supported, since subpixel layouts would be violated for non-integer translations, scales, skews or perspective
                "uniform vec2 windowSize;\n" +
                "void main(){\n" +
                "   vec2 localPos = posSize.xy + coords * posSize.zw;\n" +
                "   gl_Position = transform * vec4(localPos*2.0-1.0, 0.0, 1.0);\n" +
                "   position = localPos * windowSize;\n" +
                "   uv = coords;\n" +
                "}",
        listOf(
            Variable(GLSLType.V2F, "uv"),
            Variable(GLSLType.V2F, "position")
        ),
        listOf(
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
        ), "" +
                "uniform vec4 textColor, backgroundColor;\n" +
                "uniform vec2 windowSize;\n" +
                "uniform sampler2D tex;\n" +
                brightness +
                "void main(){\n" +
                "   float mixing = brightness(texture(tex, uv).rgb) * textColor.a;\n" +
                "   vec4 color = mix(backgroundColor, textColor, mixing);\n" +
                "   if(color.a < 0.001) discard;\n" +
                "   finalColor = color.rgb;\n" +
                "   finalAlpha = color.a;\n" +
                "}"
    )

    val subpixelCorrectTextShader = Array(2) {
        val instanced = it > 0
        val type = if (instanced) VariableMode.ATTR else VariableMode.IN
        BaseShader(
            "subpixelCorrectTextShader", listOf(
                Variable(GLSLType.V2F, "coords", VariableMode.ATTR),
                Variable(GLSLType.V3F, "instData", type),
                Variable(GLSLType.V4F, "color0", type),
                Variable(GLSLType.V4F, "color1", type),
                Variable(GLSLType.V4F, "posSize"),
                // not really supported, since subpixel layouts would be violated for non-integer translations, scales, skews or perspective
                Variable(GLSLType.M4x4, "transform"),
                Variable(GLSLType.V2F, "windowSize"),
            ), if (instanced) {
                "" +
                        "void main(){\n" +
                        "   vec2 localPos = coords * posSize.zw + instData.xy;\n" +
                        "   gl_Position = transform * vec4(localPos*2.0-1.0, 0.0, 1.0);\n" +
                        "   position = localPos * windowSize;\n" +
                        "   textColor = color0;\n" +
                        "   backgroundColor = color1;\n" +
                        "   uv = vec3(coords,instData.z);\n" +
                        "}"
            } else {
                "" +
                        "void main(){\n" +
                        "   vec2 localPos = coords * posSize.zw + posSize.xy;\n" +
                        "   gl_Position = transform * vec4(localPos*2.0-1.0, 0.0, 1.0);\n" +
                        "   position = localPos * windowSize;\n" +
                        "   uv = coords;\n" +
                        "}"
            },
            if (instanced) listOf(
                Variable(GLSLType.V3F, "uv"),
                Variable(GLSLType.V2F, "position"),
                Variable(GLSLType.V4F, "textColor"),
                Variable(GLSLType.V4F, "backgroundColor"),
            ) else listOf(
                Variable(GLSLType.V2F, "uv"),
                Variable(GLSLType.V2F, "position")
            ), listOf(
                Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
                Variable(GLSLType.V2F, "windowSize"),
                Variable(if (instanced) GLSLType.S2DA else GLSLType.S2D, "tex"),
            ) + (if (instanced) emptyList() else listOf(
                Variable(GLSLType.V4F, "textColor"),
                Variable(GLSLType.V4F, "backgroundColor"),
            )), "" +
                    brightness +
                    "void main(){\n" +
                    "#define IS_TINTED\n" +
                    // the conditions are a black border implementation for WebGL; they could be skipped on desktop
                    "   vec3 mixing = any(lessThan(uv.xy,vec2(0.0))) || any(greaterThan(uv.xy,vec2(1.0))) ? vec3(0.0) :\n" +
                    "                 texture(tex, uv).rgb * textColor.a;\n" +
                    "   float mixingAlpha = brightness(mixing);\n" +
                    // theoretically, we only need to check the axis, which is affected by subpixel-rendering, e.g., x on my screen
                    "   if(position.x < 1.0 || position.y < 1.0 || position.x > windowSize.x - 1.0 || position.y > windowSize.y - 1.0)\n" +
                    "       mixing = vec3(mixingAlpha);\n" + // on the border; color seams would become apparent here
                    "   vec4 color = mix(backgroundColor, textColor, vec4(mixing, mixingAlpha));\n" +
                    "   if(color.a < 0.001) discard;\n" +
                    "   finalColor = color.rgb;\n" +
                    "   finalAlpha = 1.0;\n" +
                    "}"
        )
    }

    val subpixelCorrectTextShader2 = Array(2) {
        val instanced = it > 0
        ComputeShader(
            "subpixelCorrectTextShader2", Vector3i(16, 16, 1), "" +
                    brightness +
                    (if (instanced)
                        "uniform sampler2DArray tex;\n" else
                        "uniform sampler2D tex;\n") +
                    "layout(rgba8, binding = 1) restrict uniform image2D dst;\n" +
                    "uniform vec4 textColor, backgroundColor;\n" +
                    "uniform ivec2 srcOffset, dstOffset, invokeSize;\n" +
                    "uniform float uvZ;\n" +
                    // the border isn't the most beautiful, but it ensures readability in front of bad backgrounds :)
                    "float read(vec2 uv){\n" +
                    (if (instanced)
                        "   vec3 col = texture(tex, vec3(uv,uvZ), 0).rgb;\n" else
                        "   vec3 col = texture(tex, uv, 0).rgb;\n") +
                    "   return uv.x >= 0.0 && uv.y >= 0.0 && uv.x <= 1.0 && uv.y <= 1.0 ? (col.x + col.y + col.z) : 0.0;\n" +
                    "}\n" +
                    "float findBorder(ivec2 uv, vec2 invSizeM1){\n" +
                    "   float sum = 0.16 * (\n" +
                    "       read(vec2(uv.x,uv.y-1)*invSizeM1) +\n" +
                    "       read(vec2(uv.x,uv.y+1)*invSizeM1) +\n" +
                    "       read(vec2(uv.x+1,uv.y)*invSizeM1) +\n" +
                    "       read(vec2(uv.x-1,uv.y)*invSizeM1)) + 0.08 * (\n" +
                    "       read(vec2(uv.x-1,uv.y-1)*invSizeM1) +\n" +
                    "       read(vec2(uv.x-1,uv.y+1)*invSizeM1) +\n" +
                    "       read(vec2(uv.x+1,uv.y-1)*invSizeM1) +\n" +
                    "       read(vec2(uv.x+1,uv.y+1)*invSizeM1));\n" +
                    "   return min(sum, 1.0);\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   ivec2 uv = ivec2(gl_GlobalInvocationID.xy), uv0 = uv;\n" +
                    "   if(uv.x >= invokeSize.x || uv.y >= invokeSize.y) return;\n" +
                    "   ivec2 size = textureSize(tex, 0).xy;\n" +
                    "   vec2 invSizeM1 = 1.0/vec2(size-1);\n" +
                    "   vec2 uv1 = vec2(uv + srcOffset)*invSizeM1;\n" +
                    (if (instanced)
                        "   vec3 mixingSrc = texture(tex, vec3(uv1,uvZ), 0).rgb;\n" else
                        "   vec3 mixingSrc = texture(tex, uv1, 0).rgb;\n") +
                    "   vec3 mixing = mixingSrc * textColor.a;\n" +
                    "   float mixingAlpha = brightness(mixing);\n" +
                    "   size = imageSize(dst);\n" +
                    // theoretically, we only need to check the axis, which is affected by subpixel-rendering, e.g., x on my screen
                    "   if(uv.x <= 0 || uv.y <= 0 || uv.x >= invokeSize.x-1 || uv.y >= invokeSize.y - 1)\n" +
                    "       mixing = vec3(mixingAlpha);\n" + // on the border; color seams would become apparent here
                    "   uv += dstOffset;\n" +
                    "   uv.y = size.y - 1 - uv.y;\n" +
                    "   vec4 backgroundColorI = imageLoad(dst, uv);\n" +
                    // todo there is awkward gray pieces around the text...
                    // "   if(mixingSrc.y < 1.0 && brightness(abs(textColor - backgroundColorI)) < 0.7)\n" +
                    "       backgroundColorI.rgb = mix(backgroundColorI.rgb, backgroundColor.rgb, findBorder(uv0+srcOffset, invSizeM1));\n" +
                    "   vec4 color = mix(backgroundColorI, textColor, vec4(mixing, mixingAlpha));\n" +
                    "   imageStore(dst, uv, color);\n" +
                    "}"
        ).apply { ignoreNameWarnings("windowSize") }
    }

    fun createShader(
        shaderName: String,
        vertexShader: String,
        varyings: List<Variable>,
        fragmentShader: String,
        textures: List<String>,
        vararg ignored: String
    ): BaseShader {
        val shader = BaseShader(shaderName, vertexShader, varyings, fragmentShader)
        shader.setTextureIndices(textures)
        shader.ignoreNameWarnings(ignored.toList())
        return shader
    }

    fun createShader(
        shaderName: String,
        vertexVariables: List<Variable>,
        vertexShader: String,
        varyings: List<Variable>,
        fragmentVariables: List<Variable>,
        fragmentShader: String,
        textures: List<String>,
        vararg ignored: String
    ): BaseShader {
        val shader = BaseShader(shaderName, vertexVariables, vertexShader, varyings, fragmentVariables, fragmentShader)
        shader.setTextureIndices(textures)
        shader.ignoreNameWarnings(ignored.toList())
        return shader
    }

}