package me.anno.tests.physics

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.ecs.Entity
import me.anno.ecs.components.audio.AudioComponent
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.Systems
import me.anno.engine.EngineBase.Companion.showRedraws
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.OS.music
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.maps.KeyPairMap
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector3d
import org.joml.Vector3f

// physics demo: dominos like https://www.youtube.com/watch?v=YZxky260O-4
// place / program / draw dominos
// done click to start it, maybe red domino or impulse, or falling ball :)
//  somehow dragging while playing is supported :)
fun main() {

    // todo why are all bricks unstable?

    OfficialExtensions.initForTests()

    registerCustomClass(BoxCollider())
    registerCustomClass(AudioComponent())

    val inch = 1f // 2.54e-2f
    val width = 17f / 16f * inch
    val height = 35f / 16f * inch
    val thickness = 7f / 16f * inch

    // todo audio debug animation to show playing sources

    val scene = Entity("Scene")

    disableRenderDoc()
    testSceneWithUI("Dominos", scene) {
        showRedraws = false
        it.renderView.renderMode = RenderMode.PHYSICS

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
            // make domino sound on every contact :3
            val contacts = KeyPairMap<Any, Any, Unit>()
            override fun step(dt: Long, printSlack: Boolean) {
                super.step(dt, printSlack)
                /*
                    val dispatcher = bulletInstance?.dispatcher ?: return
                    val numManifolds = dispatcher.numManifolds
                    for (i in 0 until numManifolds) {
                        val contactManifold = dispatcher.getManifoldByIndexInternal(i) ?: break
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
                */
            }
        }

        Systems.registerSystem(CollisionListenerPhysics().apply {
            // updateInEditMode = true
            fixedStep = 1.0 / 10e3
            maxSubSteps = 1000
            synchronousPhysics = false // todo remove jitter from async physics...
        })

        val density = 1.0
        val mass1 = width * height * thickness * density

        val halfExtends1 = Vector3d(width * 0.5, height * 0.5, thickness * 0.5)
        val mesh = flatCube.scaled(Vector3f(halfExtends1)).front.ref

        val dominos = Entity("Dominos")
        scene.add(dominos)
        fun add(x: Float, z: Float): Entity {
            // todo why are smaller bricks unstable?
            val domino = Entity()
            domino.add(MeshComponent(mesh).apply {
                isInstanced = true
            })
            domino.add(Rigidbody().apply {
                mass = mass1
                friction = 0.9
                restitution = 0.5
            })
            domino.add(BoxCollider().apply {
                halfExtends = halfExtends1
                margin = 0.0
            })
            domino.setPosition(x.toDouble(), halfExtends1.y, z.toDouble())
            dominos.add(domino)
            return domino
        }

        val floorHalfSize = 50.0 * inch
        val floors = Entity("Floors")
        scene.add(floors)
        val halfNumFloors = 1
        for (z in -halfNumFloors..halfNumFloors) {
            val floor = Entity()
            floor.add(Rigidbody().apply {
                mass = 0.0
                friction = 0.9
                restitution = 0.5
            })
            floor.add(BoxCollider().apply {
                halfExtends.set(floorHalfSize)
                margin = 0.0
            })
            floor.add(MeshComponent(flatCube.scaled(Vector3f(floorHalfSize.toFloat())).front))
            floor.setPosition(0.0, -floorHalfSize, 2 * z * floorHalfSize)
            floors.add(floor)
        }

        // todo starting structure, and image support like in video

        val spacing = height * 0.7f
        val di = ((floorHalfSize * (halfNumFloors * 2 + 1)) / spacing - 2).toInt()
        for (i in -di until di) {
            add(0f, spacing * i)
        }
        add(0f, spacing * di).apply {
            rotation = rotation.rotateX((-20.0).toRadians())
        }
    }
}