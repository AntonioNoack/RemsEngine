package me.anno.utils.test

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

fun main(){

    // low cpu usage, and it works:
    // max is 3

    val s = Semaphore(3)
    val c = AtomicInteger()
    var max = 0

    for(i in 0 until 12){
        thread {
            for(j in 0 until 1000){
                s.acquire()
                val ci = c.incrementAndGet()
                if(ci > max){
                    max = ci
                    println("max: $max")
                }
                Thread.sleep(1)
                c.decrementAndGet()
                s.release()
            }
        }
    }

}