package me.anno.utils.test

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS
import me.anno.utils.maths.Optimization.simplexAlgorithm
import org.apache.logging.log4j.LogManager
import kotlin.math.abs

fun main() {

    val logger = LogManager.getLogger("MMfO")

    loadMatrix()

    /*vectors.forEachIndexed { index, vector ->
        LOGGER.info("'${index+1}' ${vector.joinToString(" ")}")
    }

    return*/

    // error function
    // choose a valid starting point for optimization
    val average = DoubleArray(dims) { index ->
        // vectors.map { it[index] }.sorted()[vectorCount/2] // median
        vectors.map { it[index] }.sum() / vectorCount // average
    }
    val median = DoubleArray(dims) { index ->
        vectors.map { it[index] }.sorted()[vectorCount / 2] // median
        // vectors.map { it[index] }.sum() / vectorCount // average
    }

    /*for(i in 0 until vectorCount){
        LOGGER.info("trivial solution $i: ${maxDistance(vectors[i])}")
    }*/

    logger.info("average solution: ${maxDistance(average)}")
    logger.info("median solution: ${maxDistance(median)}")

    // use the median vector as a starting point
    // or the average? idk, average is simpler

    logger.info("average: ${average.joinToString()}")

    logger.info(
        maxDistance(
            "3878.09131351636 3061.17267866391 3126.88511394304 3911.66869377691 4029.84779962704 2626.88975713788 3338.29690477943 4158.66690624099 1556.08411829276 2723.49734732109 2484.52391188888 3098.30790270422 3360.56684729076 2834.89909394725 2996.04120238281 3214.85303933428 2790.2150861753 4482.14310397302 4497.95315703025 4761.62083782121"
                .split(' ').map { it.toDouble() }.toDoubleArray()
        )
    )

    var ctr = 0
    val t0 = System.nanoTime()
    val solution = simplexAlgorithm(average, 1.0, 0.0, 1000000000) { potentialSolution ->
        val distance = maxDistance(potentialSolution)
        if (++ctr % 5000 == 0) logger.info(distance)
        distance
    }
    val t1 = System.nanoTime()
    logger.info((t1 - t0) * 1e-9)

    logger.info(solution.joinToString())
    logger.info("found solution: ${maxDistance(solution)}")


    solution.forEachIndexed { index, _ ->
        solution[index] += Math.random() * 100
    }
    val solution2 = simplexAlgorithm(solution, 1.0, 0.0, 100000) { potentialSolution ->
        val distance = maxDistance(potentialSolution)
        if (++ctr % 5000 == 0) logger.info(distance)
        distance
    }
    logger.info("tested ${solution2.joinToString()}")
    logger.info(": ${maxDistance(solution2)}")

}

val vectorCount = 200
val dims = 20

val vectors = Array(vectorCount) {
    DoubleArray(dims)
}

fun maxDistance(x: DoubleArray): Double {

    return IntArray(vectorCount) { it }.maxOf {
        var sum = 0.0
        val vector = vectors[it]
        for (i in 0 until dims) {
            sum += abs(x[i] - vector[i])
        }
        sum
    }

}

fun loadMatrix() {
    // load matrix
    val srcFile = getReference(OS.downloads, "MMfO.3.data.txt")
    val reader = srcFile.inputStream().bufferedReader()
    for (lineIndex in 0 until vectorCount) {
        val line = reader.readLine()!!// ?: break
        val values = line.split(',')
        val vector = vectors[lineIndex]
        for (i in 0 until dims) {
            vector[i] = values[i].trim().toDouble()
        }
    }
}