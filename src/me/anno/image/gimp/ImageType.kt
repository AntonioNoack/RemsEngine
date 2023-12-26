package me.anno.image.gimp

enum class ImageType(val channels: Int) {
    RGB(3),
    GRAY(1),
    INDEXED(1);
}