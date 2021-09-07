package me.anno.ecs

import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.light.AmbientLight
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.physics.Rigidbody
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.physics.BulletPhysics
import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.objects.inspectable.Inspectable
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.stacked.Option
import me.anno.ui.style.Style
import me.anno.utils.LOGGER
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.AABBs.all
import me.anno.utils.types.AABBs.clear
import me.anno.utils.types.AABBs.set
import me.anno.utils.types.AABBs.transformUnion
import me.anno.utils.types.Floats.f2s
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.reflect.KClass

// entities would be an idea to make effects more modular
// it could apply new effects to both the camera and image sources

// todo buttons via annotation, which can be triggered from the editor for debugging
// todo we also could set fields for the params in the editor...
// e.g. @Action

// hide the mutable children list, -> not possible with the general approach
// todo keep track of size of hierarchy

// todo load from file whenever something changes;
//  - other way around: when a file changes, update all nodes

// todo delta settings & control: only saves as values, what was changed from the prefab

class Entity() : PrefabSaveable(), Inspectable {

    constructor(parent: Entity?) : this() {
        parent?.add(this)
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

    fun create() {
        val children = internalChildren
        for (index in children.indices) {
            children[index].create()
        }
        val components = internalComponents
        for (index in components.indices) {
            components[index].onCreate()
        }
    }

    @NotSerializedProperty
    private val internalComponents = ArrayList<Component>()

    @SerializedProperty
    val components: List<Component>
        get() = internalComponents

    // @SerializedProperty
    // override var parent: Entity? = null

    @NotSerializedProperty
    private val internalChildren = ArrayList<Entity>()

    @NotSerializedProperty
    override val children: List<Entity>
        get() = internalChildren

    override fun listChildTypes(): String = "ec" // entity children, components

    override fun addChild(child: PrefabSaveable) {
        when (child) {
            is Entity -> addEntity(child)
            is Component -> addComponent(child)
            else -> throw UnsupportedOperationException()
        }
    }

    override fun addChildByType(index: Int, type: Char, instance: PrefabSaveable) {
        if (type == 'c') addComponent(index, instance as Component)
        else addEntity(index, instance as Entity)
    }

    override fun getChildListByType(type: Char): List<PrefabSaveable> = if (type == 'c') components else children
    override fun getChildListNiceName(type: Char): String = if (type == 'c') "components" else "children"
    override fun getTypeOf(child: PrefabSaveable): Char = if (child is Component) 'c' else 'e'

    override fun getIndexOf(child: PrefabSaveable): Int {
        return if (child is Component) {
            components.indexOf(child)
        } else children.indexOf(child)
    }

    override fun getOptionsByType(type: Char): List<Option>? {
        return if (type == 'c') Component.getComponentOptions(this)
        else null
    }


    // aabb cache for faster rendering and collision checks
    @NotSerializedProperty
    val aabb = AABBd()

    @NotSerializedProperty
    var hasValidAABB = false

    // collision mask for faster collision checks
    @NotSerializedProperty
    var collisionMask = 0

    @NotSerializedProperty
    var hasValidCollisionMask = false

    @NotSerializedProperty
    var hasSpaceFillingComponents = false

    // renderable-cache for faster rendering
    @NotSerializedProperty
    var hasRenderables = false

    @SerializedProperty
    var position: Vector3d
        get() = transform.localPosition
        set(value) {
            transform.localPosition = value
            transform.calculateGlobalTransform(parentEntity?.transform)
            invalidateAABBsCompletely()
            invalidatePhysics(false)
        }

    @SerializedProperty
    var rotation: Quaterniond
        get() = transform.localRotation
        set(value) {
            transform.localRotation = value
            transform.calculateGlobalTransform(parentEntity?.transform)
            invalidateAABBsCompletely()
            invalidatePhysics(false)
        }

    @SerializedProperty
    var scale: Vector3d
        get() = transform.localScale
        set(value) {
            transform.localScale = value
            transform.calculateGlobalTransform(parentEntity?.transform)
            invalidateAABBsCompletely()
            invalidatePhysics(false)
        }

    @NotSerializedProperty
    val parentEntity
        get() = parent as? Entity

    /**
     * smoothly transitions to the next position
     * */
    fun moveTo(position: Vector3d) {
        transform.globalTransform.m30(position.x)
        transform.globalTransform.m30(position.y)
        transform.globalTransform.m30(position.z)
        transform.calculateLocalTransform(parentEntity?.transform)
        transform.update()
        invalidateAABBsCompletely()
        invalidatePhysics(false)
    }

    /**
     * teleports to the new position without interpolation
     * */
    fun teleportTo(position: Vector3d) {
        transform.globalTransform.m30(position.x)
        transform.globalTransform.m30(position.y)
        transform.globalTransform.m30(position.z)
        transform.calculateLocalTransform(parentEntity?.transform)
        transform.teleportUpdate()
        invalidateAABBsCompletely()
        invalidatePhysics(false)
    }

    fun canCollide(collisionMask: Int): Boolean {
        return this.collisionMask.and(collisionMask) != 0
    }

    fun invalidatePhysics(force: Boolean) {
        if (force || hasPhysicsInfluence()) {
            // println("inv physics: ${physics != null}, ${rigidbody != null}")
            physics?.invalidate(rigidbody ?: return)
        }
    }

    fun rebuildPhysics(physics: BulletPhysics) {
        if (hasComponent(Rigidbody::class)) {
            physics.invalidate(this)
        } else {
            for (child in children) {
                child.rebuildPhysics(physics)
            }
        }
    }

    fun invalidateAABBsCompletely() {
        invalidateOwnAABB()
        invalidateChildAABBs()
    }

    private fun invalidateOwnAABB() {
        if (hasValidAABB) {
            hasValidAABB = false
            parentEntity?.invalidateOwnAABB()
        }
    }

    private fun invalidateChildAABBs() {
        hasValidAABB = false
        val children = children
        for (i in children.indices) {
            children[i].invalidateChildAABBs()
        }
    }

    fun invalidateCollisionMask() {
        parentEntity?.invalidateCollisionMask()
        hasValidCollisionMask = false
    }

    fun validateMasks() {
        if (hasValidCollisionMask) return
        var collisionMask = 0
        val components = components
        for (i in components.indices) {
            when (val component = components[i]) {
                is MeshComponent -> {
                    collisionMask = collisionMask or component.collisionMask
                    if (collisionMask == -1) break
                }
                is Collider -> {
                    collisionMask = collisionMask or component.collisionMask
                    if (collisionMask == -1) break
                }
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
            // todo if has particle system, include
            val globalTransform = transform.globalTransform
            val components = components
            for (i in components.indices) {
                val component = components[i]
                if (component.isEnabled) {
                    when (component) {
                        is MeshComponent -> {
                            val mesh = MeshCache[component.mesh]
                            if (mesh != null) {
                                // add aabb of that mesh with the transform
                                mesh.ensureBuffer()
                                mesh.aabb.transformUnion(globalTransform, aabb)
                            }
                        }
                        is DirectionalLight -> {
                            if (component.cutoff <= 0f) {
                                aabb.all()
                            } else {
                                val mesh = component.getLightPrimitive()
                                mesh.ensureBuffer()
                                mesh.aabb.transformUnion(globalTransform, aabb)
                            }
                        }
                        is LightComponent -> {
                            val mesh = component.getLightPrimitive()
                            mesh.ensureBuffer()
                            mesh.aabb.transformUnion(globalTransform, aabb)
                        }
                        is AmbientLight -> {
                            // ambient light has influence on everything
                            aabb.all()
                        }
                        is Collider -> {
                            val tmp = JomlPools.vector3d.create()
                            component.union(globalTransform, aabb, tmp, false)
                            JomlPools.vector3d.sub(1)
                        }
                    }
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
        // return hasComponent(false, Rigidbody::class) ||
        //        hasComponentInChildren(false, Collider::class)
    }

    val transform = Transform()

    // assigned and tested for click checks
    @HideInInspector
    @NotSerializedProperty
    var clickId = 0

    fun update() {
        for (component in components) component.onUpdate()
        for (child in children) child.update()
    }

    // is set by the engine
    @NotSerializedProperty
    var isPhysicsControlled = false

    @NotSerializedProperty
    var hasBeenVisible = false

    fun updateVisible() {
        if (hasBeenVisible) {
            for (component in components) {
                component.onVisibleUpdate()
            }
        }
        for (child in children) {
            child.updateVisible()
        }
    }

    fun invalidateVisibility() {
        hasBeenVisible = false
        for (child in children) {
            child.invalidateVisibility()
        }
    }

    fun validateTransforms(time: Long = GFX.gameTime) {
        if (!isPhysicsControlled) {
            transform.update(parentEntity?.transform, time)
            val children = children
            for (i in children.indices) {
                children[i].validateTransforms(time)
            }
        }
    }

    fun invalidateChildTransforms() {
        for (i in children.indices) {
            val child = children[i]
            child.transform.invalidateGlobal()
            child.invalidateChildTransforms()
        }
    }

    fun physicsUpdate() {
        // called by physics thread
        // only called for rigidbodies
        // not called for static objects (?), since they should not move
        for (component in components) component.onPhysicsUpdate()
    }

    /*
    * val drawable = children.firstOrNull { it is DrawableComponent } ?: return
        val fragmentEffects = children.filterIsInstance<FragmentShaderComponent>()
        (drawable as DrawableComponent).draw(stack, time, color, fragmentEffects)
    * */

    override val className get() = "Entity"

    override fun isDefaultValue(): Boolean = false

    private fun transformUpdate(parent: Entity, keepWorldTransform: Boolean) {
        if (keepWorldTransform) {
            transform.calculateLocalTransform(parent.transform)
            // global transform theoretically stays the same
            // it will not, if there is an anomaly, e.g. scale 0
        }
        transform.invalidateGlobal()
        invalidateAABBsCompletely()
    }

    override fun add(child: PrefabSaveable) {
        when (child) {
            is Entity -> addEntity(child)
            is Component -> addComponent(child)
            else -> LOGGER.warn("Cannot add ${child.className} to Entity")
        }
    }

    override fun add(index: Int, child: PrefabSaveable) {
        TODO("Not yet implemented")
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

    // todo don't directly update, rather invalidate this, because there may be more to come
    fun setParent(parent: Entity, index: Int, keepWorldTransform: Boolean) {

        val oldParent = parentEntity
        if (parent === oldParent) return

        // formalities
        if (oldParent != null) {
            oldParent.remove(this)
            oldParent.invalidateOwnAABB()
            oldParent.invalidateCollisionMask()
        }

        parent.internalChildren.add(index, this)
        this.parent = parent

        // transform
        transformUpdate(parent, keepWorldTransform)
        // collision mask
        parent.invalidateCollisionMask()

        checkNeedsPhysics()

    }

    // todo don't directly update, rather invalidate this, because there may be more to come
    fun setParent(parent: Entity, keepWorldTransform: Boolean) {
        return setParent(parent, parent.children.size, keepWorldTransform)
    }

    private fun checkNeedsPhysics() {
        // physics
        if (allInHierarchy { it.isEnabled }) {
            // something can change
            val physics = physics
            if (physics != null) {
                // if there is a rigidbody in the hierarchy, update it
                val parentRigidbody = rigidbody
                if (parentRigidbody != null) {
                    // invalidate it
                    physics.invalidate(parentRigidbody)
                } else {
                    // if has collider without rigidbody, add it for click-tests
                    if (hasComponent(Collider::class, false)) {
                        // todo add it for click tests
                    }
                }
            }// else println("physics is null, $name, list of hierarchy: ${listOfHierarchy.joinToString { it.name }}")
        }
    }

    val physics get() = getRoot(Entity::class).getComponent(BulletPhysics::class, false)
    val rigidbody: Entity? get() = getComponent(Rigidbody::class, false)?.entity
    val rigidbodyComponent: Rigidbody? get() = getComponent(Rigidbody::class, false)

    fun invalidateRigidbody() {
        physics?.invalidate(rigidbody ?: return)
    }

    override fun destroy() {
        for (component in components) {
            component.onDestroy()
        }
        // todo some event based system? or just callable functions? idk...
        val parent = parent as? Entity
        if (parent != null) {
            parent.internalChildren.remove(this)
            if (hasComponentInChildren(Collider::class, false) ||
                hasComponentInChildren(AmbientLight::class, false)
            ) {
                // todo other components with aabb?
                parent.invalidateCollisionMask()
            }
            parent.invalidateOwnAABB()
        }
    }

    fun addComponent(component: Component) {
        internalComponents.add(component)
        onAddComponent(component)
    }

    fun addComponent(index: Int, component: Component) {
        internalComponents.add(index, component)
        onAddComponent(component)
    }

    private fun onAddComponent(component: Component) {
        // if component is Collider or Rigidbody, update the physics
        // todo isEnabled for Colliders and Rigidbody needs to have listeners as well
        onChangeComponent(component)
        component.entity = this
    }

    fun onChangeComponent(component: Component) {
        when (component) {
            is Collider -> {
                invalidateRigidbody()
                invalidateCollisionMask()
            }
            is Rigidbody -> {
                physics?.invalidate(this)
            }
            is MeshComponent -> {
                invalidateOwnAABB()
                invalidateCollisionMask()
            }
            is LightComponent -> invalidateOwnAABB()
        }
        hasRenderables = hasComponent(MeshComponent::class, false) ||
                hasComponent(LightComponent::class, false)
        hasSpaceFillingComponents = hasRenderables ||
                hasComponent(Collider::class, false) ||
                hasComponent(AmbientLight::class, false)
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

    fun <V : Component> hasComponent(clazz: KClass<V>, includingDisabled: Boolean = false): Boolean {
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

    fun <V : Component> getComponent(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        // elegant:
        // return components.firstOrNull { clazz.isInstance(it) && (includingDisabled || it.isEnabled) } as V?
        // without damn iterator:
        val components = components
        for (i in components.indices) {
            val component = components[i]
            if ((includingDisabled || component.isEnabled) && clazz.isInstance(component)) {
                return component as V
            }
        }
        return null
    }

    fun <V : Component> getComponentInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
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

    fun <V : Component> getComponentInHierarchy(clazz: KClass<V>, includingDisabled: Boolean = false): V? {
        return getComponent(clazz, includingDisabled) ?: parentEntity?.getComponentInHierarchy(clazz, includingDisabled)
    }

    fun <V : Component> getComponents(clazz: KClass<V>, includingDisabled: Boolean = false): List<V> {
        return components.filter { (includingDisabled || it.isEnabled) && clazz.isInstance(it) } as List<V>
    }

    fun <V : Component> allComponents(
        clazz: KClass<V>,
        includingDisabled: Boolean = false,
        lambda: (V) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            if ((includingDisabled || c.isEnabled) && clazz.isInstance(c) && !lambda(c as V))
                return false
        }
        return true
    }

    fun <V : Component> anyComponent(
        clazz: KClass<V>,
        includingDisabled: Boolean = false,
        lambda: (V) -> Boolean
    ): Boolean {
        val components = components
        for (index in components.indices) {
            val c = components[index]
            if ((includingDisabled || c.isEnabled) && clazz.isInstance(c) && lambda(c as V))
                return true
        }
        return false
    }

    fun <V : Component> getComponentsInChildren(clazz: KClass<V>, includingDisabled: Boolean = false): List<V> {
        return getComponentsInChildren(clazz, includingDisabled, ArrayList<V>())
    }

    fun <V : Component> getComponentsInChildren(
        clazz: KClass<V>,
        includingDisabled: Boolean,
        dst: MutableList<V>
    ): List<V> {
        val components = components
        for (i in components.indices) {
            val component = components[i]
            if (clazz.isInstance(component)) {
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

    fun add(child: Entity) = addChild(child)
    fun add(component: Component) = addComponent(component)

    fun remove(child: Entity) {
        if (child.parent !== this) return
        // todo invalidate physics
        internalChildren.remove(child)
        if (child.parent == this) {
            child.parent = null
        }
    }

    val sizeOfHierarchy
        get(): Int {
            val children = children
            var sum = children.size + components.size
            for (i in children.indices) {
                sum += children[i].sizeOfHierarchy
            }
            return sum
        }

    val depthInHierarchy
        get(): Int {
            val parent = parentEntity ?: return 0
            return parent.depthInHierarchy + 1
        }

    fun fromOtherLocalToLocal(other: Entity): Matrix4x3d {
        // converts the point from the local coordinates of the other one to our local coordinates
        return Matrix4x3d(transform.globalTransform).invert().mul(other.transform.globalTransform)
    }

    fun fromLocalToOtherLocal(other: Entity): Matrix4x3d {
        // converts the point from our local coordinates of the local coordinates of the other one
        return Matrix4x3d(other.transform.globalTransform).invert().mul(transform.globalTransform)
    }

    override fun clone(): Entity {
        val clone = Entity()
        copy(clone)
        return clone
    }

    /**
     * O(|E|+|C|) clone of properties and components
     * */
    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Entity
        // copy all properties
        clone.hasRenderables = hasRenderables
        clone.hasValidCollisionMask = hasValidCollisionMask
        clone.hasSpaceFillingComponents = hasSpaceFillingComponents
        clone.hasValidAABB = hasValidAABB
        clone.aabb.set(aabb)
        clone.transform.set(transform)
        clone.collisionMask = collisionMask
        // first the structure
        val children = internalChildren
        val cloneEntities = clone.internalChildren
        if (cloneEntities.isNotEmpty()) cloneEntities.clear()
        for (i in children.indices) {
            val entity = children[i].clone()
            entity.parent = clone
            cloneEntities.add(entity)
        }
        val components = internalComponents
        val cloneComponents = clone.internalComponents
        if (cloneComponents.isNotEmpty()) cloneComponents.clear()
        for (i in components.indices) {
            val component = components[i].clone()
            component.entity = clone
            cloneComponents.add(component)
        }
    }

    override fun onDestroy() {}

    override val symbol: String
        get() = ""

    override val defaultDisplayName: String
        get() = "Entity"

    // which properties were changed
    // options:
    // - child/index, child/name
    // - component/index, component/name
    // - position, rotation, scale
    // - name, description,
    // - isEnabled

    // var prefabPath: FileReference = InvalidRef

    // var prefab: Entity? = null
    // var ownPath: FileReference = InvalidRef // where our file is located

    // get root somehow? how can we detect it?
    // not possible in the whole scene for sub-scenes
    // however, when we are only editing prefabs, it would be possible :)

    /*fun pathInRoot(root: Entity): ArrayList<Int> {
        if (this == root) return arrayListOf()
        val parent = parent
        return if (parent != null) {
            val ownIndex = parent.children.indexOf(this@Entity)
            parent.pathInRoot().apply {
                add(ownIndex)
            }
        } else arrayListOf()
    }

    fun pathInRoot(): ArrayList<Int> {
        val parent = parent
        return if (parent != null) {
            val ownIndex = parent.children.indexOf(this@Entity)
            parent.pathInRoot().apply {
                add(ownIndex)
            }
        } else arrayListOf()
    }*/

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        // interpolation tests
        /*list += UpdatingTextPanel(50, style) {
            val t = transform
            "1x/${(t.lastUpdateDt * 1e-9).f3()}s, ${((GFX.gameTime - t.lastUpdateTime) * 1e-9).f3()}s ago"
        }*/
        PrefabInspector.currentInspector!!.inspect(this, list, style)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectList(this, "children", children)
        writer.writeObjectList(this, "components", components)
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

}