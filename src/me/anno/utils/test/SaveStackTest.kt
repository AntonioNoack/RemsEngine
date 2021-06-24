package me.anno.utils.test

import me.anno.utils.structures.SecureStack

fun main(){

    val stack = object: SecureStack<Int>(0){
        override fun onChangeValue(v: Int) {
            println(v)
        }
    }

    stack.use(1){
        stack.use(2){
            stack.use(3){
                println("top")
            }
        }
    }

    println("level zero")

    stack.use(1){
        stack.use(2){
            stack.use(3){
                throw Exception()
            }
        }
    }

}