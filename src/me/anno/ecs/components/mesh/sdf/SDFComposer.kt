package me.anno.ecs.components.mesh.sdf

import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.TypeValueV2
import me.anno.ecs.components.mesh.TypeValueV3
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.appendUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.mesh.sdf.shapes.SDFBox.Companion.sdBox
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.shader.builder.Function
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.maths.Maths.length
import me.anno.utils.pooling.JomlPools
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.max

/**
 * builds shaders for signed distance function rendering
 * */
object SDFComposer {

    // todo exporter for ShaderToy

    const val dot2 = "" +
            "float dot2(vec2 v){ return dot(v,v); }\n" +
            "float dot2(vec3 v){ return dot(v,v); }\n"

    // from https://www.shadertoy.com/view/Xds3zN, Inigo Quilez
    const val raycasting = "" +
            // input: vec3 ray origin, vec3 ray direction
            // output: vec2(distance, materialId)
            "vec2 raycast(vec3 ro, vec3 rd, out int i){\n" +
            // ray marching
            "   vec2 res = vec2(-1.0);\n" +
            "   float tMin = distanceBounds.x, tMax = distanceBounds.y;\n" +
            "   float t = tMin;\n" +
            "   for(i=0; i<maxSteps && t<tMax; i++){\n" +
            "     vec2 h = map(ro,rd,ro+rd*t);\n" +
            "     if(abs(h.x)<(sdfMaxRelativeError*t)){\n" + // allowed error grows with distance
            "       res = vec2(t,h.y);\n" +
            "       break;\n" +
            "     }\n" +
            // sdfReliability: sometimes, we need more steps, because the sdf is not reliable
            "     t += sdfReliability * h.x;\n" +
            "   }\n" +
            "   return res;\n" +
            "}\n"

    // http://iquilezles.org/www/articles/normalsSDF/normalsSDF.htm
    const val normal = "" +
            "vec3 calcNormal(vec3 ro, vec3 rd, vec3 pos, float epsilon) {\n" +
            // inspired by tdhooper and klems - a way to prevent the compiler from inlining map() 4 times
            "  vec3 n = vec3(0.0);\n" +
            "  for(int i=ZERO;i<4;i++) {\n" +
            // 0.5773 is just a scalar factor
            "      vec3 e = vec3((((i+3)>>1)&1),((i>>1)&1),(i&1))*2.0-1.0;\n" +
            "      n += e*map(ro,rd,pos+e*epsilon).x;\n" +
            "  }\n" +
            "  return normalize(n);\n" +
            "}\n"

    fun createECSShader(tree: SDFComponent): Pair<HashMap<String, TypeValue>, ECSMeshShader> {
        // done compute approximate bounds on cpu, so we can save computations
        // done traverse with larger step size on normals? normals don't use traversal
        // done traverse over tree to assign material ids? no, the programmer does that to reduce cases
        val functions = LinkedHashSet<String>()
        val uniforms = HashMap<String, TypeValue>()
        val shapeDependentShader = StringBuilder()
        tree.buildShader(shapeDependentShader, 0, VariableCounter(1), 0, uniforms, functions)
        uniforms["localCamPos"] = TypeValueV3(GLSLType.V3F, Vector3f()) { dst ->
            val dt = tree.transform?.getDrawMatrix()
            if (dt != null) {
                val pos = JomlPools.vec3d.create()
                val dtInverse = JomlPools.mat4x3d.create()
                dt.invert(dtInverse) // have we cached this inverse anywhere? would save .invert()
                dtInverse.transformPosition(RenderView.camPosition, pos)
                dst.set(pos)
                JomlPools.vec3d.sub(1)
                JomlPools.mat4x3d.sub(1)
            }
        }
        uniforms["localCamDir"] = TypeValueV3(GLSLType.V3F, Vector3f()) { dst ->
            if (RenderView.isPerspective) {
                val dt = tree.transform?.getDrawMatrix()
                if (dt != null) {
                    val pos = JomlPools.vec3d.create()
                    val dtInverse = JomlPools.mat4x3d.create()
                    dt.invert(dtInverse) // have we cached this inverse anywhere? would save .invert()
                    dtInverse.transformDirection(RenderView.camDirection, pos)
                    dst.set(pos).normalize()
                    JomlPools.vec3d.sub(1)
                    JomlPools.mat4x3d.sub(1)
                }
            }
        }
        uniforms["sdfReliability"] = TypeValueV2(GLSLType.V1F) { tree.globalReliability }
        uniforms["sdfNormalEpsilon"] = TypeValue(GLSLType.V1F) { tree.normalEpsilon }
        uniforms["sdfMaxRelativeError"] = TypeValueV2(GLSLType.V1F) { tree.maxRelativeError }
        uniforms["maxSteps"] = TypeValueV2(GLSLType.V1I) { tree.maxSteps }
        uniforms["distanceBounds"] = TypeValueV3(GLSLType.V2F, Vector2f()) {
            // find maximum in local space from current point to bounding box
            // best within frustum
            val dt = tree.transform?.getDrawMatrix()
            if (dt != null) {
                // compute first and second intersection with aabb
                // transform camera position into local space
                val pos = JomlPools.vec3d.create()
                val dtInverse = JomlPools.mat4x3d.create()
                dt.invert(dtInverse)
                dtInverse.transformPosition(RenderView.camPosition, pos)
                // val dir = RenderView.camDirection
                val bounds = tree.localAABB
                val dx = (pos.x - bounds.avgX()).toFloat()
                val dy = (pos.y - bounds.avgY()).toFloat()
                val dz = (pos.z - bounds.avgZ()).toFloat()
                val bdx = bounds.deltaX().toFloat()
                val bdy = bounds.deltaY().toFloat()
                val bdz = bounds.deltaZ().toFloat()
                val min = sdBox(dx, dy, dz, bdx * 0.5f, bdy * 0.5f, bdz * 0.5f)
                val max = min + length(bdx, bdy, bdz)
                tree.camNear = min
                tree.camFar = max
                it.set(max(0f, min), max)
                JomlPools.vec3d.sub(1)
                JomlPools.mat4x3d.sub(1)
            }
        }
        uniforms["localMin"] = TypeValueV3(GLSLType.V3F, Vector3f()) {
            val b = tree.localAABB
            it.set(b.minX, b.minY, b.minZ)
        }
        uniforms["localMax"] = TypeValueV3(GLSLType.V3F, Vector3f()) {
            val b = tree.localAABB
            it.set(b.maxX, b.maxY, b.maxZ)
        }
        uniforms["perspectiveCamera"] = TypeValue(GLSLType.V1B) { RenderView.isPerspective }
        uniforms["debugMode"] = TypeValue(GLSLType.V1I) { tree.debugMode.id }

        val materials = tree.sdfMaterials.map { MaterialCache[it] }
        val builder = StringBuilder(max(1, materials.size) * 128)

        val needsSwitch = materials.size > 1
        if (needsSwitch) builder
            .append("switch(clamp(int(ray.y),0,")
            .append(materials.lastIndex)
            .append(")){\n")
        for (index in 0 until max(materials.size, 1)) {
            if (needsSwitch) builder.append("case ").append(index).append(":\n")
            val material = materials.getOrNull(index) ?: defaultMaterial
            // todo support shading functions, textures and material interpolation
            // define all properties as uniforms, so they can be changed without recompilation
            val color = defineUniform(uniforms, material.diffuseBase)
            builder.append("finalColor = ").append(color).append(".xyz;\n")
            builder.append("finalAlpha = ").append(color).append(".w;\n")
            builder.append("finalMetallic = ").appendUniform(uniforms, material.metallicMinMax).append(".y;\n")
            builder.append("finalRoughness = ").appendUniform(uniforms, material.roughnessMinMax).append(".y;\n")
            builder.append("finalEmissive = ").appendUniform(uniforms, material.emissiveBase).append(";\n")
            if (needsSwitch) builder.append("break;\n")
        }
        if (needsSwitch) builder.append("}\n")

        val shader = object : ECSMeshShader("raycasting-${tree.hashCode()}") {
            override fun createFragmentStage(
                isInstanced: Boolean,
                isAnimated: Boolean,
                motionVectors: Boolean
            ): ShaderStage {
                // instancing is not supported
                val fragmentVariables = listOf(
                    Variable(GLSLType.M4x4, "transform"),
                    Variable(GLSLType.M4x3, "localTransform"),
                    Variable(GLSLType.M4x3, "invLocalTransform"),
                    Variable(GLSLType.V3F, "localCamPos"),
                    Variable(GLSLType.V3F, "localCamDir"),
                    Variable(GLSLType.V1I, "maxSteps"),
                    Variable(GLSLType.V2F, "distanceBounds"),
                    Variable(GLSLType.V3F, "localMin"),
                    Variable(GLSLType.V3F, "localMax"),
                    Variable(GLSLType.V1I, "debugMode"), // 0 = default, 1 = #steps, 2 = sdf planes
                    Variable(GLSLType.V1B, "perspectiveCamera"),
                    Variable(GLSLType.V1F, "sdfReliability"),
                    Variable(GLSLType.V1F, "sdfNormalEpsilon"),
                    Variable(GLSLType.V1F, "sdfMaxRelativeError"),
                    // is used to prevent inlining of huge functions
                    Variable(GLSLType.V1I, "ZERO"),
                    // input varyings
                    Variable(GLSLType.V3F, "finalPosition"),
                    Variable(GLSLType.V2F, "roughnessMinMax"),
                    Variable(GLSLType.V2F, "metallicMinMax"),
                    Variable(GLSLType.V1F, "occlusionStrength"),
                    // outputs
                    Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                    Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
                    Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
                    Variable(GLSLType.V3F, "finalNormal", VariableMode.OUT),
                    // Variable(GLSLType.V3F, "finalTangent", VariableMode.OUT),
                    // Variable(GLSLType.V3F, "finalBitangent", VariableMode.OUT),
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
                    Variable(GLSLType.V1F, "translucency"),
                    Variable(GLSLType.V1F, "sheen"),
                    Variable(GLSLType.V4F, "clearCoat"),
                    Variable(GLSLType.V2F, "clearCoatRoughMetallic"),
                    Variable(GLSLType.V1I, "drawMode"),
                    // todo support bridges for uniform -> fragment1 -> fragment2 (inout)
                    Variable(GLSLType.V4F, "tint", VariableMode.OUT),
                ) + uniforms.map { (k, v) -> Variable(v.type, k) }

                val stage = ShaderStage(
                    "material", fragmentVariables, "" +

                            // compute ray position & direction in local coordinates
                            // trace ray
                            // done materials
                            // convert tracing distance from local to global
                            // convert normals into global normals
                            // compute global depth

                            "if(!gl_FrontFacing) discard;\n" +
                            "vec3 localDir, localPos;\n" +
                            "if(perspectiveCamera){\n" +
                            "   localDir = normalize(invLocalTransform * vec4(finalPosition, 0.0));\n" +
                            "   localPos = localCamPos;\n" +
                            "} else {\n" +
                            // todo this is close, but not yet perfect...
                            "   localDir = localCamDir;\n" +
                            "   vec3 localHit = invLocalTransform * vec4(finalPosition, 1.0);\n" +
                            "   localPos = localHit - localDir * dot(localDir, localHit - localCamPos);\n" +
                            "}\n" +
                            "vec2 ray = map(localPos,localDir,localPos);\n" +
                            "int steps;\n" +
                            "finalAlpha = 0.0;\n" +
                            "if(ray.x >= 0.0){\n" + // not inside an object
                            "   ray = raycast(localPos, localDir, steps);\n" +
                            "   if(debugMode != ${DebugMode.NUM_STEPS.id}){\n" +
                            "       if(ray.y < 0.0 || (debugMode == ${DebugMode.SDF_ON_Y.id} && (localPos.y+ray.x*localDir.y)*sign(localDir.y) > 0.0)){\n" + // hit nothing -> sky or similar
                            "           if(debugMode == ${DebugMode.SDF_ON_Y.id}){\n" +
                            // check if & where ray hits y == 0
                            "               float distance = -localPos.y / localDir.y;\n" +
                            "               if(distance > 0.0){\n" +
                            "                   vec3 localHit = localPos + distance * localDir;\n" +
                            // check if localPos is within bounds -> only render the plane :)
                            "                   if(all(greaterThan(localHit.xz,localMin.xz)) && all(lessThan(localHit.xz,localMax.xz))){\n" +
                            "                       localHit.y = 0.0;\n" + // correct numerical issues
                            "                       finalPosition = localTransform * vec4(localHit, 1.0);\n" +
                            // "                   if(dot(vec4(finalPosition, 1.0), reflectionCullingPlane) < 0.0) discard;\n" +
                            "                       finalNormal = vec3(0.0, -sign(localDir.y), 0.0);\n" +
                            "                       vec4 newVertex = transform * vec4(finalPosition, 1.0);\n" +
                            "                       gl_FragDepth = newVertex.z/newVertex.w;\n" +
                            "                       distance = map(localPos,localDir,localHit).x;\n" + // < 0.0 removed, because we show the shape there
                            // waves like in Inigo Quilez demos, e.g. https://www.shadertoy.com/view/tt3yz7
                            // show the inside as well? mmh...
                            "                       vec3 col = vec3(0.9,0.6,0.3) * (1.0 - exp(-2.0*distance));\n" +
                            // less amplitude, when filtering issues occur
                            "                       float delta = 5.0 * length(vec2(dFdx(distance),dFdy(distance)));\n" +
                            "                       col *= 0.8 + 0.2*cos(20.0*distance)*max(1.0-delta,0.0);\n" +
                            "                       finalColor = col;\n" +
                            "                       finalAlpha = 1.0;\n" +
                            "                   } else discard;\n" +
                            "               } else discard;\n" +
                            "           } else discard;\n" +
                            "       } else {\n" +
                            "           vec3 localHit = localPos + ray.x * localDir;\n" +
                            // check bounds
                            //"           if(all(greaterThan(localHit,localMin)) && all(lessThan(localHit,localMax))){\n" +
                            "               vec3 localNormal = calcNormal(localPos, localDir, localHit, sdfNormalEpsilon);\n" +
                            // convert localHit to global hit
                            "               finalPosition = localTransform * vec4(localHit, 1.0);\n" +
                            "               finalNormal = normalize(localTransform * vec4(localNormal, 0.0));\n" +
                            // respect reflection plane
                            "               if(dot(vec4(finalPosition, 1.0), reflectionCullingPlane) < 0.0) discard;\n" +
                            // calculate depth
                            "               vec4 newVertex = transform * vec4(finalPosition, 1.0);\n" +
                            "               gl_FragDepth = newVertex.z/newVertex.w;\n" +
                            //"           } else discard;\n" + // to do instead of discarding, we should clamp the distance, and check the color there
                            // step by step define all material properties
                            builder.toString() +
                            "       }\n" +
                            "   } else {\n" + // show number of steps
                            "       if(ray.y < 0.0) ray.x = dot(distanceBounds,vec2(0.5));\n" + // avg as a guess
                            "       vec3 localHit = localPos + ray.x * localDir;\n" +
                            "       finalPosition = localTransform * vec4(localHit, 1.0);\n" +
                            // "                   if(dot(vec4(finalPosition, 1.0), reflectionCullingPlane) < 0.0) discard;\n" +
                            "       finalNormal = vec3(0.0);\n" +
                            "       vec4 newVertex = transform * vec4(finalPosition, 1.0);\n" +
                            "       gl_FragDepth = newVertex.z/newVertex.w;\n" +
                            // shading from https://www.shadertoy.com/view/WdVyDW
                            "       const vec3 a = vec3(97, 130, 234) / vec3(255.0);\n" +
                            "       const vec3 b = vec3(220, 94, 75) / vec3(255.0);\n" +
                            "       const vec3 c = vec3(221, 220, 219) / vec3(255.0);\n" +
                            "       float t = float(steps)/float(maxSteps);\n" +
                            "       finalColor = vec3(0.0);\n" +
                            "       finalEmissive = t < 0.5 ? mix(a, c, 2.0 * t) : mix(c, b, 2.0 * t - 1.0);\n" +
                            "       finalEmissive *= 2.0;\n" + // only correct with current tonemapping...
                            "       finalAlpha = 1.0;\n" +
                            "   }\n" +

                            "} else {\n" +// inside an object

                            /*"   finalColor = vec3(0.5);\n" +
                            "   finalAlpha = 0.5;\n" +
                            "   finalEmissive  = vec3(0.0);\n" +
                            "   finalMetallic  = 0.0;\n" +
                            "   finalRoughness = 1.0;\n" +

                            // distance must be set to slightly above 0, so we have valid z values
                            "   finalPosition = localTransform * vec4(localPos + localDir * 0.001, 1.0);\n" +
                            "   finalNormal = vec3(0.0);\n" +

                            // respect reflection plane
                            "   if(dot(vec4(finalPosition, 1.0), reflectionCullingPlane) < 0.0) discard;\n" +

                            // calculate depth
                            "   vec4 newVertex = transform * vec4(finalPosition, 1.0);\n" +
                            "   gl_FragDepth = newVertex.z/newVertex.w;\n" +*/
                            // todo make this behaviour modifiable
                            "   discard;\n" +

                            "}\n" +

                            // click ids of parts
                            "if(drawMode == ${ShaderPlus.DrawMode.ID.id}){\n" +
                            "   int intId = int(ray.y);\n" +
                            "   tint = vec4(vec3(float(intId&255), float((intId>>8)&255), float((intId>>16)&255))/255.0, 1.0);\n" +
                            "} else tint = vec4(1.0);\n" +
                            ""

                )
                stage.functions.ensureCapacity(stage.functions.size + functions.size + 1)
                val builder2 =
                    StringBuilder(100 + functions.sumOf { it.length } + shapeDependentShader.length + raycasting.length + normal.length)
                builder2.append(
                    "" +
                            "#define Infinity 1e20\n" +
                            "#define PI 3.141592653589793\n" +
                            "#define TAU 6.283185307179586\n" +
                            "#define PHI 1.618033988749895\n"
                )
                for (func in functions) builder2.append(func)
                builder2.append("vec2 map(vec3 ro, vec3 rd, vec3 pos0){\n")
                builder2.append("   vec2 res0;\n")
                builder2.append(shapeDependentShader)
                builder2.append("   return res0;\n}\n")
                builder2.append(raycasting)
                builder2.append(normal)
                stage.functions.add(Function(builder2.toString()))
                return stage
            }
        }
        // why are those not ignored?
        shader.ignoreNameWarnings(
            "sheenNormalMap", "occlusionMap", "metallicMap", "roughnessMap",
            "emissiveMap", "normalMap", "diffuseMap", "diffuseBase", "emissiveBase", "drawMode", "applyToneMapping",
            "ambientLight"
        )
        return uniforms to shader
    }

}