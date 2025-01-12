package me.anno.tests.physics

import me.anno.maths.Maths.factorial
import me.anno.maths.Permutations.generatePermutations
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Matrices.sampleDistanceSquared
import org.joml.Matrix3d

// trying to find the solution brute-force...
fun main() {
    val matrices = """
  baseInv: [[0.9999994667732561 -3.2526065174565123E-19 0.001032692211463891 -9.495909168587408] [-9.855168444611612E-4 0.2987925862598817 0.9543175672451389 28.286459335018638] [-3.0856077667373286E-4 -0.9543180761130593 0.29879242693568386 55.98045544548641]]
  invTransform: [[0.9999338625145275 -0.011501322371094283 -1.1767592295845143E-4 -8.916057572116808] [0.011501677779979477 0.9997931164461928 0.016776169744422777 3.409828173945268] [-7.529655679769101E-5 -0.016776413277039603 0.9998592530545618 0.6130295497790262]]
  physics: [[0.999999466773256 -9.855168444611612E-4 -3.08560776673733E-4 9.541054260070315] [-1.0842021724855044E-19 0.2987925862598818 -0.9543180761130596 44.97137619982424] [0.001032692211463891 0.9543175672451392 0.29879242693568386 -43.71099485065618]]
  bindPose: [[-4.1530438E-7 9.639043E-7 -1.0000001 -2.193451E-5] [-0.63431334 -0.7730762 -4.761216E-7 43.460014] [-0.7730761 0.63431334 9.536743E-7 -48.56196]]
  relPose: [[0.9903069 0.13889652 0.0 12.411919] [-0.13889652 0.9903069 0.0 0.0] [0.0 0.0 1.0 0.0]]
  target: [[0.99989635 -0.011388577 0.008822608 1.0638313] [0.0144061595 0.7904448 -0.6123645 -18.295223] [2.2272434E-7 0.61242795 0.7905269 -37.964104]]
   """.trimIndent().split('\n').asSequence()
        .filter { it.isNotEmpty() }
        .map { line -> line.substring(line.indexOf(": ") + 2) }
        .map { line -> line.replace('[', ' ').replace(']', ' ') }
        .map { line -> line.split(' ').filter { it.isNotEmpty() }.map { it.toDouble() } }
        .map { line ->
            Matrix3d(
                line[0], line[4], line[8],
                line[1], line[5], line[9],
                line[2], line[6], line[10]
            )
        }.toList()

    // iterate over all combinations of MTB, and their inverse, and check what's closest do dInv

    val dInv = -matrices.last()
    val names = "ABCDEFGHIJLK"
    val byName0 = matrices.subList(0, matrices.lastIndex)
    val byName1 =
        byName0.withIndex().map { (i, m) -> m to "${names[i]}" } +
                byName0.withIndex().map { (i, m) -> -m to "-${names[i]}" }
    val seen = HashSet<String>()
    val identity = Matrix3d()
    var bestScore = Double.POSITIVE_INFINITY
    println("#permutations: ${byName1.size} -> ${byName1.size.factorial()}")
    generatePermutations(byName1) { permutation0 ->
        for (len in 1..3) {
            val permutation1 = permutation0.subList(0, len)
            val permutation = permutation1.map { it.first }
            val identifier = permutation1.joinToString(",") { it.second }
            if (seen.add(identifier)) {
                val numOptions = 1
                for (flags in 0 until numOptions.pow(len - 1)) {
                    var result = Matrix3d(permutation[0])
                    var flagsRem = flags
                    for (i in 1 until permutation.size) {
                        val option = flagsRem % numOptions
                        flagsRem /= numOptions
                        val next = permutation[i]
                        when (option) {
                            0 -> result.mul(next)
                            //1 -> result.rotate(next.getUnnormalizedRotation(Quaternionf()))
                            else -> throw NotImplementedError()
                        }
                    }
                    result *= dInv
                    val score = result.sampleDistanceSquared(identity)
                    if (score <= bestScore) {
                        println("Best[$score]: $result, $identifier, $flags")
                        bestScore = score
                    }
                }
            }
        }
    }
    println("Best score: $bestScore")
}

fun Int.pow(power0: Int): Int {
    var result = 1
    var px = this
    var power = power0
    while (power != 0) {
        if (power.hasFlag(1)) {
            result *= px
        }
        power = power ushr 1
        px *= px
    }
    return result
}

operator fun Matrix3d.times(other: Matrix3d): Matrix3d {
    return mul(other, Matrix3d())
}

operator fun Matrix3d.unaryMinus(): Matrix3d {
    return invert(Matrix3d())
}