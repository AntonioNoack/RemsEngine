package me.anno.gpu.pipeline

import me.anno.ecs.Transform

class InstancedDrawRequest(
    var transform: Transform,
    var clickId: Int
) {

}