package me.anno.maybe

import java.lang.RuntimeException
import java.net.URL
import kotlin.math.exp

fun main(){
    // 15.06.
    // *.phychi.com is down it seems :/
    // I wanted it to crash, but in the first try it executed normally
    // in the second it crashed... Timeout
    // third try after a while: working again :)
    // println(String(URL("https://phychi.com").readBytes()))
}