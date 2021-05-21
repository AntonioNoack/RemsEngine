package me.anno.utils.test

import me.anno.animation.AnimatedProperty
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import java.util.*

fun main(){

    val logger = LogManager.getLogger("TestBinarySearchPerformance")

    // java 1.8.0 build 112
    // sequential 50 cycles vs random 220 cycles
    // random with values: 35-38ns slower -> binarySearch seems to be already optimized for sequential calls :)
    // random.nextDouble() = 11.5ns
    // small calc = 1.5ns
    // access + loop + small calc = 15ns
    val times = 1e8
    val pos = AnimatedProperty.pos()
    pos.addKeyframe(1.0, Vector3f())
    pos.addKeyframe(2.0, Vector3f(1f, 1f, 1f))
    pos.addKeyframe(5.0, Vector3f())
    for(i in 0 until 1000){
        pos.addKeyframe(i+0.5, Vector3f(i.toFloat(), 0f, 0f))
    }
    val random = Random()
    fun seq(){
        val t0 = System.nanoTime()
        for(i in 0 until times.toLong()){
            val t = i/1e9
            pos[t]
        }
        val t1 = System.nanoTime()
        logger.info("Sequential: ${((t1-t0)/times)} ns/try")
    }
    fun rnd(){
        val t2 = System.nanoTime()
        for(i in 0 until times.toLong()){
            val t = random.nextDouble() * 10
            pos[t]
        }
        val t3 = System.nanoTime()
        logger.info("Random:    ${((t3-t2)/times)} ns/try")
    }
    seq()
    rnd()
    seq()
    rnd()
}

