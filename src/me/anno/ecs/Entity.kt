package me.anno.ecs

import me.anno.ecs.EntityPhysics.checkNeedsPhysics
import me.anno.ecs.EntityPhysics.invalidatePhysics
import me.anno.ecs.EntityPhysics.invalidatePhysicsTransform
import me.anno.ecs.EntityPhysics.rebuildPhysics
import me.anno.ecs.EntityQuery.anyComponent
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.EntityQuery.hasComponent
import me.anno.ecs.EntityQuery.hasComponentInChildren
import me.anno.ecs.EntityStats.sizeOfHierarchy
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.PositionType
import me.anno.ecs.annotations.RotationType
import me.anno.ecs.annotations.ScaleType
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.CollidingComponent
import me.anno.ecs.components.light.LightComponentBase
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.unique.StaticMeshManager
import me.anno.ecs.components.physics.Physics
import me.anno.ecs.components.ui.UIEvent
import me.anno.ecs.interfaces.InputListener
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.inspector.Inspectable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.ui.editor.stacked.Option
import me.anno.utils.pooling.JomlPools
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Vector3d

// entities would be an idea to make effects more modular
// it could apply new effects to both the camera and image sources

// done load from file whenever something changes;
//  - other way around: when a file changes, update all nodes

// done delta settings & control: only saves as values, what was changed from the prefab

@Suppress("unused", "MemberVisibilityCanBePrivate")
class Entity() : PrefabSaveable(), Inspectable, Renderable {

    constructor(parent: Entity?) : this() {
        parent?.add(this)
    }

    constructor(name: String, parent: Entity?) : this(parent) {
        this.name = name
    }

    constructor(name: String) : this() {
        this.name = name
    }

    constructor(name: String, vararg cs: Component) : this(name) {
        for (c in cs) {
            addComponent(c)
        }
    }

    constructor(vararg cs: Component) : this() {
        for (c in cs) {
            addComponent(c)
        }
    }

    @DebugProperty
    @NotSerializedProperty
    var hasValidCollisionMask = false

    @DebugProperty
    @NotSerializedProperty
    var hasSpaceFillingComponents = false

    // renderable-cache for faster rendering
    @DebugProperty
    @NotSerializedProperty
    var hasRenderables = false

    @DebugProperty
    @NotSerializedProperty
    var hasAnyRenderables = false

    @DebugProperty
    @NotSerializedProperty
    var hasOnUpdate = true

    @DebugProperty
    @NotSerializedProperty
    var hasControlReceiver = true

    @DebugProperty
    @NotSerializedProperty
    var isCreated = false
    fun create() {
        if (isCreated) return
        transform.teleportUpdate()
        invalidateAABBsCompletely()
        isCreated = true
        val children = internalChildren
        for (index in children.indices) {
            children[index].create()
        }
        val components = internalComponents
        for (index in components.indices) {
            components[index].onCreate()
        }
        val physics = getComponent(Physics::class, false)
        if (physics != null) rebuildPhysics(physics)
    }

    val transform: Transform = Transform(this)

    @NotSerializedProperty
    private val internalComponents: ArrayList<Component> = ArrayList(4)

    @SerializedProperty
    val components: List<Component>
        get() = internalComponents

    @NotSerializedProperty
    private val internalChildren: ArrayList<Entity> = ArrayList(4)

    @NotSerializedProperty
    override val children: List<Entity>
        get() = internalChildren

    override fun listChildTypes(): String = "ec" // entity children, components

    override fun addChildByType(index: Int, type: Char, child: PrefabSaveable) {
        when (child) {
            is Component -> addComponent(index, child)
            is Entity -> addEntity(index, child)
            else -> LOGGER.warn("Cannot add ${child.className} to Entity")
        }
    }

    override fun getChildListByType(type: Char): List<PrefabSaveable> = if (type == 'c') components else children
    override fun getChildListNiceName(type: Char): String = if (type == 'c') "components" else "children"
    override fun getValidTypesForChild(child: PrefabSaveable): String = when (child) {
        is Component -> "c"
        is Entity -> "e"
        else -> ""
    }

    override fun getIndexOf(child: PrefabSaveable): Int {
        return if (child is Component) {
            components.indexOf(child)
        } else children.indexOf(child)
    }

    override fun getOptionsByType(type: Char): List<Option> {
        return if (type == 'c') getOptionsByClass(this, Component::class)
        else entityOptionList
    }

    // aabb cache for faster rendering and collision checks
    @DebugProperty
    @NotSerializedProperty
    val aabb: AABBd = AABBd()

    @DebugProperty
    @NotSerializedProperty
    var hasValidAABB: Boolean = false

    @Docs("Set by the engine; if so, transforms are ignored")
    @DebugProperty
    @NotSerializedProperty
    var isPhysicsControlled: Boolean = false

    // collision mask for faster collision checks
    @DebugProperty
    @NotSerializedProperty
    var collisionMask: Int = 0

    @PositionType
    @SerializedProperty
    @Docs("Local position, shortcut for transform.localPosition")
    var position: Vector3d
        get() = transform.localPosition
        set(value) {
            transform.localPosition = value
            invalidateAABBsCompletely()
            invalidatePhysicsTransform(false)
        }

    @RotationType
    @SerializedProperty
    @Docs("Local rotation, shortcut for transform.localRotation")
    var rotation: Quaterniond
        get() = transform.localRotation
        set(value) {
            transform.localRotation = value
            invalidateAABBsCompletely()
            invalidatePhysicsTransform(false)
        }

    @ScaleType
    @SerializedProperty
    @Docs("Local scale, shortcut for transform.localScale")
    var scale: Vector3d
        get() = transform.localScale
        set(value) {
            transform.localScale = value
            invalidateAABBsCompletely()
            // scale is not just transform in bullet, it is scaling the collider
            invalidatePhysics(false)
        }

    fun setPosition(x: Double, y: Double, z: Double): Entity {
        position = position.set(x, y, z)
        return this
    }

    fun setRotation(
        radiansX: Double,
        radiansY: Double,
        radiansZ: Double
    ): Entity {
        rotation = rotation
            .identity()
            .rotateYXZ(radiansY, radiansX, radiansZ)
        return this
    }

    fun setScale(sc: Double): Entity {
        scale = scale.set(sc)
        return this
    }

    fun setScale(sx: Double, sy: Double, sz: Double): Entity {
        scale = scale.set(sx, sy, sz)
        return this
    }

    @NotSerializedProperty
    val parentEntity: Entity?
        get() = parent as? Entity

    /**
     * smoothly transitions to the next position
     * */
    fun moveToGlobal(position: Vector3d) {
        transform.globalTransform.setTranslation(position)
        transform.globalPosition = position
        transform.smoothUpdate()
        invalidateAABBsCompletely()
        invalidatePhysicsTransform(false)
    }

    /**
     * teleports to the new position without interpolation
     * */
    fun teleportToGlobal(position: Vector3d) {
        transform.globalPosition = position
        transform.teleportUpdate()
        invalidateAABBsCompletely()
        invalidatePhysicsTransform(false)
    }

    fun canCollide(collisionMask: Int): Boolean {
        return this.collisionMask.and(collisionMask) != 0
    }

    val staticMeshManager get() = getRoot(Entity::class).getComponent(StaticMeshManager::class)

    @DebugAction
    fun invalidateAABBsCompletely() {
        invalidateOwnAABB()
        invalidateChildAABBs()
    }

    @DebugAction
    fun invalidateOwnAABB() {
        if (hasValidAABB) {
            hasValidAABB = false
            parentEntity?.invalidateOwnAABB()
        }
    }

    private fun invalidateChildAABBs() {
        hasValidAABB = false
        val children = children
        for (i in children.indices) {
            val child = children[i]
            if (!child.isPhysicsControlled) {
                child.invalidateChildAABBs()
            }
        }
    }

    @DebugAction
    fun invalidateCollisionMask() {
        parentEntity?.invalidateCollisionMask()
        hasValidCollisionMask = false
    }

    fun validateMasks() {
        if (hasValidCollisionMask) return
        var collisionMask = 0
        val components = components
        for (i in components.indices) {
            val component = components[i]
            if (component is CollidingComponent) {
                collisionMask = collisionMask or component.collisionMask
                if (collisionMask == -1) break
            }
        }
        val children = children
        for (i in children.indices) {
            val child = children[i]
            child.validateMasks()
            collisionMask = collisionMask or child.collisionMask
        }
        this.collisionMask = collisionMask
        hasValidCollisionMask = true
    }

    fun getBounds(): AABBd {
        if (hasValidAABB) {
            // to check if all invalidations were applied correctly
            /*val oldAABB = AABBd(aabb)
            hasValidAABB = false
            validateAABBs()
            if (oldAABB != aabb) LOGGER.warn("AABBs differed: $aabb vs $oldAABB, $name")*/
            return aabb
        }
        hasValidAABB = true
        val children = children
        for (i in children.indices) {
            children[i].getBounds()
        }
        aabb.clear()
        if (hasSpaceFillingComponents) {
            val globalTransform = transform.globalTransform
            val components = components
            for (i in components.indices) {
                val component = components[i]
                if (component.isEnabled) {
                    component.fillSpace(globalTransform, aabb)
                }
            }
        }
        for (i in children.indices) {
            aabb.union(children[i].aabb)
        }
        return aabb
    }

    @SerializedProperty
    override var isEnabled = true
        set(value) {
            field = value
            invalidatePhysics(false)
        }

    private inline fun executeOptimizedEvent(
        hasEvent: (Entity) -> Boolean,
        call: (Entity) -> Boolean,
        call2: (Component) -> Boolean
    ): Boolean {

        if (!isCreated) create()

        var hasEventReceiver = false

        // manual for-loops, because the number of items can be changed by events intentionally
        val components = components
        var i = -1
        while (++i < components.size) {
            if (call2(components[i])) {
                hasEventReceiver = true
            }
        }

        val children = children
        var j = -1
        while (++j < children.size) {
            val child = children[j]
            if (hasEvent(child) && call(child)) {
                hasEventReceiver = true
            }
        }

        // this.hasOnUpdate = hasOnUpdate
        return hasEventReceiver
    }

    @DebugAction
    fun update(): Boolean {
        val hasUpdate = executeOptimizedEvent(
            { it.hasOnUpdate },
            Entity::update,
            Component::callUpdate
        )
        this.hasOnUpdate = hasUpdate
        return hasUpdate
    }

    fun invalidateUpdates() {
        parentEntity?.invalidateUpdates()
        hasOnUpdate = true
    }

    fun onUIEvent(event: UIEvent): Boolean {
        val hasUpdate = executeOptimizedEvent(
            { it.hasControlReceiver },
            { it.onUIEvent(event) },
            {
                if (it is InputListener) {
                    event.call(it)
                    true
                } else false
            })
        this.hasControlReceiver = hasUpdate
        return hasControlReceiver
    }

    /**
     * when the element is moved
     * */
    fun invalidateChildTransforms() {
        transform.invalidateForChildren()
    }

    /**
     * validates all children, which are invalid
     * */
    @DebugAction
    fun validateTransform() {
        when (transform.state) {
            Transform.State.VALID -> {}
            Transform.State.CHILDREN_NEED_UPDATE -> {
                val children = children
                for (i in children.indices) {
                    val child = children[i]
                    if (!child.isPhysicsControlled) {
                        child.validateTransform()
                    }
                }
            }
            else -> propagateGlobalTransform()
        }
    }

    /**
     * validates all children, which are invalid
     * */
    fun propagateGlobalTransform() {
        transform.validate()
        val children = children
        for (i in children.indices) {
            val child = children[i]
            if (!child.isPhysicsControlled) {
                val childTransform = child.transform
                when (childTransform.state) {
                    Transform.State.VALID_GLOBAL -> {
                        // recalculate local
                        child.validateTransform()
                    }
                    else -> {
                        childTransform.invalidateGlobal()
                        child.propagateGlobalTransform()
                    }
                }
            }
        }
    }

    private fun transformUpdate(keepWorldTransform: Boolean) {
        if (keepWorldTransform) {
            // if only local is present, calculate global first
            if (transform.state == Transform.State.VALID_LOCAL) transform.validate()
            transform.invalidateLocal()
        } else {
            // if only global is present, calculate local first
            if (transform.state == Transform.State.VALID_GLOBAL) transform.validate()
            transform.invalidateGlobal()
            invalidateChildTransforms()
            invalidateAABBsCompletely()
        }
    }

    override fun addChild(child: PrefabSaveable) {
        when (child) {
            is Entity -> addEntity(child)
            is Component -> addComponent(child)
            else -> throw UnsupportedOperationException("Cannot add ${child.className} to Entity")
        }
    }

    override fun addChild(index: Int, child: PrefabSaveable) {
        throw RuntimeException("Not supported/not yet implemented")
    }

    override fun deleteChild(child: PrefabSaveable) {
        when (child) {
            is Entity -> child.destroy()
            is Component -> remove(child)
        }
    }

    fun setParent(parent: Entity, index: Int, keepWorldTransform: Boolean) {

        if (this === parent) {
            LOGGER.warn("Cannot append child to itself!")
            return
        }

        val oldParent = parentEntity
        if (parent === oldParent) return

        // formalities
        if (oldParent != null) {
            oldParent.remove(this)
            oldParent.invalidateAABBsCompletely()
            oldParent.invalidateCollisionMask()
        }

        if (index !in parent.internalChildren.indices) parent.internalChildren.add(this)
        else parent.internalChildren.add(index, this)
        this.parent = parent

        // transform
        transformUpdate(keepWorldTransform)
        // collision mask
        parent.invalidateCollisionMask()
        invalidateAABBsCompletely()
        invalidateUpdates()

        checkNeedsPhysics()

        parent.setChildPath(this, index, 'e')
    }

    // todo don't directly update, rather invalidate this, because there may be more to come
    fun setParent(parent: Entity, keepWorldTransform: Boolean) {
        return setParent(parent, parent.children.size, keepWorldTransform)
    }

    override fun destroy() {
        isCreated = false
        for (component in components) {
            component.onDestroy()
        }
        val parent = parent as? Entity
        if (parent != null) {
            parent.internalChildren.remove(this)
            val tmpAABBd = JomlPools.aabbd.borrow()
            if (anyComponent { it.fillSpace(transform.globalTransform, tmpAABBd) }) {
                parent.invalidateCollisionMask()
            }
            parent.invalidateOwnAABB()
        }
    }

    fun addComponent(component: Component): Entity {
        addComponent(-1, component)
        return this
    }

    fun addComponent(index: Int, component: Component) {
        if (index < 0 || index >= internalComponents.size) internalComponents.add(component)
        else internalComponents.add(index, component)
        onChangeComponent(component)
        component.entity = this
        setChildPath(component, index, 'c')
    }

    fun onChangeComponent(component: Component) {
        if (component is MeshComponentBase || component is LightComponentBase) {
            hasRenderables = hasComponent(MeshComponentBase::class, false) ||
                    hasComponent(LightComponentBase::class, false)
        }
        val tmpAABB = JomlPools.aabbd.create().all()
        val fillsSpace = component.fillSpace(transform.globalTransform, tmpAABB)
        if (fillsSpace) invalidateOwnAABB()
        if (component is MeshComponentBase || component is LightComponentBase || fillsSpace) {
            hasSpaceFillingComponents = hasRenderables ||
                    anyComponent {
                        it !is MeshComponentBase && it !is LightComponentBase &&
                                it.fillSpace(transform.globalTransform, tmpAABB)
                    }
        }
        if (component is InputListener) {
            hasControlReceiver = hasComponent(InputListener::class)
        }
        for (i in components.indices) {
            val comp = components.getOrNull(i) ?: break
            comp.onChangeStructure(this)
        }
        JomlPools.aabbd.sub(1)
    }

    fun addEntity(child: Entity) {
        child.setParent(this, children.size, false)
    }

    fun addEntity(index: Int, child: Entity) {
        child.setParent(this, index, false)
    }

    fun remove(component: Component) {
        internalComponents.remove(component)
        onChangeComponent(component)
    }

    /**
     * removes the component in O(1) time, but may change the order
     * todo this is O(n) without lots of caching, because we need to update status flags and deliver change-updates
     * */
    fun removeUnordered(component: Component, index: Int) {
        val test = internalComponents.getOrNull(index)
        if (test !== component) { // invalid index -> no speedup
            remove(component)
        } else {
            internalComponents[index] = internalComponents.last()
            internalComponents.removeLast()
            onChangeComponent(component)
        }
    }

    override fun toString(): String {
        return toString(0).toString().trim()
    }

    fun toString(depth: Int): StringBuilder {
        val text = StringBuilder()
        for (i in 0 until depth) text.append('\t')
        text.append("Entity('$name',$sizeOfHierarchy):\n")
        val nextDepth = depth + 1
        for (child in children)
            text.append(child.toString(nextDepth))
        for (component in components)
            text.append(component.toString(nextDepth))
        return text
    }

    fun add(child: Entity) = addEntity(child)
    fun add(component: Component) = addComponent(component)

    fun remove(child: Entity) {
        if (child.parent !== this) return
        internalChildren.remove(child)
        if (child.parent == this) {
            child.parent = null
        }
        if (child.hasComponentInChildren(Collider::class)) {
            invalidatePhysics(false)
        }
    }

    fun removeAllChildren() {
        for (index in children.indices.reversed()) {
            remove(children[index])
        }
    }

    fun removeAllComponents() {
        for (index in components.indices.reversed()) {
            remove(components[index])
        }
    }

    fun fromOtherLocalToLocal(other: Entity): Matrix4x3d {
        // converts the point from the local coordinates of the other one to our local coordinates
        return other.fromLocalToOtherLocal(this)
    }

    fun fromLocalToOtherLocal(other: Entity): Matrix4x3d {
        // converts the point from our local coordinates of the local coordinates of the other one
        return Matrix4x3d(other.transform.globalTransform).invert().mul(transform.globalTransform)
    }

    override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int) = pipeline.fill(this)

    /**
     * O(|E|+|C|) clone of properties and components
     * */
    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Entity
        // copy all properties
        dst.hasRenderables = hasRenderables
        dst.hasValidCollisionMask = hasValidCollisionMask
        dst.hasSpaceFillingComponents = hasSpaceFillingComponents
        dst.hasValidAABB = hasValidAABB
        dst.aabb.set(aabb)
        dst.transform.set(transform)
        dst.collisionMask = collisionMask
        // first the structure
        val children = internalChildren
        val cloneEntities = dst.internalChildren
        if (cloneEntities.isNotEmpty()) cloneEntities.clear()
        for (i in children.indices) {
            val entity = children[i].clone() as Entity
            entity.parent = dst
            cloneEntities.add(entity)
        }
        val components = internalComponents
        val cloneComponents = dst.internalComponents
        if (cloneComponents.isNotEmpty()) cloneComponents.clear()
        for (i in components.indices) {
            val component = components[i].clone() as Component
            component.entity = dst
            cloneComponents.add(component)
        }
    }

    override fun onDestroy() {}

    override val symbol: String
        get() = ""

    override val defaultDisplayName: String
        get() = "Entity"

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeVector3d("position", transform.localPosition)
        val scale = transform.localScale
        if (scale.x != 1.0 || scale.y != 1.0 || scale.z != 1.0) {
            writer.writeVector3d("scale", scale, true)
        }
        writer.writeQuaterniond("rotation", transform.localRotation)
        writer.writeObjectList(this, "children", children)
        writer.writeObjectList(this, "components", components)
    }

    override fun readVector3d(name: String, value: Vector3d) {
        when (name) {
            "position" -> transform.localPosition = value
            "scale" -> transform.localScale = value
            else -> super.readVector3d(name, value)
        }
    }

    override fun readQuaterniond(name: String, value: Quaterniond) {
        if (name == "rotation") transform.localRotation = value
        else super.readQuaterniond(name, value)
    }

    override fun readObjectArray(name: String, values: Array<Saveable?>) {
        when (name) {
            "children" -> {
                internalChildren.clear()
                internalChildren.ensureCapacity(values.size)
                for (value in values) {
                    if (value is Entity) {
                        addChild(value)
                    }
                }
            }
            "components" -> {
                internalComponents.clear()
                internalComponents.ensureCapacity(values.size)
                for (value in values) {
                    if (value is Component) {
                        addComponent(value)
                    }
                }
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(Entity::class)
        private val entityOptionList = listOf(Option("Entity", "Create a child entity") { Entity() })
    }
}