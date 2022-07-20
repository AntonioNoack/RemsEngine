package me.anno.fonts.keys

class FontKey(var name: String, var sizeIndex: Int, var bold: Boolean, var italic: Boolean) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FontKey) return false
        if (name != other.name) return false
        if (sizeIndex != other.sizeIndex) return false
        if (bold != other.bold) return false
        if (italic != other.italic) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + sizeIndex
        result = 31 * result + bold.hashCode()
        result = 31 * result + italic.hashCode()
        return result
    }

}