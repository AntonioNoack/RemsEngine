package me.anno.utils

object StackTrace {
    fun print(){
        val st = Throwable().stackTrace
        for(stI in st){
            System.err.println(stI)
        }
    }
}