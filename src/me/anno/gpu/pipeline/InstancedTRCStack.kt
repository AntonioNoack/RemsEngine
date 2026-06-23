package me.anno.gpu.pipeline

import me.anno.ecs.components.mesh.utils.MeshInstanceData

/**
 * instanced stack, supporting position, color, and rotation
 * */
open class InstancedTRCStack(instanceData: MeshInstanceData, capacity: Int = 64) :
    InstancedTRSStack(instanceData, capacity) {

    constructor(capacity: Int = 64) : this(MeshInstanceData.TRC, capacity)

}