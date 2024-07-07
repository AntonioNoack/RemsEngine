package me.anno.ecs

import me.anno.ecs.EntityPhysics.invalidateRigidbody
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.EditorState
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.base.BaseWriter
import org.joml.AABBd
import org.joml.Matrix4x3d

abstract class Component : PrefabSaveable() {

    override var isEnabled: Boolean
        get() = super.isEnabled
        set(value) {
            if (super.isEnabled != value) {
                super.isEnabled = value
                entity?.onChangeComponent(this)
                if (value) onEnable()
                else onDisable()
            }
        }

    @NotSerializedProperty
    open var entity: Entity?
        get() = parent as? Entity
        set(value) {
            parent = value
        }

    @NotSerializedProperty
    val transform
        get() = entity?.transform

    val isSelectedIndirectly
        get() = this in EditorState.selection ||
                entity?.anyInHierarchy { it == EditorState.lastSelection } == true

    // @DebugProperty
    @NotSerializedProperty
    var lastDrawn = 0L

    @HideInInspector
    @NotSerializedProperty
    var gfxId = 0
        private set

    @NotSerializedProperty
    var clickId: Int
        get() = gfxId and 0xffffff
        set(value) {
            gfxId = (gfxId and (255 shl 24)) or (value and 0xffffff)
        }

    @Docs("Group, e.g. for colored outlines")
    @Range(0.0, 255.0)
    @SerializedProperty
    var groupId: Int
        get() = gfxId ushr 24
        set(value) {
            gfxId = (value shl 24) or (gfxId and 0xffffff)
        }

    /**
     * returns whether it needs any space in the AABBs for visibility updates / rendering
     * if so, it fills the global transform with its bounds
     * */
    open fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean = false

    open fun invalidateAABB() {
        entity?.invalidateOwnAABB()
    }

    open fun onCreate() {}

    override fun destroy() {}

    open fun onEnable() {}
    open fun onDisable() {}

    open fun onBeginPlay() {}

    open fun onChangeStructure(entity: Entity) {}

    override fun isDefaultValue(): Boolean = false

    open fun onChangeProperty(name: String, value: Any?) {}

    fun invalidateRigidbody() {
        entity?.invalidateRigidbody()
    }

    @NotSerializedProperty
    open val components
        get() = entity?.components ?: listOf(this)

    override fun toString(): String {
        return "$className('$name')"
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }

    override fun setProperty(name: String, value: Any?) {
        if (name == "isCollapsed") isCollapsed = value == true
        else if (!setSerializableProperty(name, value)) {
            super.setProperty(name, value)
        }
    }

    fun toString(depth: Int): StringBuilder {
        val builder = StringBuilder()
        for (i in 0 until depth) builder.append('\t')
        builder.append(toString())
        builder.append('\n')
        return builder
    }
}