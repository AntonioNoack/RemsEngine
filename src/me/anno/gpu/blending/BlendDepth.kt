package me.anno.gpu.blending

import me.anno.cache.Cache
import me.anno.gpu.GFX
import me.anno.gpu.GFX.isFinalRendering
import me.anno.video.MissingFrameException
import org.lwjgl.opengl.GL11.*
import java.util.*

data class BlendDepth(val blendMode: BlendMode?, val depth: Boolean, val depthMask: Boolean = true) {

    private var underThis: BlendDepth? = null
    private var actualBlendMode = blendMode

    constructor(blendMode: BlendMode?, depth: Boolean, action: () -> Unit) : this(blendMode, depth) {
        use(action)
    }

    constructor(blendMode: BlendMode?, depth: Boolean, depthMask: Boolean = true, action: () -> Unit) :
            this(blendMode, depth, depthMask) {
        use(action)
    }

    private fun use(render: () -> Unit) {
        GFX.check()
        bind()
        try {
            render()
            GFX.check()
        } catch (e: MissingFrameException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            if (isFinalRendering) throw MissingFrameException(e.message ?: e.javaClass.toString())
        } catch (e: Error) {
            // e.g. OutOfMemoryError: OpenGL Exception
            e.printStackTrace()
            Cache.clear() // dangerous, but maybe it's our only hope to survive
            if (isFinalRendering) throw MissingFrameException(e.message ?: e.javaClass.toString())
        }
        unbind()
    }

    private fun bind() {
        underThis = if (stack.isEmpty()) null else stack.peek()
        actualBlendMode = if (blendMode == BlendMode.UNSPECIFIED) underThis?.actualBlendMode else blendMode
        stack.push(this)
        apply(actualBlendMode, underThis)
    }

    private fun apply(previousApplied: BlendDepth?) {
        apply(actualBlendMode, previousApplied)
    }

    private fun apply(blendMode: BlendMode?, last: BlendDepth?) {
        if (blendMode != last?.blendMode) {
            if (blendMode != null) {
                if (!blendIsEnabled) {
                    glEnable(GL_BLEND)
                    blendIsEnabled = true
                }
                blendMode.apply()
            } else {
                if (blendIsEnabled) {
                    glDisable(GL_BLEND)
                    blendIsEnabled = false
                }
            }
        }

        if (depth) {
            //if(!depthIsEnabled){
            glEnable(GL_DEPTH_TEST)
            depthIsEnabled = true
            //}
        } else {
            //if(depthIsEnabled){
            glDisable(GL_DEPTH_TEST)
            depthIsEnabled = false
            //}
        }

        if (depthMask) {
            if (!depthMaskIsEnabled) {
                glDepthMask(true)
                depthMaskIsEnabled = true
            }
        } else {
            if (depthMaskIsEnabled) {
                glDepthMask(false)
                depthMaskIsEnabled = false
            }
        }

    }

    private fun unbind() {
        if (stack.pop() != this) throw RuntimeException("BlendDepth not matching")
        val toBind = stack.peek()
        toBind.apply(this)
    }

    companion object {

        var blendIsEnabled = false
        var depthIsEnabled = false
        var depthMaskIsEnabled = false

        val stack = Stack<BlendDepth>()

        fun reset() {
            stack.clear()
            glDisable(GL_BLEND)
            blendIsEnabled = false
            glDisable(GL_DEPTH_TEST)
            depthIsEnabled = false
            glDepthMask(true)
            depthMaskIsEnabled = true
            stack += BlendDepth(null, false)
        }

    }

}