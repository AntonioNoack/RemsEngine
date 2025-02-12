package me.anno.tests.physics

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.bullet.constraints.ConeTwistConstraint
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
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.CapsuleCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Systems
import me.anno.engine.DefaultAssets
import me.anno.engine.OfficialExtensions
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.input.Input
import me.anno.ui.UIColors
import me.anno.utils.OS
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Vectors.normalToQuaternionY
import org.joml.Matrix3d
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f

fun main() {

    OfficialExtensions.initForTests()

    // load fox
    val source = OS.downloads.getChild("3d/azeria/scene.gltf")
    val totalMass = 10.0

    // val source = OS.downloads.getChild("3d/Talking On Phone.fbx")
    val meshScene = PrefabCache.newPrefabInstance(source)!! as Entity
    val meshComponents = meshScene.getComponentsInChildren(AnimMeshComponent::class)
    val sampleComponent = meshComponents.first()

    @Suppress("MoveVariableDeclarationIntoWhen", "RedundantSuppression")
    val baseAnimation = AnimationCache[sampleComponent.animations[0].source]!!

    val skeleton = SkeletonCache[sampleComponent.getMesh()?.skeleton]!!
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
        initialPose[it.id].transformPosition(pos)
        meshMatrix.transformPosition(pos)
    }

    val capsules = ArrayList<CapsuleCollider>()
    for (bone in bones) {
        val parent = bone.getParent(bones) ?: continue
        val bonePos = bonePositions[bone.id]
        val parentPos = bonePositions[parent.id]
        DebugShapes.debugLines.add(DebugLine(bonePos, parentPos, UIColors.gold, 1000f))
    }

    for (pos in bonePositions) {
        DebugShapes.debugPoints.add(DebugPoint(pos, UIColors.fireBrick, 1000f))
    }

    // todo add bone visually
    val entities = ArrayList<Entity?>()
    val rigidbodies = ArrayList<Rigidbody?>()
    val baseTransformInvs = ArrayList<Matrix4x3d?>()
    val roots = ArrayList<Pair<Double, Rigidbody>>()
    var isRootBone = true
    for (bone in bones) {

        val bonePos = bonePositions[bone.id]

        fun findParent(bone: Bone): Bone? {
            val parent = bone.getParent(bones) ?: return null
            return if (bonePositions[parent.id].distance(bonePos) > 0.001) parent
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

        val parentPos = bonePositions[parent.id]
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

        val length = parentPos.distance(bonePos)
        val centerPos = parentPos.add(bonePos, Vector3d()).mul(0.5)
        val direction = parentPos - bonePos
        val rigidbody = Rigidbody().apply {
            mass = length
            rigidbodies.add(this)
        }

        val collider = CapsuleCollider().apply {
            axis = Axis.Y
            halfHeight = length * 0.32
            radius = halfHeight * 0.3
            margin = 0.01
            capsules.add(this)
        }

        val entity = Entity(bone.name, scene)
            .add(rigidbody)
            .add(collider)

        val baseRotation = direction.normalize().normalToQuaternionY()
        val parentBody = rigidbodies[parent.id]
        if (parentBody != null) {
            if (false) entity.add(ConeTwistConstraint().apply {
                other = parentBody
                selfPosition.set(0.0, +length * 0.5, 0.0)
                otherPosition.set(0.0, -parent.length(bones) * 0.5, 0.0)
                // twist = 0.01
                // angleX = 0.1
                // angleY = 0.1
            })
        } else {
            roots.add(length to rigidbody)
        }

        entity.position = centerPos
        entity.rotation = baseRotation
        entity.validateTransform()
        entities.add(entity)
        baseTransformInvs.add(Matrix4x3d(entity.transform.globalTransform).invert())
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
            })
        }
    }

    var firstFrame = true
    val animationUpdateComponent = object : Component(), OnUpdate {
        override fun onUpdate() {
            val invTransformD = sampleComponent.transform!!.globalTransform.invert(Matrix4x3d())
            val invTransform = Matrix4x3f().set(invTransformD)
            for (boneId in bones.indices) {
                val ragdoll = entities[boneId] ?: continue
                val ragdollT = ragdoll.transform
                val physicsTransform = ragdollT.globalTransform
                val dstMatrix = ragdollAnimation.frames[0][boneId]
                val bone = bones[boneId]
                if (Input.isShiftDown) {
                    dstMatrix.set(initialPose[boneId])
                } else {
                    // todo this transform isn't correct yet, this must be equal for the first frame
                    val baseTransformInv = baseTransformInvs[boneId]!!
                    val target = initialPose[boneId]

                    val length = bone.length(bones)
                    // physicsPos must be corrected by half a bone length,
                    // because we simulate the center, but actually mean the root
                    val physicsPos = Vector3d(ragdollT.localPosition)
                        .sub(physicsTransform.transformDirection(Vector3d(0.0, length * 0.5, 0.0)))
                    val bindPos = Vector3d(bone.bindPosition) // baseTransform.getTranslation(Vector3d())

                    val physicsRot = physicsTransform.getUnnormalizedRotation(Quaterniond())
                    // baseTransform.transformRotation(physicsRot)
                    val baseRotInv = baseTransformInv.getUnnormalizedRotation(Quaterniond())

                    if (firstFrame) {
                        println("Checking '${bone.name}':")
                        println("  baseInv: $baseTransformInv")
                        println("  invTransform: $invTransformD")
                        println("  physics: $physicsTransform")
                        println("  bindPose: ${bone.bindPose}")
                        println("  relPose: ${bone.relativeTransform}")
                        println("  target: ${initialPose[boneId]}")
                    }

                    // physicsRot should be the same as baseRotation before physics sets in
                    // why is it changing soo much after just one frame???
                    if (firstFrame) {
                        println("Checking '${bone.name}':")
                        println("  physics: $physicsRot")
                        println("  base:    $baseRotInv")
                        assertEquals(
                            Matrix3d(), Matrix3d()
                                .rotation(physicsRot.mul(baseRotInv, Quaterniond())), 0.01
                        )
                    }

                    // todo what is the correct local rotation???
                    //  * baseRot^-1 *


                    dstMatrix.identity()
                        // .rotate(Quaternionf(baseRotInv.mul(physicsRot)))
                        .translate(Vector3f(physicsPos - bindPos))

                    invTransform.mul(dstMatrix, dstMatrix)

                    if (boneId == 160) {
                        assertEquals(dstMatrix, target, 0.9)
                    }
                }
            }
            firstFrame = false
            AnimationCache.invalidate(ragdollAnimation)
        }
    }
    scene.add(animationUpdateComponent)
    animationUpdateComponent.onUpdate()

    Entity("Floor", scene)
        .setPosition(0.0, -15.0, 0.0)
        .setScale(250.0, 15.0, 250.0)
        .add(Rigidbody().apply { mass = 0.0 })
        .add(BoxCollider())
        .add(MeshComponent(DefaultAssets.flatCube))

    // make space, so we can see the bones properly
    val state = AnimationState(ragdollAnimation.ref)
    for (meshComponentI in meshComponents) {
        meshComponentI.animations = listOf(state)
    }

    // add rotational constraints, bones can't just rotate along each other freely
    // to do (hard) if it is a knee or elbow (by name), only allow rotation along one axis

    val physics = BulletPhysics()
    Systems.registerSystem(physics)

    // create ragdoll
    // adjust ragdoll to start on specific animation state/frame (e.g., walking)
    // make skin stick to ragdoll physics
    // test physics for ragdoll -> looks very weird, and there is two parts for some reason :/
    testSceneWithUI("Ragdoll Test", scene) {
        it.renderView.renderMode = RenderMode.PHYSICS
    }
}