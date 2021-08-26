package me.anno.ecs.components.camera

import me.anno.ecs.Component
import me.anno.ecs.annotations.Range
import me.anno.io.serialization.SerializedProperty

abstract class PostProcessingEffectComponent : Component() {

    @Range(0.0, 1.0)
    @SerializedProperty
    var effectStrength = 1f

    // todo apply the post effect...
    // todo this has to influence the pipeline...
    // todo it should have effect strength

}