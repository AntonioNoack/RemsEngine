package me.anno.ecs.components.anim

import me.anno.cache.ICacheData
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Texture2D
import me.anno.mesh.assimp.AnimGameItem.Companion.tmpMatrices
import org.joml.Matrix4x3f
import java.nio.FloatBuffer
import kotlin.math.min

// done for low end gpus
//  a) change the jointTransforms to a texture
// done jointTransforms could be baked over all available/used animations, as we just send the weights directly to the shader <3
//  this would allow us to render animated meshes instanced as well <3, and with independent animations [just manually created animations would need extra care]
//  b) or separate the shader all together

// done integrate this into AnimRenderer
// done merge this into ECSMeshShader
// todo merge multiple skeletons if needed, so we don't have duplicate textures
// todo compact animation if no movement takes place
// todo allow for bone-by-bone animation with dynamic anim textures

/**
 * a texture saving joint transforms;
 * 1) better than joints, because doesn't take up uniforms;
 * 2) better than joints, because can be used with instanced rendering;
 * 3) better than joints, because saves bandwidth, because data is sent only once
 * */
class AnimTexture(val skeleton: Skeleton) : ICacheData {

    class AnimTexIndex(
        val anim: Animation,
        val retargeting: Retargeting?,
        val start: Int,
        val length: Int,
        var needsUpdate: Boolean
    )

    private val animationMap = HashMap<Animation, AnimTexIndex>()
    private val animationList = ArrayList<AnimTexIndex>()
    private var nextIndex = 0
    private val textureWidth = skeleton.bones.size * 3
    private var internalTexture = Texture2D("anim", textureWidth, 64, 1)

    val texture: Texture2D?
        get() {
            return if (internalTexture.isCreated) internalTexture
            else null
        }

    fun addAnimation(anim: Animation, retargeting: Retargeting?): AnimTexIndex {
        val index = animationMap.getOrPut(anim) {
            val ni = nextIndex
            val v = AnimTexIndex(anim, retargeting, ni, anim.numFrames, false)
            animationList.add(v)
            putNewAnim(anim, retargeting)
            v
        }
        if (index.needsUpdate) {
            updateAnim(anim, retargeting, index.start)
        }
        return index
    }

    fun invalidate(anim: Animation) {
        val index = animationMap[anim] ?: return
        index.needsUpdate = true
    }

    fun getIndex(anim: Animation, retargeting: Retargeting?, index: Int): Int {
        val animTexIndex = addAnimation(anim, retargeting)
        return animTexIndex.start + index
    }

    fun getIndex(anim: Animation, retargeting: Retargeting?, index: Float): Float {
        val animTexIndex = addAnimation(anim, retargeting)
        return animTexIndex.start + index
    }

    private fun putNewAnim(animation: Animation, retargeting: Retargeting?): Int {
        val numFrames = animation.numFrames + 1
        updateAnim(animation, retargeting, nextIndex)
        nextIndex += numFrames
        return numFrames
    }

    private fun updateAnim(animation: Animation, retargeting: Retargeting?, start: Int): Int {
        val numFrames = animation.numFrames + 1
        ensureCapacity(start + numFrames)
        val textureType = TargetType.FloatTarget4
        if (internalTexture.isCreated) {
            // extend texture
            // 4 for sizeof(float), 4 for rgba
            val buffer = Texture2D.bufferPool[internalTexture.w * internalTexture.h * 4 * 4, false, false]
            val data = buffer.asFloatBuffer()
            fillData(data, animation, retargeting)
            data.position(0)
            internalTexture.overridePartially(
                buffer,
                0, 0, start,
                internalTexture.w,
                numFrames,
                textureType
            )
            Texture2D.bufferPool.returnBuffer(buffer)
        } else {
            if (start != 0) throw IllegalStateException()
            // create new texture
            val buffer = Texture2D.bufferPool[internalTexture.w * internalTexture.h * 4 * 4, false, false]
            val data = buffer.asFloatBuffer()
            fillData(data)
            data.position(0)
            internalTexture.create(textureType, buffer)
            Texture2D.bufferPool.returnBuffer(buffer)
        }
        return numFrames
    }

    private fun fillData(
        data: FloatBuffer,
        anim: Animation,
        retargeting: Retargeting?
    ) {
        val tmp = tmpMatrices
        for (frameIndex in 0 until anim.numFrames) {
            fillData(data, anim, retargeting, tmp, frameIndex)
        }
        // repeat last frame, so we can interpolate between last and first frame
        fillData(data, anim, retargeting, tmp, 0)
    }

    private fun fillData(
        data: FloatBuffer,
        anim: Animation,
        retargeting: Retargeting?,
        tmp: Array<Matrix4x3f>,
        frameIndex: Int
    ) {
        // get frame
        val tmp1 = anim.getMappedMatricesSafely(frameIndex, tmp, skeleton, retargeting)
        val boneCount = min(skeleton.bones.size, tmp1.size)
        // put into texture
        val startPosition = data.position()
        for (i in 0 until boneCount) {
            tmp1[i].putInto(data)
        }
        data.position(startPosition + 4 * textureWidth)// 4x for rgba
    }

    private fun fillData(data: FloatBuffer) {
        for (index in animationList.indices) {
            val data2 = animationList[index]
            fillData(data, data2.anim, data2.retargeting)
        }
    }

    fun ensureCapacity(size: Int) {
        if (internalTexture.h < size) {
            internalTexture.destroy()
            internalTexture.reset()
        }
        // increase by larger steps
        while (internalTexture.h < size) {
            internalTexture.h *= 2
        }
    }

    override fun destroy() {
        internalTexture.destroy()
        animationMap.clear()
        animationList.clear()
        nextIndex = 0
    }

    companion object {

        // will become default, when everything works
        // then we can remove the old code
        var useAnimTextures = true

    }

}