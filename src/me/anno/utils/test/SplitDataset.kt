package me.anno.utils.test

import me.anno.utils.types.Strings.isBlank2
import java.io.File
import java.util.*
import kotlin.math.roundToInt

fun main() {

    val seed = 1234L
    val ratio = 0.5f
    val file = File("E:\\Documents\\Uni\\Master\\Uni\\sv\\NIR.csv")

    val lines = file.readLines()

    val header = lines.first()

    val data = lines.subList(1, lines.size).filter { !it.isBlank2() }
    val dataSize = data.size

    // split data into equally large sets
    val random = Random(seed)
    val indices = IntArray(dataSize){ it }.toList()
    val shuffled = indices.shuffled(random)

    val splitIndex = (ratio * dataSize).roundToInt()

    val firstGroup = shuffled.subList(0, splitIndex).sorted()
    val secondGroup = shuffled.subList(splitIndex, dataSize).sorted()

    // write results
    fun write(group: List<Int>, fileName: String){
        val file1 = File(file.parentFile, fileName)
        file1.writeText(
            header + group.joinToString(separator = "\n", prefix = "\n", postfix = "") { data[it] }
        )
    }

    write(firstGroup, "NIR1.csv")
    write(secondGroup, "NIR2.csv")

}