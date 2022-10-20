package me.anno.tests.assimp

import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.types.Booleans.toInt
import java.util.*
import kotlin.math.max
import kotlin.math.min

class TablePrinter {

    val builder = StringBuilder()
    val columns = ArrayList<ArrayList<List<String>>>()
    val offsets = ExpandingIntArray(8)

    val colTitles = ArrayList<List<String>>()
    val colTitleCentered = BitSet(0)

    val rowTitles = ArrayList<List<String>>()
    val rowTitleCentered = BitSet(0)

    val contentCentered = HashSet<Pair<Int, Int>>()

    fun print(s: Any?) {
        builder.append(s.toString())
    }

    fun println(s: Any?) {
        print(s)
        builder.append('\n')
    }

    fun println() {
        builder.append('\n')
    }

    fun addColumnTitle(title: CharSequence, titleCentered: Boolean = false) {
        colTitles.add(title.split('\n'))
        colTitleCentered[colTitles.size - 1] = titleCentered
    }

    fun addRowTitle(title: CharSequence, titleCentered: Boolean = false) {
        rowTitles.add(title.split('\n'))
        rowTitleCentered[rowTitles.size - 1] = titleCentered
    }

    fun finishCell(x: Int, y: Int, centered: Boolean = false) {
        val lines = builder.toString()
            .trimEnd()
            .split('\n')
        builder.clear()
        while (columns.size <= x) columns.add(ArrayList())
        val cx = columns[x]
        while (cx.size <= y) cx.add(emptyList())
        cx[y] = lines
        if (centered) contentCentered.add(Pair(x, y))
    }

    fun finish(
        separator: CharSequence = "|", prefix: CharSequence = "|", suffix: CharSequence = "|",
        overTitles: Char? = null, underTitles: Char? = null, underContents: Char? = null
    ) {

        offsets.clear()

        offsets.add(0)
        offsets.add(rowTitles.maxOf { it.maxOf { it.length } })

        for ((xi, column) in columns.withIndex()) {
            offsets.add(
                offsets.last() +
                        max(column.maxOf { it.maxOf { it.length } },
                            if (xi < colTitles.size) colTitles[xi].maxOf { it.length }
                            else 0
                        )
            )
        }

        // todo print row titles
        if (columns.isNotEmpty()) {

            val totalLength = prefix.length +
                    separator.length * (columns.size - rowTitles.isEmpty().toInt()) +
                    suffix.length + offsets.last()

            if (colTitles.isNotEmpty()) {
                if (overTitles != null) {
                    for (i in 0 until totalLength)
                        kotlin.io.print(overTitles)
                    kotlin.io.println()
                }
                // print titles
                for (y in 0 until colTitles.maxOf { it.size }) {
                    kotlin.io.print(prefix)
                    if (rowTitles.isNotEmpty()) {
                        // todo write here the table name :)
                        printValue(0, "", separator, false)
                    }
                    for (x in 0 until min(columns.size, colTitles.size)) {
                        printValue(x + 1, colTitles[x].getOrNull(y) ?: "", separator, colTitleCentered[x])
                    }
                    kotlin.io.println(suffix)
                }
            }
            // print content lines
            for (yi in 0 until columns.maxOf { it.size }) {
                if (underTitles != null) {
                    for (i in 0 until totalLength)
                        kotlin.io.print(underTitles)
                    kotlin.io.println()
                }
                val data = columns.map { it[yi] }
                for (y in 0 until data.maxOf { it.size }) {
                    kotlin.io.print(prefix)
                    if (rowTitles.isNotEmpty()) {
                        // print row title
                        // todo center vertically (optional)
                        printValue(0, rowTitles.getOrNull(yi)?.getOrNull(y) ?: "", separator, rowTitleCentered[yi])
                    }
                    for (x in data.indices) {
                        printValue(x + 1, data[x].getOrNull(y) ?: "", separator, Pair(x, yi) in contentCentered)
                    }
                    kotlin.io.println(suffix)
                }
            }
            if (underContents != null) {
                for (i in 0 until totalLength)
                    kotlin.io.print(underContents)
                kotlin.io.println()
            }
        }
    }

    private fun printValue(x: Int, text: CharSequence, separator: CharSequence, centered: Boolean, space: Char = ' ') {
        if (x > 0) kotlin.io.print(separator)
        val x0 = offsets[x]
        val x1 = offsets[x + 1]
        val spacing = (x1 - x0) - text.length
        if (centered) {
            for (i in 0 until spacing / 2)
                kotlin.io.print(space)
        }
        kotlin.io.print(text)
        if (centered) {
            for (i in 1 until spacing / 2)
                kotlin.io.print(space)
        } else {
            for (i in 0 until spacing)
                kotlin.io.print(space)
        }
    }

}