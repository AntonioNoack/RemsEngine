package me.anno.tests.image.jpg

class ImgComp {
    var id = 0
    var h = 0
    var v = 0
    var tqIndex = 0
    var dcIndex = 0
    var acIndex = 0
    var x = 0
    var y = 0
    var w2 = 0
    var h2 = 0
    var coeffW = 0
    var coeffH = 0
    var dcPred = 0
    lateinit var data: ByteArray
    lateinit var lineBuff: ByteArray
    lateinit var coeff: ShortArray
}