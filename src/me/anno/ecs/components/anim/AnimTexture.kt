package me.anno.ecs.components.anim

import me.anno.cache.ICacheData
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import org.joml.Matrix4x3f
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

// done for low end gpus
//  a) change the jointTransforms to a texture
// done jointTransforms could be baked over all available/used animations, as we just send the weights directly to the shader <3
//  this would allow us to render animated meshes instanced as well <3, and with independent animations [just manually created animations would need extra care]
//  b) or separate the shader all together

// done integrate this into AnimMeshComponent
// done merge this into ECSMeshShader
// todo merge multiple skeletons if needed, so we don't have duplicate textures
// todo compact animation if no movement takes place
// todo improve performance for dynamic animations?

/**
 * Texture saving joint transforms, better than uniform[], because
 *  - doesn't take up uniforms;
 *  - can be used with instanced rendering;
 *  - saves bandwidth, because data is sent only once
 * */
class AnimTexture(val skeleton: Skeleton) : ICacheData {

    private class AnimTexIndex(
        val anim: Animation,
        val start: Int,
        val length: Int,
        var needsUpdate: Boolean
    ) {
        override fun toString() = "${anim.ref}/$start+=$length/$needsUpdate"
    }

    private val animationMap = HashMap<Animation, AnimTexIndex>()
    private val animationList = ArrayList<AnimTexIndex>()
    private var nextStart = 0
    private val textureWidth = max(skeleton.bones.size * 3, 1)
    private var internalTexture = Texture2D("anim", textureWidth, 64, 1)

    val texture: ITexture2D?
        get() = internalTexture.createdOrNull()

    private fun addAnimation1(animation: Animation): AnimTexIndex {
        val index = animationMap.getOrPut(animation) {
            val ni = nextStart
            val animTexIndex = AnimTexIndex(animation, ni, animation.numFrames, false)
            animationList.add(animTexIndex)
            putNewAnimation(animation)
            animTexIndex
        }
        if (index.needsUpdate) {
            updateAnimation(animation, index.start)
        }
        return index
    }

    fun addAnimation(animation: Animation) {
        addAnimation1(animation)
    }

    fun invalidate(anim: Animation) {
        val index = animationMap[anim] ?: return
        index.needsUpdate = true
    }

    fun getIndex(anim: Animation, index: Int): Int {
        val animTexIndex = addAnimation1(anim)
        return animTexIndex.start + index
    }

    fun getIndex(anim: Animation, index: Float): Float {
        val animTexIndex = addAnimation1(anim)
        return animTexIndex.start + index
    }

    private fun putNewAnimation(animation: Animation): Int {
        val numFrames = animation.numFrames + 1
        updateAnimation(animation, nextStart)
        nextStart += numFrames
        return numFrames
    }

    private fun updateAnimation(animation: Animation, start: Int): Int {
        val numFrames = animation.numFrames + 1
        ensureCapacity(start + numFrames)
        val textureType = TargetType.Float32x4
        if (internalTexture.wasCreated) {
            // extend texture
            // 4 for sizeof(float), 4 for rgba
            val buffer = Texture2D.bufferPool[internalTexture.width * internalTexture.height * 4 * 4, false, false]
            val data = buffer.asFloatBuffer()
            fillData(data, animation)
            data.position(0)
            internalTexture.overridePartially(
                buffer,
                0, 0, start,
                internalTexture.width,
                numFrames,
                textureType
            )
            Texture2D.bufferPool.returnBuffer(buffer)
        } else {
            if (start != 0) throw IllegalStateException("Internal texture hasn't been created, but start isn't zero, $start")
            // create new texture
            val buffer = Texture2D.bufferPool[internalTexture.width * internalTexture.height * 4 * 4, false, false]
            val data = buffer.asFloatBuffer()
            fillData(data)
            data.position(0)
            internalTexture.create(textureType, buffer)
            Texture2D.bufferPool.returnBuffer(buffer)
        }
        return numFrames
    }

    private fun fillData(
        dst: FloatBuffer,
        animation: Animation
    ) {
        val tmp = BoneData.tmpMatrices
        for (frameIndex in 0 until animation.numFrames) {
            fillData(dst, animation, frameIndex, tmp)
        }
        // repeat last frame, so we can interpolate between last and first frame
        fillData(dst, animation, 0, tmp)
    }

    private fun fillData(
        data: FloatBuffer,
        animation: Animation,
        frameIndex: Int,
        tmp: List<Matrix4x3f>,
    ) {
        // get frame
        val tmp1 = animation.getMappedMatricesSafely(frameIndex, tmp, skeleton.ref)
        // put into texture
        val startPosition = data.position()
        for (i in 0 until min(skeleton.bones.size, tmp1.size)) {
            tmp1[i].putInto(data)
        }
        data.position(startPosition + 4 * textureWidth)// 4x for rgba
    }

    private fun fillData(data: FloatBuffer) {
        for (index in animationList.indices) {
            val data2 = animationList[index]
            fillData(data, data2.anim)
        }
    }

    fun ensureCapacity(size: Int) {
        val oldSize = internalTexture.height
        if (oldSize < size) {
            internalTexture.destroy()
            internalTexture.reset()
            // increase by larger steps
            while (internalTexture.height < size) {
                internalTexture.height *= 2
            }
            for (data in animationList) {
                if (data.start < oldSize) {
                    updateAnimation(data.anim, data.start)
                }
            }
        }
    }

    override fun destroy() {
        internalTexture.destroy()
        animationMap.clear()
        animationList.clear()
        nextStart = 0
    }

    companion object {
        // will become default, when everything works
        // then we can remove the old code
        @JvmField
        var useAnimTextures = true
    }
}