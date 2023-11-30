package me.anno.ecs.components.mesh

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.texture.Clamping
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.types.Arrays.resize
import me.anno.video.ffmpeg.MediaMetadata.Companion.getMeta
import kotlin.math.max

// todo video plane? :D
class ImagePlane(var source: FileReference = InvalidRef) : ProceduralMesh() {

    var async = false

    var alignmentX = AxisAlignment.CENTER
    var alignmentY = AxisAlignment.CENTER

    val material = Material()

    init {
        material.diffuseMap = source
        material.clamping = Clamping.CLAMP
        materials = listOf(material.ref)
    }

    override fun generateMesh(mesh: Mesh) {
        var sx = 1f
        var sy = 1f
        val meta = getMeta(source, async)
        if (meta != null && meta.videoWidth > 0 && meta.videoHeight > 0) {
            val max = max(meta.videoWidth, meta.videoHeight).toFloat()
            sx = meta.videoWidth / max
            sy = meta.videoHeight / max
            println("meta isn't null, $sx, $sy")
        }
        val pos = mesh.positions.resize(3 * 4)
        mesh.positions = pos
        mesh.normals = normals
        mesh.indices = indices
        mesh.uvs = uvs
        for (i in 0 until 4) {
            pos[i * 3 + 2] = 0f
        }
        val x0: Float
        val x1: Float
        when (alignmentX) {
            AxisAlignment.MIN -> {
                x0 = -sx
                x1 = 0f
            }
            AxisAlignment.CENTER -> {
                sx *= 0.5f
                x0 = -sx
                x1 = +sx
            }
            AxisAlignment.MAX -> {
                x0 = 0f
                x1 = sx
            }
            AxisAlignment.FILL -> {
                x0 = -0.5f
                x1 = +0.5f
            }
        }
        val y0: Float
        val y1: Float
        when (alignmentY) {
            AxisAlignment.MIN -> {
                y0 = -sy
                y1 = 0f
            }
            AxisAlignment.CENTER -> {
                sy *= 0.5f
                y0 = -sy
                y1 = +sy
            }
            AxisAlignment.MAX -> {
                y0 = 0f
                y1 = sy
            }
            AxisAlignment.FILL -> {
                y0 = -0.5f
                y1 = +0.5f
            }
        }
        pos[0] = x0
        pos[1] = y0
        pos[3] = x0
        pos[4] = y1
        pos[6] = x1
        pos[7] = y1
        pos[9] = x1
        pos[10] = y0
        mesh.invalidateGeometry()
    }

    override fun clone(): ImagePlane {
        val clone = ImagePlane(source)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as ImagePlane
        dst.async = async
        dst.source = source
        dst.alignmentX = alignmentX
        dst.alignmentY = alignmentY
    }

    override val className: String get() = "ImagePlane"

    companion object {
        val indices = intArrayOf(0, 2, 1, 0, 3, 2)
        val normals = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f)
        val uvs = floatArrayOf(0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f)
    }

}