package me.anno.objects

import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.objects.cache.Cache
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.FileInput
import me.anno.ui.style.Style
import me.anno.utils.plus
import me.anno.utils.times
import me.anno.video.MissingFrameException
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL11
import java.io.File

class Cubemap(var file: File = File(""), parent: Transform? = null): GFXTransform(parent){

    var nearestFiltering = true
    var otherFormat = false

    // todo create a cubemap on the gpu instead to support best-ram-usage mipmapping and linear filtering?
    override fun onDraw(stack: Matrix4fArrayList, time: Float, color: Vector4f) {

        val texture = Cache.getImage(file, 1000, true) ?:
            if(GFX.isFinalRendering) throw MissingFrameException(file)
            else GFX.whiteTexture

        val sphericalProjection = file.name.endsWith(".hdr", true) != otherFormat

        if(sphericalProjection){
            GFX.drawSpherical(stack, buffer, texture, color, isBillboard[time], nearestFiltering, GL11.GL_QUADS)
        } else {
            GFX.drawXYZUV(stack, buffer, texture, color, isBillboard[time], nearestFiltering, GL11.GL_QUADS)
        }

    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += FileInput("Texture", style, file.toString())
            .setChangeListener { file = File(it) }
            .setIsSelectedListener { show(null) }
    }

    companion object {

        val buffer = StaticFloatBuffer(listOf(Attribute("attr0", 3), Attribute("attr1", 2)), 4 * 6)
        init {

            fun put(v0: Vector3f, dx: Vector3f, dy: Vector3f, x: Float, y: Float, u: Int, v: Int){
                val pos = v0 + dx*x + dy*y
                buffer.put(pos.x, pos.y, pos.z, u/4f, v/3f)
            }

            fun addFace(u: Int, v: Int, v0: Vector3f, dx: Vector3f, dy: Vector3f){
                put(v0, dx, dy, -1f, -1f, u+1, v)
                put(v0, dx, dy, -1f, +1f, u+1, v+1)
                put(v0, dx, dy, +1f, +1f, u, v+1)
                put(v0, dx, dy, +1f, -1f, u, v)
            }

            val mxAxis = Vector3f(-1f,0f,0f)
            val myAxis = Vector3f(0f,-1f,0f)
            val mzAxis = Vector3f(0f,0f,-1f)

            addFace(1, 1, mzAxis, mxAxis, yAxis) // center, front
            addFace(0, 1, mxAxis, zAxis, yAxis) // left, left
            addFace(2, 1, xAxis, mzAxis, yAxis) // right, right
            addFace(3, 1, zAxis, xAxis, yAxis) // 2x right, back
            addFace(1, 0, myAxis, mxAxis, mzAxis) // top
            addFace(1, 2, yAxis, mxAxis, zAxis) // bottom

        }

    }

    override fun getClassName() = "Cubemap"

}