package me.anno.ecs.prefab

import me.anno.animation.Type
import me.anno.ecs.Component
import me.anno.ecs.Component.Companion.getPrefabComponentOptions
import me.anno.ecs.Entity
import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.objects.inspectable.Inspectable
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.stacked.Option
import me.anno.ui.editor.stacked.StackPanel
import me.anno.ui.input.TextInput
import me.anno.ui.input.VectorInput
import me.anno.ui.style.Style
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.Hierarchical
import me.anno.utils.types.Quaternions.toQuaternionDegrees
import org.apache.logging.log4j.LogManager
import org.joml.Quaterniond
import org.joml.Vector3d

open class PrefabEntity1() : NamedSaveable(), Hierarchical<PrefabEntity1>, Inspectable {

    constructor(name: String) : this() {
        this.name = name
    }

    var prefab: PrefabEntity1? = null
    var path: FileReference = InvalidRef

    // not relevant in the game -> no overrides necessary
    override var isCollapsed = false

    // todo use the overrides
    override var isEnabled = false

    override val symbol: String = ""

    override val defaultDisplayName: String = "Entity"

    // overrides are not really correct here...
    // I guess it's correct this way...
    // prefabs shouldn't be reordered though... this would probably cause issues
    // parent always null?
    // -> children are just slaves / we need to save their changes only, not themselves
    override var parent: PrefabEntity1? = null

    override val children: MutableList<PrefabEntity1> = PrefabChildrenList(this) {
        getListValue(childrenPath).filterIsInstance<PrefabEntity1>()
    }

    override fun onDestroy() {
        // idk...
    }

    // todo import values from real instance

    override fun save(writer: BaseWriter) {
        super.save(writer)
        // todo save all changes inside self and children
        // todo collect all changes
        writer.writeFile("prefab", prefab?.path)
        for ((path, change) in collectChanges()) {
            // we force the change to be written here, because the order is important
            writer.writeObject(null, "c:$path", change)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        if (name.startsWith("c:")) {
            if (value !is Change0) return
            val path = Path0.fromString(name.substring(2))
            add(path, value)
        } else super.readObject(name, value)
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "prefab" -> prefab = PrefabCache.getPrefab1(value.toGlobalFile(), false)!!
            else -> super.readString(name, value)
        }
    }

    fun collectChanges(): Sequence<Pair<Path0, Change0>> {
        return sequence {
            for ((path, change) in changes) {
                yield(path to change)
            }
            for (child in children) {
                val childName = child.name
                for ((path, change) in child.collectChanges()) {
                    yield(Path0(childName, path) to change)
                }
            }
        }
    }

    fun add(child: PrefabEntity1) {
        child.parent = this
        apply(Path0(child.name, false), Change0(ChangeType0.ADD_ELEMENT, child, children.size))
    }

    fun add(component: Component) {
        add(PrefabComponent1(component))
    }

    fun add(component: PrefabComponent1) {
        component.entity = this
        apply(Path0(component.name, true), Change0(ChangeType0.ADD_ELEMENT, component, components.size))
    }

    fun apply(path: Path0, change: Change0) {
        changes[path] = change
    }

    fun add(path: Path0, change: Change0) {
        if (path in changes) {
            changes[path]!!.append(change)
        } else {
            changes[path] = change
        }
    }

    fun createInstance(): Entity {
        val instance = prefab?.createInstance() ?: Entity()
        for ((path, change) in changes) {
            changeInstance(instance, path, change)
        }
        return instance
    }

    fun addListChange(path: Path0, entry: Any, after: Int) {
        addListChange(path, Change0(ChangeType0.ADD_ELEMENT, entry, after))
    }

    fun addListChange(path: Path0, change: Change0) {
        if (path in changes) {
            changes[path]!!.append(change)
        } else changes[path] = change
    }

    /**
     * returns true, if the element was found and removed
     * */
    fun removeListChange(path: Path0, entry: NamedSaveable): Boolean {
        // todo an element was removed, which has references, ofc all reference names need to be replaced
        val entry0 = changes[path] ?: return false
        val wasFound = removeListChange(entry0, entry)
        if (wasFound && entry0.nextChange == null) {
            // we need to remove the add-element-change
            changes.remove(path)
        } // else already done
        return wasFound
    }

    fun removeListChange(entry0: Change0, entry: NamedSaveable): Boolean {
        return if (entry0.type == ChangeType0.ADD_ELEMENT && entry0.value == entry) {
            // found it :)
            entry0.removeSelf()
            true
        } else {
            val nextChange = entry0.nextChange
            if (nextChange != null) {
                removeListChange(nextChange, entry)
            } else false
        }
    }

    fun changesContainListElement(path: Path0, entry: Any?): Boolean {
        val changes = changes[path] ?: return false
        return changes.indexOfFirst { it.type == ChangeType0.ADD_ELEMENT && it.value == entry } > -1
    }

    fun setProperty(path: Path0, value: Any?) {
        val change = changes[path]
        if (change == null) {
            changes[path] = Change0(ChangeType0.SET_VALUE, value, 0)
        } else change.value = value
    }

    fun reset(path: Path0) {
        changes.remove(path)
    }

    val changes = HashMap<Path0, Change0>()

    fun changeInstance(entity0: Entity, path: Path0, change: Change0) {
        // apply the change here
        // first find which entity/component is meant
        if (path.size == 0) return
        var entity: Entity? = entity0
        for (index in 0 until path.size - 2) {
            val name = path[index]
            entity!!
            entity = entity.children.firstOrNull { it.name == name }
            if (entity == null) {
                LOGGER.warn("Could not find $path in ${entity0.name}")
                return
            }
        }
        entity!!
        val element = if (path.size >= 2) {
            val list: List<NamedSaveable> = if (path.inComponent) entity.components else entity.children
            val name = path[path.size - 1]
            list.firstOrNull { it.name == name }
        } else entity
        if (element == null) {
            LOGGER.warn("Could not find $path in ${entity0.name}")
        } else {
            // if element is Entity, and path is position/rotation/scale, change the transform
            val propertyName = path.last
            if (element is Entity) {
                when (propertyName) {
                    "position" -> entity.transform.localPosition = change.value as? Vector3d ?: return
                    "rotation" -> entity.transform.localRotation = change.value as? Quaterniond ?: return
                    "scale" -> entity.transform.localScale = change.value as? Vector3d ?: return
                    else -> change.applyChange(element, propertyName)
                }
            } else change.applyChange(element, propertyName)
        }
    }

    var localPosition: Vector3d
        get() = changes[posPath]?.value as? Vector3d ?: prefab?.localPosition ?: Vector3d()
        set(value) {
            setProperty(posPath, value)
        }

    var localRotation: Quaterniond
        get() = changes[rotPath]?.value as? Quaterniond ?: prefab?.localRotation ?: Quaterniond()
        set(value) {
            setProperty(rotPath, value)
        }

    var localScale: Vector3d
        get() = changes[scaPath]?.value as? Vector3d ?: prefab?.localScale ?: Vector3d()
        set(value) {
            setProperty(scaPath, value)
        }

    fun getListValue(path: Path0): ArrayList<NamedSaveable> {
        val baseList = prefab?.getListValue(path) ?: arrayListOf()
        var change = changes[path]
        while (change != null) {
            if (change.type == ChangeType0.ADD_ELEMENT) {
                change.addElement(baseList)
            }// support element removal? not really...
            change = change.nextChange
        }
        return baseList
    }

    // list of read-only components
    // should not be changed...
    // todo clone the elements of the prefab, and override the entity property
    val components: List<PrefabComponent1> get() = getListValue(compPath).filterIsInstance<PrefabComponent1>()

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        list.add(TextButton("Copy", false, style).setSimpleClickListener { LOGGER.info(toString()) })
        list.add(TextInput("Name", "", style, name).setChangeListener { name = it })
        list.add(TextInput("Description", "", style, description).setChangeListener { description = it })
        list.add(
            VectorInput(style, "Position", "pos", localPosition, Type.POSITION).apply {
                setChangeListener { x, y, z, _ -> localPosition = Vector3d(x, y, z) }
                setResetListener { prefab?.localPosition ?: Vector3d() }
            }
        )
        list.add(
            VectorInput(style, "Rotation", "rot", localRotation, Type.ROT_YXZ).apply {
                setChangeListener { x, y, z, _ -> setProperty(rotPath, Vector3d(x, y, z).toQuaternionDegrees()) }
                setResetListener { prefab?.localRotation ?: Quaterniond() }
            }
        )
        list.add(
            VectorInput(style, "Scale", "scale", localScale, Type.SCALE).apply {
                setChangeListener { x, y, z, _ -> localScale.set(x, y, z) }
                setResetListener { prefab?.localScale ?: Vector3d(1.0) }
            }
        )
        list.add(
            object : StackPanel(
                "Components",
                "Customize properties and behaviours",
                getPrefabComponentOptions(),
                components,
                style
            ) {
                override fun onAddComponent(component: Inspectable, index: Int) {
                    component as NamedSaveable
                    addListChange(compPath, Change0(ChangeType0.ADD_ELEMENT, component, index))
                }

                override fun onRemoveComponent(component: Inspectable) {
                    // only can be removed, if it was added
                    component as NamedSaveable
                    if (!removeListChange(compPath, component)) {
                        // otherwise it only can be disabled
                        // was not found in local changes -> disable it
                        setProperty(Path0(listOf("components", component.name, "isEnabled"), true), false)
                    } // else done
                }

                override fun getOptionFromInspectable(inspectable: Inspectable): Option {
                    inspectable as ISaveable
                    return Option(inspectable.className, "") { inspectable }
                }

            }
        )
    }

    override val className: String = "PrefabEntity"

    companion object {
        private val LOGGER = LogManager.getLogger(PrefabEntity1::class)
        val posPath = Path0("position", false)
        val rotPath = Path0("rotation", false)
        val scaPath = Path0("scale", false)
        val compPath = Path0("components", false)
        val childrenPath = Path0("children", false)
    }


}


