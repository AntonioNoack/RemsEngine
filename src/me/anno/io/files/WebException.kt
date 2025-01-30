package me.anno.io.files

import java.io.IOException

class WebException(val absolutePath: String, val statusCode: Int, val content: String):
    IOException("$absolutePath failed with code $statusCode")