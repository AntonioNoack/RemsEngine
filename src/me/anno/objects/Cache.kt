package me.anno.objects

import java.io.File

class Cache {

    companion object {
        val cache = HashMap<Pair<File, Int>, Cache>()

    }

}