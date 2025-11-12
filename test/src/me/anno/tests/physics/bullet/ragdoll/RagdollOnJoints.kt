package me.anno.tests.physics.bullet.ragdoll

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
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
import me.anno.ecs.components.collider.ConvexCollider
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Systems
import me.anno.engine.DefaultAssets
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.graph.octtree.KdTree
import me.anno.graph.octtree.KdTreeNearest.findNearest
import me.anno.graph.octtree.OctTree
import me.anno.input.Input
import me.anno.maths.Maths.PIf
import me.anno.maths.geometry.convexhull.ConvexHulls
import me.anno.utils.OS
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import org.joml.Matrix4x3
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

class BonePos(val position: Vector3d, val boneId: Int)
class BonePosTree() : OctTree<BonePos>(16) {
    override fun createChild(): KdTree<Vector3d, BonePos> = BonePosTree()
    override fun getMin(data: BonePos): Vector3d = data.position
    override fun getMax(data: BonePos): Vector3d = data.position
}

fun List<Vector3d>.join(bonePos: Vector3d): FloatArray {
    val dst = FloatArray(size * 3)
    val tmp = Vector3d()
    for (i in indices) {
        this[i].sub(bonePos, tmp)
            .get(dst, i * 3)
    }
    return dst
}

/**
 * done create shapes for joints, not for bones
 * done extend the base sphere shape with a convex hull of the closest vertices
 * */
fun main() {

    OfficialExtensions.initForTests()

    // load fox
    val source = OS.downloads.getChild("3d/azeria/scene.gltf")
    val totalMass = 10.0

    // val source = OS.downloads.getChild("3d/Talking On Phone.fbx")
    val joinedMesh = MeshCache.getEntry(source).waitFor() as Mesh
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
    ragdollAnimation.unlinkPrefab()

    val initialPose = ragdollAnimation.frames[0]
        .map { Matrix4x3f(it) } // cloned, because we manipulate exactly these matrices

    val bonePositions = bones.map {
        val pos = Vector3d(it.bindPosition)
        initialPose[it.index].transformPosition(pos)
        meshMatrix.transformPosition(pos)
    }

    val entities = ArrayList<Entity?>()
    val rigidbodies = ArrayList<DynamicBody?>()
    val baseTransformInvs = Array(bones.size) { Matrix4x3() }
    val roots = ArrayList<Pair<Bone, DynamicBody>>()
    var isRootBone = true

    val sphereRadii = DoubleArray(bones.size) { boneId ->
        val bonePos = bonePositions[boneId]
        val minDistSq = bones.minOf { otherBone ->
            val otherBoneId = otherBone.index
            if (otherBoneId != boneId) {
                val otherPos = bonePositions[otherBoneId]
                otherPos.distanceSquared(bonePos)
            } else Double.POSITIVE_INFINITY
        }
        0.5 * sqrt(minDistSq)
    }

    val boneTree = BonePosTree()
    for (bone in bones) {
        val boneId = bone.index
        boneTree.add(BonePos(bonePositions[boneId], boneId))
    }

    val hullVertices = Array(bones.size) {
        ArrayList<Vector3d>()
    }
    val positions = joinedMesh.positions!!
    forLoopSafely(positions.size, 3) { idx ->
        val v = Vector3d(positions, idx)
        val bone = boneTree.findNearest(v)!!
        hullVertices[bone.boneId].add(v)
    }

    for (bone in bones) {

        // todo this is all based on positions ->
        //  apply the first frame of the animation as start transforms?

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
            continue
        }

        val parentPos = bonePositions[parent.index]
        if (isRootBone && parentPos.distance(bonePos) < 0.001) {
            entities.add(null)
            rigidbodies.add(null)
            continue
        }

        // todo skip the first one, or make it artificially smaller???
        if (isRootBone) {
            isRootBone = false
            entities.add(null)
            rigidbodies.add(null)
            continue
        }

        val radius = sphereRadii[bone.index] * 0.95 // safety-margin
        val dynamicBody = DynamicBody().apply {
            mass = radius.pow(3).toFloat() * 4f / PIf
            friction = 0.7f
            // todo why do we need high friction to prevent explosions?
           // linearDamping = 0.7
           // angularDamping = 0.7
            rigidbodies.add(this)
        }

        val collider = SphereCollider().apply {
            this.radius = radius.toFloat()
            roundness = 0.1f
        }

        val entity = Entity(bone.name, scene)
            .add(dynamicBody)
            .add(collider)

        // also add convex collider of closest vertices
        // todo there is a weird offset, fix that
        val hullVerticesI = hullVertices[bone.index]
        if (false && hullVerticesI.size >= 3) {
            hullVerticesI.add(bonePos)
            // generate convex hull shape
            val hull = ConvexHulls.calculateConvexHull(hullVerticesI)
            if (hull != null) {
                val positions = hull.vertices.join(bonePos)
                entity.add(ConvexCollider().apply {
                    points = positions
                })
            }
        }

        entity.add(object : Component(), OnUpdate {
            override fun onUpdate() {
                LineShapes.drawSphere(null, radius, bonePos, -1)
            }
        })

        val parentBody = rigidbodies[parent.index]
        if (parentBody != null) {
            // todo use spring constraints instead: we only want to keep the distance, not necessarily the bone positions
            val constraint =
                if (true) PointConstraint()
                else ConeTwistConstraint()
            entity.add(constraint.apply {
                other = parentBody
                val parentPos = bonePositions[bone.parentIndex]
                val delta = parentPos - bonePos
                selfPosition.set(delta).mul(0.5)
                otherPosition.set(delta).mul(-0.5)
                breakingImpulseThreshold = 50f
            })
            // disableCollisionsBetweenLinked = false
            (constraint as? PointConstraint)?.apply {
                damping = 0.5f
            }
            (constraint as? ConeTwistConstraint)?.apply {
                // twist = 0.1
                // angleX = 0.1
                // angleY = 0.1
            }
        } else {
            roots.add(bone to dynamicBody)
        }

        entity.position = bonePos
        entity.validateTransform()
        entities.add(entity)

        baseTransformInvs[bone.index]
            .translation(-bonePos)
    }

    val totalBoneMass = rigidbodies.filterNotNull().sumOf { it.mass.toDouble() }
    val massCorrectionFactor = (totalMass / totalBoneMass).toFloat()
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
            val delta = bonePositions[root2Len.index] - bonePositions[root1Len.index]
            entity.add(PointConstraint().apply {
                other = root2
                selfPosition.set(delta).mul(0.5)
                otherPosition.set(delta).mul(-0.5)
                breakingImpulseThreshold = 50f
                // disableCollisionsBetweenLinked = false
                damping = 0.5f
            })
        }
    }

    val animationUpdateComponent = object : Component(), OnUpdate {

        val physicsRot = Quaternionf()
        val baseRotInv = Quaternionf()
        val physicsPos = Vector3f()

        override fun onUpdate() {
            val invTransformD = sampleComponent.transform!!.globalTransform.invert(Matrix4x3())
            val invTransform = Matrix4x3f().set(invTransformD)
            for (boneId in bones.indices) {

                val dstMatrix = ragdollAnimation.frames[0][boneId]
                if (Input.isShiftDown) {
                    dstMatrix.set(initialPose[boneId])
                    continue
                }

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
                val bone = bones[boneId1]

                val baseTransformInv = baseTransformInvs[boneId1]

                physicsPos.set(ragdollT.localPosition)

                val bindPos = bone.bindPosition // baseTransform.getTranslation(Vector3d())
                physicsTransform.getUnnormalizedRotation(physicsRot)
                baseTransformInv.getUnnormalizedRotation(baseRotInv)

                dstMatrix.identity()
                    .translate(physicsPos)
                    .rotate(baseRotInv.premul(physicsRot).normalize())
                    .translate(-bindPos.x, -bindPos.y, -bindPos.z)

                invTransform.mul(dstMatrix, dstMatrix)
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
    testSceneWithUI("Ragdoll Joint Test", scene, RenderMode.LINES)
}