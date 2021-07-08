package me.anno.engine.scene

import me.anno.ecs.Component
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.SerializedProperty

class RefComponent: Component() {

    @SerializedProperty
    var location: FileReference = InvalidRef

    override val className get() = "RefComponent"

}