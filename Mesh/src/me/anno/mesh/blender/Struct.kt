package me.anno.mesh.blender

class Struct(file: BinaryFile) {
    val type = file.readShort()
    // type, name
    val fieldsAsTypeName = ShortArray(file.readShort().toUShort().toInt() * 2) {
        file.readShort()
    }
}