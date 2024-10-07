package me.anno.io.files

object ImportType {
    const val IMAGE = "Image" // photos and textures, .png, .jpg, .webp
    const val VIDEO = "Video" // film, .mp4, .avi, .gif
    const val AUDIO = "Audio" // music, .mp3, .ogg
    const val METADATA = "Metadata" // may store any structured data
    const val MESH = "Mesh" // mesh or scene file, typically native
    const val CONTAINER = "Container" // .zip, .7z
    const val CODE = "Code" // .java, .py, ...
    const val TEXT = "Text"
    const val EXECUTABLE = "Executable" // .exe
    const val LIBRARY = "Library" // .dll, .o, .so
    const val COMPILED = "Compiled" // .class
    const val FONT = "Font" // .woff
    const val LINK = "Link" // .url
    const val CUBEMAP_EQU = "Cubemap-Equ" // .hdr
}