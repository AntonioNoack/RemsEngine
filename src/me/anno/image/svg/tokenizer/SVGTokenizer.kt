package me.anno.image.svg.tokenizer

class SVGTokenizer(val str: String){

    val tokens = ArrayList<Any>()

    var i = 0

    fun run(){
        while(i < str.length){
            when(val char = str[i]){
                ' ', '\t', '\n', '\r', '+' -> { i++ } // idc
                '(', ')', ',' -> {
                    tokens += char
                    i++
                }
                '-' -> {
                    i++
                    tokens += readNumber()
                }
                in 'A' .. 'Z', in 'a' .. 'z' -> {
                    tokens += readName()
                }
                in '0' .. '9' -> {
                    tokens += readNumber()
                }
                else -> throw RuntimeException("Unknown char $char in $str at position $i, $tokens")
            }
        }
    }

    fun readName(): String {
        val i0 = i
        while(i < str.length){
            when(str[i]){
                in 'A' .. 'Z', in 'a' .. 'z', '_', in '0' .. '9' -> {
                    i++
                }
                else -> {
                    return str.substring(i0, i)
                }
            }
        }
        return str.substring(i0)
    }

    fun readNumber(): Double {
        val i0 = i
        digits@ while(i < str.length){
            when(str[i]){
                in '0' .. '9' -> i++
                else -> break@digits
            }
        }
        if(i < str.length && str[i] == '.'){
            i++
            digits@ while(i < str.length){
                when(str[i]){
                    in '0' .. '9' -> i++
                    else -> break@digits
                }
            }
        }
        if(i+1 < str.length && (str[i] == 'e' || str[i] == 'E')){
            i++
            if(str[i] == '+' || str[i] == '-') i++
            digits@ while(i < str.length){
                when(str[i]){
                    in '0' .. '9' -> i++
                    else -> break@digits
                }
            }
        }
        if(i < str.length && when(str[i]){
                'f', 'F', 'd', 'D', 'l', 'L' -> true
                else -> false
            }){
            i++
            return str.substring(i0, i-1).toDouble()
        }
        return str.substring(i0, i).toDouble()
    }

    init {
        run()
    }
}