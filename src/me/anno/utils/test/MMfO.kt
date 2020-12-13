package me.anno.utils.test

import me.anno.utils.Optimization.simplexAlgorithm
import me.anno.utils.OS
import java.io.File
import kotlin.math.abs

fun main() {

    loadMatrix()

    // error function
    // choose a valid starting point for optimization
    val average = DoubleArray(dims){ index ->
        // vectors.map { it[index] }.sorted()[vectorCount/2] // median
        vectors.map { it[index] }.sum() / vectorCount // average
    }
    val median = DoubleArray(dims){ index ->
        vectors.map { it[index] }.sorted()[vectorCount/2] // median
        // vectors.map { it[index] }.sum() / vectorCount // average
    }

    /*for(i in 0 until vectorCount){
        println("trivial solution $i: ${maxDistance(vectors[i])}")
    }*/

    println("average solution: ${maxDistance(average)}")
    println("median solution: ${maxDistance(median)}")

    // use the median vector as a starting point
    // or the average? idk, average is simpler

    println("average: ${average.joinToString()}")

    var ctr = 0
    val t0 = System.nanoTime()
    val solution = simplexAlgorithm(average, 1.0, 0.0, 100000) { potentialSolution ->
        val distance = maxDistance(potentialSolution)
        if(++ctr % 5000 == 0) println(distance)
        distance
    }
    val t1 = System.nanoTime()
    println((t1-t0)*1e-9)

    println(solution.joinToString())
    println("found solution: ${maxDistance(solution)}")

    /*val solution2 = gradientDescent(solution, 1.0, 0.0, 100000) { potentialSolution ->
        val distance = maxDistance(potentialSolution)
        if(++ctr % 5000 == 0) println(distance)
        distance
    }
    println("tested ${solution2.joinToString()}")
    println(": ${maxDistance(solution2)}")*/

}

val vectorCount = 200
val dims = 20

val vectors = Array(vectorCount) {
    DoubleArray(dims)
}

fun maxDistance(x: DoubleArray): Double {

    return IntArray(vectorCount) { it }.map {
        var sum = 0.0
        val vector = vectors[it]
        for (i in 0 until dims) {
            sum += abs(x[i] - vector[i])
        }
        sum
    }.max()!!

}

fun loadMatrix() {
    // load matrix
    val srcFile = File(OS.downloads, "MMfO.3.data.txt")
    val reader = srcFile.bufferedReader()
    for(lineIndex in 0 until vectorCount){
        val line = reader.readLine()!!// ?: break
        val values = line.split(',')
        val vector = vectors[lineIndex]
        for(i in 0 until dims){
            vector[i] = values[i].trim().toDouble()
        }
    }
}