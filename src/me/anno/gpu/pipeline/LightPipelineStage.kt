package me.anno.gpu.pipeline

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.light.*
import me.anno.engine.pbr.PBRLibraryGLTF.specularBRDFv2NoColor
import me.anno.engine.pbr.PBRLibraryGLTF.specularBRDFv2NoColorEnd
import me.anno.engine.pbr.PBRLibraryGLTF.specularBRDFv2NoColorStart
import me.anno.engine.ui.render.Renderers
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.M4x3Delta.m4x3delta
import me.anno.gpu.M4x3Delta.m4x3x
import me.anno.gpu.pipeline.PipelineStage.Companion.instancedBatchSize
import me.anno.gpu.pipeline.PipelineStage.Companion.setupLocalTransform
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.OpenGLShader.Companion.attribute
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.io.Saveable
import me.anno.maths.Maths.min
import me.anno.utils.structures.lists.SmallestKList
import me.anno.utils.types.Booleans.toInt
import org.joml.Matrix4fc
import org.joml.Vector3d
import org.lwjgl.opengl.GL15C.GL_DYNAMIC_DRAW

class LightPipelineStage(
    val deferred: DeferredSettingsV2
) : Saveable() {

    companion object {

        private val lightInstancedAttributes = listOf(
            // transform
            Attribute("instanceTrans0", 3),
            Attribute("instanceTrans1", 3),
            Attribute("instanceTrans2", 3),
            Attribute("instanceTrans3", 3),
            // inverse transform for light mapping
            Attribute("invInsTrans0", 3),
            Attribute("invInsTrans1", 3),
            Attribute("invInsTrans2", 3),
            Attribute("invInsTrans3", 3),
            // light properties like type, color, cone angle
            Attribute("lightData0", 4),
            Attribute("lightData1", 4),
            Attribute("shadowData", 4)
            // instanced rendering does not support shadows -> no shadow data / as a uniform
        )

        private val lightCountInstancedAttributes = listOf(
            // transform
            Attribute("instanceTrans0", 3),
            Attribute("instanceTrans1", 3),
            Attribute("instanceTrans2", 3),
            Attribute("instanceTrans3", 3),
            Attribute("shadowData", 1)
            // instanced rendering does not support shadows -> no shadow data / as a uniform
        )

        // test: can we get better performance, when we eliminate dependencies?
        // pro: less dependencies
        // con: we use more vram
        // con: we remove cache locality
        /*private val lightInstanceBuffers = Array(512) {
            StaticBuffer(lightInstancedAttributes, instancedBatchSize, GL_DYNAMIC_DRAW)
        }
        private var libIndex = 0
        // performance: the same
        */

        private val lightInstanceBuffer = StaticBuffer(lightInstancedAttributes, instancedBatchSize, GL_DYNAMIC_DRAW)
        private val lightCountInstanceBuffer =
            StaticBuffer(lightCountInstancedAttributes, instancedBatchSize, GL_DYNAMIC_DRAW)

        fun getPostShader(settingsV2: DeferredSettingsV2): Shader {
            return shaderCache.getOrPut(settingsV2 to -1) {
                /*
                * vec3 diffuseColor  = finalColor * (1.0 - finalMetallic);
                * vec3 specularColor = finalColor * finalMetallic;
                * finalColor = diffuseColor * diffuseLight + specularLight; // specular already contains the color
                * finalColor = finalColor * (1.0 - finalOcclusion) + finalEmissive; // color, happens in post-processing
                * finalColor = tonemap(finalColor); // tone mapping
                * */
                val builder = ShaderBuilder("post")
                builder.addVertex(
                    ShaderStage(
                        "v", listOf(
                            Variable(GLSLType.V2F, "coords", VariableMode.ATTR),
                            Variable(GLSLType.V2F, "uv", VariableMode.OUT)
                        ), "gl_Position = vec4(coords*2.0-1.0,0.5,1.0);\n" +
                                "uv = coords;\n"
                    )
                )

                val fragment = ShaderStage(
                    "f", listOf(
                        Variable(GLSLType.V3F, "finalColor"),
                        Variable(GLSLType.V3F, "finalPosition"),
                        Variable(GLSLType.V1F, "finalOcclusion"),
                        Variable(GLSLType.V3F, "finalEmissive"),
                        Variable(GLSLType.V1B, "applyToneMapping"),
                        Variable(GLSLType.S2D, "finalLight"),
                        Variable(GLSLType.S2D, "ambientOcclusion"),
                        Variable(GLSLType.V3F, "ambientLight"),
                        Variable(GLSLType.V4F, "color", VariableMode.OUT)
                    ), "" +
                            "   vec3 color3;\n" +
                            "   if(length(finalPosition) < 1e34){\n" +
                            "       vec3 light = texture(finalLight, uv).rgb + ambientLight;\n" +
                            "       float occlusion = (1.0 - finalOcclusion) * (1.0 - texture(ambientOcclusion, uv).r);\n" +
                            "       color3 = finalColor * light * occlusion + finalEmissive;\n" +
                            "   } else color3 = finalColor + finalEmissive;\n" + // sky
                            "   if(applyToneMapping) color3 = color3/(1.0+color3);\n" +
                            "   color = vec4(color3, 1.0);\n"
                )

                // deferred inputs
                // find deferred layers, which exist, and appear in the shader
                val deferredCode = StringBuilder()
                val deferredInputs = ArrayList<Variable>()
                deferredInputs += Variable(GLSLType.V2F, "uv")
                val imported = HashSet<String>()
                for (layer in settingsV2.layers) {
                    // if this layer is present,
                    // then define the output,
                    // and write the mapping
                    if (layer.type.glslName in fragment.variables.map { it.name }) {
                        layer.appendMapping(deferredCode, "Tmp", "uv", imported)
                    }
                }
                deferredInputs += imported.map { Variable(GLSLType.S2D, it, VariableMode.IN) }
                builder.addFragment(ShaderStage("deferred", deferredInputs, deferredCode.toString()))
                builder.addFragment(fragment)
                val shader = builder.create()
                // find all textures
                // first the ones for the deferred data
                // then the ones for the shadows
                val textures = listOf("finalLight", "ambientOcclusion") + settingsV2.layers2.map { it.name }
                shader.ignoreNameWarnings(
                    "tint", "invLocalTransform",
                    "defLayer0", "defLayer1", "defLayer2", "defLayer3",
                    "defLayer4", "defLayer5", "defLayer6", "defLayer7"
                )
                shader.setTextureIndices(textures)
                shader
            }
        }

        var countPerPixel = 0.25f

        private val shaderCache = HashMap<Pair<DeferredSettingsV2, Int>, Shader>()
        fun getShader(settingsV2: DeferredSettingsV2, type: LightType): Shader {
            val isInstanced = GFXState.instanced.currentValue
            val key = type.ordinal * 2 + isInstanced.toInt()
            return shaderCache.getOrPut(settingsV2 to key) {
                /*
                * vec3 diffuseColor  = finalColor * (1.0 - finalMetallic);
                * vec3 specularColor = finalColor * finalMetallic;
                * finalColor = diffuseColor * diffuseLight + specularLight; // specular already contains the color
                * finalColor = finalColor * finalOcclusion + finalEmissive; // color, happens in post-processing
                * finalColor = tonemap(finalColor); // tone mapping
                * */
                val builder = ShaderBuilder("$type-$isInstanced")
                builder.addVertex(
                    if (isInstanced) {
                        ShaderStage(
                            "v", listOf(
                                Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
                                Variable(GLSLType.V3F, "instanceTrans0", VariableMode.ATTR),
                                Variable(GLSLType.V3F, "instanceTrans1", VariableMode.ATTR),
                                Variable(GLSLType.V3F, "instanceTrans2", VariableMode.ATTR),
                                Variable(GLSLType.V3F, "instanceTrans3", VariableMode.ATTR),
                                Variable(GLSLType.V3F, "invInsTrans0", VariableMode.ATTR),
                                Variable(GLSLType.V3F, "invInsTrans1", VariableMode.ATTR),
                                Variable(GLSLType.V3F, "invInsTrans2", VariableMode.ATTR),
                                Variable(GLSLType.V3F, "invInsTrans3", VariableMode.ATTR),
                                Variable(GLSLType.V4F, "lightData0", VariableMode.ATTR),
                                Variable(GLSLType.V4F, "lightData1", VariableMode.ATTR),
                                Variable(GLSLType.V4F, "shadowData", VariableMode.ATTR),
                                Variable(GLSLType.M4x4, "transform", VariableMode.IN),
                                Variable(GLSLType.V4F, "data0", VariableMode.OUT),
                                Variable(GLSLType.V4F, "data1", VariableMode.OUT),
                                Variable(GLSLType.V4F, "data2", VariableMode.OUT),
                                Variable(GLSLType.M4x3, "WStoLightSpace", VariableMode.OUT),
                                Variable(GLSLType.V3F, "uvw", VariableMode.OUT)
                            ), "" +
                                    "data0 = lightData0;\n" +
                                    "data1 = lightData1;\n" +
                                    "data2 = shadowData;\n" +
                                    // cutoff = 0 -> scale onto the whole screen, has effect everywhere
                                    "if(${type == LightType.DIRECTIONAL} && data2.a <= 0.0){\n" +
                                    "   gl_Position = vec4(coords.xy, 0.5, 1.0);\n" +
                                    "} else {\n" +
                                    "   mat4x3 localTransform = mat4x3(instanceTrans0,instanceTrans1,instanceTrans2,instanceTrans3);\n" +
                                    "   gl_Position = transform * vec4(localTransform * vec4(coords, 1.0), 1.0);\n" +
                                    "}\n" +
                                    "WStoLightSpace = mat4x3(invInsTrans0,invInsTrans1,invInsTrans2,invInsTrans3);\n" +
                                    "uvw = gl_Position.xyw;\n"
                        )
                    } else {
                        ShaderStage(
                            "v", listOf(
                                Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
                                Variable(GLSLType.M4x4, "transform", VariableMode.IN),
                                Variable(GLSLType.M4x3, "localTransform", VariableMode.IN),
                                Variable(GLSLType.V1F, "cutoff", VariableMode.IN),
                                Variable(GLSLType.V3F, "uvw", VariableMode.OUT)
                            ), "" +
                                    // cutoff = 0 -> scale onto the whole screen, has effect everywhere
                                    "if(${type == LightType.DIRECTIONAL} && cutoff <= 0.0){\n" +
                                    "   gl_Position = vec4(coords.xy, 0.5, 1.0);\n" +
                                    "} else {\n" +
                                    "   gl_Position = transform * vec4(localTransform * vec4(coords, 1.0), 1.0);\n" +
                                    "}\n" +
                                    "uvw = gl_Position.xyw;\n"
                        )
                    }
                )
                builder.addFragment(
                    ShaderStage(
                        "uv", listOf(
                            Variable(GLSLType.V3F, "uvw", VariableMode.IN),
                            Variable(GLSLType.V2F, "uv", VariableMode.OUT)
                        ), "uv = uvw.xy/uvw.z*.5+.5;\n"
                    )
                )
                val withShadows = !isInstanced
                val cutoffContinue = "discard"
                val coreFragment = when (type) {
                    LightType.SPOT -> SpotLight.getShaderCode(cutoffContinue, withShadows)
                    LightType.DIRECTIONAL -> DirectionalLight.getShaderCode(cutoffContinue, withShadows)
                    LightType.POINT -> PointLight.getShaderCode(cutoffContinue, withShadows, true)
                }
                val fragment = ShaderStage(
                    "f", listOf(
                        Variable(GLSLType.V4F, "data0"),
                        Variable(GLSLType.V4F, "data1"),
                        Variable(GLSLType.V4F, "data2"), // only if with shadows
                        // light maps for shadows
                        // - spot lights, directional lights
                        Variable(GLSLType.S2D, "shadowMapPlanar", Renderers.MAX_PLANAR_LIGHTS),
                        // - point lights
                        Variable(GLSLType.SCube, "shadowMapCubic", 1),
                        Variable(GLSLType.V1B, "receiveShadows"),
                        // Variable(GLSLType.V3F, "finalColor"), // not really required
                        Variable(GLSLType.V3F, "finalPosition"),
                        Variable(GLSLType.V3F, "finalNormal"),
                        // Variable(GLSLType.V1F, "finalOcclusion"), post-process, including ambient
                        Variable(GLSLType.V1F, "finalMetallic"),
                        Variable(GLSLType.V1F, "finalRoughness"),
                        Variable(GLSLType.V1F, "finalSheen"),
                        Variable(GLSLType.V1F, "finalTranslucency"),
                        Variable(GLSLType.M4x3, "WStoLightSpace"), // invLightMatrices[i]
                        Variable(GLSLType.V4F, "light", VariableMode.OUT)
                    ), "" +
                            // light calculation including shadows if !instanced
                            "vec3 diffuseLight = vec3(0.0), specularLight = vec3(0.0);\n" +
                            "bool hasSpecular = finalMetallic > 0.0;\n" +
                            "vec3 V = normalize(-finalPosition);\n" +
                            "float NdotV = dot(finalNormal,V);\n" +
                            "int shadowMapIdx0 = 0;\n" + // always 0 at the start
                            "int shadowMapIdx1 = int(data2.g);\n" +
                            // light properties, which are typically inside the loop
                            "vec3 lightColor = data0.rgb;\n" +
                            "vec3 dir = WStoLightSpace * vec4(finalPosition, 1.0);\n" +
                            "vec3 localNormal = normalize(WStoLightSpace * vec4(finalNormal, 0.0));\n" +
                            "float NdotL = 0.0;\n" + // normal dot light
                            "vec3 effectiveDiffuse, effectiveSpecular, lightPosition, lightDirWS = vec3(0.0);\n" +
                            coreFragment +
                            "if(hasSpecular && NdotL > 0.0001 && NdotV > 0.0001){\n" +
                            "   vec3 H = normalize(V + lightDirWS);\n" +
                            specularBRDFv2NoColorStart +
                            specularBRDFv2NoColor +
                            "   specularLight = effectiveSpecular * computeSpecularBRDF;\n" +
                            specularBRDFv2NoColorEnd +
                            "}\n" +
                            // translucency; looks good and approximately correct
                            // sheen is a fresnel effect, which adds light at the edge, e.g. for clothing
                            "NdotL = mix(NdotL, 0.23, finalTranslucency) + finalSheen;\n" +
                            "diffuseLight += effectiveDiffuse * clamp(NdotL, 0.0, 1.0);\n" +
                            "light = vec4(mix(diffuseLight, specularLight, finalMetallic), 1.0);\n"
                )

                // deferred inputs
                // find deferred layers, which exist, and appear in the shader
                val deferredCode = StringBuilder()
                val deferredInputs = ArrayList<Variable>()
                deferredInputs += Variable(GLSLType.V2F, "uv")
                val imported = HashSet<String>()
                for (layer in settingsV2.layers) {
                    // if this layer is present,
                    // then define the output,
                    // and write the mapping
                    if (layer.type.glslName in fragment.variables.map { it.name }) {
                        layer.appendMapping(deferredCode, "Tmp", "uv", imported)
                    }
                }
                deferredInputs += imported.map { Variable(GLSLType.S2D, it, VariableMode.IN) }
                builder.addFragment(ShaderStage("deferred", deferredInputs, deferredCode.toString()))
                builder.addFragment(fragment)
                val shader = builder.create()
                // find all textures
                // first the ones for the deferred data
                // then the ones for the shadows
                val textures = settingsV2.layers2.map { it.name } +
                        listOf("shadowMapCubic0") +
                        Array(Renderers.MAX_PLANAR_LIGHTS) { "shadowMapPlanar$it" }
                shader.ignoreNameWarnings(
                    "tint", "invLocalTransform", "colors",
                    "tangents", "uvs", "normals", "isDirectional",
                    "defLayer0", "defLayer1", "defLayer2", "defLayer3", "defLayer4"
                )
                shader.setTextureIndices(textures)
                shader
            }
        }

        val visualizeLightCountShader = lazy {
            Shader(
                "visualize-light-count", "" +
                        "$attribute vec3 coords;\n" +
                        "uniform mat4 transform;\n" +
                        "uniform mat4x3 localTransform;\n" +
                        "uniform bool fullscreen;\n" +
                        "void main(){\n" +
                        // cutoff = 0 -> scale onto the whole screen, has effect everywhere
                        "   if(fullscreen){\n" +
                        "      gl_Position = vec4(coords.xy, 0.5, 1.0);\n" +
                        "   } else {\n" +
                        "      gl_Position = transform * vec4(localTransform * vec4(coords, 1.0), 1.0);\n" +
                        "   }\n" +
                        "}\n", listOf(), "" +
                        "out vec4 glFragColor;\n" +
                        "uniform float countPerPixel;\n" +
                        "void main(){ glFragColor = vec4(countPerPixel); }"
            )
        }

        val visualizeLightCountShaderInstanced = lazy {
            Shader(
                "visualize-light-count-instanced", "" +
                        "$attribute vec3 coords;\n" +
                        "$attribute vec3 instanceTrans0;\n" +
                        "$attribute vec3 instanceTrans1;\n" +
                        "$attribute vec3 instanceTrans2;\n" +
                        "$attribute vec3 instanceTrans3;\n" +
                        "$attribute vec4 shadowData;\n" +
                        "uniform mat4 transform;\n" +
                        "uniform bool isDirectional;\n" +
                        "void main(){\n" +
                        // cutoff = 0 -> scale onto the whole screen, has effect everywhere
                        "   if(isDirectional && shadowData.a <= 0.0){\n" +
                        "      gl_Position = vec4(coords.xy, 0.5, 1.0);\n" +
                        "   } else {\n" +
                        "       mat4x3 localTransform = mat4x3(instanceTrans0,instanceTrans1,instanceTrans2,instanceTrans3);\n" +
                        "      gl_Position = transform * vec4(localTransform * vec4(coords, 1.0), 1.0);\n" +
                        "   }\n" +
                        "}", listOf(), "" +
                        "out vec4 glFragColor;\n" +
                        "uniform float countPerPixel;\n" +
                        "void main(){ glFragColor = vec4(countPerPixel); }"
            ).apply {
                ignoreNameWarnings("normals", "uvs", "tangents", "colors", "receiveShadows")
            }
        }
    }

    var visualizeLightCount = false

    var depthMode = DepthMode.ALWAYS
    var blendMode = BlendMode.ADD
    var writeDepth = false
    var cullMode = CullMode.FRONT

    // not yet optimized
    val environmentMaps = ArrayList<EnvironmentMap>()

    val size get() = instanced.size + nonInstanced.size

    class Group {

        val dirs = ArrayList<LightRequest<DirectionalLight>>()
        val spots = ArrayList<LightRequest<SpotLight>>()
        val points = ArrayList<LightRequest<PointLight>>()

        var dirIndex = 0
        var spotIndex = 0
        var pointsIndex = 0

        fun clear() {
            dirIndex = 0
            spotIndex = 0
            pointsIndex = 0
        }

        private fun <V : LightComponent> add(
            list: MutableList<LightRequest<V>>,
            index: Int, light: V, transform: Transform
        ) {
            if (index >= list.size) {
                list.add(LightRequest(light, transform))
            } else {
                list[index].set(light, transform)
            }
        }

        fun add(light: LightComponent, transform: Transform) {
            when (light) {
                is DirectionalLight -> add(dirs, dirIndex++, light, transform)
                is PointLight -> add(points, pointsIndex++, light, transform)
                is SpotLight -> add(spots, spotIndex++, light, transform)
            }
        }

        fun listOfAll(): List<LightRequest<*>> {
            return dirs.subList(0, dirIndex) +
                    spots.subList(0, spotIndex) +
                    points.subList(0, pointsIndex)
        }

        fun listOfAll(dst: SmallestKList<LightRequest<*>>): Int {
            dst.addAll(dirs, 0, dirIndex)
            dst.addAll(spots, 0, spotIndex)
            dst.addAll(points, 0, pointsIndex)
            return dst.size
        }

        operator fun get(index: Int): LightRequest<*> {
            return when {
                index < dirIndex -> dirs[index]
                index < dirIndex + spotIndex -> spots[index - dirIndex]
                else -> points[index - (dirIndex + spotIndex)]
            }
        }

        inline fun forEachType(run: (lights: List<LightRequest<*>>, type: LightType, size: Int) -> Unit) {
            if (dirIndex > 0) run(dirs, LightType.DIRECTIONAL, dirIndex)
            if (spotIndex > 0) run(spots, LightType.SPOT, spotIndex)
            if (pointsIndex > 0) run(points, LightType.POINT, pointsIndex)
        }

        val size get() = dirIndex + spotIndex + pointsIndex
        fun isNotEmpty() = size > 0

    }

    private val instanced = Group()
    private val nonInstanced = Group()

    fun bindDraw(source: IFramebuffer, cameraMatrix: Matrix4fc, cameraPosition: Vector3d, worldScale: Double) {
        if (instanced.isNotEmpty() || nonInstanced.isNotEmpty()) {
            GFXState.blendMode.use(blendMode) {
                GFXState.depthMode.use(depthMode) {
                    GFXState.depthMask.use(writeDepth) {
                        GFXState.cullMode.use(cullMode) {
                            draw(source, cameraMatrix, cameraPosition, worldScale)
                        }
                    }
                }
            }
        }
    }

    private fun initShader(shader: Shader, cameraMatrix: Matrix4fc) {
        // information for the shader, which is material agnostic
        // add all things, the shader needs to know, e.g., light direction, strength, ...
        // (for the cheap shaders, which are not deferred)
        shader.m4x4("transform", cameraMatrix)
    }

    fun getShader(type: LightType, isInstanced: Boolean): Shader {
        return if (visualizeLightCount) {
            if (isInstanced) visualizeLightCountShaderInstanced.value
            else visualizeLightCountShader.value
        } else {
            Companion.getShader(deferred, type)
        }
    }

    fun draw(source: IFramebuffer, cameraMatrix: Matrix4fc, cameraPosition: Vector3d, worldScale: Double) {

        val time = Engine.gameTime

        source.bindTextures(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

        nonInstanced.forEachType { lights, type, size ->

            val sample = lights[0].light
            val mesh = sample.getLightPrimitive()

            val shader = getShader(type, false)
            shader.use()

            shader.v4f("tint", -1)
            shader.v1b("receiveShadows", true)
            shader.v1f("countPerPixel", countPerPixel)

            initShader(shader, cameraMatrix)

            mesh.ensureBuffer()

            val maxTextureIndex = 31
            val planarIndex0 = shader.getTextureIndex("shadowMapPlanar0")
            val cubicIndex0 = shader.getTextureIndex("shadowMapCubic0")
            val supportsPlanarShadows = planarIndex0 in 0..maxTextureIndex
            val supportsCubicShadows = cubicIndex0 in 0..maxTextureIndex

            for (index in 0 until size) {

                val request = lights[index]
                val light = request.light

                val transform = request.transform

                shader.v1b("fullscreen", light is DirectionalLight && light.cutoff <= 0.0)

                setupLocalTransform(shader, transform, time)

                val m = transform.getDrawMatrix(time)

                // define the light data
                // data0: color, type;
                // type is ignored by the shader -> just use 1
                shader.v4f("data0", light.color, 1f)

                // data1: camera position, shader specific value (cone angle / size)
                shader.v4f(
                    "data1",
                    ((m.m30() - cameraPosition.x) * worldScale).toFloat(),
                    ((m.m31() - cameraPosition.y) * worldScale).toFloat(),
                    ((m.m32() - cameraPosition.z) * worldScale).toFloat(),
                    light.getShaderV0(m, worldScale)
                )

                if (light is DirectionalLight) shader.v1f("cutoff", light.cutoff)

                shader.m4x3("WStoLightSpace", light.invWorldMatrix)

                var shadowIdx1 = 0
                if (light is PointLight) {
                    if (supportsCubicShadows) {
                        val cascades = light.shadowTextures
                        if (cascades != null) {
                            val texture = cascades[0].depthTexture!!
                            // bind the texture, and don't you dare to use mipmapping ^^
                            // (at least without variance shadow maps)
                            texture.bind(cubicIndex0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                            shadowIdx1 = 1 // end index
                        }
                    }
                } else {
                    if (supportsPlanarShadows) {
                        var planarSlot = 0
                        val cascades = light.shadowTextures
                        if (cascades != null) for (j in cascades.indices) {
                            val slot = planarIndex0 + planarSlot
                            if (slot > maxTextureIndex) break
                            val texture = cascades[j].depthTexture!!
                            // bind the texture, and don't you dare to use mipmapping ^^
                            // (at least without variance shadow maps)
                            texture.bind(slot, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                            if (++planarSlot >= Renderers.MAX_PLANAR_LIGHTS) break
                        }
                        shadowIdx1 = planarSlot // end index
                    }
                }

                val shadowIdx0 = 0f
                shader.v4f("data2", shadowIdx0, shadowIdx1.toFloat(), light.getShaderV1(), light.getShaderV2())

                mesh.draw(shader, 0)

            }
        }

        // draw instanced meshes
        if (instanced.isNotEmpty()) {
            this.cameraMatrix = cameraMatrix
            this.cameraPosition = cameraPosition
            this.worldScale = worldScale
            GFXState.instanced.use(true) {
                instanced.forEachType(::drawBatches)
            }
        }

    }

    private var cameraMatrix: Matrix4fc? = null
    private var cameraPosition: Vector3d? = null
    private var worldScale: Double = 1.0

    fun drawBatches(lights: List<LightRequest<*>>, type: LightType, size: Int) {
        if (type == LightType.DIRECTIONAL) {
            GFXState.depthMode.use(DepthMode.ALWAYS) {
                drawBatches2(lights, type, size)
            }
        } else drawBatches2(lights, type, size)
    }

    fun drawBatches2(lights: List<LightRequest<*>>, type: LightType, size: Int) {

        val batchSize = instancedBatchSize
        val visualizeLightCount = visualizeLightCount

        val sample = lights[0].light
        val mesh = sample.getLightPrimitive()
        mesh.ensureBuffer()

        val shader = getShader(type, true)
        shader.use()

        shader.v1b("isDirectional", type == LightType.DIRECTIONAL)
        shader.v1b("receiveShadows", true)
        shader.v1f("countPerPixel", countPerPixel)

        val cameraMatrix = cameraMatrix!!
        val cameraPosition = cameraPosition!!
        val worldScale = worldScale

        initShader(shader, cameraMatrix)

        val time = Engine.gameTime

        val buffer =
            if (visualizeLightCount) lightCountInstanceBuffer
            else lightInstanceBuffer
        val nioBuffer = buffer.nioBuffer!!
        val stride = buffer.attributes[0].stride

        // draw them in batches of size <= batchSize
        // converted from for(.. step ..) to while to avoid allocation
        var baseIndex = 0
        while (baseIndex < size) {

            buffer.clear()
            nioBuffer.limit(nioBuffer.capacity())
            // fill the data
            for (index in baseIndex until min(size, baseIndex + batchSize)) {
                nioBuffer.position((index - baseIndex) * stride)
                val lightI = lights[index]
                val light = lightI.light
                val m = lightI.transform.getDrawMatrix(time)
                m4x3delta(m, cameraPosition, worldScale, nioBuffer, false)
                if (!visualizeLightCount) {
                    val mInv = light.invWorldMatrix
                    m4x3x(mInv, nioBuffer, false)
                    // put all light data: lightData0, lightData1
                    // put data0:
                    val color = light.color
                    nioBuffer.putFloat(color.x)
                    nioBuffer.putFloat(color.y)
                    nioBuffer.putFloat(color.z)
                    nioBuffer.putFloat(0f) // type, not used
                    // put data1/xyz: world position
                    nioBuffer.putFloat(((m.m30() - cameraPosition.x) * worldScale).toFloat())
                    nioBuffer.putFloat(((m.m31() - cameraPosition.y) * worldScale).toFloat())
                    nioBuffer.putFloat(((m.m32() - cameraPosition.z) * worldScale).toFloat())
                    // put data1/a: custom property
                    nioBuffer.putFloat(light.getShaderV0(m, worldScale))
                    // put data2:
                    nioBuffer.putFloat(0f)
                    nioBuffer.putFloat(0f)
                    nioBuffer.putFloat(light.getShaderV1())
                }
                nioBuffer.putFloat(light.getShaderV2())
            }
            buffer.ensureBufferWithoutResize()
            mesh.drawInstanced(shader, 0, buffer)

            baseIndex += batchSize

        }
    }

    operator fun get(index: Int): LightRequest<*> {
        val nSize = nonInstanced.size
        return if (index < nSize) {
            nonInstanced[index]
        } else {
            instanced[index - nSize]
        }
    }

    fun clear() {
        instanced.clear()
        nonInstanced.clear()
        environmentMaps.clear()
    }

    fun add(light: LightComponent, entity: Entity) {
        val group = if (light.hasShadow && light.shadowTextures != null) nonInstanced else instanced
        group.add(light, entity.transform)
    }

    fun add(environmentMap: EnvironmentMap) {
        environmentMaps.add(environmentMap)
    }

    fun listOfAll(): List<LightRequest<*>> {
        return instanced.listOfAll() + nonInstanced.listOfAll()
    }

    fun listOfAll(dst: SmallestKList<LightRequest<*>>): Int {
        instanced.listOfAll(dst)
        nonInstanced.listOfAll(dst)
        return dst.size
    }

    override val className: String = "LightPipelineStage"
    override val approxSize: Int = 5

}