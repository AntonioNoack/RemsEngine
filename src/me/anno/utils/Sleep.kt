package me.anno.utils

object Sleep {
    fun sleepShortly() {
        Thread.sleep(0, 100_000)
    }
    fun sleepABit(){
        Thread.sleep(1)
    }
}