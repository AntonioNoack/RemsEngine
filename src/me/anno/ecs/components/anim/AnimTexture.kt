package me.anno.ecs.components.anim

import me.anno.cache.data.ICacheData
import me.anno.ecs.components.cache.AnimationCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.engine.ECSRegistry
import me.anno.gpu.copying.FramebufferToMemory
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Texture2D
import me.anno.mesh.assimp.AnimGameItem.Companion.tmpMatrices
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import org.joml.Matrix4x3f
import java.nio.FloatBuffer
import kotlin.math.min

// done for low end gpus
//  a) change the jointTransforms to a texture
// done jointTransforms could be baked over all available/used animations, as we just send the weights directly to the shader <3
//  this would allow us to render animated meshes instanced as well <3, and with independent animations [just manually created animations would need extra care]
//  b) or separate the shader all together

// todo still not working for outlines... why?

// done RTX 3070 just like GT 1030 have the same bone uniform count problem
//  - transform this uniform array to textures instead; this would allow for instanced animations as well, which would be nice :)

// done integrate this into AnimRenderer
// done merge this into ECSMeshShader
// todo merge multiple skeletons if needed, so we don't have duplicate textures...
class AnimTexture(val skeleton: Skeleton) : ICacheData {

    class AnimTexIndex(
        val anim: Animation,
        val retargeting: Retargeting,
        val index: Int,
        val start: Int,
        val length: Int
    )

    private val animationMap = HashMap<Animation, AnimTexIndex>()
    private val animationList = ArrayList<AnimTexIndex>()
    private var nextIndex = 0
    private val textureWidth = skeleton.bones.size * 3
    private var texture = Texture2D("anim", textureWidth, 64, 1)

    fun getTexture(): Texture2D? {
        return if (texture.isCreated) texture
        else null
    }

    @Suppress("unused")
    fun addAnimation(anim: Animation, retargeting: Retargeting): AnimTexIndex {
        return animationMap.getOrPut(anim) {
            val ni = nextIndex
            val v = AnimTexIndex(anim, retargeting, animationList.size, ni, anim.numFrames)
            animationList.add(v)
            putNewAnim(anim, retargeting)
            v
        }
    }

    fun getIndex(anim: Animation, retargeting: Retargeting, index: Int): Int {
        val animTexIndex = addAnimation(anim, retargeting)
        return animTexIndex.start + index
    }

    fun getIndex(anim: Animation, retargeting: Retargeting, index: Float): Float {
        val animTexIndex = addAnimation(anim, retargeting)
        return animTexIndex.start + index
    }

    private fun putNewAnim(animation: Animation, retargeting: Retargeting): Int {
        val numFrames = animation.numFrames + 1
        ensureCapacity(nextIndex + numFrames)
        if (texture.isCreated) {
            // extend texture
            // 4 for sizeof(float), 4 for rgba
            val buffer = Texture2D.bufferPool[texture.w * texture.h * 4 * 4, false, false]
            val data = buffer.asFloatBuffer()
            fillData(data, animation, retargeting)
            data.position(0)
            texture.overridePartially(buffer, 0, nextIndex, texture.w, numFrames, TargetType.FloatTarget4)
            println("overrode texture partially")
            Texture2D.bufferPool.returnBuffer(buffer)
        } else {
            // create new texture
            val buffer = Texture2D.bufferPool[texture.w * texture.h * 4 * 4, false, false]
            val data = buffer.asFloatBuffer()
            fillData(data)
            data.position(0)
            texture.create(TargetType.FloatTarget4, buffer)
            println("created new texture")
            Texture2D.bufferPool.returnBuffer(buffer)
        }
        nextIndex += numFrames
        return numFrames
    }

    private fun fillData(data: FloatBuffer, anim: Animation, retargeting: Retargeting) {
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
        retargeting: Retargeting,
        tmp: Array<Matrix4x3f>,
        frameIndex: Int
    ) {
        val tmpBoneCount = min(skeleton.bones.size, tmp.size)
        // get frame
        anim.getMappedMatricesSafely(frameIndex, tmp, retargeting)
        anim.getMatrices(frameIndex, tmp)
        // put into texture
        val startPosition = data.position()
        for (i in 0 until tmpBoneCount) {
            val pos = data.position()
            tmp[i].get(data)
            data.position(pos + 12)
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
        if (texture.h < size) {
            texture.destroy()
            texture.reset()
        }
        // increase by larger steps
        while (texture.h < size) {
            texture.h *= 2
        }
    }

    override fun destroy() {
        texture.destroy()
    }

    companion object {

        var useAnimTextures = true

        @JvmStatic
        fun main(args: Array<String>) {
            // create a test texture, so we can see whether the texture is correctly created
            ECSRegistry.initWithGFX()
            val source = downloads.getChild("3d/Rumba Dancing.fbx") // animated mesh file
            val skeletonSource = source.getChild("Skeleton.json")
            val animationsSources = source.getChild("animations").listChildren()!!
            val skeleton = SkeletonCache[skeletonSource]!!
            val animations = animationsSources.map { AnimationCache[it]!! }
            val texture = AnimTexture(skeleton)
            val retargeting = Retargeting()
            for (anim in animations.sortedBy { it.name }) {
                texture.addAnimation(anim, retargeting)
            }
            FramebufferToMemory.createImage(texture.texture, flipY = false, withAlpha = false)
                .write(desktop.getChild("animTexture.png"))
        }
    }

}