package me.anno.games.spiderplaguehotel

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.DefaultAssets.plane
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.games.spiderplaguehotel.SpiderLogic.Companion.sizes
import me.anno.games.spiderplaguehotel.SpiderLogic.Companion.spiderWorldScale
import me.anno.tests.utils.createSpider
import org.joml.Vector2f
import kotlin.random.Random

// basic mechanics [I]:
//  - one or two rooms -> later each room will have its own infestation,
//         maybe spiders could dig tunnels to other rooms or migrate if doors are left open
//  - spiders live there
//  - spiders have neural networks controlling them
//  - spider traps (claps) warn spiders, and then (try to) kill them


// todo visualization [II]:
//  - spiders on wall
//  - graphs for numbers of spiders, upgrades and so on
//  - clock in corner

// todo spiders spawn randomly over time, not on death

// todo gamification [III]:
//  - upgrade traps using made money?
//  - guests dislike spiders to different degree?
//  - guess leave without paying when too many spiders??
//  - faster traps,
//  - moving traps,
//  - laser traps (laser-pointer, might follow spider a bit),
//  - spawning trap on spider vs randomly on wall

// todo additional gamification [IV]
//  - beautify/decorate rooms
//  - add more rooms
//  - upgrade rooms
//  - upgrade & hire staff

// todo beautification [V]:
//  - spiders build nets ^^

// todo publishing/controls [VI]:
//  - steam??
//  - PlayStore?
//  - itch.io?

// todo advertising [VII?]:
//  - make YT video about it?

/**
 * Spider-Evolution Hotel game inspired by Carykh
 * https://www.youtube.com/watch?v=SBfR3ftM1cU
 *
 * just a prototype at the moment
 * */
fun main() {

    // todo optimize spiders when far enough away
    // done optimize base spider mesh

    // start with a simple scene and a few randomly-initialized spiders
    val scene = Entity("Scene")
    Entity("Floor", scene)
        .add(MeshComponent(plane))
        .setScale(spiderWorldScale.toFloat())

    val random = Random(Time.nanoTime)
    val spiders = Entity("Spiders", scene)
    val traps = Entity("Traps", scene)
    for (i in 0 until 500) {
        val network = FullyConnectedNN(sizes)
        network.initRandomly(random)
        createSpider(spiders, 0.0)
            // .add(MeshComponent(spiderMesh))
            .add(SpiderLogic(network, traps).apply {
                position.set(random.nextFloat(), random.nextFloat())
            })
    }

    // todo can we design a Fliegenklatsche?
    //  - rod
    //  - 4:3 area in the shape of a grid
    val trapMesh = flatCube

    val redMaterial = Material.diffuse(0xff0000)
    for (i in 0 until 75) {
        val pos = Vector2f(random.nextFloat(), random.nextFloat())
        SpiderLogic.trapPositions.add(pos)
        val trap = SpiderTrap(i, random, spiders)
        Entity(traps).add(trap)
            .setPosition((pos.x * 2f - 1f) * spiderWorldScale, 5.0, (pos.y * 2f - 1f) * spiderWorldScale)
            .add(MeshComponent(trapMesh, redMaterial))
            .setScale(10f, 0.4f, 10f)
    }

    testSceneWithUI("Spider Plague Hotel", scene)
}