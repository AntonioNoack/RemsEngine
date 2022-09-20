package me.anno.ecs.components.mesh.sdf

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.mesh.sdf.shapes.SDFBox
import me.anno.ecs.components.mesh.sdf.shapes.SDFSphere
import me.anno.ecs.components.physics.BulletPhysics
import me.anno.ecs.components.physics.Rigidbody
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.ui.debug.TestStudio.Companion.testUI
import org.joml.Vector3d
import java.util.*

fun main() {

    // todo create reliable sdf physics

    // done -> create scene with default mesh
    // todo -> compare the functions of their colliders with our custom implementation

    fun test0() {
        val scene = Entity()
        val physics = BulletPhysics()
        scene.add(physics)
        physics.updateInEditMode = true

        val instance = Entity()
        instance.add(Rigidbody().apply { mass = 0.0 })
        instance.add(SphereCollider())
        instance.scale = Vector3d(20.0)
        instance.position = Vector3d(0.0, -20.0, 0.0)
        instance.add(SDFSphere())

        val projectile1 = Entity()
        projectile1.add(Rigidbody().apply { mass = 1.0 })
        projectile1.add(BoxCollider())
        projectile1.position = Vector3d(-3.3, 15.0, 0.0)
        projectile1.scale = Vector3d(3.0)
        projectile1.add(SDFBox())

        val projectile2 = Entity()
        projectile2.add(Rigidbody().apply { mass = 1.0 })
        projectile2.add(SDFCollider().apply { isConvex = true })
        projectile2.position = Vector3d(+3.3, 15.0, 0.0)
        projectile2.scale = Vector3d(3.0)
        projectile2.add(SDFBox())

        scene.add(instance)
        scene.add(projectile1)
        scene.add(projectile2)
        scene.add(SkyBox()) // beauty ^^

        testUI {
            testScene(scene)
        }
    }

    fun test1() {

        val random = Random(1234L)
        val samples = Array(64) {
            javax.vecmath.Vector3d(random.nextGaussian(), random.nextGaussian(), random.nextGaussian())
        }

        println(samples.toList())

        fun print(shape: CollisionShape) {

            // convex sdf collisions shapes look quite good now :)
            // todo scaled convex shapes don't work correctly, looks as if it wasn't scaled

            println("${shape.isConvex}, ${shape.isConvex}, ${shape.isCompound}, ${shape.isPolyhedral}, ${shape.isInfinite}")
            val min = javax.vecmath.Vector3d()
            val max = javax.vecmath.Vector3d()
            val tr = Transform()
            tr.setIdentity()
            shape.getAabb(tr, min, max)
            println("$min - $max")
            println(shape.margin)
            println(shape.shapeType)
            val r = DoubleArray(1)
            shape.getBoundingSphere(javax.vecmath.Vector3d(), r)
            println(r[0])
            shape as ConvexShape
            println("${shape.numPreferredPenetrationDirections}") // only used for hull building
            println(samples.map { shape.localGetSupportingVertex(it, javax.vecmath.Vector3d()) })
            println(samples.map { shape.localGetSupportingVertexWithoutMargin(it, javax.vecmath.Vector3d()) })
            println(samples.map {
                val out = arrayOf(javax.vecmath.Vector3d())
                shape.batchedUnitVectorGetSupportingVertexWithoutMargin(
                    arrayOf(it),
                    out,
                    1
                )
                out[0]
            })
            println()
        }

        val shape0 = BoxCollider().createBulletShape(Vector3d(1.0))
        val shape1c = SDFCollider()
        val entity = Entity()
        entity.add(SDFBox())
        entity.add(shape1c)
        val shape1 = shape1c.createBulletShape(Vector3d(1.0))
        print(shape0)
        print(shape1)
    }

    test1()
    test0()

}