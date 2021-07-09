package me.anno.objects.meshes

import me.anno.animation.skeletal.SkeletalAnimation
import me.anno.cache.data.ICacheData
import me.anno.cache.instances.ImageCache.getImage
import me.anno.ecs.Entity
import me.anno.gpu.GFX
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.GFX.matrixBufferFBX
import me.anno.gpu.ShaderLib.shaderAssimp
import me.anno.gpu.ShaderLib.shaderFBX
import me.anno.gpu.ShaderLib.shaderObjMtl
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.drawing.GFXx3D.shader3DUniforms
import me.anno.gpu.drawing.GFXx3D.transformUniform
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.io.files.FileReference
import me.anno.mesh.assimp.AnimGameItem
import me.anno.mesh.assimp.AssimpModel
import me.anno.mesh.fbx.model.FBXGeometry
import me.anno.mesh.fbx.model.FBXModel
import me.anno.mesh.gltf.CustomGlContext
import me.anno.mesh.obj.Material
import me.anno.objects.GFXTransform
import me.anno.objects.GFXTransform.Companion.uploadAttractors
import me.anno.video.MissingFrameException
import me.karl.main.Camera
import me.karl.scene.Scene
import org.joml.*
import kotlin.math.sin

open class MeshData : ICacheData {

    var lastWarning: String? = null

    var objData: Map<Material, StaticBuffer>? = null
    var fbxData: List<FBXData>? = null
    var gltfData: GlTFData? = null
    var daeScene: Scene? = null
    var assimpModel: AnimGameItem? = null

    fun drawAssimp(
        transform: GFXTransform?,
        stack: Matrix4fArrayList,
        time: Double,
        color: Vector4fc,
        animationIndex: Int,
        useMaterials: Boolean
    ) {

        val shader = shaderAssimp.value
        shader.use()
        shader3DUniforms(shader, stack, color)
        uploadAttractors(transform, shader, time)
        // draw it
        // todo bind the correct shader, best gltf
        // todo test different renderers: PBR, normal-black-white, ...
        // upload the required uniforms
        // todo bind emission textures
        // todo emission colors
        // todo other material properties
        // todo play animation
        val model0 = assimpModel!!
        val animation = model0.animations.toSortedMap().values.toList().getOrNull(animationIndex)
        shader.v1("hasAnimation", if (animation == null) 0f else 1f)
        if (animation != null) {
            model0.uploadJointMatrices(shader, animation, time)
        }

        /*val diffuse = Vector4f()
        for (model in model0.hierarchy) {
            stack.next {
                // todo this isn't yet correct
                // todo is it just local?
                stack.mul(model.transform)
                transformUniform(shader, stack)
                for (mesh in model.meshes) {
                    val material = mesh.material
                    val texturePath = material?.texture
                    val textureOrNull = if (texturePath == null) null else getImage(texturePath, 1000, true)
                    val texture = textureOrNull ?: whiteTexture
                    texture.bind(0, Filtering.LINEAR, Clamping.REPEAT)
                    diffuse.set(color)
                    if (material != null) diffuse.mul(material.diffuse)
                    GFX.shaderColor(shader, "tint", diffuse)
                    mesh.buffer.draw(shader)
                }
            }
        }*/

        transformUniform(shader, stack)

        val localStack = Matrix4x3fArrayList()
        drawHierarchy(shader, localStack, color, model0, model0.hierarchy, useMaterials)

        // draw skeleton for debugging purposes
        // makes sense for the working skeletons, but is broken for the incorrect ones...
        // so at least that seems to be correct...
        /*for(bone in model0.bones){
            stack.next {
                stack.mul(Matrix4f(bone.offsetMatrix).invert())
                GFXx3D.draw3DCircle(null, 0.0, stack, 0.7f, 0f, 360f, color)
            }
        }*/


    }

    fun vec3(v: Vector3d): Vector3f = Vector3f(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    fun quat(q: Quaterniond): Quaternionf = Quaternionf(q.x.toFloat(), q.y.toFloat(), q.z.toFloat(), q.w.toFloat())

    fun drawHierarchy(
        shader: Shader,
        stack: Matrix4x3fArrayList,
        color: Vector4fc,
        model0: AnimGameItem,
        entity: Entity,
        useMaterials: Boolean
    ) {
        stack.pushMatrix()


        val transform = entity.transform
        val local = transform.localTransform

        // this moves the engine parts correctly, but ruins the rotation of the ghost
        // and scales it totally incorrectly
        stack.mul(
            Matrix4x3f(
                local.m00().toFloat(), local.m01().toFloat(), local.m02().toFloat(),
                local.m10().toFloat(), local.m11().toFloat(), local.m12().toFloat(),
                local.m20().toFloat(), local.m21().toFloat(), local.m22().toFloat(),
                local.m30().toFloat(), local.m31().toFloat(), local.m32().toFloat(),
            )
        )

        val assimpModel = entity.getComponent<AssimpModel>()
        if (assimpModel != null && assimpModel.meshes.isNotEmpty()) {

            shader.m4x3("localTransform", stack)

            if (useMaterials) {
                val diffuse = Vector4f()
                for (mesh in assimpModel.meshes) {
                    val material = mesh.material
                    val texturePath = material?.diffuseMap
                    val textureOrNull = if (texturePath == null) null else getImage(texturePath, 1000, true)
                    val texture = textureOrNull ?: whiteTexture
                    texture.bind(0, Filtering.LINEAR, Clamping.REPEAT)
                    diffuse.set(color)
                    if (material != null) diffuse.mul(material.diffuseBase)
                    GFX.shaderColor(shader, "tint", diffuse)
                    mesh.draw(shader, 0)
                }
            } else {
                whiteTexture.bind(0)
                for (mesh in assimpModel.meshes) {
                    GFX.shaderColor(shader, "tint", -1)
                    mesh.draw(shader, 0)
                }
            }
        }

        for (child in entity.children) {
            drawHierarchy(shader, stack, color, model0, child, useMaterials)
        }

        stack.popMatrix()
    }

    fun drawObj(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {
        val objData = objData!!
        if (objData.isEmpty()) return
        val shader = shaderObjMtl.value
        shader.use()
        shader3DUniforms(shader, stack, 1, 1, color, null, Filtering.NEAREST, null)
        for ((material, buffer) in objData) {
            getTexture(material.diffuseTexture, whiteTexture).bind(0)
            buffer.draw(shader)
            GFX.check()
        }
    }

    /**
     * todo looping modes for the animation...
     * see also {@link de.javagl.jgltf.model.animation.AnimationManager#performStep(long)}
     * */
    fun drawGlTF(stack: Matrix4fArrayList, time: Double, color: Vector4fc, animationIndex: Int) {

        // todo make textures and material properties customizable (maybe...)

        // todo for the correct rendering with lighting, we need to split perspective and world matrix
        // the perspective matrix should be the first one, so we could use it's inverse to extract the world matrix :)
        val data = gltfData!!
        val viewer = data.viewer
        val camera = data.camera
        // todo non-color mode?
        // update color
        CustomGlContext.tint.set(color)
        // todo somehow only the first animation is playing correctly...
        camera.update(stack)

        // viewer.animationManager.setTime(time.toFloat())
        val animations = data.animations
        if (animations != null) {
            val animation = animations.getOrNull(animationIndex)
            if (animation != null) {
                val timeF = time.toFloat()
                animation.update((timeF - animation.startTimeS) % animation.durationS)
            }
        }

        viewer.glRender()

    }

    fun drawDae(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {
        GFX.check()
        val scene = daeScene!!
        val renderer = Mesh.daeRenderer!!
        val camera = scene.camera as Camera
        camera.updateTransformMatrix(stack)
        scene.animatedModel.update(time)
        renderer.render(scene.animatedModel, scene.camera, scene.lightDirection)
        GFX.check()
    }

    // doesn't work :/
    fun drawFBX(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {

        val data = fbxData ?: return

        val shader = shaderFBX.value
        shader.use()

        for (datum in data) {
            val geo = datum.geometry
            val model = geo.parents.filterIsInstance<FBXModel>().firstOrNull()
            stack.next {

                if (model != null) {

                    stack.rot(model.preRotation)

                    stack.translate(model.localTranslation)
                    stack.scale(model.localScale)
                    stack.rot(model.localRotation)

                    stack.rot(model.postRotation)

                }

                for ((material, buffer) in datum.objData) {
                    drawFBX(stack, time, color, shader, geo, datum.animation, material, buffer)
                }

            }
        }
    }

    fun Matrix4f.rot(v: Vector3f) {
        rotateX(v.x)
        rotateY(v.y)
        rotateZ(v.z)
    }

    fun drawFBX(
        stack: Matrix4fArrayList,
        time: Double,
        color: Vector4fc,
        shader: Shader,
        geo: FBXGeometry,
        animation: SkeletalAnimation,
        material: Material,
        buffer: StaticBuffer
    ) {

        // todo calculate all bone transforms, and upload them to the shader...
        // todo is weight 0 automatically set, and 1-sum???
        // todo root motion might be saved in object...

        // todo reset the matrixBuffer somehow??
        matrixBufferFBX.position(0)

        // todo get animation

        val rot = Vector3f(0f, sin(time.toFloat()), 0f)
        animation.bones.forEach { bone ->
            bone.rotation.set(rot)
        }

        shader.v1("boneLimit", animation.skeleton.boneCount - 1)
        animation.updateAllAndUpload(shader["transforms"], Matrix4x3f())

        /*geo.bones.forEach { bone ->

            // todo apply local translation and rotation...
            // global -> local -> rotated local -> global
            // stack.get(GFX.matrixBufferFBX)

            val jointMatrix = bone.transform!!
            val invJointMatrix = bone.transformLink!!

            val bp = bone.parent
            val parentMatrix = bp?.localJointMatrix
            val angle = 1f * (GFX.gameTime / 3 * 1e-9f).rem(1f)

            val dx = jointMatrix[3, 0] - (bp?.transform?.get(3, 0) ?: 0f)
            val dy = jointMatrix[3, 1] - (bp?.transform?.get(3, 1) ?: 0f)
            val dz = jointMatrix[3, 2] - (bp?.transform?.get(3, 2) ?: 0f)

            // effectively bone-space parent-2-child-transform
            val translateMat =
                Matrix4f().translate(dx, dy, dz).rotate(angle, yAxis) // Vector3f(dx,dy,dz).normalize()

            var jointMat = translateMat// .mul(rotationMat)

            if (parentMatrix != null) {
                jointMat.mul(parentMatrix)
                //jointMat = Matrix4f(parentMatrix).mul(jointMat)
            }


            bone.localJointMatrix = jointMat

            val mat = Matrix4f(jointMat)
            mat.mul(invJointMatrix) // invJointMatrix

            // (mat)

            mat.identity()

            for (i in 0 until 16) {
                matrixBufferFBX.put(mat.get(i / 4, i and 3))
            }

        }

        matrixBufferFBX.position(0)
        GFX.check()
        GL20.glUniformMatrix4fv(shader["transforms"], false, matrixBufferFBX)
        GFX.check()*/

        shader3DUniforms(shader, stack, 1, 1, color, null, Filtering.NEAREST, null)
        getTexture(material.diffuseTexture, whiteTexture).bind(0, whiteTexture.filtering, whiteTexture.clamping)
        buffer.draw(shader)
        GFX.check()

    }


    override fun destroy() {
        objData?.entries?.forEach {
            it.value.destroy()
        }
        // fbxGeometry?.destroy()
        daeScene?.animatedModel?.destroy()
        fbxData?.forEach { it.objData.values.forEach { buffer -> buffer.destroy() } }
    }

    companion object {
        fun getTexture(file: FileReference?, defaultTexture: Texture2D): Texture2D {
            if (file == null) return defaultTexture
            val tex = getImage(file, 1000, true)
            if (tex == null && isFinalRendering) throw MissingFrameException(file)
            return tex ?: defaultTexture
        }
    }

}