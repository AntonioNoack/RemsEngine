package me.anno.ecs

import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.ECSTypeLibrary
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializedProperty
import me.anno.objects.inspectable.Inspectable
import me.anno.ui.editor.stacked.Option
import me.anno.utils.strings.StringHelper
import me.anno.utils.structures.lists.UpdatingList
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4x3d

abstract class Component : PrefabSaveable(), Inspectable {

    // todo call onEnable, onDisable
    // todo event listener lists

    override var isEnabled: Boolean = true
        get() = super.isEnabled
        set(value) {
            if (field != value) {
                field = value
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

    val isSelectedIndirectly get() = entity!!.anyInHierarchy { it == ECSTypeLibrary.lastSelection }

    // can be overridden, e.g. for materials
    override fun listChildTypes(): String = ""
    override fun getChildListByType(type: Char): List<PrefabSaveable> = emptyList()
    override fun getChildListNiceName(type: Char): String = ""
    override fun getIndexOf(child: PrefabSaveable): Int = -1

    @HideInInspector
    @NotSerializedProperty
    var clickId = 0

    /**
     * returns whether it needs any space in the AABBs for visibility updates / rendering
     * if so, it fills the global transform with its bounds
     * */
    open fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean = false

    abstract override fun clone(): Component

    override fun addChildByType(index: Int, type: Char, instance: PrefabSaveable) {
        // may be implemented, e.g. for Materials in Mesh
        LOGGER.warn("$className.addChildByType(index,type,instance) is not supported")
    }

    override fun add(index: Int, child: PrefabSaveable) {
        // may be implemented, e.g. for Materials in Mesh
        LOGGER.warn("$className.add(index,child) is not supported")
    }

    override fun add(child: PrefabSaveable) {
        // may be implemented, e.g. for Materials in Mesh
        LOGGER.warn("$className.add(child) is not supported")
    }

    override fun deleteChild(child: PrefabSaveable) {
        // may be implemented, e.g. for Materials in Mesh
        LOGGER.warn("$className.add(child) is not supported")
    }

    open fun onCreate() {}

    override fun onDestroy() {}

    open fun onEnable() {}
    open fun onDisable() {}

    open fun onBeginPlay() {}

    /**
     * called every x frames
     * return 0, if you don't need this event
     * return n, if you need this event every n-th frame; there is no strict guarantee,
     *      that you will be called exactly then, because this would allow us to reduce events, when the fps are low
     * */
    open fun onUpdate(): Int = 0
    private var onUpdateItr = 1
    private var onUpdateCtr = 0

    fun callUpdate(): Boolean {
        return if (onUpdateItr > 0) {
            if (onUpdateCtr++ >= onUpdateItr) {
                onUpdateCtr = 0
                onUpdateItr = onUpdate()
            }
            true
        } else false
    }

    /**
     * whether onUpdate() needs to be called
     * */
    fun needsUpdate() = onUpdateItr > 0

    /**
     * is called every frame, when the entity was visible
     * return true, if you need this event
     * */
    open fun onVisibleUpdate(): Boolean = false

    /**
     * called on rigidbodies, when the physics engine does a simulation step; async
     * return true, if you need this update
     * */
    open fun onPhysicsUpdate(): Boolean = false

    override fun isDefaultValue(): Boolean = false

    open fun onDrawGUI() {}

    open fun onClick() {}

    open fun onChangeProperty(name: String, value: Any?) {}

    // automatic property inspector by reflection
    // property inspector annotations, e.g. Range, ExecuteInEditMode, HideInInspector,
    // todo GraphicalValueTracker

    @NotSerializedProperty
    val components
        get() = entity!!.components

    override fun toString(): String {
        return "$className('$name')"
    }

    override val className: String = javaClass.name

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }

    override fun readSomething(name: String, value: Any?) {
        if (!readSerializableProperty(name, value)) {
            super.readSomething(name, value)
        }
    }

    fun toString(depth: Int): StringBuilder {
        val builder = StringBuilder()
        for (i in 0 until depth) builder.append('\t')
        builder.append(toString())
        builder.append('\n')
        return builder
    }

    // todo create title bar, where you can change the script
    // todo save values to history

    // todo system to quickly load the scene from multiple files:
    //  - use zipping for a shipped game -> faster file load speed and only a single file access
    //  - just do it serially, it's not that much data

    companion object {

        private val LOGGER = LogManager.getLogger(Component::class)

        fun create(type: String): Component {
            return (ISaveable.createOrNull(type) ?: throw TypeNotPresentException(
                type,
                NullPointerException()
            )) as Component
        }

        fun getComponentOptions(entity: Entity?): List<Option> {
            // registry over all options... / todo search the raw files + search all scripts
            val knownComponents = ISaveable.objectTypeRegistry.filterValues { it.sampleInstance is Component }
            return UpdatingList {
                knownComponents.map {
                    Option(StringHelper.splitCamelCase(it.key), "") {
                        val comp = it.value.generator() as Component
                        comp.entity = entity
                        comp
                    }
                }.sortedBy { it.title }
            }
        }

    }

}