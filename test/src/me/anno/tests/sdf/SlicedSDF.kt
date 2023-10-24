package me.anno.tests.sdf

import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimTexture
import me.anno.ecs.components.light.AmbientLight
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.SpotLight
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.shapes.PlaneModel
import me.anno.ecs.components.shaders.Skybox
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.PlaneShapes
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.CullMode
import me.anno.gpu.shader.*
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.maths.Maths.hasFlag
import me.anno.mesh.Shapes
import me.anno.sdf.SDFComposer
import me.anno.sdf.SDFGroup
import me.anno.sdf.VariableCounter
import me.anno.sdf.shapes.SDFBox
import me.anno.sdf.shapes.SDFSphere
import me.anno.sdf.shapes.SDFTorus
import me.anno.utils.OS.documents
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Floats.toRadians
import org.joml.AABBd
import org.joml.Matrix4x3d
import kotlin.math.*

fun main() {

    // todo other thing: screen space shadows :)
    // https://panoskarabelas.com/posts/screen_space_shadows/

    ECSRegistry.init()

    // todo render dust, fog, modelled by SDF
    // doesn't work yet :/, and I get black flickering :(
    // - sorted back to front
    // - alpha blending
    class FogSDFController : SDFGroup() {

        // todo make base mesh a stack of slices, that rotates towards the camera
        // todo compare to background depth
        // todo sample light at currently traced range
        init {
            // material.pipelineStage = 1
        }

        // todo use AABB for calculation
        override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
            return super.fillSpace(globalTransform, aabb)
        }

        override fun generateMesh(mesh: Mesh) {
            val aabb = JomlPools.aabbf.create()
            aabb.clear()
            calculateBounds(aabb)

            val base = Shapes.flat11
            val base1 = base.back
            val base2 = base1.positions!!
            val position = mesh.positions.resize(base2.size)
            mesh.positions = position

            val sx = aabb.deltaX * 0.5f
            val sy = aabb.deltaY * 0.5f
            val offsetX = aabb.centerX
            val offsetY = aabb.centerY
            for (i in position.indices step 3) {
                position[i + 0] = sign(base2[i + 0]) * sx + offsetX
                position[i + 1] = sign(base2[i + 1]) * sy + offsetY
                position[i + 2] = 0f
            }
            mesh.indices = base1.indices
            mesh.normals = base1.normals

            mesh.aabb.set(aabb)
            mesh.inverseOutline = true
            mesh.invalidateGeometry()
            // todo calculate procedural length based on state
            mesh.proceduralLength = 16
            mesh.cullMode = CullMode.BOTH
            JomlPools.aabbf.sub(1)
        }

        override fun createShader(): Pair<Map<String, TypeValue>, BaseShader> {
            val tree = this
            // todo modify shader to our needs :3
            val functions = LinkedHashSet<String>()
            val uniforms = HashMap<String, TypeValue>()
            val shapeDependentShader = StringBuilder()
            buildShader(shapeDependentShader, 0, VariableCounter(1), 0, uniforms, functions, ArrayList())
            // val materials = sdfMaterials.map { MaterialCache[it] }
            // val materialCode = SDFComposer.buildMaterialCode(tree, materials, uniforms)
            val shader = object : SDFComposer.SDFShader(tree) {
                override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
                    super.bind(shader, renderer, instanced)

                    // todo use size of smallest details for minZ

                    val minZ = max(tree.camNear, 0.01f)
                    val maxZ = max(minZ * 1.001f, tree.camFar)

                    val maxStripes = 64
                    val maxPower = log2(1.5f)

                    val totalPower = log2(maxZ / minZ)
                    val numStripes = max(1, min(maxStripes, ceil(totalPower / maxPower).toInt()))

                    val stripePower = totalPower / numStripes

                    // val distance = tree.transform!!.globalPosition.distance(RenderState.cameraPosition).toFloat()
                    shader.v2f("stripePowers", minZ, stripePower)
                    shader.v1i("subSamples", 25)

                    tree.getMeshOrNull().proceduralLength = numStripes
                }

                override fun createVertexStages(flags: Int): List<ShaderStage> {
                    val defines = createDefines(flags)
                    val variables = createVertexVariables(flags) + listOf(
                        Variable(GLSLType.M3x3, "displayTransform"),
                        Variable(GLSLType.V2F, "stripePowers"),
                        Variable(GLSLType.V2F, "distanceBounds", VariableMode.OUT)
                    )
                    val stage = ShaderStage(
                        "vertex",
                        variables, defines.toString() +
                                // calculate plane coordinates and z,dz for evaluation
                                "distanceBounds.x = 0.0;//stripePowers.x * pow(2.0, 1.0 + stripePowers.y * float(gl_InstanceID));\n" +
                                "distanceBounds.y = 1000.0;//distanceBounds.x * stripePowers.y;\n" +
                                "localPosition = coords;// matMul(displayTransform, vec3(coords.xy, 0.0));\n" +
                                motionVectorInit +
                                normalInitCode +
                                applyTransformCode +
                                "gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                                ShaderLib.positionPostProcessing
                    )
                    if (flags.hasFlag(IS_ANIMATED) && AnimTexture.useAnimTextures) stage.add(getAnimMatrix)
                    if (flags.hasFlag(USES_PRS_TRANSFORM)) stage.add(ShaderLib.quatRot)
                    return listOf(stage)
                }

                override fun createFragmentStages(flags: Int): List<ShaderStage> {
                    // instancing is not supported
                    val fragmentVariables = SDFComposer.fragmentVariables1 +
                            uniforms.map { (k, v) -> Variable(v.type, k) } +
                            listOf(
                                Variable(GLSLType.V2F, "stripePowers"),
                                Variable(GLSLType.V1I, "subSamples"),
                            )
                    val stage = ShaderStage(
                        name, fragmentVariables, "" +
                                "vec2 uv0 = gl_FragCoord.xy / renderSize;\n" +
                                "vec3 localDir = normalize(matMul(invLocalTransform, vec4(rawCameraDirection(uv0),0.0)));\n" +
                                "vec3 localPos = localPosition - localDir * max(0.0,dot(localPosition-localCamPos,localDir));\n" +
                                "localPos = matMul(invLocalTransform, vec4(depthToPosition(uv0,perspectiveCamera?0.0:1.0),1.0));\n" +
                                // trace the section from distanceBounds.x to distanceBounds.y, and accumulate volume density
                                "vec3 emissive = vec3(0.0);\n" +
                                "vec3 color = vec3(0.0);\n" +
                                "float alpha = 1.0;\n" +
                                "float roughness = 1.0;\n" +
                                "float metallic = 0.0;\n" +
                                "int steps=0;\n" +

                                "vec4 ray = raycast(localPos, localDir, steps);\n" + // distance, material, uv
                                "if(ray.x > distanceBounds.y) discard;\n" +
                                "vec3 localHit = localPos + ray.x * localDir;\n" +
                                "vec3 localNormal1 = calcNormal(localPos, localDir, localHit, ray.x * sdfNormalEpsilon, ray.x);\n" +
                                "finalNormal = normalize(matMul(localTransform, vec4(localNormal1, 0.0)));\n" +

                                // todo use depth texture to find the distance to the background
                                // todo if we're out of bounds, just skip this entirely
                                // todo respect that range for smooth blending
                                // blending: from back to front, like default blend mode
                                "float lastZ = distanceBounds.y;\n" +
                                "float litZ = ray.y;//lastZ;\n" +
                                "tint = vec4(1);\n" +
                                /*  "for(int i=subSamples-1;i>=0;i--){\n" +
                                  "   float z = distanceBounds.x * pow(2.0, 1.0 + stripePowers.y * float(i)/float(subSamples));\n" +
                                  "   float dz = lastZ-z;\n" +
                                  "   vec4 ray = map(localPos, localDir, localPos, z);\n" +
                                  "   ray.x -= dz * 0.5;\n" +
                                  "   if(ray.x < 0.0) {\n" +
                                  // we hit something
                                  "       float thickness = dz;\n" +
                                  "       float density = -ray.x;\n" +
                                  // eval material
                                  // materialCode +
                                  // calculate opacity using beers law
                                  "       finalAlpha = 1.0;// - pow(0.5, density * thickness * finalAlpha);\n" +
                                  "       if(finalAlpha > 0.0){\n" +
                                  // mix properties by alpha of slice :)
                                  "           float f = mix(finalAlpha/(alpha+finalAlpha),1.0,finalAlpha);\n" +
                                  "           alpha = mix(alpha,1.0,finalAlpha);\n" + // or f?
                                  "           emissive = mix(emissive,finalEmissive,f);\n" +
                                  "           color = mix(color,finalColor,f);\n" +
                                  "           roughness = mix(roughness,finalRoughness,f);\n" +
                                  "           metallic = mix(metallic,finalMetallic,f);\n" +
                                  "           litZ = mix(litZ,z,f);\n" +
                                  "       }\n" +
                                  "   } else {\n" +
                                  // skip large sections, if possible
                                  "       int worthySteps = int(floor(ray.x / dz));\n" +
                                  "       i -= max(worthySteps-1, 0);\n" +
                                  "   }\n" +
                                  "   lastZ = z;\n" +
                                  "}\n" +*/
                                "if(alpha <= 0.0) discard;\n" +
                                "finalColor = color;\n" +
                                "finalAlpha = alpha;\n" +
                                "finalEmissive = emissive;\n" +
                                "finalRoughness = roughness;\n" +
                                "finalMetallic = metallic;\n" +
                                "finalTranslucency = 1.0;\n" + // because it's gas
                                // depth and position calculation for light calculations
                                "localPosition = localPos + litZ * localDir;\n" +
                                "finalPosition = matMul(localTransform, vec4(localPosition, 1.0));\n" +
                                // depth calculation isn't really necessary and could be skipped
                                "vec4 newVertex = matMul(transform, vec4(finalPosition, 1.0));\n" + // calculate depth
                                "gl_FragDepth = newVertex.z/newVertex.w;\n"
                    )
                    functions.add(SDFBox.sdBox)
                    functions.add(DepthTransforms.rawToDepth)
                    functions.add(DepthTransforms.depthToPosition)
                    stage.add(SDFComposer.build(functions, shapeDependentShader))
                    return listOf(stage)
                }
            }
            // why are those not ignored?
            SDFComposer.ignoreCommonNames(shader)
            return uniforms to shader
        }
    }

    val scene = Entity()
    scene.add(Skybox())
    scene.add(Entity("Floor").apply {
        position = position.set(0.0, -1.0, 0.0)
        scale = scale.set(20.0)
        val mesh = MeshComponent(PlaneModel.createPlane(2, 2))
        mesh.materials = listOf(Material().apply {
            diffuseBase.set(0.05f, 0.05f, 0.05f, 1f)
            cullMode = CullMode.BOTH
        }.ref)
        addChild(mesh)
    })
    scene.add(MeshComponent(documents.getChild("monkey.obj")))
    scene.add(FogSDFController().apply {
        name = "SDFs"
        addChild(SDFSphere().apply {
            position.set(4f, 0f, 0f)
        })
        addChild(SDFTorus().apply {
            position.set(-4f, 0f, 0f)
        })
    })
    scene.add(Entity("Directional").apply {
        rotation = rotation
            .rotateY((-50.0).toRadians())
            .rotateX((-45.0).toRadians())
        scale = scale.set(10.0)
        addChild(DirectionalLight().apply {
            shadowMapCascades = 1
            color = color.set(20f)
        })
        addChild(AmbientLight())
    })
    scene.add(Entity("Point").apply {
        position = position.set(4.0, 2.0, 0.0)
        scale = scale.set(20.0)
        addChild(PointLight().apply {
            color.set(2f, 10f, 2f)
            shadowMapCascades = 1
            shadowMapResolution = 256
        })
    })
    scene.add(Entity("Spot").apply {
        position = position.set(-5.0, 2.5, 0.6)
        rotation = rotation
            .rotateY((-68.0).toRadians())
            .rotateX((-72.0).toRadians())
        scale = scale.set(30.0)
        addChild(SpotLight().apply {
            color.set(10f, 2f, 2f)
            near = 0.01
            shadowMapCascades = 1
            shadowMapResolution = 256
            coneAngle = 0.57f
        })
    })
    testSceneWithUI("Sliced SDF", scene)
}