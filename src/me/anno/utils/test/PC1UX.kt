package me.anno.utils.test

import java.io.File
import kotlin.math.sqrt

fun main() {

    val folder = File("E:\\Documents\\Uni\\Master\\Uni\\pc1")
    val files = folder.listFiles()!!

    val singleStepSeries = files.filter {
        it.name.startsWith("10k") && it.name.endsWith(".txt")
    }

    println("Border,Calculation,Transfer")
    singleStepSeries.map { file ->
        val lines = file.readLines()
        val l2 = lines.mapNotNull {
            val i = it.indexOf(':')
            if (i > -1) {
                val key = it.substring(0, i).trim()
                val value = it.substring(i + 1).trim()
                val vd = value.toDoubleOrNull()
                if (vd == null) null
                else key to vd
            } else null
        }
        val calc = l2.filter { it.first.startsWith("Calc") }
        val tran = l2.filter { it.first.startsWith("Tran") }
        val calcT = calc.map { it.second }.average()
        val tranT = tran.map { it.second }.average()
        println("${file.name.split('-')[1].split('.')[0]},$calcT,$tranT")
    }

    val wholeSimulationSeries = files.filter {
        it.name.startsWith("-10k") && it.name.endsWith(".txt")
    }

    println("Border,Time")
    wholeSimulationSeries.map { file ->
        val line = file.readLines().map { it.trim() }.first { it.startsWith("Used ") }
        val value = line.substring(5).replace("s", "").toDouble()
        println("${file.name.split('-')[2].split('.')[0]},$value")
    }

    val sizeVariationSeries = files.filter { file ->
        file.name.count { it == 'x' } >= 2 && file.name.endsWith(".txt")
    }

    println("Size,GFlops,Duration")
    sizeVariationSeries.map { file ->
        val lines = file.readLines()
        // Config: FieldSizeX=32768, FieldSizeY=32768, Alpha=0.1, SpaceStep=1, TimeStep=1, Border=1, Times=[100]
        val l1 = lines[1].split('=')
        val fieldSizeX = l1[1].split(',')[0].toDouble()
        val fieldSizeY = l1[2].split(',')[0].toDouble()
        val iterations = l1.last().replace("[", "").replace("]", "").trim().toDouble()
        // alpha + beta * (a + b + c + d)
        val flopsPerStep = 5
        val ops =  fieldSizeX * fieldSizeY * iterations * flopsPerStep
        val duration = lines[2].substring(5).replace("s","").trim().toDouble()
        val flops = ops / duration
        val gigaFlops = flops * 1e-9
        val size = sqrt(fieldSizeX * fieldSizeY) // geometric mean of size
        println("$size,$gigaFlops,$duration")
    }

}