package me.anno.image.svg.gradient

class LinearGradient(): Gradient1D() {

    // todo parse transform properties etc...

    constructor(children: List<Any>): this(){
        parseStops(children)
    }

}