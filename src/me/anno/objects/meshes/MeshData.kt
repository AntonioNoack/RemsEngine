package me.anno.objects.meshes

import me.anno.cache.data.ICacheData
import me.anno.cache.instances.ImageCache.getImage
import me.anno.gpu.GFX
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.GFX.matrixBufferFBX
import me.anno.gpu.GFXx3D.shader3DUniforms
import me.anno.gpu.ShaderLib.shaderFBX
import me.anno.gpu.ShaderLib.shaderObjMtl
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.io.FileReference
import me.anno.mesh.fbx.model.FBXGeometry
import me.anno.mesh.obj.Material
import me.anno.objects.Transform.Companion.yAxis
import me.anno.video.MissingFrameException
import me.karl.main.Camera
import me.karl.scene.Scene
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector4fc
import org.lwjgl.opengl.GL20

open class MeshData : ICacheData {

    var lastWarning: String? = null

    var objData: Map<Material, StaticBuffer>? = null
    var fbxData: List<FBXData>? = null
    var daeScene: Scene? = null

    fun drawObj(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {
        for ((material, buffer) in objData!!) {
            val shader = shaderObjMtl
            shader.use()
            shader3DUniforms(shader, stack, 1, 1, color, null, Filtering.NEAREST, null)
            getTexture(material.diffuseTexture, whiteTexture).bind(0, whiteTexture.filtering, whiteTexture.clamping)
            buffer.draw(shader)
            GFX.check()
        }
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

        val shader = shaderFBX
        shader.use()

        for (datum in data) {
            val geo = datum.geometry
            for ((material, buffer) in datum.objData) {
                drawFBX(stack, time, color, shader, geo, material, buffer)
            }
        }

    }

    fun drawFBX(
        stack: Matrix4fArrayList,
        time: Double,
        color: Vector4fc,
        shader: Shader,
        geo: FBXGeometry,
        material: Material,
        buffer: StaticBuffer
    ) {

        // todo calculate all bone transforms, and upload them to the shader...
        // todo is weight 0 automatically set, and 1-sum???
        // todo root motion might be saved in object...

        // todo reset the matrixBuffer somehow??
        matrixBufferFBX.position(0)
        geo.bones.forEach { bone ->

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
        GFX.check()

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