package me.anno.gpu

import me.anno.ecs.components.mesh.utils.MeshInstanceData
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.gpu.GFXState.COLOR_MASK_A
import me.anno.gpu.GFXState.COLOR_MASK_B
import me.anno.gpu.GFXState.COLOR_MASK_G
import me.anno.gpu.GFXState.COLOR_MASK_R
import me.anno.gpu.GFXState.animated
import me.anno.gpu.GFXState.bakedInstLayout
import me.anno.gpu.GFXState.bakedMeshLayout
import me.anno.gpu.GFXState.blendMode
import me.anno.gpu.GFXState.colorMask
import me.anno.gpu.GFXState.cullMode
import me.anno.gpu.GFXState.depthMask
import me.anno.gpu.GFXState.depthMode
import me.anno.gpu.GFXState.ditherMode
import me.anno.gpu.GFXState.drawLines
import me.anno.gpu.GFXState.drawingSky
import me.anno.gpu.GFXState.framebuffer
import me.anno.gpu.GFXState.instanceData
import me.anno.gpu.GFXState.scissorTest
import me.anno.gpu.GFXState.stencilTest
import me.anno.gpu.GFXState.vertexData
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.NullFramebuffer
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Texture2D
import me.anno.utils.assertions.assertFail
import me.anno.utils.types.Booleans.hasFlag
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL11C.GL_BACK
import org.lwjgl.opengl.GL11C.GL_BLEND
import org.lwjgl.opengl.GL11C.GL_CULL_FACE
import org.lwjgl.opengl.GL11C.GL_FILL
import org.lwjgl.opengl.GL11C.GL_FRONT
import org.lwjgl.opengl.GL11C.GL_FRONT_AND_BACK
import org.lwjgl.opengl.GL11C.GL_LINE
import org.lwjgl.opengl.GL11C.glColorMask
import org.lwjgl.opengl.GL11C.glCullFace
import org.lwjgl.opengl.GL11C.glDepthFunc
import org.lwjgl.opengl.GL11C.glDepthMask
import org.lwjgl.opengl.GL11C.glDisable
import org.lwjgl.opengl.GL11C.glEnable
import org.lwjgl.opengl.GL11C.glPolygonMode
import org.lwjgl.opengl.GL20C.GL_LOWER_LEFT
import org.lwjgl.opengl.GL45C.GL_NEGATIVE_ONE_TO_ONE
import org.lwjgl.opengl.GL45C.GL_ZERO_TO_ONE
import org.lwjgl.opengl.GL45C.glClipControl

object GFXContext {

    private val LOGGER = LogManager.getLogger(GFXContext::class)

    private class State {
        var lastProgram: Int = -1
        val textures: LongArray = LongArray(GFX.maxBoundTextures)
    }

    private val statePool = ArrayList<State>()

    fun invalidateState() {
        lastBlendMode = Unit
        lastDepthMode = null
        lastDepthMask = null
        lastCullMode = null
        lastDrawLines = null
        lastColorMask = -1
    }

    private var lastBlendMode: Any? = Unit
    private var lastDepthMode: DepthMode? = null
    private var lastDepthMask: Boolean? = null
    private var lastCullMode: CullMode? = null
    private var lastDrawLines: Boolean? = null
    private var lastColorMask: Int = -1

    fun useState(run: () -> Unit) {
        if (!GFX.isGFXThread()) run()
        else {
            val state = defaultPush()
            try {
                run()
            } finally {
                defaultPop(state)
                statePool.add(state)
            }
        }
    }

    private fun defaultPush(): State {
        blendMode.internalPush(null)
        depthMask.internalPush(true)
        depthMode.internalPush(DepthMode.ALWAYS)
        colorMask.internalPush(-1)
        drawLines.internalPush(false)
        cullMode.internalPush(CullMode.BOTH)
        vertexData.internalPush(MeshVertexData.DEFAULT)
        instanceData.internalPush(MeshInstanceData.DEFAULT)
        animated.internalPush(false)
        ditherMode.internalPush(DitherMode.DRAW_EVERYTHING)
        drawingSky.internalPush(false)
        scissorTest.internalPush(null)
        stencilTest.internalPush(false)
        bakedMeshLayout.internalPush(null)
        bakedInstLayout.internalPush(null)
        framebuffer.internalPush(NullFramebuffer)

        // todo store bound buffers and attributes
        val state = statePool.removeLastOrNull() ?: State()
        state.lastProgram = GPUShader.lastProgram
        collectTextureState(state.textures)

        // todo bind x,y,w,h,changeSize?
        return state
    }

    private fun collectTextureState(dst: LongArray) {
        for (slot in dst.indices) {
            Texture2D.getBindState(slot)
        }
    }

    private fun defaultPop(state: State) {
        blendMode.internalPop()
        depthMask.internalPop()
        depthMode.internalPop()
        colorMask.internalPop()
        drawLines.internalPop()
        cullMode.internalPop()
        vertexData.internalPop()
        instanceData.internalPop()
        animated.internalPop()
        ditherMode.internalPop()
        drawingSky.internalPop()
        scissorTest.internalPop()
        stencilTest.internalPop()
        bakedMeshLayout.internalPop()
        bakedInstLayout.internalPop()
        framebuffer.internalPop()
        GPUShader.restoreBinding(state.lastProgram)

        // todo restore bound buffers and attributes
        for (i in state.textures.indices) {
            Texture2D.restoreBindState(i, state.textures[i])
        }
    }

    private fun bindBlendMode(newValue: Any?) {
        if (newValue == lastBlendMode) return
        when (newValue) {
            null -> glDisable(GL_BLEND)
            BlendMode.INHERIT -> {
                val stack = blendMode
                var index = stack.index
                var self: Any?
                do {
                    self = stack.values[index--]
                } while (self == BlendMode.INHERIT)
                return bindBlendMode(self)
            }
            is BlendMode -> {
                if (lastBlendMode == Unit || lastBlendMode == null) {
                    glEnable(GL_BLEND)
                }
                newValue.forceApply()
            }
            is List<*> -> {
                if (lastBlendMode == Unit || lastBlendMode == null) {
                    glEnable(GL_BLEND)
                }
                for (i in newValue.indices) {
                    val v = newValue[i] as BlendMode
                    v.forceApply(i)
                }
            }
            else -> assertFail("Unknown blend mode type")
        }
        lastBlendMode = newValue
    }

    private fun bindDepthMode() {
        val newValue = depthMode.currentValue
        if (lastDepthMode == newValue) return
        glDepthFunc(newValue.id)
        val reversedDepth = newValue.reversedDepth
        if (lastDepthMode?.reversedDepth != reversedDepth) {
            if (GFX.supportsClipControl) {
                glClipControl(GL_LOWER_LEFT, if (reversedDepth) GL_ZERO_TO_ONE else GL_NEGATIVE_ONE_TO_ONE)
            } else {
                LOGGER.warn("Reversed depth is not supported (because it's pointless without glClipControl")
            }
        }
        lastDepthMode = newValue
    }

    fun bindDepthMask() {
        val newValue = depthMask.currentValue
        if (lastDepthMask == newValue) return
        glDepthMask(newValue)
        lastDepthMask = newValue
    }

    fun bindColorMask() {
        val newValue = colorMask.currentValue and 15
        if (lastColorMask == newValue) return
        glColorMask(
            newValue.hasFlag(COLOR_MASK_R),
            newValue.hasFlag(COLOR_MASK_G),
            newValue.hasFlag(COLOR_MASK_B),
            newValue.hasFlag(COLOR_MASK_A)
        )
        lastColorMask = newValue
    }

    private fun bindDrawLines() {
        val newValue = drawLines.currentValue
        if (lastDrawLines == newValue) return
        glPolygonMode(GL_FRONT_AND_BACK, if (newValue) GL_LINE else GL_FILL)
        lastDrawLines = newValue
    }

    private fun bindCullMode() {
        var newValue = cullMode.currentValue
        if (drawLines.currentValue) newValue = CullMode.BOTH
        if (lastCullMode == newValue) return
        when (newValue) {
            CullMode.BOTH -> {
                glDisable(GL_CULL_FACE) // both visible -> disabled
            }
            CullMode.FRONT -> {
                glEnable(GL_CULL_FACE)
                glCullFace(GL_BACK) // front visible -> back hidden
            }
            CullMode.BACK -> {
                glEnable(GL_CULL_FACE)
                glCullFace(GL_FRONT) // back visible -> front hidden
            }
        }
        lastCullMode = newValue
    }

    fun bind() {
        bindBlendMode(blendMode.currentValue)
        bindDepthMode()
        bindDepthMask()
        bindColorMask()
        bindCullMode()
        bindDrawLines()
    }
}