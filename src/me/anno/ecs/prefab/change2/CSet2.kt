package me.anno.ecs.prefab.change2

import me.anno.io.Saveable

class CSet2 : Saveable() {

    var res: CRes? = null
    var name: String = ""
    var value: Any? = null

    fun apply() {
        res!!.instance!![name] = value
    }

}