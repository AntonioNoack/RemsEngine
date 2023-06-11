package me.anno.tests.utils

import java.math.BigInteger

// coding with my brother :3

fun div(
    links: Int,
    rechts: Int,
    hatteSchonKomma: Boolean,
    wievielteKommastelle: Int,
    hattenWirSchon: MutableMap<Int, Int>
) {
    if (hatteSchonKomma) {
        if (links !in hattenWirSchon) {
            hattenWirSchon[links] = wievielteKommastelle
            if (links < rechts) {
                print('0')
                div(links * 10, rechts, hatteSchonKomma, wievielteKommastelle + 1, hattenWirSchon)
            } else {
                print(links / rechts)
                val rest = links % rechts
                div(rest * 10, rechts, hatteSchonKomma, wievielteKommastelle + 1, hattenWirSchon)
            }
        } else {
            val wiederholungen = wievielteKommastelle - hattenWirSchon[links]!!
            println(" mit $wiederholungen Wiederholungen")
        }
    } else {
        print(links / rechts)
        print(',')
        div((links % rechts) * 10, rechts, true, 1, hattenWirSchon)
    }
}


fun div(links1: Int, rechts: Int) {

    var links = links1
    var wievielteKommastelle = 1
    val hattenWirSchon = HashMap<Int, Int>()

    print(links / rechts)
    print(',')
    links = (links % rechts) * 10

    while (true) {
        if (links !in hattenWirSchon) {
            hattenWirSchon[links] = wievielteKommastelle
            if (links < rechts) {
                print('0')
                wievielteKommastelle += 1
                links *= 10
            } else {
                print(links / rechts)
                val rest = links % rechts
                links = rest * 10
                wievielteKommastelle += 1
            }
        } else {
            val wiederholungen = wievielteKommastelle - hattenWirSchon[links]!!
            println(" mit $wiederholungen Wiederholungen")
            return
        }
    }
}

fun rechne() {

    val start = System.nanoTime()

    var produkt = BigInteger.valueOf(1L)
    for (zahl in 2..100000) {
        produkt *= BigInteger.valueOf(zahl.toLong())
    }

    val length = produkt.toString().length

    val end = System.nanoTime()
    val sekunden = (end - start) / 1e9

    println(length)
    println(sekunden)

}

//fun div(links: Int, rechts: Int) =
//    div(links, rechts, false, 0, hashMapOf())

fun main() {

    rechne()

    div(99, 2_000_001)

}