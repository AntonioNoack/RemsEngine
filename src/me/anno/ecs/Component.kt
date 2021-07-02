package me.anno.ecs

import me.anno.io.NamedSaveable
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import java.lang.StringBuilder

abstract class Component : NamedSaveable() {

    @NotSerializedProperty
    var entity: Entity? = null

    @SerializedProperty
    var isEnabled = true

    open fun onCreate() {}

    open fun onDestroy() {}

    open fun onBeginPlay() {}

    open fun onUpdate() {}

    open fun onPhysicsUpdate() {}

    override fun getApproxSize(): Int = 1000
    override fun isDefaultValue(): Boolean = false

    open fun onClick() {}

    // todo automatic property inspector by reflection
    // todo property inspector annotations, e.g. Range, ExecuteInEditMode, HideInInspector, GraphicalValueTracker...

    // todo nice ui inputs for array types and maps

    override fun toString(): String {
        return "${getClassName()}('$name')"
    }

    fun toString(depth: Int): StringBuilder {
        val builder = StringBuilder()
        for(i in 0 until depth) builder.append('\t')
        builder.append(toString())
        builder.append('\n')
        return builder
    }

}