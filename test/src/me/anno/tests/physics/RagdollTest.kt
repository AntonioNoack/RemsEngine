package me.anno.tests.physics

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.bullet.constraints.PointConstraint
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponentsInChildren
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.AnimationCache
import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.components.anim.Bone
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.anim.SkeletonCache
import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.collider.CapsuleCollider
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Systems
import me.anno.engine.DefaultAssets
import me.anno.engine.OfficialExtensions
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.input.Input
import me.anno.maths.Maths.sq
import me.anno.sdf.shapes.SDFCapsule
import me.anno.ui.UIColors
import me.anno.utils.OS
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.none2
import me.anno.utils.types.Vectors.normalToQuaternionY
import org.joml.Matrix4x3
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f

fun main() {

    OfficialExtensions.initForTests()

    // load fox
    val source = OS.downloads.getChild("3d/azeria/scene.gltf")
    val totalMass = 10.0

    // val source = OS.downloads.getChild("3d/Talking On Phone.fbx")
    val meshScene = PrefabCache[source].waitFor()!!.newInstance() as Entity
    val meshComponents = meshScene.getComponentsInChildren(AnimMeshComponent::class)
    val sampleComponent = meshComponents.first()

    @Suppress("MoveVariableDeclarationIntoWhen", "RedundantSuppression")
    val baseAnimation = AnimationCache.getEntry(sampleComponent.animations[0].source).waitFor()!!

    val skeleton = SkeletonCache.getEntry(sampleComponent.getMesh()?.skeleton).waitFor()!!
    val bones = skeleton.bones

    val scene = Entity("Scene").add(meshScene)
    meshScene.position += Vector3d(0.0, 5.0, 0.0) // make it stand on floor properly
    meshScene.validateTransform()

    val meshMatrix = sampleComponent.transform!!.globalTransform

    val ragdollAnimation = when (baseAnimation) {
        // is BoneByBoneAnimation -> baseAnimation.clone() as BoneByBoneAnimation
        is ImportedAnimation -> baseAnimation.withFrames(listOf(0))
        else -> throw IllegalStateException()
    }

    val initialPose = ragdollAnimation.frames[0].map { Matrix4x3f(it) }
    val bonePositions = bones.map {
        val pos = Vector3d(it.bindPosition)
        initialPose[it.index].transformPosition(pos)
        meshMatrix.transformPosition(pos)
    }

    val capsules = ArrayList<CapsuleCollider>()

    val entities = ArrayList<Entity?>()
    val rigidbodies = ArrayList<DynamicBody?>()
    val baseTransformInvs = ArrayList<Matrix4x3?>()
    val roots = ArrayList<Pair<Double, DynamicBody>>()
    var isRootBone = true

    class Sphere(val position: Vector3d, var radius: Double) {
        fun overlaps(other: Sphere): Boolean {
            val distanceSq = position.distanceSquared(other.position)
            val radiusSq = sq(radius + other.radius)
            return distanceSq <= radiusSq
        }
    }

    val spheres = ArrayList<Sphere>()
    val capsules2 = ArrayList<SDFCapsule>()
    val seeds = IntArrayList(0)
    val tmp4 = Vector4f()

    for (bone in bones) {

        val bonePos = bonePositions[bone.index]

        fun findParent(bone: Bone): Bone? {
            val parent = bone.getParent(bones) ?: return null
            return if (bonePositions[parent.index].distance(bonePos) > 0.001) parent
            else findParent(parent)
        }

        // spawn ragdoll bone
        val parent = findParent(bone)
        if (parent == null) {
            entities.add(null)
            rigidbodies.add(null)
            baseTransformInvs.add(null)
            continue
        }

        val parentPos = bonePositions[parent.index]
        if (isRootBone && parentPos.distance(bonePos) < 0.001) {
            entities.add(null)
            rigidbodies.add(null)
            baseTransformInvs.add(null)
            continue
        }

        // todo skip the first one, or make it artificially smaller???
        if (isRootBone) {
            isRootBone = false
            entities.add(null)
            rigidbodies.add(null)
            baseTransformInvs.add(null)
            continue
        }

        // todo the "behind" of the fox has vertices, but no bones or animations... what do we set for it?, is there physics for it?

        val length = parentPos.distance(bonePos)
        val centerPos = parentPos.add(bonePos, Vector3d()).mul(0.5)
        val direction = Vector3f(parentPos - bonePos)
        val dynamicBody = DynamicBody().apply {
            mass = length
            friction = 1.0
            // todo why do we need to high friction to prevent explosions?
            linearDamping = 0.9
            angularDamping = 0.9
            rigidbodies.add(this)
        }

        // while parent's sphere is still colliding,
        //  shrink effective length
        var effectiveLength = length.toFloat() * 0.4
        val radiusFactor = 0.3
        val parentSphere = Sphere(Vector3d(), 1.0)
        fun defineSphere(sign: Double, dstSphere: Sphere) {
            centerPos.fma(sign * effectiveLength / length, direction, dstSphere.position)
            dstSphere.radius = effectiveLength * radiusFactor
        }
        while (true) {
            defineSphere(1.0, parentSphere)
            if (spheres.none2 { it.overlaps(parentSphere) } &&
                capsules2.none2 {
                    tmp4.set(parentSphere.position, 0.0)
                    it.computeSDF(tmp4, seeds) < parentSphere.radius
                }) break
            effectiveLength *= 0.95f
        }

        // just for safety
        effectiveLength *= 0.5f

        val selfSphere = Sphere(Vector3d(), 1.0)
        defineSphere(-1.0, selfSphere)

        spheres.add(parentSphere)
        spheres.add(selfSphere)
        capsules2.add(SDFCapsule().apply {
            p0.set(parentSphere.position)
            p1.set(selfSphere.position)
            radius = (effectiveLength * radiusFactor).toFloat()
        })

        // todo somehow generate better, non-overlapping colliders...
        val collider = CapsuleCollider().apply {
            axis = Axis.Y
            halfHeight = effectiveLength.toFloat()
            radius = halfHeight * radiusFactor.toFloat()
            roundness = 0.1f
            capsules.add(this)
        }

        val entity = Entity(bone.name, scene)
            .add(dynamicBody)
            .add(collider)

        entity.add(object : Component(), OnUpdate {
            fun displaySphere(sphere: Sphere) {
                LineShapes.drawSphere(null, sphere.radius, sphere.position, -1)
            }

            override fun onUpdate() {
                displaySphere(parentSphere)
                displaySphere(selfSphere)
            }
        })

        val baseRotation = direction.normalize().normalToQuaternionY()
        val parentBody = rigidbodies[parent.index]
        if (parentBody != null) {
            // how TF does everything just explode???
            //  because some cylinders are still overlapping
            entity.add(PointConstraint().apply {
                other = parentBody
                selfPosition.set(0.0, +length * 0.5, 0.0)
                otherPosition.set(0.0, -parent.length(bones) * 0.5, 0.0)
                breakingImpulseThreshold = 50.0
                // disableCollisionsBetweenLinked = false
                damping = 0.5
                // twist = 0.01
                // angleX = 0.1
                // angleY = 0.1
            })
        } else {
            roots.add(length to dynamicBody)
        }

        entity.position = centerPos
        entity.rotation = baseRotation
        entity.validateTransform()
        entities.add(entity)
        baseTransformInvs.add(entity.transform.globalTransform.invert(Matrix4x3()))
    }

    val totalBoneMass = rigidbodies.filterNotNull().sumOf { it.mass }
    val massCorrectionFactor = totalMass / totalBoneMass
    for (rigidbody in rigidbodies) {
        rigidbody ?: continue
        rigidbody.mass *= massCorrectionFactor
    }

    // link all children of root...
    if (roots.size >= 2) {
        val (root1Len, root1) = roots.first()
        for (i in 1 until roots.size) {
            val (root2Len, root2) = roots[i]
            val entity = root1.entity!!
            entity.add(PointConstraint().apply {
                other = root2
                selfPosition.set(0.0, root1Len * 0.5, 0.0)
                otherPosition.set(0.0, root2Len * 0.5, 0.0)
                breakingImpulseThreshold = 50.0
                // disableCollisionsBetweenLinked = false
                damping = 0.5
            })
        }
    }

    val animationUpdateComponent = object : Component(), OnUpdate {

        val physicsRot = Quaternionf()
        val baseRotInv = Quaternionf()
        val physicsPos = Vector3f()
        val boneOffset = Vector3f()

        override fun onUpdate() {
            val invTransformD = sampleComponent.transform!!.globalTransform.invert(Matrix4x3())
            val invTransform = Matrix4x3f().set(invTransformD)
            for (boneId in bones.indices) {

                var boneId1 = boneId
                var ragdoll = entities[boneId]
                val isFirstBone = ragdoll == null
                if (isFirstBone) {
                    // todo average the rotations of all children???
                    // todo position is also a bit weird...
                    //  baseTransformInvs probably doesn't match
                    val firstChildRagdoll = bones.indexOfFirst {
                        it.parentIndex == boneId && entities[it.index] != null
                    }
                    if (firstChildRagdoll <= 0) continue
                    boneId1 = firstChildRagdoll
                    ragdoll = entities[boneId1]!!
                }

                ragdoll.validateTransform()

                val ragdollT = ragdoll.transform
                val physicsTransform = ragdollT.globalTransform
                val dstMatrix = ragdollAnimation.frames[0][boneId]
                val bone = bones[boneId1]
                if (Input.isShiftDown) {
                    dstMatrix.set(initialPose[boneId])
                } else {

                    val baseTransformInv = baseTransformInvs[boneId1]!!
                    val length = bone.length(bones)

                    // physicsPos must be corrected by half a bone length,
                    // because we simulate the center, but actually mean the root
                    boneOffset.set(0f, length * 0.5f, 0f)
                    physicsTransform.transformDirection(boneOffset)

                    physicsPos.set(ragdollT.localPosition)
                    if (isFirstBone) physicsPos.add(boneOffset)
                    else physicsPos.sub(boneOffset)

                    val bindPos = bone.bindPosition // baseTransform.getTranslation(Vector3d())
                    physicsTransform.getUnnormalizedRotation(physicsRot)
                    baseTransformInv.getUnnormalizedRotation(baseRotInv)

                    dstMatrix.identity()
                        .translate(physicsPos)
                        .rotate(baseRotInv.premul(physicsRot).normalize())
                        .translate(-bindPos.x, -bindPos.y, -bindPos.z)

                    invTransform.mul(dstMatrix, dstMatrix)
                }
            }
            AnimationCache.invalidate(ragdollAnimation)
        }
    }
    scene.add(animationUpdateComponent)
    animationUpdateComponent.onUpdate()

    Entity("Floor", scene)
        .setPosition(0.0, -10.0, 0.0)
        .setScale(250f)
        .add(StaticBody())
        .add(InfinitePlaneCollider())
        .add(MeshComponent(DefaultAssets.plane))

    // make space, so we can see the bones properly
    val state = AnimationState(ragdollAnimation.ref)
    for (meshComponentI in meshComponents) {
        meshComponentI.animations = listOf(state)
    }

    // add rotational constraints, bones can't just rotate along each other freely
    // to do (hard) if it is a knee or elbow (by name), only allow rotation along one axis

    val physics = BulletPhysics()
    physics.enableDebugRendering = true
    Systems.registerSystem(physics)

    // create ragdoll
    // adjust ragdoll to start on specific animation state/frame (e.g., walking)
    // make skin stick to ragdoll physics
    // test physics for ragdoll -> looks very weird, and there is two parts for some reason :/
    testSceneWithUI("Ragdoll Test", scene, RenderMode.LINES)
}