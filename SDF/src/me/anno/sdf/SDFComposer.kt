package me.anno.sdf

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MaterialCache
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.ecs.components.mesh.TypeValue
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.ECSMeshShader.Companion.discardByCullingPlane
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.shader.*
import me.anno.gpu.shader.DepthTransforms.bindDepthToPosition
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.maths.Maths.length
import me.anno.sdf.SDFComponent.Companion.appendUniform
import me.anno.sdf.SDFComponent.Companion.defineUniform
import me.anno.sdf.shapes.SDFBox.Companion.sdBox
import me.anno.sdf.shapes.SDFShape
import me.anno.sdf.uv.LinearUVMapper
import me.anno.sdf.uv.UVMapper
import me.anno.utils.pooling.JomlPools
import org.joml.*
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
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
            "   return normalize(n);\n" +
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
        if (dt != null) {
            // compute first and second intersection with aabb
            // transform camera position into local space
            val localPos = JomlPools.vec3d.create()
            val dtInverse = JomlPools.mat4x3d.create()
            dt.invert(dtInverse)
            dtInverse.transformPosition(RenderState.cameraPosition, localPos)
            val bounds = tree.localAABB
            val dx = (localPos.x - bounds.avgX()).toFloat()
            val dy = (localPos.y - bounds.avgY()).toFloat()
            val dz = (localPos.z - bounds.avgZ()).toFloat()
            val bdx = bounds.deltaX().toFloat()
            val bdy = bounds.deltaY().toFloat()
            val bdz = bounds.deltaZ().toFloat()
            val min = sdBox(dx, dy, dz, bdx * 0.5f, bdy * 0.5f, bdz * 0.5f)
            val max = min + length(bdx, bdy, bdz) // to do max could be calculated more accurately
            tree.camNear = min
            tree.camFar = max
            dst.set(max(0f, min), max)
            JomlPools.vec3d.sub(1)
            JomlPools.mat4x3d.sub(1)
        }
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
            override fun createFragmentStages(
                isInstanced: Boolean,
                isAnimated: Boolean,
                motionVectors: Boolean
            ): List<ShaderStage> {
                // instancing is not supported
                val fragmentVariables =
                    fragmentVariables1 + uniforms.map { (k, v) -> Variable(v.type, k) }
                val stage = ShaderStage(
                    name, fragmentVariables, "" +

                            // todo calculate motion vectors, if requested

                            // todo if transparency is enabled, trace through the model until the result is opaque

                            // compute ray position & direction in local coordinates
                            // trace ray
                            // done materials
                            // convert tracing distance from local to global
                            // convert normals into global normals
                            // compute global depth

                            "vec2 uv0 = gl_FragCoord.xy / renderSize;\n" +
                            "vec3 localDir = normalize(mat3x3(invLocalTransform) * rawCameraDirection(uv0));\n" +
                            // todo why is this slightly incorrect if orthographic????
                            //  (both cases wobble from the view of the point light)
                            "vec3 localPos = localPosition - localDir * max(0.0,dot(localPosition-localCamPos,localDir));\n" +
                            "if(uv0.x > 0.5) localPos = invLocalTransform * vec4(depthToPosition(uv0,perspectiveCamera?0.0:1.0),1.0);\n" +
                            // "vec4 ray = vec4(0,1,0,0);\n" +
                            "float tmpNear = 0.001;\n" +
                            "vec4 ray = distanceBounds.x <= tmpNear ? map(localPos,localDir,localPos,tmpNear) : vec4(0.0);\n" +
                            "int steps;\n" +
                            "finalAlpha = 0.0;\n" +
                            "if(ray.x >= 0.0){\n" + // outside objects
                            "   ray = raycast(localPos, localDir, steps);\n" +
                            "   if(debugMode == ${DebugMode.NUM_STEPS.id}){\n" +
                            showNumberOfSteps +
                            "   } else {\n" +
                            "       if(debugMode == ${DebugMode.SDF_ON_Y.id} && (-localPos.y / localDir.y < ray.x || ray.y < -0.5)){\n" +
                            sdfOnY +
                            "       } else if(ray.y < 0.0){ discard; } else {\n" +
                            // proper material calculation
                            "           vec3 localHit = localPos + ray.x * localDir;\n" +
                            "           vec3 localNormal = calcNormal(localPos, localDir, localHit, ray.x * sdfNormalEpsilon, ray.x);\n" +
                            // todo normal could be guessed from depth aka dFdx(ray.x),dFdy(ray.x)
                            // todo calculate tangent from dFdx(uv) and dFdy(uv)
                            "           finalNormal = normalize(mat3x3(localTransform) * localNormal);\n" +
                            "           finalPosition = localTransform * vec4(localHit, 1.0);\n" + // convert localHit to global hit
                            discardByCullingPlane + // respect reflection plane
                            "           vec4 newVertex = transform * vec4(finalPosition, 1.0);\n" + // calculate depth
                            "           gl_FragDepth = newVertex.z/newVertex.w;\n" +
                            // step by step define all material properties
                            "           finalUV = ray.zw;\n" +
                            materialCode +
                            "       }\n" +
                            "   }\n" +
                            "} else discard;\n" + // inside an object
                            partClickIds
                )

                functions.add(sdBox)
                functions.add(rawToDepth)
                functions.add(depthToPosition)
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
            shader.v1f("sdfNormalEpsilon", tree.normalEpsilon)
            shader.v1f("sdfMaxRelativeError", tree.maxRelativeError)

            shader.v1i("maxSteps", tree.maxSteps)
            shader.v2f("distanceBounds", distanceBounds(tree, tmp2))

            val b = tree.localAABB
            shader.v3f("localMin", tmp3.set(b.minX, b.minY, b.minZ))
            shader.v3f("localMax", tmp3.set(b.maxX, b.maxY, b.maxZ))

            shader.v1b("perspectiveCamera", RenderState.isPerspective)
            shader.v1i("debugMode", tree.debugMode.id)
            shader.v1b("renderIds", GFXState.currentRenderer == Renderer.idRenderer)

            shader.v2f("renderSize", GFXState.currentBuffer.w.toFloat(), GFXState.currentBuffer.h.toFloat())

            bindDepthToPosition(shader)

        }

        override fun createDepthShader(
            isInstanced: Boolean,
            isAnimated: Boolean,
            limitedTransform: Boolean
        ): Shader {
            val builder1 = createBuilder()
            builder1.addVertex(
                createVertexStages(
                    isInstanced, isAnimated, colors = false,
                    motionVectors = false, limitedTransform
                )
            )
            builder1.addFragment(createFragmentStages(isInstanced, isAnimated, motionVectors))
            GFX.check()
            val shader = builder1.create()
            shader.glslVersion = glslVersion
            GFX.check()
            return shader
        }
    }

    fun ignoreCommonNames(shader: BaseShader) {
        shader.ignoreNameWarnings(
            "sheenNormalMap", "occlusionMap", "metallicMap", "roughnessMap",
            "emissiveMap", "normalMap", "diffuseMap", "diffuseBase", "emissiveBase", "drawMode", "applyToneMapping",
            "ambientLight"
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
            "finalPosition = localTransform * vec4(localHit, 1.0);\n" +
            //      discardByCullingPlane +
            "vec3 localNormal = calcNormal(localPos, localDir, localHit, ray.x * sdfNormalEpsilon, ray.x);\n" +
            "finalNormal = normalize(mat3x3(localTransform) * localNormal);\n" +
            "vec4 newVertex = transform * vec4(finalPosition, 1.0);\n" +
            "gl_FragDepth = newVertex.z/newVertex.w;\n" +
            // shading from https://www.shadertoy.com/view/WdVyDW
            "const vec3 a = vec3(97, 130, 234) / vec3(255.0);\n" +
            "const vec3 b = vec3(220, 94, 75) / vec3(255.0);\n" +
            "const vec3 c = vec3(221, 220, 219) / vec3(255.0);\n" +
            "float t = float(steps)/float(maxSteps);\n" +
            "finalEmissive = t < 0.5 ? mix(a, c, 2.0 * t) : mix(c, b, 2.0 * t - 1.0);\n" +
            "finalColor = finalEmissive;\n" +
            "finalEmissive *= 10.0;\n" + // only correct with current tonemapping...
            "finalAlpha = 1.0;\n"

    val sdfOnY = "" + // check if & where ray hits y == 0
            "float distance = -localPos.y / localDir.y;\n" +
            "if(distance > 0.0){\n" +
            "    vec3 localHit = localPos + distance * localDir;\n" +
            // check if localPos is within bounds -> only render the plane :)
            "    if(all(greaterThan(localHit.xz,localMin.xz)) && all(lessThan(localHit.xz,localMax.xz))){\n" +
            "        localHit.y = 0.0;\n" + // correct numerical issues
            "        finalPosition = localTransform * vec4(localHit, 1.0);\n" +
            discardByCullingPlane +
            "        finalNormal = vec3(0.0, -sign(localDir.y), 0.0);\n" +
            "        vec4 newVertex = transform * vec4(finalPosition, 1.0);\n" +
            "        gl_FragDepth = newVertex.z/newVertex.w;\n" +
            "        distance = map(localPos,localDir,localHit,distance).x;\n" + // < 0.0 removed, because we show the shape there
            // waves like in Inigo Quilez demos, e.g. https://www.shadertoy.com/view/tt3yz7
            // show the inside as well? mmh...
            "        vec3 col = vec3(0.9,0.6,0.3) * (1.0 - exp(-2.0*distance));\n" +
            // less amplitude, when filtering issues occur
            "        float delta = 5.0 * length(vec2(dFdx(distance),dFdy(distance)));\n" +
            "        col *= 0.8 + 0.2*cos(20.0*distance)*max(1.0-delta,0.0);\n" +
            "        finalColor = col;\n" +
            "        finalAlpha = 1.0;\n" +
            "    } else discard;\n" +
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
        Variable(GLSLType.V4F, "finalClearCoat", VariableMode.INOUT),
        Variable(GLSLType.V2F, "finalClearCoatRoughMetallic", VariableMode.INOUT),
        // for reflections;
        // we could support multiple
        Variable(GLSLType.V1B, "hasReflectionPlane"),
        Variable(GLSLType.V3F, "reflectionPlaneNormal"),
        Variable(GLSLType.S2D, "reflectionPlane"),
        Variable(GLSLType.V4F, "reflectionCullingPlane"),
        Variable(GLSLType.V1B, "renderIds"),
        Variable(GLSLType.V2F, "renderSize"),
        Variable(GLSLType.V4F, "tint", VariableMode.OUT),
    ) + depthVars

    fun createShaderToyShader(tree: SDFComponent): String {

        val stateScript = "" +
                // adjusted from https://www.shadertoy.com/view/ttVfDc
                // iChannel0 of buffer A must be itself
                // iChannel1 of buffer A must be keyboard
                // todo calculate pos and dir
                "void mainImage(out vec4 col, in vec2 uv) {  \n" +
                "   if(uv.y > 1.0 || uv.x > 4.0) discard;\n" +
                "   col = vec4(0.0);\n" +
                "   vec4 iMouseLast      = texelFetch(iChannel0, ivec2(0, 0), 0);\n" +
                "   vec4 iMouseAccuLast  = texelFetch(iChannel0, ivec2(1, 0), 0);\n" +
                "   float kW = texelFetch(iChannel1, ivec2(0x57, 0), 0).x;\n" +
                "   float kA = texelFetch(iChannel1, ivec2(0x41, 0), 0).x;\n" +
                "   float kS = texelFetch(iChannel1, ivec2(0x53, 0), 0).x;\n" +
                "   float kD = texelFetch(iChannel1, ivec2(0x44, 0), 0).x;\n" +
                "   float kQ = texelFetch(iChannel1, ivec2(0x50, 0), 0).x;\n" +
                "   float kE = texelFetch(iChannel1, ivec2(0x45, 0), 0).x;\n" +
                "   vec2 mouseDelta = iMouse.z > 0.0 && iMouseLast.z > 0.0 ? iMouse.xy - iMouseLast.xy : vec2(0.0);\n" +
                "   vec4 lastRot = texelFetch(iChannel0, ivec2(3, 0), 0);\n" +
                "   vec3 dir = quatRot(vec3(0,0,-1),lastRot);\n" +
                // *iDeltaTime is somehow unknown :/
                "   vec3 pos = texelFetch(iChannel0,ivec2(2,0),0).xyz + quatRot(vec3(kD-kA,kQ-kE,kS-kW),lastRot);\n" +
                "   vec4 rot = vec4(0,0,0,1);\n" + // todo rotate y and x
                "   if(uv.x == 0.5) col = iMouse;\n" +
                "   if(uv.x == 1.5) col = vec4(iMouseAccuLast.xy + mouseDelta,0,1);\n" +
                "   if(uv.x == 2.5) col = vec4(pos,1.0);\n" +
                "   if(uv.x == 3.5) col = rot;\n" +
                "}"

        // done compute approximate bounds on cpu, so we can save computations
        // done traverse with larger step size on normals? normals don't use traversal
        // done traverse over tree to assign material ids? no, the programmer does that to reduce cases
        val functions = LinkedHashSet<String>()
        val uniforms = HashMap<String, TypeValue>()
        val shapeDependentShader = StringBuilder()
        val seeds = ArrayList<String>()
        tree.buildShader(shapeDependentShader, 0, VariableCounter(1), 0, uniforms, functions, seeds)

        uniforms["sdfReliability"] = TypeValue(GLSLType.V1F, tree.globalReliability)
        uniforms["sdfNormalEpsilon"] = TypeValue(GLSLType.V1F, tree.normalEpsilon)
        uniforms["sdfMaxRelativeError"] = TypeValue(GLSLType.V1F, tree.maxRelativeError)
        uniforms["maxSteps"] = TypeValue(GLSLType.V1I, tree.maxSteps)
        uniforms["distanceBounds"] = TypeValue(GLSLType.V2F, Vector2f(0.01f, 1000f))

        val materials = tree.sdfMaterials.map { MaterialCache[it] }
        val builder = StringBuilder(max(1, materials.size) * 128)

        val needsSwitch = materials.size > 1
        if (needsSwitch) builder
            .append("switch(clamp(int(ray.y),0,")
            .append(materials.lastIndex)
            .append(")){\n")

        // register all materials that use textures
        val materialsUsingTextures = BitSet(materials.size)
        if (materials.isNotEmpty()) {
            tree.simpleTraversal(false) {
                if (it is SDFComponent && it.positionMappers.any { pm -> pm is me.anno.sdf.random.SDFRandomUV }) {
                    it.simpleTraversal(false) { c ->
                        if (c is SDFShape) {
                            if (c.materialId < materialsUsingTextures.size())
                                materialsUsingTextures.set(c.materialId)
                        }
                        false
                    }
                }
                false
            }
        }

        for (index in 0 until max(materials.size, 1)) {
            if (needsSwitch) builder.append("case ").append(index).append(":\n")
            val material = materials.getOrNull(index) ?: defaultMaterial
            builder.append("finalColor = vec3(${material.diffuseBase.x},${material.diffuseBase.y},${material.diffuseBase.z});\n")
            builder.append("finalAlpha = ${material.diffuseBase.w};\n")
            builder.append("finalMetallic = ${material.metallicMinMax.y};\n")
            builder.append("finalRoughness = ${material.roughnessMinMax.y};\n")
            builder.append("finalEmissive = vec3(${material.emissiveBase.x},${material.emissiveBase.y},${material.emissiveBase.z});\n")
            if (needsSwitch) builder.append("break;\n")
        }
        if (needsSwitch) builder.append("}\n")

        val defines = uniforms.entries.joinToString("\n") {
            "#define ${it.key} ${
                when (val v = it.value.value) {
                    is Vector2f -> "vec2(${v.x},${v.y})"
                    is Vector3f -> "vec3(${v.x},${v.y},${v.z})"
                    is Vector4f -> "vec4(${v.x},${v.y},${v.z},${v.w})"
                    is Quaternionf -> "vec4(${v.x},${v.y},${v.z},${v.w})"
                    is Vector2i -> "ivec2(${v.x},${v.y})"
                    is Vector3i -> "ivec3(${v.x},${v.y},${v.z})"
                    is Vector4i -> "ivec4(${v.x},${v.y},${v.z},${v.w})"
                    else -> v.toString()
                }
            }"
        }

        val trace = "vec4 trace(vec3 localPos, vec3 localDir,\n" +
                "out vec3 finalColor, out float finalAlpha, out vec3 finalNormal,\n" +
                "out float finalMetallic, out float finalRoughness, out vec3 finalEmissive) {\n" +
                "   int steps; finalAlpha = 0.0;\n" +
                "   vec4 ray = raycast(localPos, localDir, steps);\n" +
                "   if(ray.y >= 0.0) {\n" +
                "      vec3 localHit = localPos + ray.x * localDir;\n" +
                "      finalNormal = calcNormal(localPos, localDir, localHit, ray.x * sdfNormalEpsilon, ray.x);\n" +
                // step by step define all material properties
                "      vec2 uv = ray.zw;\n" +
                builder.toString() +
                "   }\n" +
                "   return ray;\n" +
                "}\n"

        val res = StringBuilder()
        res.append(quatRot)
        res.append(stateScript)
        res.append("\n\n// this belongs in the image slot\n")
        res.append("#define ZERO min(int(iFrame),0)\n")
        res.append(defines).append("\n")
        res.append(sdfConstants)
        functions.add(quatRot)
        for (func in functions) res.append(func)
        res.append("vec4 map(vec3 ro, vec3 rd, vec3 pos0, float t){\n")
        res.append("   vec4 res0; vec3 dir0=rd; float sca0=1.0/t; vec2 uv=vec2(0.0);\n")
        res.append(shapeDependentShader)
        res.append("   return res0;\n}\n")
        res.append(raycasting)
        res.append(normal)

        // trace a ray :)
        val mainScript = "" +
                "void mainImage(out vec4 col, in vec2 uv){\n" +
                "   uv = (uv-iResolution.xy*0.5) / iResolution.y;" +
                "   vec3 color,normal,emissive;\n" +
                "   float alpha,metallic,roughness;\n" +
                "   vec3 pos = texelFetch(iChannel0,ivec2(2,0),0).xyz;\n" +
                "   vec4 rot = texelFetch(iChannel0,ivec2(3,0),0);\n" +
                "   vec3 dir = quatRot(normalize(vec3(uv-0.5,-1.0)),rot);\n" +
                "   vec4 ray = trace(pos,dir,color,alpha,normal,metallic,roughness,emissive);\n" +
                // todo nicer shading, maybe 1st degree reflections
                "   col = vec4(color,alpha);\n" +
                "}\n"

        res.append(trace)
        res.append(mainScript)

        return res.toString()
    }

}