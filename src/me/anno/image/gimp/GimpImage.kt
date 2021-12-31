package me.anno.image.gimp

class GimpImage {

    var fileVersion = 0
    var bytesPerOffset = 4
    var compression = Compression.NONE
    var precision = DataType.U8_NON_LINEAR

    var width = 0
    var height = 0
    var imageType = ImageType.INDEXED

    var propType = PropertyType.END
    var propSize = 0

    var colorMap: IntArray? = null

    var tmp: ByteArray? = null

    var channels = ArrayList<Channel>()
    var layers = ArrayList<Layer>()

}