package me.anno.ui.base.components

class Padding(l: Int, t: Int, r: Int, b: Int): LTRB(l,t,r,b){
    constructor(all: Int): this(all,all,all,all)
    constructor(): this(0)

    companion object {
        val Zero = Padding()
    }

    fun set(all: Int){
        left = all
        top = all
        right = all
        bottom = all
    }

    operator fun plusAssign(s: Padding){
        left += s.left
        top += s.top
        right += s.right
        bottom += s.bottom
    }

}