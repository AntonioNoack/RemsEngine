package me.anno.io.csv

import me.anno.utils.structures.lists.Lists.transposed
import me.anno.utils.types.Strings.isBlank2

@Suppress("unused")
object CSVReader {

    // a few simple and dirty readers
    // to do later maybe dedicated, optimized readers
    // which can detect the separator even maybe :)

    fun readNumerical(
        textData: String,
        comma: Char,
        lineSplit: Char,
        getDefaultValueByColumn: (String) -> Double
    ): Map<String, DoubleArray> {
        return read(textData, comma, lineSplit).mapValues { (key, stringList) ->
            val defaultValue = getDefaultValueByColumn(key)
            DoubleArray(stringList.size) { index ->
                val stringValue = stringList[index]?.trim()?.removeQuotes() ?: ""
                stringValue.toDoubleOrNull() ?: defaultValue
            }
        }
    }

    fun readNumerical(
        textData: String,
        comma: Char,
        lineSplit: Char,
        defaultValue: Double
    ): Map<String, DoubleArray> {
        return read(textData, comma, lineSplit).mapValues { (_, stringList) ->
            DoubleArray(stringList.size) { index ->
                val stringValue = stringList[index] ?: ""
                stringValue.toDoubleOrNull() ?: defaultValue
            }
        }
    }

    fun read(textData: String, comma: Char, lineSplit: Char): Map<String, List<String?>> {
        val dataRows = textData.split(lineSplit)
            .filter { !it.isBlank2() }
            .map { it.stringSplit(comma) }
        val dataColumns = dataRows.transposed()
        return dataColumns.associate {
            val columnName = it.first().trim().removeQuotes()
            val values = it.subList(1, it.size)
            columnName to values
        }
    }

    private fun String.removeQuotes(): String {
        var str = this
        if (startsWith('"') && endsWith('"')) str = substring(1, length - 1)
        if (str.startsWith('\'') && endsWith('\'')) str = str.substring(1, str.length - 1)
        return str
    }

    /**
     * splits the string, ignores separators inside quotes
     * */
    private fun String.stringSplit(comma: Char): List<String> {
        if (comma == '"') return split(comma)
        val result = ArrayList<String>()
        var i = 0
        val length = length
        val builder = StringBuilder(64)
        while (i < length) {
            when (val char = this[i]) {
                '"' -> {
                    //read the string as a whole, skip over commas
                    builder.append(char)
                    search@ while (i < length) {
                        when (val char2 = this[i]) {
                            '"' -> break@search
                            '\\' -> {
                                builder.append(char2)
                                builder.append(this[++i])
                            }
                            else -> builder.append(char2)
                        }
                        i++
                    }
                    builder.append(char)
                }
                comma -> {
                    result.add(builder.toString())
                    builder.clear()
                }
                else -> builder.append(char)
            }
            i++
        }
        result.add(builder.toString())
        return result
    }

}