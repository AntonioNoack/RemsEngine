package me.anno.ecs

import me.anno.ecs.EntityPhysics.invalidatePhysicsIfEnabled
import me.anno.ecs.EntityPhysics.invalidatePhysicsTransform
import me.anno.ecs.EntityQuery.forAllChildren
import me.anno.ecs.EntityQuery.forAllComponents
import me.anno.ecs.EntityStats.sizeOfHierarchy
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.PositionType
import me.anno.ecs.annotations.RotationType
import me.anno.ecs.annotations.ScaleType
import me.anno.ecs.components.collider.CollidingComponent
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.components.FillSpace
import me.anno.ecs.systems.OnChangeStructure
import me.anno.ecs.systems.OnEnable
import me.anno.ecs.systems.Systems
import me.anno.engine.inspector.Inspectable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.base.BaseWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.editor.stacked.Option
import me.anno.utils.algorithms.Recursion
import me.anno.utils.types.AnyToBool
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.withFlag
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

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

    @DebugProperty
    @NotSerializedProperty
    var hasValidMasks: Boolean
        get() = flags.hasFlag(VALID_COLLISION_MASK_FLAG)
        set(value) {
            flags = flags.withFlag(VALID_COLLISION_MASK_FLAG, value)
        }

    @DebugProperty
    @NotSerializedProperty
    var isCreated: Boolean
        get() = flags.hasFlag(CREATED_FLAG)
        set(value) {
            flags = flags.withFlag(CREATED_FLAG, value)
        }

    // todo there should be a system and an interface for this, shouldn't they?
    fun create() {
        if (isCreated) return
        transform.teleportUpdate()
        invalidateAABBsCompletely()
        isCreated = true
        forAllChildren(true, Entity::create)
        forAllComponents(OnEnable::class, true, OnEnable::onEnable)
    }

    val transform: Transform = Transform(this)

    @NotSerializedProperty
    private val internalComponents: ArrayList<Component> = ArrayList(4)

    @NotSerializedProperty
    val components: List<Component>
        get() = internalComponents

    @NotSerializedProperty
    private val internalChildren: ArrayList<Entity> = ArrayList(4)

    @NotSerializedProperty
    override val children: List<Entity>
        get() = internalChildren

    override fun listChildTypes(): String = "ec" // entity children, components

    override fun addChildByType(index: Int, type: Char, child: PrefabSaveable) {
        addChild(index, child) // type is determined by child-class, so just use that function
    }

    override fun getChildListByType(type: Char): List<PrefabSaveable> = if (type == 'c') components else children
    override fun getChildListNiceName(type: Char): String = if (type == 'c') "components" else "children"
    override fun getValidTypesForChild(child: PrefabSaveable): String = when (child) {
        is Component -> "c"
        is Entity -> "e"
        else -> ""
    }

    override fun getIndexOf(child: PrefabSaveable): Int {
        val childrenByType = if (child is Component) components else children
        return childrenByType.indexOf(child)
    }

    override fun getOptionsByType(type: Char): List<Option<PrefabSaveable>> {
        return if (type == 'c') getOptionsByClass(this, Component::class)
        else entityOptionList
    }

    @DebugProperty
    @NotSerializedProperty
    @Docs("Bounds in global space for faster collision tests and optimized rendering")
    private val aabb: AABBd = AABBd()

    @DebugProperty
    @NotSerializedProperty
    var hasValidAABB: Boolean
        get() = flags.hasFlag(VALID_AABB_FLAG)
        set(value) {
            flags = flags.withFlag(VALID_AABB_FLAG, value)
        }

    @Docs("Set by the engine; if so, transforms are ignored")
    @DebugProperty
    @NotSerializedProperty
    var isPhysicsControlled: Boolean
        get() = flags.hasFlag(PHYSICS_CONTROLLED_FLAG)
        set(value) {
            flags = flags.withFlag(PHYSICS_CONTROLLED_FLAG, value)
        }

    @DebugProperty
    @NotSerializedProperty
    @Docs("Mask of what colliders this Entity contains")
    var collisionBits: Int = 0

    @PositionType
    @SerializedProperty
    @Docs("Local position, shortcut for transform.localPosition")
    var position: Vector3d
        get() = transform.localPosition
        set(value) {
            transform.localPosition = value
            onChangeTransform()
        }

    @RotationType
    @SerializedProperty
    @Docs("Local rotation, shortcut for transform.localRotation")
    var rotation: Quaternionf
        get() = transform.localRotation
        set(value) {
            transform.localRotation = value
            onChangeTransform()
        }

    @ScaleType
    @SerializedProperty
    @Docs("Local scale, shortcut for transform.localScale")
    var scale: Vector3f
        get() = transform.localScale
        set(value) {
            transform.localScale = value
            onChangeTransform()
        }

    private fun onChangeTransform() {
        invalidateAABBsCompletely()
        // scale is not just transform in bullet, it is scaling the collider
        invalidatePhysicsTransform()
        validateTransform()
    }

    fun setPosition(v: Vector3d): Entity {
        position = v
        return this
    }

    fun setPosition(x: Double, y: Double, z: Double): Entity {
        return setPosition(position.set(x, y, z))
    }

    fun setRotation(q: Quaternionf): Entity {
        rotation = q
        return this
    }

    fun setRotation(radiansX: Float, radiansY: Float, radiansZ: Float): Entity {
        rotation = rotation
            .identity()
            .rotateYXZ(radiansY, radiansX, radiansZ)
        return this
    }

    fun setRotationDegrees(degreesX: Float, degreesY: Float, degreesZ: Float): Entity {
        return setRotation(
            degreesX.toRadians(),
            degreesY.toRadians(),
            degreesZ.toRadians()
        )
    }

    fun setScale(sc: Float): Entity {
        return setScale(sc, sc, sc)
    }

    fun setScale(v: Vector3f): Entity {
        scale = v
        return this
    }

    fun setScale(scaleX: Float, scaleY: Float, scaleZ: Float): Entity {
        return setScale(scale.set(scaleX, scaleY, scaleZ))
    }

    @NotSerializedProperty
    val parentEntity: Entity?
        get() = parent as? Entity

    /**
     * smoothly transitions to the next position
     * */
    fun moveToGlobal(position: Vector3d) {
        transform.globalPosition = position
        validateTransform()
        invalidateAABBsCompletely()
        invalidatePhysicsTransform()
    }

    /**
     * teleports to the new position without interpolation
     * */
    fun teleportToGlobal(position: Vector3d) {
        transform.globalPosition = position
        validateTransform()
        transform.teleportUpdate()
        invalidateAABBsCompletely()
        invalidatePhysicsTransform()
    }

    fun canCollide(collisionMask: Int): Boolean {
        return this.collisionBits.and(collisionMask) != 0
    }

    @DebugAction("Invalidate AABBs Completely")
    fun invalidateAABBsCompletely() {
        invalidateOwnAABB()
        invalidateChildAABBs()
    }

    @DebugAction
    fun invalidateOwnAABB() {
        hasValidAABB = false
        parentEntity?.invalidateOwnAABB()
    }

    private fun invalidateChildAABBs() {
        hasValidAABB = false
        forAllChildren(false) { child ->
            if (!child.isPhysicsControlled) {
                child.invalidateChildAABBs()
            }
        }
    }

    @DebugAction
    fun invalidateMasks() {
        parentEntity?.invalidateMasks()
        hasValidMasks = false
    }

    fun validateMasks() {
        if (hasValidMasks) return
        var collisionMask = 0
        forAllComponents(CollidingComponent::class, false) { component ->
            collisionMask = collisionMask or component.collisionMask
        }
        forAllChildren(false) { child ->
            child.validateMasks()
            collisionMask = collisionMask or child.collisionBits
        }
        this.collisionBits = collisionMask
        hasValidMasks = true
    }

    override fun getGlobalBounds(): AABBd {
        if (hasValidAABB) {
            // to check if all invalidations were applied correctly
            /*val oldAABB = AABBd(aabb)
            hasValidAABB = false
            validateAABBs()
            if (oldAABB != aabb) LOGGER.warn("AABBs differed: $aabb vs $oldAABB, $name")*/
            return aabb
        }
        fillBounds()
        return aabb
    }

    private fun fillBounds() {
        aabb.clear()
        forAllChildren(false) { child ->
            aabb.union(child.getGlobalBounds())
        }
        val globalTransform = transform.globalTransform
        forAllComponents(FillSpace::class, false) { component ->
            component.fillSpace(globalTransform, aabb)
        }
        hasValidAABB = true
    }

    override var isEnabled: Boolean
        get() = super.isEnabled
        set(value) {
            if (super.isEnabled != value) {
                super.isEnabled = value
                getSystems()?.setContainsRecursively(this, value)
            }
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
            else -> warnCannotAdd(child)
        }
    }

    override fun addChild(index: Int, child: PrefabSaveable) {
        when (child) {
            is Entity -> addEntity(index, child)
            is Component -> addComponent(index, child)
            else -> warnCannotAdd(child)
        }
    }

    private fun warnCannotAdd(child: PrefabSaveable) {
        LOGGER.warn("Cannot add {} to Entity", child.className)
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
            oldParent.invalidateMasks()
        }

        if (index !in parent.internalChildren.indices) parent.internalChildren.add(this)
        else parent.internalChildren.add(index, this)
        this.parent = parent

        // transform
        transformUpdate(keepWorldTransform)
        // collision mask
        parent.invalidateMasks()
        invalidateAABBsCompletely()

        invalidatePhysicsIfEnabled()

        getSystems()?.setContainsRecursively(this, true)
        parent.setChildPath(this, index, 'e')
    }

    // todo don't directly update, rather invalidate this, because there may be more to come
    fun setParent(parent: Entity, keepWorldTransform: Boolean) {
        return setParent(parent, parent.children.size, keepWorldTransform)
    }

    override fun destroy() {
        isCreated = false
        forAllComponents(true) {
            it.destroy()
        }
        val parent = parent as? Entity
        if (parent != null) {
            getSystems()?.setContainsRecursively(this, false)
            parent.internalChildren.remove(this)
            parent.invalidateMasks()
            parent.invalidateOwnAABB()
        }
    }

    fun addComponent(component: Component): Entity {
        return addComponent(-1, component)
    }

    fun addComponent(index: Int, component: Component): Entity {
        if (index < 0 || index >= internalComponents.size) internalComponents.add(component)
        else internalComponents.add(index, component)
        component.entity = this
        onChangeComponent(component, true)
        getSystems()?.setContainsRecursively(component, true)
        setChildPath(component, index, 'c')
        return this
    }

    private fun getSystems(): Systems? {
        return if (root === Systems.world) Systems else null
    }

    fun onChangeComponent(component: Component, wasAdded: Boolean) {
        invalidateOwnAABB()
        invalidateMasks()
        getSystems()?.setContainsRecursively(component, wasAdded)
        forAllComponents(OnChangeStructure::class, false) { comp ->
            comp.onChangeStructure(this)
        }
    }

    private fun onExtendAABB() {
        var parent = parentEntity
        while (parent != null) {
            parent.aabb.union(aabb)
            parent = parent.parentEntity
        }
    }

    fun addEntity(child: Entity): Entity {
        child.setParent(this, children.size, false)
        return this
    }

    fun addEntity(index: Int, child: Entity): Entity {
        child.setParent(this, index, false)
        return this
    }

    fun remove(component: Component): Boolean {
        val found = internalComponents.remove(component)
        if (!found) return false
        onChangeComponent(component, false)
        getSystems()?.setContainsRecursively(component, false)
        return true
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
            onChangeComponent(component, false)
            getSystems()?.setContainsRecursively(component, false)
        }
    }

    override fun toString(): String {
        return toString(StringBuilder(), 0).toString().trim()
    }

    fun toString(result: StringBuilder, depth: Int): StringBuilder {
        for (i in 0 until depth) result.append('\t')
        result.append("Entity('").append(name).append("',").append(sizeOfHierarchy).append("):\n")
        val nextDepth = depth + 1
        forAllChildren(true) { child ->
            child.toString(result, nextDepth)
        }
        forAllComponents(true) { component ->
            for (i in 0 until nextDepth) result.append('\t')
            result.append(component.className).append("('").append(component.name).append("')\n")
        }
        return result
    }

    fun add(child: Entity) = addEntity(child)
    fun add(component: Component) = addComponent(component)

    fun remove(child: Entity): Boolean {
        if (child.parent !== this) return false
        val found = internalChildren.remove(child) // should be true
        if (!found) LOGGER.warn("Weird removal, where child.parent === this, but not found in children list")
        getSystems()?.setContainsRecursively(child, false)
        child.parent = null
        return found
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

    fun fromOtherLocalToLocal(other: Entity, dst: Matrix4x3 = Matrix4x3()): Matrix4x3 {
        // converts the point from the local coordinates of the other one to our local coordinates
        return other.fromLocalToOtherLocal(this, dst)
    }

    fun fromLocalToOtherLocal(other: Entity, dst: Matrix4x3 = Matrix4x3()): Matrix4x3 {
        // converts the point from our local coordinates of the local coordinates of the other one
        return other.transform.globalTransform
            .invert(dst)
            .mul(transform.globalTransform)
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {
        Recursion.processRecursive(this) { entity, remaining ->
            entity.forAllComponents(Renderable::class, false) { component ->
                if (component !== pipeline.ignoredComponent) {
                    val bounds = component.getGlobalBounds()
                    if (bounds == null || pipeline.frustum.isVisible(bounds)) {
                        component.fill(pipeline, entity.transform)
                    }
                }
            }
            entity.forAllChildren(false) { child ->
                if (child !== pipeline.ignoredEntity && pipeline.frustum.isVisible(child.getGlobalBounds())) {
                    remaining.add(child)
                }
            }
        }
    }

    /**
     * O(|E|+|C|) clone of properties and components
     * */
    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Entity) return
        // copy all properties
        dst.hasValidMasks = hasValidMasks
        dst.hasValidAABB = hasValidAABB
        dst.aabb.set(aabb)
        dst.transform.set(transform)
        dst.collisionBits = collisionBits
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

    override val symbol: String
        get() = ""

    override val defaultDisplayName: String
        get() = "Entity"

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeVector3d("position", transform.localPosition)
        val scale = transform.localScale
        if (scale.x != 1f || scale.y != 1f || scale.z != 1f) {
            writer.writeVector3f("scale", scale, true)
        }
        writer.writeQuaternionf("rotation", transform.localRotation)
        writer.writeObjectList(this, "children", children)
        writer.writeObjectList(this, "components", components)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "position" -> transform.localPosition = value as? Vector3d ?: return
            "scale" -> when (value) {
                is Vector3f -> transform.localScale = value
                is Vector3d -> transform.localScale = transform.localScale.set(value)
            }
            "rotation" -> when (value) {
                is Quaternionf -> transform.localRotation = value
                is Quaterniond -> transform.localRotation = transform.localRotation.set(value)
            }
            "children" -> addMembers(value, internalChildren) { if (it is Entity) addChild(it) }
            "components" -> addMembers(value, internalComponents) { if (it is Component) addComponent(it) }
            "isCollapsed" -> isCollapsed = AnyToBool.getBool(value)
            "isEnabled" -> isEnabled = AnyToBool.getBool(value)
            else -> super.setProperty(name, value)
        }
    }

    private fun <V> addMembers(value: Any?, dst: ArrayList<V>, add: (Any?) -> Unit) {
        val values = value as? List<*> ?: return
        dst.clear()
        dst.ensureCapacity(values.size)
        for (valueI in values) {
            add(valueI)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(Entity::class)
        private val entityOptionList =
            listOf(Option<PrefabSaveable>(NameDesc("Entity", "Create a child entity", "")) { Entity() })
        private const val VALID_COLLISION_MASK_FLAG = 4
        private const val SPACE_FILLING_FLAG = 8
        private const val RENDERABLES_FLAG = 16
        private const val PHYSICS_CONTROLLED_FLAG = 32
        private const val CREATED_FLAG = 64
        private const val ON_UPDATE_FLAG = 128
        private const val VALID_AABB_FLAG = 256
    }
}