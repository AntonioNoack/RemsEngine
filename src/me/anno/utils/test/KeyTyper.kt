package me.anno.utils.test

import java.awt.Robot
import java.awt.event.KeyEvent
import java.io.File
import java.util.*
import kotlin.collections.HashMap

fun main(){

    val robot = Robot()
    val text = File("E:\\Documents\\Uni\\Master\\Uni\\wr\\u7\\gpuStern.cu").readText()

    val keyMap = HashMap<String, Int>()
    for(i in 0 until 1000){
        val chars = KeyEvent.getKeyText(i)
        if ("unknown" !in chars.lowercase(Locale.getDefault())) println(chars)
        keyMap[chars] = i
    }

    Thread.sleep(3000)

    loop@for(char in text){
        if(char != '\r'){

            var isUpper2 = false
            var isAlt = false

            fun alt(key: Char): Int {
                isAlt = true
                return keyMap["$key"]!!
            }

            fun shift(key: Char): Int {
                isUpper2 = true
                return keyMap["$key"]!!
            }

            val keyCode = when(char){
                in 'a' .. 'z', in 'A' .. 'Z', in '0' .. '9' -> {
                    keyMap[char.uppercaseChar().toString()]
                }
                '|' -> keyMap[""]
                '!' -> shift('1')
                '"' -> shift('2')
                '/' -> shift('7')
                '\\' -> keyMap["Back Slash"]
                ' ' -> keyMap["Space"]
                '#' -> keyMap["Number Sign"]
                '\n' -> keyMap["Enter"]
                '(' -> shift('8')
                ')' -> shift('9')
                '&' -> shift('6')
                '%' -> shift('5')
                '+' -> keyMap["NumPad +"]
                '-' -> keyMap["NumPad -"]
                '*' -> keyMap["NumPad *"]
                '{' -> alt('7')
                '}' -> alt('0')
                '[' -> alt('8')
                ']' -> alt('9')
                '=' -> shift('0')
                '_' -> {
                    isUpper2 = true
                    keyMap["Minus"]
                }
                '>' -> {
                    isUpper2 = true
                    KeyEvent.getExtendedKeyCodeForChar('<'.code)
                }
                else -> KeyEvent.getExtendedKeyCodeForChar(char.code)
                //else -> continue@loop
            }!!

            val needsShift = isUpper2 || Character.isUpperCase(char)
            val needsAlt = isAlt
            if (needsShift) robot.keyPress(KeyEvent.VK_SHIFT)
            if(needsAlt) robot.keyPress(KeyEvent.VK_ALT)
            robot.keyPress(keyCode)
            Thread.sleep(15)
            robot.keyRelease(keyCode)
            if (needsShift) robot.keyRelease(KeyEvent.VK_SHIFT)
            if(needsAlt) robot.keyRelease(KeyEvent.VK_ALT)
            Thread.sleep(5)
        }
    }


}