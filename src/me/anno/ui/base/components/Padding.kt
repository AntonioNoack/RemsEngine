package me.anno.ui.base.components

class Padding(l: Int, t: Int, r: Int, b: Int): LTRB(l,t,r,b){
    constructor(x: Int): this(x,x,x,x)

    companion object {
        val Zero = LTRB(0,0,0,0)
    }

}