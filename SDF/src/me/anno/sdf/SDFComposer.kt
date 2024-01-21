package me.anno.sdf

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Material.Companion.defaultMaterial
import me.anno.ecs.components.mesh.MaterialCache
import me.anno.ecs.components.mesh.TypeValue
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.ECSMeshShader.Companion.discardByCullingPlane
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RendererLib
import me.anno.gpu.GFXState
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.DepthTransforms.bindDepthToPosition
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.maths.Maths.hasFlag
import me.anno.maths.Maths.length
import me.anno.sdf.SDFComponent.Companion.appendUniform
import me.anno.sdf.SDFComponent.Companion.defineUniform
import me.anno.sdf.shapes.SDFBox.Companion.sdBox
import me.anno.sdf.shapes.SDFShape
import me.anno.sdf.uv.UVMapper
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Strings.iff
import org.joml.Vector2f
import org.joml.Vector3f
import java.util.BitSet
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.max

/**
 * builds shaders for signed distance function rendering
 * */
object SDFComposer {

    const val dot2 = "" +
            "float dot2(vec2 v){ return dot(v,v); }\n" +
            "float dot2(vec3 v){ return dot(v,v); }\n"

    const val sdfConstants = "" +
            "#define Infinity 1e20\n" +
            "#define PI 3.141592653589793\n" +
            "#define TAU 6.283185307179586\n" +
            "#define PHI 1.618033988749895\n"

    // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
    const val raycasting = "" +
            // input: vec3 ray origin, vec3 ray direction
            // output: vec2(distance, materialId)
            "vec4 raycast(vec3 ro, vec3 rd, out int i){\n" +
            // ray marching
            "   vec4 res = vec4(-1.0);\n" +
            "   float tMin = distanceBounds.x, tMax = distanceBounds.y;\n" +
            "   float t = tMin;\n" +
            "   for(i=0; i<maxSteps && t<tMax; i++){\n" +
            "     vec4 h = map(ro,rd,ro+rd*t,t);\n" +
            "     if(abs(h.x)<(sdfMaxRelativeError*t)){\n" + // allowed error grows with distance
            "       res = vec4(t,h.yzw);\n" +
            "       break;\n" +
            "     }\n" +
            // sdfReliability: sometimes, we need more steps, because the sdf is not reliable
            "     t += sdfReliability * h.x;\n" +
            "   }\n" +
            "   return res;\n" +
            "}\n"

    // http://iquilezles.org/www/articles/normalsSDF/normalsSDF.htm
    const val normal = "" +
            "vec3 calcNormal(vec3 ro, vec3 rd, vec3 pos, float epsilon, float t) {\n" +
            // inspired by tdhooper and klems - a way to prevent the compiler from inlining map() 4 times
            "   vec3 n = vec3(0.0);vec2 uv;\n" +
            "   for(int i=ZERO;i<4;i++) {\n" +
            // 0.5773 is just a scalar factor
            "      vec3 e = vec3((((i+3)>>1)&1),((i>>1)&1),(i&1))*2.0-1.0;\n" +
            "      n += e*map(ro,rd,pos+e*epsilon,t).x;\n" +
            "   }\n" +
            "   return sign(-dot(rd,n)) * normalize(n);\n" +
            "}\n"

    fun localCamPos(tree: SDFComponent, dst: Vector3f): Vector3f {
        val dt = tree.transform?.getDrawMatrix()
        if (dt != null) {
            val pos = JomlPools.vec3d.create()
            val dtInverse = JomlPools.mat4x3d.create()
            dt.invert(dtInverse) // have we cached this inverse anywhere? would save .invert()
            dtInverse.transformPosition(RenderState.cameraPosition, pos)
            dst.set(pos)
            JomlPools.vec3d.sub(1)
            JomlPools.mat4x3d.sub(1)
        }
        return dst
    }

    fun distanceBounds(tree: SDFComponent, dst: Vector2f): Vector2f {
        // find maximum in local space from current point to bounding box
        // best within frustum
        val dt = tree.transform?.getDrawMatrix()
        // compute first and second intersection with aabb
        // transform camera position into local space
        val localPos = JomlPools.vec3d.create()
        val dtInverse = JomlPools.mat4x3d.create()
        if (dt != null) dt.invert(dtInverse)
        else dtInverse.identity()
        dtInverse.transformPosition(RenderState.cameraPosition, localPos)
        val bounds = tree.localAABB
        val dx = (localPos.x - bounds.centerX).toFloat()
        val dy = (localPos.y - bounds.centerY).toFloat()
        val dz = (localPos.z - bounds.centerZ).toFloat()
        val bdx = bounds.deltaX.toFloat()
        val bdy = bounds.deltaY.toFloat()
        val bdz = bounds.deltaZ.toFloat()
        val min = sdBox(dx, dy, dz, bdx * 0.5f, bdy * 0.5f, bdz * 0.5f)
        val max = min + length(bdx, bdy, bdz) // to do max could be calculated more accurately
        tree.camNear = min
        tree.camFar = max
        dst.set(max(0f, min), max)
        JomlPools.vec3d.sub(1)
        JomlPools.mat4x3d.sub(1)
        return dst
    }

    fun createECSShader(tree: SDFComponent): Pair<HashMap<String, TypeValue>, ECSMeshShader> {
        // done compute approximate bounds on cpu, so we can save computations
        // done traverse with larger step size on normals? normals don't use traversal
        // done traverse over tree to assign material ids? no, the programmer does that to reduce cases
        val functions = LinkedHashSet<String>()
        val uniforms = HashMap<String, TypeValue>()
        val shapeDependentShader = StringBuilder()
        tree.buildShader(shapeDependentShader, 0, VariableCounter(1), 0, uniforms, functions, ArrayList())

        val materials = tree.sdfMaterials.map { MaterialCache[it] }
        val materialCode = buildMaterialCode(tree, materials, uniforms)

        val shader = object : SDFShader(tree) {
            override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
                // instancing is not supported
                val fragmentVariables = fragmentVariables1 + uniforms.map { (k, v) -> Variable(v.type, k) }
                val defines = concatDefines(key, StringBuilder()).toString()
                val renderer = key.renderer
                val stage = ShaderStage(
                    name, fragmentVariables, defines +

                            // todo calculate motion vectors, if requested

                            // todo if transparency is enabled, trace through the model until the result is opaque

                            // compute ray position & direction in local coordinates
                            // trace ray
                            // done materials
                            // convert tracing distance from local to global
                            // convert normals into global normals
                            // compute global depth

                            // shut the compiler up about it possibly not being initialized
                            "gl_FragDepth = 0.5;\n" +

                            // compute
                            "vec2 uv0 = gl_FragCoord.xy;\n" +
                            (if (tree.highQualityMSAA) "uv0 += gl_SamplePosition - 0.5;\n" else "") +
                            "uv0 /= renderSize;\n" +

                            "vec3 localDir = normalize(matMul(invLocalTransform, vec4(rawCameraDirection(uv0),0.0)));\n" +
                            // todo why is this slightly incorrect if orthographic????
                            //  (both cases wobble from the view of the point light)
                            "vec3 localPos = localPosition - localDir * max(0.0,dot(localPosition-localCamPos,localDir));\n" +
                            "localPos = matMul(invLocalTransform, vec4(depthToPosition(uv0,perspectiveCamera?0.0:1.0),1.0));\n" +
                            // "vec4 ray = vec4(0,1,0,0);\n" +
                            "float tmpNear = 0.001;\n" +
                            "vec4 ray = distanceBounds.x <= tmpNear ? map(localPos,localDir,localPos,tmpNear) : vec4(0.0);\n" +
                            "int steps;\n" +
                            "finalAlpha = 0.0;\n" +
                            "if(ray.x >= 0.0){\n" + // outside objects
                            "ray = raycast(localPos, localDir, steps);\n" +
                            (when (renderer) {
                                Renderer.nothingRenderer -> "" +
                                        "if(ray.y < 0.0) { discard; } else {\n" +
                                        "   vec3 localHit = localPos + ray.x * localDir;\n" +
                                        "   finalPosition = matMul(localTransform, vec4(localHit, 1.0));\n" + // convert localHit to global hit
                                        "   vec4 newVertex = matMul(transform, vec4(finalPosition, 1.0));\n" + // calculate depth
                                        "   gl_FragDepth = newVertex.z/newVertex.w;\n" +
                                        "}\n"
                                SDFRegistry.NumStepsRenderer -> showNumberOfSteps
                                else -> {
                                    "" +
                                            "float distOnY = -localPos.y / localDir.y;\n" +
                                            "vec3 localHit = localPos + distOnY * localDir;\n" +
                                            // in this mode, just adding a plane might be best
                                            (if (renderer == SDFRegistry.SDFOnYRenderer) {
                                                "" +
                                                        "bool inPlane = all(greaterThan(localHit.xz,localMin.xz)) && all(lessThan(localHit.xz,localMax.xz));\n" +
                                                        "if(((distOnY > 0.0 && distOnY < ray.x) || ray.y < 0.0) && inPlane){\n" +
                                                        sdfOnY +
                                                        "} else "
                                            } else "") +
                                            "if(ray.y < 0.0){ discard; } else {\n" +
                                            // proper material calculation
                                            "   vec3 localHit = localPos + ray.x * localDir;\n" +
                                            "   vec3 localNormal = calcNormal(localPos, localDir, localHit, ray.x * sdfNormalEpsilon, ray.x);\n" +
                                            // todo normal could be guessed from depth aka dFdx(ray.x),dFdy(ray.x)
                                            // todo calculate tangent from dFdx(uv) and dFdy(uv)
                                            "   finalNormal = normalize(matMul(localTransform, vec4(localNormal,0.0)));\n" +
                                            "   finalPosition = matMul(localTransform, vec4(localHit, 1.0));\n" + // convert localHit to global hit
                                            discardByCullingPlane + // respect reflection plane
                                            "   vec4 newVertex = matMul(transform, vec4(finalPosition, 1.0));\n" + // calculate depth
                                            "   #define CUSTOM_DEPTH\n" +
                                            "   gl_FragDepth = newVertex.z/newVertex.w;\n" +
                                            // step by step define all material properties
                                            "   finalUV = ray.zw;\n" +
                                            materialCode +
                                            "}\n"
                                }
                            }) +
                            "} else discard;\n" + // inside an object

                            v0 +
                            // sheenCalculation +
                            // clearCoatCalculation +
                            reflectionCalculation.iff(key.flags.hasFlag(NEEDS_COLORS)) +

                            partClickIds
                )
                functions.add(sdBox)
                functions.add(quatRot)
                functions.add(rawToDepth)
                functions.add(depthToPosition)
                functions.add(RendererLib.getReflectivity)
                stage.add(build(functions, shapeDependentShader))
                return listOf(stage)
            }
        }
        // why are those not ignored?
        ignoreCommonNames(shader)
        return uniforms to shader
    }

    open class SDFShader(val tree: SDFComponent) : ECSMeshShader("raycasting-${tree.hashCode()}") {

        private val tmp3 = Vector3f()
        private val tmp2 = Vector2f()
        override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
            super.bind(shader, renderer, instanced)

            shader.v3f("localCamPos", localCamPos(tree, tmp3))
            shader.v1f("sdfReliability", tree.globalReliability)
            val target = GFXState.currentBuffer
            shader.v1f("sdfNormalEpsilon", tree.normalEpsilon / (target.width + target.height))
            shader.v1f("sdfMaxRelativeError", tree.maxRelativeError)

            shader.v1i("maxSteps", tree.maxSteps)
            shader.v2f("distanceBounds", distanceBounds(tree, tmp2))

            val b = tree.localAABB
            val s = 1f + tree.relativeMeshMargin
            shader.v3f("localMin", b.getMin(tmp3).mul(s))
            shader.v3f("localMax", b.getMax(tmp3).mul(s))

            shader.v1b("perspectiveCamera", RenderState.isPerspective)
            shader.v1b("renderIds", GFXState.currentRenderer == Renderer.idRenderer)

            shader.v2f("renderSize", GFXState.currentBuffer.width.toFloat(), GFXState.currentBuffer.height.toFloat())

            bindDepthToPosition(shader)
        }
    }

    fun ignoreCommonNames(shader: BaseShader) {
        shader.ignoreNameWarnings(
            "sheenNormalMap", "occlusionMap", "metallicMap", "roughnessMap",
            "emissiveMap", "normalMap", "diffuseMap", "diffuseBase", "emissiveBase", "drawMode", "applyToneMapping"
        )
    }

    fun collectMaterialsUsingTextures(tree: SDFComponent, materials: List<Material?>): BitSet {
        val flags = BitSet(materials.size)
        if (materials.isNotEmpty()) {
            tree.simpleTraversal(false) {
                if (it is SDFComponent && it.positionMappers.any { pm -> pm is UVMapper }) {
                    it.simpleTraversal(false) { c ->
                        if (c is SDFShape) {
                            if (c.materialId < flags.size())
                                flags.set(c.materialId)
                        }
                        false
                    }
                }
                false
            }
        }
        return flags
    }


    fun buildMaterialCode(
        tree: SDFComponent, materials: List<Material?>,
        uniforms: HashMap<String, TypeValue>
    ): CharSequence {
        val materialsUsingTextures = collectMaterialsUsingTextures(tree, materials)
        return buildMaterialCode(materials, materialsUsingTextures, uniforms)
    }

    fun buildMaterialCode(
        materials: List<Material?>, materialsUsingTextures: BitSet,
        uniforms: HashMap<String, TypeValue>
    ): CharSequence {
        val builder = StringBuilder(max(1, materials.size) * 128)
        val needsSwitch = materials.size > 1
        if (needsSwitch) builder
            .append("switch(clamp(int(ray.y),0,")
            .append(materials.lastIndex)
            .append(")){\n")
        for (index in 0 until max(materials.size, 1)) {
            if (needsSwitch) builder.append("case ").append(index).append(": {\n")
            val material = materials.getOrNull(index) ?: defaultMaterial
            // todo support shading functions, textures and material interpolation
            // define all properties as uniforms, so they can be changed without recompilation
            // todo this is pretty limited by the total number of textures :/
            val canUseTextures = materialsUsingTextures[index]
            val color = defineUniform(uniforms, material.diffuseBase)
            if (canUseTextures && material.diffuseMap.exists) {
                builder.append("vec4 color = texture(")
                    .append(defineUniform(uniforms, GLSLType.S2D) { material.diffuseMap })
                    .append(",finalUV) * ").append(color).append(";\n")
                builder.append("finalColor = color.rgb;\n")
                builder.append("finalAlpha = color.w;\n")
            } else {
                builder.append("finalColor = ").append(color).append(".rgb;\n")
                builder.append("finalAlpha = ").append(color).append(".w;\n")
            }
            // todo create textures for these
            builder.append("finalMetallic = ").appendUniform(uniforms, material.metallicMinMax).append(".y;\n")
            builder.append("finalRoughness = ").appendUniform(uniforms, material.roughnessMinMax).append(".y;\n")
            builder.append("finalEmissive = ").appendUniform(uniforms, material.emissiveBase).append(";\n")
            if (needsSwitch) builder.append("break; }\n")
        }
        if (needsSwitch) builder.append("}\n")
        return builder
    }

    fun build(functions: Collection<String>, shapeDependentShader: CharSequence): String {
        val size0 =
            shapeDependentShader.length + raycasting.length + normal.length + sdfConstants.length
        val size1 = 150 + functions.sumOf { it.length } + size0
        val builder2 = StringBuilder(size1)
        builder2.append(sdfConstants)
        for (func in functions) builder2.append(func)
        builder2.append("vec4 map(vec3 ro, vec3 rd, vec3 pos0, float t){\n")
        builder2.append("   vec4 res0; vec3 dir0 = rd; float sca0 = 1.0/t; vec2 uv = vec2(0.0);\n")
        builder2.append(shapeDependentShader)
        builder2.append("   return res0;\n}\n")
        builder2.append(raycasting)
        builder2.append(normal)
        return builder2.toString()
    }

    val partClickIds = "" +
            "if(renderIds){\n" +
            "   int intId = int(ray.y);\n" +
            "   tint = vec4(vec3(float(intId&255), float((intId>>8)&255), float((intId>>16)&255))/255.0, 1.0);\n" +
            "} else tint = vec4(1.0);\n"

    val showNumberOfSteps = "" +
            "if(ray.y < 0.0) ray.x = dot(distanceBounds,vec2(0.5));\n" + // avg as a guess
            "vec3 localHit = localPos + ray.x * localDir;\n" +
            "finalPosition = matMul(localTransform, vec4(localHit, 1.0));\n" +
            //      discardByCullingPlane +
            "vec3 localNormal = calcNormal(localPos, localDir, localHit, ray.x * sdfNormalEpsilon, ray.x);\n" +
            "finalNormal = normalize(matMul(localTransform, vec4(localNormal,0.0)));\n" +
            "vec4 newVertex = matMul(transform, vec4(finalPosition, 1.0));\n" +
            "#define CUSTOM_DEPTH\n" +
            "gl_FragDepth = newVertex.z/newVertex.w;\n" +
            // shading from https://www.shadertoy.com/view/WdVyDW
            "const vec3 a = vec3(97, 130, 234) / vec3(255.0);\n" +
            "const vec3 b = vec3(220, 94, 75) / vec3(255.0);\n" +
            "const vec3 c = vec3(221, 220, 219) / vec3(255.0);\n" +
            "float t = float(steps)/float(maxSteps);\n" +
            "finalEmissive = t < 0.5 ? mix(a, c, 2.0 * t) : mix(c, b, 2.0 * t - 1.0);\n" +
            "finalColor = finalEmissive;\n" +
            "finalAlpha = 1.0;\n"

    val sdfOnY = "" + // check if & where ray hits y == 0
            "if(distOnY > 0.0){\n" +
            // check if localPos is within bounds -> only render the plane :)
            "        localHit.y = 0.0;\n" + // correct numerical issues
            "        finalPosition = matMul(localTransform, vec4(localHit, 1.0));\n" +
            discardByCullingPlane +
            "        finalNormal = vec3(0.0, -sign(localDir.y), 0.0);\n" +
            "        vec4 newVertex = matMul(transform, vec4(finalPosition, 1.0));\n" +
            "        #define CUSTOM_DEPTH\n" +
            "        gl_FragDepth = newVertex.z/newVertex.w;\n" +
            "        float distance = map(localPos,vec3(0.0),localHit,distOnY).x;\n" + // < 0.0 removed, because we show the shape there
            // waves like in Inigo Quilez demos, e.g. https://www.shadertoy.com/view/tt3yz7
            // show the inside as well? mmh...
            "        vec3 col = vec3(0.9,0.6,0.3) * (1.0 - exp(-2.0*distance));\n" +
            // less amplitude, when filtering issues occur
            "        float delta = 5.0 * length(vec2(dFdx(distance),dFdy(distance)));\n" +
            "        col *= 0.8 + 0.2*cos(20.0*distance)*max(1.0-delta,0.0);\n" +
            "        finalColor = col;\n" +
            "        finalAlpha = 1.0;\n" +
            "} else discard;\n"

    val fragmentVariables1 = listOf(
        Variable(GLSLType.M4x4, "transform"),
        Variable(GLSLType.M4x3, "localTransform"),
        Variable(GLSLType.M4x3, "invLocalTransform"),
        Variable(GLSLType.V3F, "localCamPos"),
        Variable(GLSLType.V1I, "maxSteps"),
        Variable(GLSLType.V2F, "distanceBounds"),
        Variable(GLSLType.V3F, "localMin"),
        Variable(GLSLType.V3F, "localMax"),
        Variable(GLSLType.V1I, "debugMode"),
        Variable(GLSLType.V1B, "perspectiveCamera"),
        Variable(GLSLType.V1F, "sdfReliability"),
        Variable(GLSLType.V1F, "sdfNormalEpsilon"),
        Variable(GLSLType.V1F, "sdfMaxRelativeError"),
        // is used to prevent inlining of huge functions
        Variable(GLSLType.V1I, "ZERO"),
        // input varyings
        Variable(GLSLType.V3F, "finalPosition", VariableMode.INOUT),
        Variable(GLSLType.V3F, "localPosition", VariableMode.INOUT),
        Variable(GLSLType.V2F, "roughnessMinMax"),
        Variable(GLSLType.V2F, "metallicMinMax"),
        Variable(GLSLType.V1F, "occlusionStrength"),
        // outputs
        Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
        Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
        Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
        Variable(GLSLType.V3F, "finalNormal", VariableMode.OUT),
        Variable(GLSLType.V2F, "finalUV", VariableMode.OUT),
        Variable(GLSLType.V3F, "finalEmissive", VariableMode.OUT),
        Variable(GLSLType.V1F, "finalMetallic", VariableMode.OUT),
        Variable(GLSLType.V1F, "finalRoughness", VariableMode.OUT),
        // we could compute occlusion
        // disadvantage: we cannot prevent duplicate occlusion currently,
        // and it would be applied twice... (here + ssao)
        Variable(GLSLType.V1F, "finalSheen", VariableMode.OUT),
        // just passed from uniforms
        Variable(GLSLType.V1F, "finalTranslucency", VariableMode.INOUT),
        Variable(GLSLType.V4F, "finalClearCoat", VariableMode.OUT),
        Variable(GLSLType.V2F, "finalClearCoatRoughMetallic", VariableMode.OUT),
        // for reflections;
        // we could support multiple
        Variable(GLSLType.V1B, "hasReflectionPlane"),
        Variable(GLSLType.V3F, "reflectionPlaneNormal"),
        Variable(GLSLType.S2D, "reflectionPlane"),
        Variable(GLSLType.V4F, "reflectionCullingPlane"),
        Variable(GLSLType.SCube, "reflectionMap"),
        Variable(GLSLType.V1B, "renderIds"),
        Variable(GLSLType.V2F, "renderSize"),
        Variable(GLSLType.V4F, "tint", VariableMode.OUT),
    ) + depthVars
}