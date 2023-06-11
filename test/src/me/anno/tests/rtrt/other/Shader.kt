package me.anno.tests.rtrt.other

import me.anno.maths.Maths.pow
import me.anno.utils.types.Floats.f3
import me.anno.utils.types.Floats.step
import kotlin.math.max
import kotlin.math.sqrt

// for my thesis:
// testing the fresnelConductorExact method from Mitsuba

fun fresnelConductorExact(cosThetaI: Float, eta: Float, k: Float): Float {
    /* Modified from "Optics" by K.D. Moeller, University Science Books, 1988 */

    val cosThetaI2 = cosThetaI * cosThetaI
    val sinThetaI2 = 1 - cosThetaI2
    val sinThetaI4 = sinThetaI2 * sinThetaI2

    val temp1 = eta * eta - k * k - sinThetaI2
    val a2pb2 = sqrt(max(temp1 * temp1 + 4 * k * k * eta * eta, 0f))
    val a = sqrt(max(0.5f * (a2pb2 + temp1), 0f))

    val term1 = a2pb2 + cosThetaI2
    val term2 = 2 * a * cosThetaI;

    val Rs2 = (term1 - term2) / (term1 + term2);

    val term3 = a2pb2 * cosThetaI2 + sinThetaI4
    val term4 = term2 * sinThetaI2;

    val Rp2 = Rs2 * (term3 - term4) / (term3 + term4);

    return 0.5f * (Rp2 + Rs2);
}

fun main() {
    // > the approximate behaviour is
    // mix((1-x)^5, 1, 0.96)
    for (w in 0f..1f step 0.01f) {
        println(fresnelConductorExact(w, 1.5f, 0f).f3() + " vs ${(0.04f + 0.96f * pow(1f - w, 5f)).f3()}")
    }
}