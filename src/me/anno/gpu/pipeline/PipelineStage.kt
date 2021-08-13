package me.anno.gpu.pipeline

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.light.*
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshRenderer
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.shaderColor
import me.anno.gpu.RenderState
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.pipeline.M4x3Delta.buffer16x256
import me.anno.gpu.pipeline.M4x3Delta.m4x3delta
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.Shader
import me.anno.input.Input.isKeyDown
import me.anno.io.Saveable
import me.anno.utils.Maths.clamp
import org.joml.Matrix3f
import org.joml.Matrix4fc
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL21

// todo somehow the textures are not found...

class PipelineStage(
    var name: String,
    var sorting: Sorting,
    var maxNumberOfLights: Int,
    var blendMode: BlendMode?,
    var depthMode: DepthMode,
    var writeDepth: Boolean,
    var cullMode: Int, // 0 = both, gl_front, gl_back
    var defaultShader: BaseShader
) : Saveable() {

    companion object {
        val defaultMaterial = Material()
        val lastMaterial = HashMap<Shader, Material>(64)
        private val tmp3x3 = Matrix3f()
    }

    // todo what is not sorted could be collected here, and then be drawn using instances
    // this ofc only works well, if we have limited lights
    // or we would need light groups
    val meshInstances = HashMap<Mesh, ArrayList<DrawRequest>>()

    var nextInsertIndex = 0
    val drawRequests = ArrayList<DrawRequest>()

    val size get() = nextInsertIndex

    fun bindDraw(pipeline: Pipeline, cameraMatrix: Matrix4fc, cameraPosition: Vector3d, worldScale: Double) {
        RenderState.blendMode.use(blendMode) {
            RenderState.depthMode.use(depthMode) {
                RenderState.depthMask.use(writeDepth) {
                    RenderState.cullMode.use(cullMode) {
                        draw(pipeline, cameraMatrix, cameraPosition, worldScale)
                    }
                }
            }
        }
    }

    fun getDrawMatrix(transform: Transform, time: Long): Matrix4x3d {
        val drawTransform = transform.drawTransform
        val factor = transform.updateDrawingLerpFactor(time)
        if (factor > 0.0) {
            val extrapolatedTime = (GFX.gameTime - transform.lastUpdateTime).toDouble() / transform.lastUpdateDt
            // needs to be changed, if the extrapolated time changes -> it changes if the phyisics engine is behind
            // its target -> in the physics engine, we send the game time instead of the physics time,
            // and this way, it's relatively guaranteed to be roughly within [0,1]
            val fac2 = factor / (clamp(1.0 - extrapolatedTime, 0.001, 1.0))
            if (fac2 < 1.0) {
                drawTransform.lerp(transform.globalTransform, fac2)
                transform.checkDrawTransform()
            } else {
                drawTransform.set(transform.globalTransform)
                transform.checkDrawTransform()
            }
        }
        return drawTransform
    }

    fun setupLocalTransform(
        shader: Shader,
        transform: Transform,
        cameraPosition: Vector3d,
        worldScale: Double,
        time: Long
    ) {

        val drawTransform = getDrawMatrix(transform, time)
        shader.m4x3delta("localTransform", drawTransform, cameraPosition, worldScale)

        val invLocalUniform = shader["invLocalTransform"]
        if (invLocalUniform >= 0) {
            val invLocal = tmp3x3.set(
                drawTransform.m00().toFloat(), drawTransform.m01().toFloat(), drawTransform.m02().toFloat(),
                drawTransform.m10().toFloat(), drawTransform.m11().toFloat(), drawTransform.m12().toFloat(),
                drawTransform.m20().toFloat(), drawTransform.m21().toFloat(), drawTransform.m22().toFloat(),
            ).invert()
            shader.m3x3(invLocalUniform, invLocal)
        }

    }

    // todo only if this requires lights ofc...
    fun setupLights(
        pipeline: Pipeline,
        shader: Shader,
        cameraPosition: Vector3d,
        worldScale: Double,
        request: DrawRequest
    ) {
        val numberOfLightsPtr = shader["numberOfLights"]
        // println("#0: $numberOfLightsPtr")
        if (numberOfLightsPtr < 0) return
        val maxNumberOfLights = RenderView.MAX_LIGHTS
        val lights = pipeline.lights
        val numberOfLights = pipeline.getClosestRelevantNLights(request.entity.aabb, maxNumberOfLights, lights)
        shader.v1(numberOfLightsPtr, numberOfLights)
        if (numberOfLights > 0) {
            val invLightMatrices = shader["invLightMatrices"]
            val buffer = buffer16x256
            if (invLightMatrices >= 0) {
                // fill all transforms
                buffer.limit(12 * numberOfLights)
                for (i in 0 until numberOfLights) {
                    buffer.position(12 * i)
                    val light = lights[i]!!.component as LightComponent
                    light.invWorldMatrix.get(buffer)
                }
                buffer.position(0)
                GL21.glUniformMatrix4x3fv(invLightMatrices, false, buffer)
            }
            // and sharpness; implementation depending on type
            val lightIntensities = shader["lightData0"]
            if (lightIntensities >= 0) {
                // fill all light colors
                buffer.limit(4 * numberOfLights)
                for (i in 0 until numberOfLights) {
                    val light = lights[i]!!.component as LightComponent
                    val color = light.color
                    buffer.put(color.x)
                    buffer.put(color.y)
                    buffer.put(color.z)
                    val type = when (light) {
                        is DirectionalLight -> LightType.DIRECTIONAL.id
                        is PointLight -> LightType.POINT_LIGHT.id
                        is SpotLight -> LightType.SPOT_LIGHT.id
                        else -> -1
                    }
                    buffer.put(type + 0.25f)
                }
                buffer.position(0)
                GL20.glUniform4fv(lightIntensities, buffer)
            }
            // type, and cone angle (or other data, if required)
            // additional, whether we have a texture, and maybe other data
            val lightTypes = shader["lightData1"]
            if (lightTypes >= 0) {
                // fill the type, and cone angle, sharpness, and such
                buffer.limit(4 * numberOfLights)
                for (i in 0 until numberOfLights) {

                    buffer.position(4 * i)

                    val light = lights[i]!!.component as LightComponent
                    val m = light.entity!!.transform.drawTransform

                    buffer.put(((m.m30() - cameraPosition.x) * worldScale).toFloat())
                    buffer.put(((m.m31() - cameraPosition.y) * worldScale).toFloat())
                    buffer.put(((m.m32() - cameraPosition.z) * worldScale).toFloat())

                    when (light) {
                        is PointLight -> {
                            // put light size * world scale
                            // avg, and then /3
                            // but the center really is much smaller -> *0.01
                            val lightSize = m.getScale(Vector3d()).dot(1.0, 1.0, 1.0) * light.lightSize / 9.0
                            buffer.put((lightSize * worldScale).toFloat())
                        }
                        is SpotLight -> {
                            // todo third buffer, because here the light size would be useful as well...
                            buffer.put(light.coneAngle.toFloat())
                        }
                        // other light types could write their data here
                    }

                }
                buffer.position(0)
                GL20.glUniform4fv(lightTypes, buffer)
            }
            // todo bind all shadow textures
        }
    }

    fun draw(pipeline: Pipeline, cameraMatrix: Matrix4fc, cameraPosition: Vector3d, worldScale: Double) {

        // the dotViewDir may be easier to calculate, and technically more correct, but it has one major flaw:
        // it changes when the cameraDirection is changing. This ofc is not ok, since it would resort the entire list,
        // and that's expensive

        // todo sorting function, that also uses the materials, so we need to switch seldom?
        // todo and light groups, so we don't need to update lights that often

        // val viewDir = pipeline.frustum.cameraRotation.transform(Vector3d(0.0, 0.0, 1.0))
        when (sorting) {
            Sorting.NO_SORTING -> {
            }
            Sorting.FRONT_TO_BACK -> {
                drawRequests.sortBy {
                    // - it.entity.transform.dotViewDir(cameraPosition, viewDir)
                    it.entity.transform.distanceSquaredGlobally(cameraPosition)
                }
            }
            Sorting.BACK_TO_FRONT -> {
                drawRequests.sortByDescending {
                    // - it.entity.transform.dotViewDir(cameraPosition, viewDir)
                    it.entity.transform.distanceSquaredGlobally(cameraPosition)
                }
            }
        }

        var lastEntity: Entity? = null
        var lastMesh: Mesh? = null
        var lastShader: Shader? = null

        val time = GFX.gameTime

        // we could theoretically cluster them to need fewer uploads
        // but that would probably be quite hard to implement reliably
        val hasLights = maxNumberOfLights > 0
        val needsLightUpdateForEveryMesh = hasLights && pipeline.lightPseudoStage.size > maxNumberOfLights

        pipeline.lights.fill(null)

        for (index in 0 until nextInsertIndex) {

            // gl_Position = cameraMatrix * v4(localTransform * v4(pos,1),1)

            val request = drawRequests[index]
            val mesh = request.mesh
            val entity = request.entity

            GFX.drawnId = request.clickId

            val transform = entity.transform

            val materialIndex = request.materialIndex
            val material = mesh.materials.getOrNull(materialIndex) ?: defaultMaterial
            val baseShader = material.shader ?: defaultShader
            val renderer = request.component

            // mesh.draw(shader, material)
            val shader = baseShader.value
            shader.use()

            val previousMaterial = lastMaterial.put(shader, material)

            if (previousMaterial == null) {
                // information for the shader, which is material agnostic
                // add all things, the shader needs to know, e.g. light direction, strength, ...
                // (for the cheap shaders, which are not deferred)
                shader.m4x4("transform", cameraMatrix)
                shader.v3("ambientLight", pipeline.ambient)
                shader.v1("visualizeLightCount", if (isKeyDown('t')) 1 else 0)
                // shader.m4x4("cameraMatrix", cameraMatrix)
                // shader.m4x3("worldTransform", ) // todo world transform could be used for procedural shaders...
                // todo local transform could be used for procedural shaders as well
            }

            if (hasLights) {
                // todo or if shader has changed
                if (previousMaterial == null || needsLightUpdateForEveryMesh) {
                    // upload all light data
                    setupLights(pipeline, shader, cameraPosition, worldScale, request)
                }
            }

            setupLocalTransform(shader, transform, cameraPosition, worldScale, time)

            if (previousMaterial != material) {
                // bind textures for the material
                // bind all default properties, e.g. colors, roughness, metallic, clear coat/sheen, ...
                material.defineShader(shader)
            }

            shaderColor(shader, "tint", -1)

            // only if the entity or mesh changed
            // not if the material has changed
            // this updates the skeleton and such
            if (entity !== lastEntity || lastMesh !== mesh || lastShader !== shader) {
                if (renderer is MeshRenderer)
                    renderer.defineVertexTransform(shader, entity, mesh)
                else shader.v1("hasAnimation", 0f)
                lastEntity = entity
                lastMesh = mesh
                lastShader = shader
            }

            mesh.draw(shader, materialIndex)

        }

        lastMaterial.clear()

    }

    fun reset() {
        // there is too much space
        if (nextInsertIndex < drawRequests.size / 2) {
            drawRequests.clear()
        }
        nextInsertIndex = 0
    }

    fun add(component: Component, mesh: Mesh, entity: Entity, materialIndex: Int, clickId: Int) {
        if (nextInsertIndex >= drawRequests.size) {
            val request = DrawRequest(mesh, component, entity, materialIndex, clickId)
            drawRequests.add(request)
        } else {
            val request = drawRequests[nextInsertIndex]
            request.mesh = mesh
            request.component = component
            request.entity = entity
            request.materialIndex = materialIndex
            request.clickId = clickId
        }
        nextInsertIndex++
    }

    override val className: String = "PipelineStage"
    override val approxSize: Int = 5
    override fun isDefaultValue(): Boolean = false

}