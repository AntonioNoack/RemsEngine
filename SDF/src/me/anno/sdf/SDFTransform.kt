package me.anno.sdf

class SDFTransform(var posIndex: Int, var scaleName: String?, var offsetName: String?) {
    constructor() : this(-1, null, null)

    fun set(posIndex: Int, scaleName: String?, offsetName: String?): SDFTransform {
        this.posIndex = posIndex
        this.scaleName = scaleName
        this.offsetName = offsetName
        return this
    }
}
