package me.anno.tests

import java.net.URL

fun main(){
    println(String(URL("http://api.phychi.com/elemental/?n=60&v=1&sid=0")
        .openConnection().getInputStream()
        .readBytes()))
}