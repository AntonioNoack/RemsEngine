package me.anno.ecs.components.mesh.utils

import me.anno.ecs.components.mesh.MeshBufferUtils
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.maths.Maths.clamp
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.types.Floats.float32ToFloat16
import me.anno.utils.types.Floats.toIntOr
import org.apache.logging.log4j.LogManager
import java.nio.ByteBuffer
import kotlin.math.min

fun interface VertexAttr {

    fun fill(srcIndex: Int, dstBuffer: ByteBuffer)

    companion object {

        private val LOGGER = LogManager.getLogger(MeshBufferUtils::class)

        fun map(attr: Attribute, srcData: Any?): VertexAttr {
            val comp = attr.numComponents
            val dstType = attr.type
            when (srcData) {
                is FloatArray -> when (dstType) {
                    AttributeType.HALF ->
                        return VertexAttr { srcIndex, dstBuffer ->
                            var srcI = srcIndex * comp
                            repeat(min(comp, srcData.size - srcI)) {
                                dstBuffer.putShort(float32ToFloat16(srcData[srcI++]).toShort())
                            }
                        }
                    AttributeType.FLOAT ->
                        return VertexAttr { srcIndex, dstBuffer ->
                            var srcI = srcIndex * comp
                            repeat(min(comp, srcData.size - srcI)) {
                                dstBuffer.putFloat(srcData[srcI++])
                            }
                        }
                    AttributeType.SINT8_NORM -> return FloatToNormIntAttr(srcData, comp, -1f, 127f)
                    AttributeType.UINT8_NORM -> return FloatToNormIntAttr(srcData, comp, +0f, 255f)
                    AttributeType.SINT16_NORM -> return FloatToNormIntAttr(srcData, comp, -1f, 32767f)
                    AttributeType.UINT16_NORM -> return FloatToNormIntAttr(srcData, comp, +0f, 65535f)
                    AttributeType.SINT32_NORM -> return FloatToNormIntAttr(srcData, comp, -1f, (1L shl 31) - 1f)
                    AttributeType.UINT32_NORM -> return FloatToNormIntAttr(srcData, comp, +0f, (1L shl 32) - 1f)
                    else -> {} // not implemented
                }
                is ByteArray -> when (dstType) {
                    AttributeType.SINT8_NORM, AttributeType.UINT8_NORM,
                    AttributeType.SINT8, AttributeType.UINT8 ->
                        return VertexAttr { srcIndex, dstBuffer ->
                            var srcI = srcIndex * comp
                            repeat(min(comp, srcData.size - srcI)) {
                                dstBuffer.put(srcData[srcI++])
                            }
                        }
                    else -> {} // not implemented
                }
                is ShortArray -> when (dstType) {
                    AttributeType.HALF,
                    AttributeType.SINT16_NORM, AttributeType.UINT16_NORM,
                    AttributeType.SINT16, AttributeType.UINT16 ->
                        return VertexAttr { srcIndex, dstBuffer ->
                            var srcI = srcIndex * comp
                            repeat(min(comp, srcData.size - srcI)) {
                                dstBuffer.putShort(srcData[srcI++])
                            }
                        }
                    else -> {} // not implemented
                }
                is IntArray -> when (dstType) {
                    AttributeType.UINT8_NORM ->
                        return VertexAttr { srcIndex, dstBuffer ->
                            val value = srcData.getOrElse(srcIndex) { -1 }
                            dstBuffer.put(value.r().toByte())
                            if (comp > 1) dstBuffer.put(value.g().toByte())
                            if (comp > 2) dstBuffer.put(value.b().toByte())
                            if (comp > 3) dstBuffer.put(value.a().toByte())
                        }
                    AttributeType.SINT32_NORM, AttributeType.UINT32_NORM ->
                        return VertexAttr { srcIndex, dstBuffer ->
                            var srcI = srcIndex * comp
                            repeat(min(comp, srcData.size - srcI)) {
                                dstBuffer.putInt(srcData[srcI++])
                            }
                        }
                    else -> {} // not implemented
                }
                else -> {}
            }
            LOGGER.warn("Unknown attr-data ${srcData?.javaClass} -> $dstType")
            return VertexAttr { _, _ -> }
        }

        class FloatToNormIntAttr(
            val srcData: FloatArray, val comp: Int,
            val minValue: Float, val multiplier: Float,
        ) : VertexAttr {
            override fun fill(srcIndex: Int, dstBuffer: ByteBuffer) {
                var srcI = srcIndex * comp
                val bytes = multiplier < 1e3f
                val shorts = multiplier < 70e3f
                repeat(min(comp, srcData.size - srcI)) {
                    val value = clamp(srcData[srcI++], minValue, +1f) * multiplier
                    if (bytes) dstBuffer.put(value.toIntOr().toByte())
                    else if (shorts) dstBuffer.putShort(value.toIntOr().toShort())
                    else dstBuffer.putInt(value.toIntOr())
                }
            }
        }
    }
}