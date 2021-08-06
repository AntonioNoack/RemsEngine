package me.anno.engine.ui.render

import me.anno.config.DefaultStyle.white4
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.camera.CameraComponent
import me.anno.ecs.components.light.LightType
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.RendererComponent
import me.anno.ecs.components.player.LocalPlayer
import me.anno.engine.pbr.DeferredRenderer
import me.anno.engine.pbr.PBRLibraryGLTF
import me.anno.engine.ui.ECSTypeLibrary
import me.anno.engine.ui.ECSTypeLibrary.Companion.lastSelection
import me.anno.engine.ui.render.DrawAABB.drawAABB
import me.anno.engine.ui.render.MovingGrid.drawGrid
import me.anno.engine.ui.render.Outlines.drawOutline
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.RenderState
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.ShaderLib.pbrModelShader
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.Screenshots
import me.anno.gpu.pipeline.M4x3Delta.mul4x3delta
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.shader.BaseShader.Companion.cullFaceColoringGeometry
import me.anno.gpu.shader.BaseShader.Companion.lineGeometry
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Renderer.Companion.depthRenderer
import me.anno.gpu.shader.Renderer.Companion.idRenderer
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.input.Input
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isShiftDown
import me.anno.objects.Transform
import me.anno.studio.Build
import me.anno.studio.rems.Scene
import me.anno.ui.base.Panel
import me.anno.ui.style.Style
import me.anno.utils.Clock
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.mix
import me.anno.utils.Maths.sq
import me.anno.utils.types.Quaternions.toQuaternionDegrees
import org.joml.*
import org.joml.Math.toRadians
import org.lwjgl.opengl.GL20.GL_LOWER_LEFT
import org.lwjgl.opengl.GL45.*
import kotlin.math.min


// todo optional blender like controls?


// todo clickable & draggable gizmos, e.g. for translation, rotation scaling:
// todo thick arrows for 1d, and planes for 2d, and a center cube for 3d

// todo drag stuff onto surfaces using raytracing
// todo or drag and keep it in the same distance

// todo controls
// todo show the scene
// todo drag stuff
// todo translate, rotate, scale with gizmos
// todo render in different modes: overdraw, color blindness, normals, color, before-post-process, with-post-process

// todo blend between multiple cameras, only allow 2? yes :)


// todo easily allow for multiple players in the same instance, with just player key mapping
// -> camera cannot be global, or todo it must be switched whenever the player changes

class RenderView(
    val library: ECSTypeLibrary,
    val getWorld: () -> Entity,
    val mode: Mode,
    style: Style
) : Panel(style) {

    enum class Mode {
        EDITING,
        PLAY_TESTING,
        PLAYING
    }
    // to do scene scale, which is premultiplied with everything to allow stuff outside the 1e-38 - 1e+38 range?
    // not really required, since our universe just has a scale of 1e-10 (~ size of an atom) - 1e28 (~ size of the observable universe) meters


    // todo buttons:
    // todo start game -> always in new tab, so we can make changes while playing
    // todo stop game
    // todo restart game (just re-instantiate the whole scene)


    // todo create the different pipeline stages: opaque, transparent, post-processing, ...


    // can exist (game/game mode), but does not need to (editor)
    // todo in the editor it becomes the prefab for a local player -> ui shall always be placed in the local player
    var localPlayer: LocalPlayer? = null

    var editorCamera = CameraComponent()
    val editorCameraNode = Entity(editorCamera)

    var isFinalRendering = false

    var selectedAttribute = 0

    // todo different control schemes of the camera like in MagicaVoxel
    var radius = 50.0
    val worldScale get() = if (Input.isKeyDown('f')) 1.0 else 1.0 / radius
    var position = Vector3d()
    var rotation = Vector3d(-20.0, 0.0, 0.0)

    init {
        editorCameraNode.transform.localRotation = rotation.toQuaternionDegrees()
    }

    var movement = Vector3d()

    var renderer = DeferredRenderer
    val deferred = renderer.deferredSettings!!

    val lightBuffer = deferred.createLightBuffer()
    val baseBuffer = deferred.createBaseBuffer()

    val showOverdraw get() = Input.isKeyDown('n')

    val overdrawRenderer = object : Renderer(true, ShaderPlus.DrawMode.COLOR) {
        override fun getPostProcessing(): String {
            return "void main(){\n" +
                    "   finalColor = vec3(0.125);\n" +
                    "   finalAlpha = 1.0;\n" +
                    "}\n"
        }
    }

    val baseRenderer = object : Renderer(false, ShaderPlus.DrawMode.COLOR) {
        override fun getPostProcessing(): String {
            return "" +
                    // define all light positions, radii, types, and colors
                    // use the lights to illuminate the model
                    // light data
                    "uniform vec3 ambientLight;\n" +
                    "uniform int numberOfLights;\n" +
                    "uniform mat4x3 invLightMatrices[$MAX_LIGHTS];\n" +
                    "uniform vec4 lightData0[$MAX_LIGHTS];\n" +
                    "uniform vec4 lightData1[$MAX_LIGHTS];\n" +
                    "uniform int visualizeLightCount;\n" +
                    "#define M_PI 3.141592653589793\n" +
                    PBRLibraryGLTF.specularBRDFv2NoDiv +
                    Scene.reinhardToneMapping +
                    Scene.noiseFunc +
                    "void main(){\n" +
                    // shared pbr data
                    "   vec3 V = normalize(-finalPosition);\n" +
                    // light calculations
                    // "   vec3 lightSum = ambientLight;\n" +
                    "   vec3 specularBRDF = ambientLight, diffuseLight = ambientLight;\n" +
                    "   vec3 diffuseColor = finalColor * (1.0 - finalMetallic);\n" +
                    "   vec3 specularColor = finalColor * finalMetallic;\n" +
                    "   float lightCount = 0;\n" +
                    "   bool hasSpecular = dot(specularColor,vec3(1.0)) > 0.001;\n" +
                    "   bool hasDiffuse = dot(diffuseColor,vec3(1.0)) > 0.001;\n" +
                    "   if(hasDiffuse || hasSpecular) for(int i=0;i<numberOfLights;i++){\n" +
                    "       mat4x3 WStoLightSpace = invLightMatrices[i];\n" +
                    "       vec3 dir = invLightMatrices[i] * vec4(finalPosition,1.0);\n" + // local coordinates for falloff
                    //"       if(!hasSpecular && dot(dir,dir) >= 1.0) continue;\n" +
                    "       vec4 data0 = lightData0[i];\n" +
                    "       vec4 data1 = lightData1[i];\n" +
                    "       vec3 lightColor = data0.rgb;\n" +
                    "       int lightType = int(data0.a);\n" +
                    "       vec3 lightPosition, lightDirWS, localNormal, effectiveLightColor, effectiveSpecular, effectiveDiffuse;\n" +
                    "       localNormal = normalize(WStoLightSpace * vec4(finalNormal,0.0));\n" +
                    "       float NdotL;\n" + // normal dot light
                    // local coordinates of the point in the light "cone"
                    "       switch(lightType){\n" +
                    LightType.values().joinToString("") {
                        "case ${it.id}:\n" +
                                when (it) {
                                    LightType.DIRECTIONAL -> {
                                        "" +
                                                "NdotL = localNormal.z;\n" + // dot(lightDirWS, globalNormal) = dot(lightDirLS, localNormal)
                                                // inv(W->L) * vec4(0,0,1,0) =
                                                // transpose(m3x3(W->L)) * vec3(0,0,1)
                                                "lightDirWS = normalize(vec3(WStoLightSpace[0][2],WStoLightSpace[1][2],WStoLightSpace[2][2]));\n" +
                                                "effectiveDiffuse = lightColor * NdotL;\n" +
                                                "effectiveSpecular = lightColor;\n"
                                    }
                                    LightType.POINT_LIGHT -> {
                                        "" +
                                                "float lightRadius = data1.a;\n" +
                                                "lightPosition = data1.rgb;\n" +
                                                // when light radius > 0, then adjust the light direction such that it looks as if the light was a sphere
                                                "lightDirWS = normalize(lightPosition - finalPosition);\n" +
                                                "if(lightRadius > 0.0){\n" +
                                                // todo effect is much more visible in the diffuse part
                                                // it's fine for small increased, but we wouldn't really use them...
                                                // should be more visible in the specular case...
                                                // in the ideal case, we move the light such that it best aligns the sphere...
                                                "   vec3 idealLightDirWS = normalize(reflect(finalPosition, finalNormal));\n" +
                                                "   lightDirWS = normalize(mix(lightDirWS, idealLightDirWS, clamp(lightRadius/(length(lightPosition-finalPosition)),0,1)));\n" +
                                                "}\n" +
                                                "NdotL = dot(lightDirWS, finalNormal);\n" +
                                                "effectiveDiffuse = lightColor * NdotL * ${it.falloff};\n" +
                                                "dir *= 0.2;\n" + // less falloff by a factor of 5,
                                                // because specular light is more directed and therefore reached farther
                                                "effectiveSpecular = lightColor * ${it.falloff};\n"
                                    }
                                    LightType.SPOT_LIGHT -> {
                                        "" +
                                                "lightPosition = data1.rgb;\n" +
                                                "lightDirWS = normalize(lightPosition - finalPosition);\n" +
                                                "NdotL = dot(lightDirWS, finalNormal);\n" +
                                                "float coneAngle = data1.a;\n" +
                                                "effectiveDiffuse = lightColor * NdotL * ${it.falloff};\n" +
                                                "dir *= 0.2;\n" + // less falloff by a factor of 5,
                                                // because specular light is more directed and therefore reached farther
                                                "effectiveSpecular = lightColor * ${it.falloff};\n"
                                    }
                                } +
                                "break;\n"
                    } +
                    "       }\n" +
                    "       if(dot(effectiveSpecular, vec3(NdotL)) > ${0.5 / 255.0}){\n" +
                    "           if(hasSpecular){\n" +
                    "               vec3 H = normalize(V + lightDirWS);\n" +
                    "               specularBRDF += effectiveSpecular * computeSpecularBRDF(specularColor, finalRoughness, V, finalNormal, lightDirWS, NdotL, H);" +
                    "           }\n" +
                    "           diffuseLight += effectiveDiffuse;\n" +
                    "           lightCount++;\n" +
                    "       }\n" +
                    "   }\n" +
                    "   finalColor = reinhard(visualizeLightCount > 0 ? vec3(lightCount * 0.125) :" +
                    "       diffuseColor * diffuseLight + specularColor * specularBRDF);\n" +
                    "   " +
                    "   finalColor += finalEmissive;\n" +
                    // banding prevention
                    // -0.5, so we don't destroy blacks on OLEDs
                    "   finalColor -= random(uv) * ${1.0 / 255.0};\n" +
                    "}"
        }
    }

    val uiRenderer = object : Renderer(false, ShaderPlus.DrawMode.COLOR) {
        override fun getPostProcessing(): String {
            return "" +
                    "void main(){\n" +
                    "   finalColor *= 0.6 - 0.4 * normalize(finalNormal).x;\n" +
                    "   finalColor += finalEmissive;\n" +
                    "}"
        }
    }

    val attributeRenderers: List<Renderer> = DeferredLayerType.values().run { toList().subList(0, min(size, 9)) }
        .map {
            object : Renderer(false, ShaderPlus.DrawMode.COLOR) {
                override fun getPostProcessing(): String {
                    return "" +
                            "void main(){\n" +
                            "   finalColor = ${
                                when (it.dimensions) {
                                    1 -> "vec3(${it.glslName}${it.map01})"
                                    2 -> "vec3(${it.glslName}${it.map01},1)"
                                    3 -> "(${it.glslName}${it.map01})"
                                    4 -> "(${it.glslName}${it.map01}).rgb"
                                    else -> ""
                                }
                            };\n}"
                }
            }
        }

    override fun tickUpdate() {
        super.tickUpdate()
        // we could optimize that: if not has updated in some time, don't redraw
        invalidateDrawing()
    }

    fun checkMovement() {
        val dt = GFX.deltaTime
        val factor = clamp(1.0 - 20.0 * dt, 0.0, 1.0)
        movement.mul(factor)
        val s = (1.0 - factor) * 0.035
        if (parent!!.children.any { it.isInFocus }) {// todo check if "in focus"
            if (Input.isKeyDown('a')) movement.x -= s
            if (Input.isKeyDown('d')) movement.x += s
            if (Input.isKeyDown('w')) movement.z -= s
            if (Input.isKeyDown('s')) movement.z += s
            if (Input.isKeyDown('q')) movement.y -= s
            if (Input.isKeyDown('e')) movement.y += s
        }
        val normXZ = !Input.isShiftDown // todo use UI toggle instead
        val rotQuad = rotation.toQuaternionDegrees()
        val right = rotQuad.transform(Vector3d(1.0, 0.0, 0.0))
        val forward = rotQuad.transform(Vector3d(0.0, 0.0, 1.0))
        val up = if (normXZ) {
            right.y = 0.0
            forward.y = 0.0
            right.normalize()
            forward.normalize()
            Vector3d(0.0, 1.0, 0.0)
        } else {
            rotQuad.transform(Vector3d(0.0, 1.0, 0.0))
        }
        position.x += movement.dot(right.x, up.x, forward.x) * radius
        position.y += movement.dot(right.y, up.y, forward.y) * radius
        position.z += movement.dot(right.z, up.z, forward.z) * radius
    }

    val clock = Clock()

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        clock.start()

        checkMovement()

        clock.stop("movement", 0.01)

        // todo go through the rendering pipeline, and render everything

        // todo draw all local players in their respective fields
        // todo use customizable masks for the assignment (plus a button mapping)
        // todo if a color doesn't appear on the mapping, it doesn't need to be drawn
        // todo more important players can have a larger field
        // todo slope of these partial windows can be customized for nicer looks

        // localPlayer = world.localPlayers.children.firstOrNull() as? LocalPlayer

        // todo find which sections shall be rendered for what camera
        val camera = localPlayer?.camera?.currentCamera ?: editorCamera
        if (localPlayer == null) {
            // todo calculate camera location
            val rotation = editorCameraNode.transform.localRotation
            editorCameraNode.transform.localPosition =
                Vector3d(position).add(rotation.transform(Vector3d(0.0, 0.0, radius)))
            editorCameraNode.validateTransforms()
        }

        val showIds = Input.isKeyDown('g')
        val showOverdraw = showOverdraw
        val showSpecialBuffer = showIds || showOverdraw || Input.isKeyDown('j')
        val useDeferredRendering = !showSpecialBuffer && Input.isKeyDown('k')
        val samples = if (Input.isKeyDown('p')) 8 else 1
        val buffer = if (useDeferredRendering) baseBuffer else FBStack["scene", w, h, 4, false, samples]

        stage0.blendMode = if (showOverdraw) BlendMode.ADD else null
        stage0.sorting = if (isShiftDown) Sorting.FRONT_TO_BACK
        else if (isControlDown) Sorting.BACK_TO_FRONT
        else Sorting.NO_SORTING

        val renderer = when {
            showOverdraw -> overdrawRenderer
            showIds -> Renderer.idRenderer
            showSpecialBuffer -> attributeRenderers[selectedAttribute]
            useDeferredRendering -> renderer
            else -> baseRenderer
        }

        var aspect = w.toFloat() / h

        val layers = deferred.settingsV1.layers
        val size = layers.size + 1
        val rows = when {
            size % 2 == 0 -> 2
            size % 3 == 0 -> 3
            size > 12 -> 3
            size > 6 -> 2
            else -> 1
        }
        val cols = (size + rows - 1) / rows
        if (useDeferredRendering) {
            aspect *= rows.toFloat() / cols.toFloat()
        }

        stage0.cullMode = if (isFinalRendering) GL_BACK else 0

        clock.stop("initialization", 0.05)

        prepareDrawScene(w / 2f, h / 2f, w, h, aspect, camera, camera, 1f)

        clock.stop("preparing", 0.05)

        drawScene(w, h, camera, camera, 1f, renderer, buffer, true)

        clock.stop("drawing scene", 0.05)

        if (useDeferredRendering) {

            // bind all the required buffers: position, normal
            val lightBuffer = lightBuffer
            baseBuffer.bindTextures(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

            // todo for the scene, the environment map and shadow cascades need to be updated,
            // todo and used in the calculation
            drawSceneLights(camera, camera, 1f, Renderer.copyRenderer, lightBuffer)

            clock.stop("drawing lights")

            // todo calculate the colors via post processing
            // todo this would also allow us to easier visualize all the layers


            // todo post processing could do screen space reflections :)

            for (index in 0 until size) {

                // rows x N field
                val col = index % cols
                val x02 = x0 + (x1 - x0) * (col + 0) / cols
                val x12 = x0 + (x1 - x0) * (col + 1) / cols
                val row = index / cols
                val y02 = y0 + (y1 - y0) * (row + 0) / 2
                val y12 = y0 + (y1 - y0) * (row + 1) / 2

                // draw the light buffer as the last stripe
                val texture = if (index < layers.size) {
                    buffer.textures[index]
                } else {
                    lightBuffer.textures[0]
                }

                // y flipped, because it would be incorrect otherwise
                DrawTextures.drawTexture(x02, y12, x12 - x02, y02 - y12, texture, true, -1, null)

            }

            clock.stop("presenting deferred buffers", 0.1)


        } else {

            GFX.copy(buffer)
            // DrawRectangles.drawRect(x, y, 10, 10, 0xff0000 or black)

        }

        if (showSpecialBuffer) {
            DrawTexts.drawSimpleTextCharByChar(
                x, y, 2,
                if (showIds) "IDs" else DeferredLayerType.values()[selectedAttribute].glslName
            )
        }

        clock.total("drawing the scene", 0.1)

    }

    fun resolveClick(
        px: Float,
        py: Float,
        callback: (entity: Entity?, component: Component?) -> Unit
    ) {

        // pipeline should already be filled
        val camera = editorCamera
        val buffer = FBStack["click", GFX.width, GFX.height, 4, false, 1]

        val diameter = 5

        val px2 = px.toInt() - x
        val py2 = py.toInt() - y

        val ids = Screenshots.getPixels(diameter, 0, 0, px2, py2, buffer, idRenderer) {
            drawScene(w, h, camera, camera, 0f, idRenderer, buffer, false)
            drawGizmos(camPosition, false)
        }

        val depths = Screenshots.getPixels(diameter, 0, 0, px2, py2, buffer, depthRenderer) {
            drawScene(w, h, camera, camera, 0f, depthRenderer, buffer, false)
            drawGizmos(camPosition, false)
        }

        val clickedId = Screenshots.getClosestId(diameter, ids, depths, if (reverseDepth) -10 else +10)
        val clicked = if (clickedId == 0) null else pipeline.findDrawnSubject(clickedId, getWorld())
        // println("$clickedId -> $clicked")
        // val ids2 = world.getComponentsInChildren(MeshComponent::class, false).map { it.clickId }
        // println(ids2.joinToString())
        // println(clickedId in ids2)
        callback(clicked as? Entity, clicked as? Component)

    }

    val tmp4f = Vector4f()

    val pipeline = Pipeline()
    val stage0 = PipelineStage(
        "default", Sorting.NO_SORTING, 8,
        null, DepthMode.LESS, true, GL_BACK,
        pbrModelShader
    )

    init {
        pipeline.defaultStage = stage0
        pipeline.stages.add(stage0)
    }

    var entityBaseClickId = 0

    fun prepareDrawScene(
        centerX: Float,
        centerY: Float,
        width: Int,
        height: Int,
        aspectRatio: Float,
        camera: CameraComponent,
        previousCamera: CameraComponent,
        blending: Float,
    ) {

        val blend = clamp(blending, 0f, 1f).toDouble()
        val blendFloat = blend.toFloat()

        val near = mix(previousCamera.near, camera.near, blend)
        val far = mix(previousCamera.far, camera.far, blend)
        val fov = mix(previousCamera.fov, camera.fov, blending)
        val rot0 = previousCamera.entity!!.transform.globalTransform.getUnnormalizedRotation(Quaternionf())
        val rot1 = camera.entity!!.transform.globalTransform.getUnnormalizedRotation(Quaternionf())
        val rot2 = rot0.slerp(rot1, blendFloat)
        val rot = Quaternionf(rot2).conjugate() // conjugate is quickly inverting, when already normalized

        val fovYRadians = toRadians(fov)

        // this needs to be separate from the stack
        // (for normal calculations and such)
        Perspective.setPerspective(
            viewTransform,
            fovYRadians,
            aspectRatio,
            (near * worldScale).toFloat(),
            (far * worldScale).toFloat()
        )
        viewTransform.rotate(rot)
        if (viewTransform.get(FloatArray(16)).any { it.isNaN() }) throw RuntimeException()

        // lerp the world transforms
        val camTransform = camTransform
        camTransform.set(previousCamera.entity!!.transform.globalTransform)
        camTransform.lerp(camera.entity!!.transform.globalTransform, blend)

        camTransform.transformPosition(camPosition.set(0.0))
        camInverse.set(camTransform).invert()

        pipeline.reset()
        pipeline.frustum.define(
            near, far,
            fovYRadians.toDouble(),
            width.toDouble(),
            height.toDouble(),
            aspectRatio.toDouble(),
            camPosition,
            camRotation.set(rot2),
        )

        pipeline.fill(getWorld(), camPosition, worldScale)
        entityBaseClickId = pipeline.lastClickId

    }

    val reverseDepth get() = !Input.isKeyDown('r')

    fun setClearDepth() {
        val reverseDepth = reverseDepth
        glClearDepth(if (reverseDepth) 0.0 else 1.0)
        glClipControl(GL_LOWER_LEFT, if (reverseDepth) GL_ZERO_TO_ONE else GL_NEGATIVE_ONE_TO_ONE)
        stage0.depthMode = depthMode
        pipeline.lightPseudoStage.depthMode = depthMode
        pipeline.stages.forEach { it.depthMode = depthMode }
    }

    val depthMode get() = if (reverseDepth) DepthMode.GREATER else DepthMode.LESS

    // todo we could do the blending of the scenes using stencil tests <3 (very efficient)
    //  - however it would limit us to a single renderer...
    // todo -> first just draw a single scene and later make it multiplayer
    fun drawScene(
        w: Int, h: Int,
        camera: CameraComponent,
        previousCamera: CameraComponent,
        blending: Float,
        renderer: Renderer,
        dst: Framebuffer,
        changeSize: Boolean
    ) {

        useFrame(w, h, changeSize, dst, renderer) {

            Frame.bind()

            if (renderer.isFakeColor) {
                glClearColor(0f, 0f, 0f, 0f)
            } else {
                tmp4f.set(previousCamera.clearColor).lerp(camera.clearColor, blending)
                glClearColor(tmp4f.x, tmp4f.y, tmp4f.z, 1f)
            }

            setClearDepth()
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            if (!renderer.isFakeColor && !isFinalRendering) {
                useFrame(w, h, changeSize, dst, uiRenderer) {
                    drawGizmos(camPosition, true)
                    drawSelected()
                }
            } else if (renderer == idRenderer) {
                drawGizmos(camPosition, false)
            }

            val canRenderDebug = Build.isDebug
            val renderNormals = canRenderDebug && Input.isKeyDown('n')
            val renderLines = canRenderDebug && Input.isKeyDown('l')

            if (renderNormals || renderLines) {

                val shader = if (renderLines) lineGeometry else cullFaceColoringGeometry
                RenderState.geometryShader.use(shader) {
                    pipeline.draw(viewTransform, camPosition, worldScale)
                }

            } else pipeline.draw(viewTransform, camPosition, worldScale)

            // Thread.sleep(500)

        }

    }

    fun drawSelected() {
        if (library.fineSelection.isEmpty()) return
        // draw scaled, inverted object (for outline), which is selected
        RenderState.depthMode.use(depthMode) {
            RenderState.cullMode.use(GL_FRONT) {
                for (selected in library.fineSelection) {
                    when (selected) {
                        is Entity -> drawOutline(selected, worldScale)
                        is MeshComponent -> {
                            val renderer = selected.entity?.getComponent(RendererComponent::class, false)
                            drawOutline(renderer, selected, worldScale)
                        }
                        is Component -> drawOutline(selected.entity ?: continue, worldScale)
                    }
                }
            }
        }
    }

    fun drawSceneLights(
        camera: CameraComponent,
        previousCamera: CameraComponent,
        blending: Float,
        renderer: Renderer,
        dst: Framebuffer
    ) {

        useFrame(w, h, true, dst, renderer) {

            Frame.bind()

            tmp4f.set(previousCamera.clearColor).lerp(camera.clearColor, blending)
            glClearColor(0f, 0f, 0f, 0f)
            glClearDepth(1.0)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            pipeline.lightPseudoStage.bindDraw(pipeline, viewTransform, camPosition, worldScale)

        }

    }

    val selectedColor = Vector4f(1f, 1f, 0.7f, 1f)

    fun drawGizmos(camPosition: Vector3d, drawGridLines: Boolean) {

        // draw UI

        // now works, after making the physics async :)
        // maybe it just doesn't work with the physics debugging together
        val drawAABBs = Input.isKeyDown('o')

        RenderState.blendMode.use(BlendMode.DEFAULT) {
            RenderState.depthMode.use(depthMode) {

                stack.set(viewTransform)

                Companion.worldScale = worldScale

                val maximumCircleDistance = 200f
                val maxCircleLenSq = sq(maximumCircleDistance).toDouble()

                var clickId = entityBaseClickId
                val scaleV = Vector3d()

                val world = getWorld()
                world.simpleTraversal(false) { entity ->

                    entity as Entity

                    val transform = entity.transform
                    val globalTransform = transform.globalTransform

                    val doDrawCircle = camPosition.distanceSquared(
                        globalTransform.m30(),
                        globalTransform.m31(),
                        globalTransform.m32()
                    ) < maxCircleLenSq

                    val nextClickId = clickId++
                    GFX.drawnId = nextClickId
                    entity.clickId = nextClickId

                    val stack = stack
                    stack.pushMatrix()
                    stack.mul4x3delta(globalTransform, camPosition, worldScale)

                    // only draw the circle, if its size is larger than ~ a single pixel
                    if (doDrawCircle) {
                        scale = globalTransform.getScale(scaleV).dot(0.3, 0.3, 0.3)
                        val ringColor = if (entity == lastSelection) selectedColor else white4
                        Transform.drawUICircle(stack, 0.5f / scale.toFloat(), 0.7f, ringColor)
                    }

                    val components = entity.components
                    for (i in components.indices) {
                        val component = components[i]
                        if (component !is MeshComponent) {
                            // mesh components already got their id
                            val componentClickId = clickId++
                            component.clickId = componentClickId
                            GFX.drawnId = componentClickId
                            component.onDrawGUI(this)
                        }
                    }

                    stack.popMatrix()

                    if (drawAABBs) drawAABB(entity, worldScale)

                    false
                }

                if (drawGridLines) {
                    drawGrid(radius, worldScale)
                }

                LineBuffer.finish(viewTransform)

            }
        }

    }

    companion object {

        val MAX_LIGHTS = 64

        var scale = 1.0
        var worldScale = 1.0
        val stack = Matrix4fArrayList()

        val viewTransform = Matrix4f()

        val camTransform = Matrix4x3d()
        val camInverse = Matrix4d()
        val camPosition = Vector3d()
        val camRotation = Quaterniond()

        val scaledMin = Vector4d()
        val scaledMax = Vector4d()
        val tmpVec4f = Vector4d()

    }

}