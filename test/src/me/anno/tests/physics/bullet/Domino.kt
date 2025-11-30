package me.anno.tests.physics.bullet

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.ecs.Entity
import me.anno.ecs.components.audio.AudioComponent
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.Systems
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.maths.Maths.clamp
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.OS.music
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.maps.KeyPairMap
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector3f

// physics demo: dominos like https://www.youtube.com/watch?v=YZxky260O-4
// place / program / draw dominos
// done click to start it, maybe red domino or impulse, or falling ball :)
//  somehow dragging while playing is supported :)
fun main() {

    // todo why are all bricks unstable?
    //  smaller bricks fall faster -> less stable
    //  no margin -> fake collisions -> rocking/dancing dominos

    OfficialExtensions.initForTests()

    registerCustomClass(BoxCollider())
    registerCustomClass(AudioComponent())

    val inch = 0.1f//2.54e-2f
    val width = 17f / 16f * inch
    val height = 35f / 16f * inch
    val thickness = 7f / 16f * inch

    // todo audio debug animation to show playing sources

    val scene = Entity("Scene")

    disableRenderDoc()
    testSceneWithUI("Dominos", scene) {

        val audiosEntity = Entity("Audios")
        scene.add(audiosEntity)
        var audioIndex = 0
        val audios = createArrayList(16) {
            AudioComponent().apply {
                source = music.getChild("domino-click.wav")
                maxDistance = inch * 250f
                val helper = Entity()
                helper.add(this)
                audiosEntity.add(helper)
            }
        }

        class CollisionListenerPhysics : BulletPhysics() {
            // make clack sound on every contact :3
            val contacts = KeyPairMap<Any, Any, Unit>()
            override fun step(printSlack: Boolean) {
                super.step(printSlack)
                if (false) {
                    val dispatcher = bulletInstance.dispatcher
                    for (i in 0 until dispatcher.numManifolds) {
                        val contactManifold = dispatcher.getManifold(i)
                        if (contactManifold.numContacts < 1) continue
                        val a = contactManifold.body0
                        val b = contactManifold.body1
                        contacts.getOrPut(a, b) { _, _ ->
                            val audio = audios[(audioIndex++) % (audios.size - 1)]
                            audio.stop()
                            // to do choose source randomly from random set of sounds
                            // place audio at the correct position
                            val pos = contactManifold.getContactPoint(0).positionWorldOnA
                            audio.entity!!.setPosition(pos.x, pos.y, pos.z)
                            audio.start()
                        }
                    }
                }
            }
        }

        Systems.registerSystem(CollisionListenerPhysics().apply {
            // updateInEditMode = true
            stepsPerSecond = 60f
            synchronousPhysics = false
            maxSubSteps = 2
            enableDebugRendering = true
        })

        val density = 1.0
        val mass1 = width * height * thickness * density

        val margin1 = 0.1f * inch
        val halfExtents1 = Vector3f(width * 0.5f, height * 0.5f, thickness * 0.5f)
        val mesh = flatCube.scaled(halfExtents1).front.ref

        // todo spawn dominos as inactive?

        val dominos = Entity("Dominos")
        scene.add(dominos)
        fun add(x: Float, z: Float): Entity {
            // todo why are smaller bricks unstable?
            return Entity(dominos)
                .setPosition(x.toDouble(), (halfExtents1.y).toDouble(), z.toDouble())
                .add(MeshComponent(mesh))
                .add(DynamicBody().apply {
                    mass = mass1.toFloat()
                    friction = 0.3f
                    restitution = 0.0f
                    linearSleepingThreshold = 0.0f
                    angularSleepingThreshold = 0.0f
                })
                .add(BoxCollider().apply {
                    halfExtents = halfExtents1
                    roundness = margin1
                })
        }

        val floorHalfSize = 10.0 * inch
        val halfNumFloors = 5

        Entity("Floor", scene)
            .add(InfinitePlaneCollider())
            .add(StaticBody().apply {
                friction = 0.9f
                restitution = 0.0f
            })
        Entity("Floor Visuals", scene)
            .add(MeshComponent(flatCube.front))
            .setPosition(0.0, -floorHalfSize, 0.0)
            .setScale(
                floorHalfSize.toFloat(), floorHalfSize.toFloat(),
                floorHalfSize.toFloat() * (halfNumFloors * 2 + 1)
            )

        // todo image support like in video

        val spacingZ = height * 0.7f
        val spacingX = width * 1.2f
        val di = ((floorHalfSize * (halfNumFloors * 2 + 1)) / spacingZ - 2).toInt()
        for (i in -di until di) {
            val k = clamp(-(i - di), 1, 15)
            val offsetX = (k - 1) * 0.5f * spacingX
            for (j in 0 until k) {
                add(j * spacingX - offsetX, spacingZ * i)
            }
        }
        add(0f, spacingZ * di).apply {
            rotation = rotation.rotateX((-20f).toRadians())
        }
    }
}