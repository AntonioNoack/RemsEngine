package me.anno.gpu.pipeline

import me.anno.ecs.components.mesh.utils.MeshInstanceData

/**
 * instanced stack, supporting position, color, rotation and uniform scale
 * */
open class InstancedTRCStack(instanceData: MeshInstanceData, capacity: Int = 64) :
    InstancedTRSStack(instanceData, capacity) {

    constructor(capacity: Int = 64) : this(MeshInstanceData.TRC, capacity)

}