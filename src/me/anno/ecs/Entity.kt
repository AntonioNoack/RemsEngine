package me.anno.ecs

import me.anno.ecs.annotations.*
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.CollidingComponent
import me.anno.ecs.components.light.LightComponentBase
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.physics.Physics
import me.anno.ecs.components.ui.UIEvent
import me.anno.ecs.interfaces.ControlReceiver
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.studio.Inspectable
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.stacked.Option
import me.anno.ui.style.Style
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.f2s
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.reflect.KClass

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

    // assigned and tested for click checks
    @HideInInspector
    @NotSerializedProperty
    var clickId: Int = 0

    @NotSerializedProperty
    private val internalComponents: ArrayList<Component> = ArrayList(4)

    @SerializedProperty
    val components: List<Component>
        get() = internalComponents

    // @SerializedProperty
    // override var parent: Entity? = null

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

    // is set by the engine
    @DebugProperty
    @NotSerializedProperty
    var isPhysicsControlled: Boolean = false

    // collision mask for faster collision checks
    @DebugProperty
    @NotSerializedProperty
    var collisionMask: Int = 0

    @PositionType
    @SerializedProperty
    var position: Vector3d
        get() = transform.localPosition
        set(value) {
            transform.localPosition = value
            invalidateAABBsCompletely()
            invalidatePhysics(false)
        }

    @RotationType
    @SerializedProperty
    var rotation: Quaterniond
        get() = transform.localRotation
        set(value) {
            transform.localRotation = value
            invalidateAABBsCompletely()
            invalidatePhysics(false)
        }

    @ScaleType
    @SerializedProperty
    var scale: Vector3d
        get() = transform.localScale
        set(value) {
            transform.localScale = value
            invalidateAABBsCompletely()
            invalidatePhysics(false)
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
        invalidatePhysics(false)
    }

    /**
     * teleports to the new position without interpolation
     * */
    fun teleportToGlobal(position: Vector3d) {
        transform.globalPosition = position
        transform.teleportUpdate()
        invalidateAABBsCompletely()
        invalidatePhysics(false)
    }

    fun canCollide(collisionMask: Int): Boolean {
        return this.collisionMask.and(collisionMask) != 0
    }

    fun invalidatePhysics(force: Boolean) {
        if (force || hasPhysicsInfluence()) {
            // LOGGER.debug("inv physics: ${physics != null}, ${rigidbody != null}")
            physics?.invalidate(this)
        }
    }

    fun invalidateRigidbody() {
        physics?.invalidate(this)
    }

    val physics get() = getRoot(Entity::class).getComponent(Physics::class, false)

    fun rebuildPhysics(physics: Physics<*, *>) {
        if (hasComponent(physics.rigidComponentClass)) {
            physics.invalidate(this)
        } else {
            val children = children
            for (index in children.indices) {
                children[index].rebuildPhysics(physics)
            }
        }
    }

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

    fun validateAABBs() {
        if (hasValidAABB) {
            // to check if all invalidations were applied correctly
            /*val oldAABB = AABBd(aabb)
            hasValidAABB = false
            validateAABBs()
            if (oldAABB != aabb) LOGGER.warn("AABBs differed: $aabb vs $oldAABB, $name")*/
            return
        }
        hasValidAABB = true
        val children = children
        for (i in children.indices) {
            children[i].validateAABBs()
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
    }

    @SerializedProperty
    override var isEnabled = true
        set(value) {
            field = value
            invalidatePhysics(false)
        }

    fun hasPhysicsInfluence(): Boolean {
        return isPhysicsControlled || parentEntity?.hasPhysicsInfluence() == true
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

    /*// when something is selected, this needs to be updated,
    // because many things only draw, if they are selected
    // -> generally this was a good idea, but sadly we also need the pipeline optimization...
    @NotSerializedProperty
    var hasDrawGUI = true
    fun drawGUI(): Boolean {
        val hasDrawGUI = executeOptimizedEvent({ it.drawGUI() }) { it.onDrawGUI() }
        this.hasDrawGUI = hasDrawGUI
        return hasDrawGUI
    }*/

    fun invalidateUpdates() {
        parentEntity?.invalidateUpdates()
        hasOnUpdate = true
    }

    fun onUIEvent(event: UIEvent): Boolean {
        val hasUpdate = executeOptimizedEvent(
            { it.hasControlReceiver },
            { it.onUIEvent(event) },
            {
                if (it is ControlReceiver) {
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

    fun physicsUpdate() {
        // called by physics thread
        // only called for rigidbodies
        // not called for static objects (?), since they should not move
        val components = components
        for (i in components.indices) {
            val c = components[i]
            if (c.isEnabled) c.onPhysicsUpdate()
        }
    }

    /*
    * val drawable = children.firstOrNull { it is DrawableComponent } ?: return
        val fragmentEffects = children.filterIsInstance<FragmentShaderComponent>()
        (drawable as DrawableComponent).draw(stack, time, color, fragmentEffects)
    * */

    override val className: String get() = "Entity"

    override fun isDefaultValue(): Boolean = false

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
            is Entity -> deleteEntity(child)
            is Component -> deleteComponent(child)
        }
    }

    fun deleteEntity(child: Entity) {
        child.destroy()
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

        if (index < 0) parent.internalChildren.add(this)
        else parent.internalChildren.add(index, this)
        this.parent = parent

        // transform
        transformUpdate(keepWorldTransform)
        // collision mask
        parent.invalidateCollisionMask()
        invalidateAABBsCompletely()

        checkNeedsPhysics()

        parent.setChildPath(this, index, 'e')
    }

    // todo don't directly update, rather invalidate this, because there may be more to come
    fun setParent(parent: Entity, keepWorldTransform: Boolean) {
        return setParent(parent, parent.children.size, keepWorldTransform)
    }

    private fun checkNeedsPhysics() {
        // physics
        if (allInHierarchy { it.isEnabled }) {
            // something can change
            physics?.invalidate(this)
        }
    }

    fun Entity.invalidateRigidbody() {
        physics?.invalidate(this)
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

    fun addComponent(component: Component) {
        addComponent(-1, component)
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
        if (component is ControlReceiver) {
            hasControlReceiver = hasComponent(ControlReceiver::class)
        }
        for (idx in components.indices) {
            components[idx].onChangeStructure(this)
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
        deleteComponent(component)
    }

    fun deleteComponent(component: Component) {
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

    fun <V : Any> hasComponent(clazz: KClass<V>, includingDisabled: Boolean = false): Boolean {
        return getComponent(clazz, includingDisabled) != null
    }

    fun <V : Component> hasComponentInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): Boolean {
        if (hasComponent(clazz, includingDisabled)) return true
        val children = children
        for (index in children.indices) {
            val child = children[index]
            if (includingDisabled || child.isEnabled) {
                if (child.hasComponent(clazz, includingDisabled)) {
                    return true
                }
            }
        }
        return false
    }

    fun <V : Any> getComponent(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        // elegant:
        // return components.firstOrNull { clazz.isInstance(it) && (includingDisabled || it.isEnabled) } as V?
        // without damn iterator:
        val components = components
        for (i in components.indices) {
            val component = components[i]
            if ((includingDisabled || component.isEnabled) && clazz.isInstance(component)) {
                @Suppress("unchecked_cast")
                return component as V
            }
        }
        return null
    }

    fun <V : Any> getComponentInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        var comp = getComponent(clazz, includingDisabled)
        if (comp != null) return comp
        val children = children
        for (i in children.indices) {
            val child = children[i]
            if (includingDisabled || child.isEnabled) {
                comp = child.getComponentInChildren(clazz, includingDisabled)
                if (comp != null) return comp
            }
        }
        return null
    }

    fun <V : Any> getComponentInHierarchy(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        return getComponent(clazz, includingDisabled) ?: parentEntity?.getComponentInHierarchy(clazz, includingDisabled)
    }

    fun <V : Any> getComponents(clazz: KClass<V>, includingDisabled: Boolean = false): List<V> {
        @Suppress("unchecked_cast")
        return components.filter { (includingDisabled || it.isEnabled) && clazz.isInstance(it) } as List<V>
    }

    fun <V : Any> allComponents(
        clazz: KClass<V>,
        includingDisabled: Boolean = false,
        lambda: (V) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            @Suppress("unchecked_cast")
            if ((includingDisabled || c.isEnabled) && clazz.isInstance(c) && !lambda(c as V))
                return false
        }
        return true
    }

    fun anyComponent(
        includingDisabled: Boolean = false,
        test: (Component) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            if ((includingDisabled || c.isEnabled) && test(c))
                return true
        }
        return false
    }

    fun <V : Any> anyComponent(
        clazz: KClass<V>,
        includingDisabled: Boolean = false,
        test: (V) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            @Suppress("unchecked_cast")
            if ((includingDisabled || c.isEnabled) && clazz.isInstance(c) && test(c as V))
                return true
        }
        return false
    }

    fun <V : Any> anyComponentInChildren(
        clazz: KClass<V>,
        includingDisabled: Boolean = false,
        test: (V) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            @Suppress("unchecked_cast")
            if ((includingDisabled || c.isEnabled) && clazz.isInstance(c) && test(c as V))
                return true
        }
        val children = children
        for (index in children.indices) {
            val c = children[index]
            if ((includingDisabled || c.isEnabled) && c.anyComponentInChildren(clazz, includingDisabled, test)) {
                return true
            }
        }
        return false
    }

    fun <V : Any> sumComponents(
        clazz: KClass<V>,
        includingDisabled: Boolean = false,
        test: (V) -> Int
    ): Int {
        val components = components
        var counter = 0
        for (index in components.indices) {
            val c = components[index]
            @Suppress("unchecked_cast")
            if ((includingDisabled || c.isEnabled) && clazz.isInstance(c))
                counter += test(c as V)
        }
        return counter
    }

    fun <V : Any> getComponentsInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): List<V> {
        return getComponentsInChildren(clazz, includingDisabled, ArrayList())
    }

    fun <V : Any> getComponentsInChildren(
        clazz: KClass<V>,
        includingDisabled: Boolean,
        dst: MutableList<V>
    ): List<V> {
        val components = components
        for (i in components.indices) {
            val component = components[i]
            if (clazz.isInstance(component)) {
                @Suppress("unchecked_cast")
                dst.add(component as V)
            }
        }
        val children = children
        for (i in children.indices) {
            val child = children[i]
            child.getComponentsInChildren(clazz, includingDisabled, dst)
        }
        return dst
    }

    fun <V : Any> firstComponentInChildren(
        clazz: KClass<V>,
        includingDisabled: Boolean = false,
        action: (V) -> Boolean
    ): V? {
        val components = components
        for (i in components.indices) {
            val component = components[i]
            if (clazz.isInstance(component)) {
                @Suppress("unchecked_cast")
                component as V
                if (action(component)) return component
            }
        }
        val children = children
        for (i in children.indices) {
            val child = children[i]
            val v = child.firstComponentInChildren(clazz, includingDisabled, action)
            if (v != null) return v
        }
        return null
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

    fun toStringWithTransforms(depth: Int): StringBuilder {
        val text = StringBuilder()
        for (i in 0 until depth) text.append('\t')
        val p = transform.localPosition
        val r = transform.localRotation
        val s = transform.localScale
        text.append(
            "Entity((${p.x.f2s()},${p.y.f2s()},${p.z.f2s()})," +
                    "(${r.x.f2s()},${r.y.f2s()},${r.z.f2s()},${r.w.f2s()})," +
                    "(${s.x.f2s()},${s.y.f2s()},${s.z.f2s()}),'$name',$sizeOfHierarchy):\n"
        )
        val nextDepth = depth + 1
        for (child in children)
            text.append(child.toStringWithTransforms(nextDepth))
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

    val sizeOfHierarchy
        get(): Int {
            val children = children
            var sum = children.size + components.size
            for (i in children.indices) {
                sum += children[i].sizeOfHierarchy
            }
            // the root would be missing
            return if (parent == null) sum + 1 else sum
        }

    val sizeOfAllChildren get() = sizeOfHierarchy - 1 // hierarchy - 1

    fun fromOtherLocalToLocal(other: Entity): Matrix4x3d {
        // converts the point from the local coordinates of the other one to our local coordinates
        return Matrix4x3d(transform.globalTransform).invert().mul(other.transform.globalTransform)
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

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        // all tests can be removed
        // interpolation tests
        /*list += UpdatingTextPanel(50, style) {
            val t = transform
            "1x/${(t.lastUpdateDt * 1e-9).f3()}s, ${((Engine.gameTime - t.lastUpdateTime) * 1e-9).f3()}s ago"
        }.apply { useMonospaceCharacters = true }*/
        PrefabInspector.currentInspector!!.inspect(this, list, style)
    }

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
        if (name == "rotation") {
            transform.localRotation = value
        } else {
            super.readQuaterniond(name, value)
        }
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
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