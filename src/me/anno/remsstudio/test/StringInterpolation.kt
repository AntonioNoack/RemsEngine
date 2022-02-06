package me.anno.remsstudio.test

import me.anno.remsstudio.animation.AnimatedProperty
import me.anno.animation.Interpolation
import org.apache.logging.log4j.LogManager

fun test(a: String, b: String, t: Double, expected: String){
    val logger = LogManager.getLogger("StringTest")
    val p = AnimatedProperty.string()
    p.isAnimated = true
    val int = Interpolation.LINEAR_BOUNDED
    p.addKeyframe(0.0, a)?.interpolation = int
    p.addKeyframe(1.0, b)?.interpolation = int
    val value = p.getAnimatedValue(t)
    if(value != expected){
        logger.warn("Got '$value', expected '$expected'")
    } else {
        logger.info("'$value' worked")
    }
}

fun test(a: String, b: String, c: String, d: String, t: Double, expected: String){
    val logger = LogManager.getLogger("StringTest")
    val p = AnimatedProperty.string()
    p.isAnimated = true
    val int = Interpolation.LINEAR_BOUNDED
    p.addKeyframe(0.0, a)?.interpolation = int
    p.addKeyframe(1.0, b)?.interpolation = int
    p.addKeyframe(2.0, c)?.interpolation = int
    p.addKeyframe(3.0, d)?.interpolation = int
    val value = p.getAnimatedValue(t)
    if(value != expected){
        logger.warn("Got '$value', expected '$expected'")
    } else {
        logger.info("'$value' worked")
    }
}

fun test(a: Double, b: Double, c: Double, d: Double, t: Double, expected: Double){
    val logger = LogManager.getLogger("StringTest")
    val p = AnimatedProperty.double()
    p.isAnimated = true
    val int = Interpolation.LINEAR_BOUNDED
    p.addKeyframe(0.0, a)?.interpolation = int
    p.addKeyframe(1.0, b)?.interpolation = int
    p.addKeyframe(2.0, c)?.interpolation = int
    p.addKeyframe(3.0, d)?.interpolation = int
    val value = p.getAnimatedValue(t)
    if(value != expected){
        logger.warn("Got '$value', expected '$expected'")
    } else {
        logger.info("'$value' worked")
    }
}

fun main(){
    test("012", "345", "678", "9ab", 0.0, "012")
    test("012", "345", "678", "9ab", 1.0, "345")
    test("012", "345", "678", "9ab", 1+3.0/8, "348")
    test("012", "345", "678", "9ab", 1+6.0/8, "378")
    test("012", "345", "678", "9ab", 2.0, "678")
    test("012", "345", "678", "9ab", 3.0, "9ab")
    test("012", "345", "678", "9ab", 13.0, "9ab")
    test(12.0, 345.0, 678.0, 900.0, 1.5, (345.0+678.0)/2.0)
    test("Hallo", "Hallo Welt", 0.51, "Hallo We")
    test("Hallo Welt", "Hallo", 0.49, "Hallo We")
    test("Hallo Welt", "Hallo", 0.99, "Hallo")
}