package me.anno.gpu.pipeline

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.StaticBuffer

object InstancedBuffers {

    // 16k is ~ 20% better than 1024: 9 fps instead of 7 fps with 150k instanced lights on my RX 580
    const val instancedBatchSize = 1024 * 16

    // these are all lazy to avoid allocations, if we don't use them;
    // each buffer is ~1MB in size

    val instancedBuffer = StaticBuffer(
        "instanced", bind(
            Attribute("instanceTrans0", 4),
            Attribute("instanceTrans1", 4),
            Attribute("instanceTrans2", 4),
            Attribute("instanceFinalId", AttributeType.UINT8_NORM, 4)
        ), instancedBatchSize, BufferUsage.DYNAMIC
    )

    val instancedBufferA = StaticBuffer(
        "instancedA", bind(
            Attribute("instanceTrans0", 4),
            Attribute("instanceTrans1", 4),
            Attribute("instanceTrans2", 4),
            Attribute("animWeights", 4),
            Attribute("animIndices", 4),
            Attribute("instanceFinalId", AttributeType.UINT8_NORM, 4)
        ), instancedBatchSize, BufferUsage.DYNAMIC
    )

    val instancedBufferM = StaticBuffer(
        "instancedM", bind(
            Attribute("instanceTrans0", 4),
            Attribute("instanceTrans1", 4),
            Attribute("instanceTrans2", 4),
            Attribute("instancePrevTrans0", 4),
            Attribute("instancePrevTrans1", 4),
            Attribute("instancePrevTrans2", 4),
            Attribute("instanceFinalId", AttributeType.UINT8_NORM, 4)
        ), instancedBatchSize, BufferUsage.DYNAMIC
    )

    val instancedBufferMA = StaticBuffer(
        "instancedMA", bind(
            Attribute("instanceTrans0", 4),
            Attribute("instanceTrans1", 4),
            Attribute("instanceTrans2", 4),
            Attribute("instancePrevTrans0", 4),
            Attribute("instancePrevTrans1", 4),
            Attribute("instancePrevTrans2", 4),
            // todo somehow add instance-prefix
            Attribute("animWeights", 4),
            Attribute("animIndices", 4),
            Attribute("prevAnimWeights", 4),
            Attribute("prevAnimIndices", 4),
            Attribute("instanceFinalId", AttributeType.UINT8_NORM, 4)
        ), instancedBatchSize, BufferUsage.DYNAMIC
    )

    val instancedBufferSlim = StaticBuffer(
        "instancedSlim", bind(
            Attribute("instancePosSize", 4),
            Attribute("instanceRot", 4),
        ), instancedBatchSize * 2, BufferUsage.DYNAMIC
    )

    val instancedBufferI32 = StaticBuffer(
        "instancedI32", bind(Attribute("instanceI32", AttributeType.SINT32, 1)),
        instancedBatchSize * 16, BufferUsage.DYNAMIC
    )
}