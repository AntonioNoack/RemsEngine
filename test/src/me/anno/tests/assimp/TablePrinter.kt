package me.anno.tests.assimp

import me.anno.utils.structures.arrays.BooleanArrayList
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.splitLines
import org.joml.Vector2i
import kotlin.math.max
import kotlin.math.min

class TablePrinter {

    val builder = StringBuilder()
    val columns = ArrayList<ArrayList<List<String>>>()
    val offsets = IntArrayList(8)

    val colTitles = ArrayList<List<String>>()
    val colTitleCentered = BooleanArrayList()

    val rowTitles = ArrayList<List<String>>()
    val rowTitleCentered = BooleanArrayList()

    val contentCentered = HashSet<Vector2i>()

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
        colTitles.add(title.splitLines())
        colTitleCentered[colTitles.size - 1] = titleCentered
    }

    fun addRowTitle(title: CharSequence, titleCentered: Boolean = false) {
        rowTitles.add(title.splitLines())
        rowTitleCentered[rowTitles.size - 1] = titleCentered
    }

    fun finishCell(x: Int, y: Int, centered: Boolean = false) {
        val lines = builder.toString()
            .trimEnd()
            .splitLines()
        builder.clear()
        while (columns.size <= x) columns.add(ArrayList())
        val cx = columns[x]
        while (cx.size <= y) cx.add(emptyList())
        cx[y] = lines
        if (centered) contentCentered.add(Vector2i(x, y))
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
                        print(overTitles)
                    println()
                }
                // print titles
                for (y in 0 until colTitles.maxOf { it.size }) {
                    print(prefix)
                    if (rowTitles.isNotEmpty()) {
                        // todo write here the table name :)
                        printValue(0, "", separator, false)
                    }
                    for (x in 0 until min(columns.size, colTitles.size)) {
                        printValue(x + 1, colTitles[x].getOrNull(y) ?: "", separator, colTitleCentered[x])
                    }
                    println(suffix)
                }
            }
            // print content lines
            for (yi in 0 until columns.maxOf { it.size }) {
                if (underTitles != null) {
                    for (i in 0 until totalLength)
                        print(underTitles)
                    println()
                }
                val data = columns.map { it[yi] }
                for (y in 0 until data.maxOf { it.size }) {
                    print(prefix)
                    if (rowTitles.isNotEmpty()) {
                        // print row title
                        // todo center vertically (optional)
                        printValue(0, rowTitles.getOrNull(yi)?.getOrNull(y) ?: "", separator, rowTitleCentered[yi])
                    }
                    for (x in data.indices) {
                        printValue(x + 1, data[x].getOrNull(y) ?: "", separator, Vector2i(x, yi) in contentCentered)
                    }
                    println(suffix)
                }
            }
            if (underContents != null) {
                for (i in 0 until totalLength)
                    print(underContents)
                println()
            }
        }
    }

    private fun printValue(x: Int, text: CharSequence, separator: CharSequence, centered: Boolean, space: Char = ' ') {
        if (x > 0) print(separator)
        val x0 = offsets[x]
        val x1 = offsets[x + 1]
        val spacing = (x1 - x0) - text.length
        if (centered) {
            for (i in 0 until spacing / 2)
                print(space)
        }
        print(text)
        if (centered) {
            for (i in 1 until spacing / 2)
                print(space)
        } else {
            for (i in 0 until spacing)
                print(space)
        }
    }
}