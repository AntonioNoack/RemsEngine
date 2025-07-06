package me.anno.mesh.blender

class DNAStruct(val index: Int, val type: DNAType, val fields: Array<DNAField>, pointerSize: Int) {

    init {
        var pointer = 0
        for (field in fields) {

            val sn = field.decoratedName
            val isArray = sn.endsWith("]")
            val arraySizeOr1 = if (isArray) parseArraySize(sn.substring(sn.indexOf('[') + 1, sn.lastIndex)) else 1

            val size = arraySizeOr1 * (if (field.isPointer) pointerSize else field.type.size)

            field.offset = pointer
            field.arraySizeOr1 = arraySizeOr1

            pointer += size
        }
        if (pointer > type.size) throw RuntimeException("Size calculation must be wrong! $pointer > ${type.size}")
    }

    private fun parseArraySize(str: String): Int {
        var size = 1
        var sum = 0
        for (i in str.indices) {
            when (val char = str[i]) {
                in '0'..'9' -> sum = sum * 10 + char.code - 48
                else -> {
                    if (sum > 0) size *= sum
                    sum = 0
                }
            }
        }
        return size * sum
    }

    val byName = HashMap<String, DNAField>()

    init {
        for (fi in fields) {
            val name = fi.decoratedName
            byName[name] = fi
            val idx = name.indexOf('[')
            if (idx > 0) byName[name.substring(0, idx)] = fi
        }
    }

    override fun toString(): String {
        return "$type:${fields.joinToString()}"
    }
}