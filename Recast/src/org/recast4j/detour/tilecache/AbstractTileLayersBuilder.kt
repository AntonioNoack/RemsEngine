/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
recast4j copyright (c) 2015-2019 Piotr Piastucki piotr@jtilia.org

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/
package org.recast4j.detour.tilecache

import java.nio.ByteOrder
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class AbstractTileLayersBuilder {
    protected fun build(order: ByteOrder, cCompatibility: Boolean, threads: Int, tw: Int, th: Int): List<ByteArray> {
        return if (threads == 1) {
            buildSingleThread(order, cCompatibility, tw, th)
        } else buildMultiThread(order, cCompatibility, tw, th, threads)
    }

    private fun buildSingleThread(order: ByteOrder, cCompatibility: Boolean, tw: Int, th: Int): List<ByteArray> {
        val layers: MutableList<ByteArray> = ArrayList()
        for (y in 0 until th) {
            for (x in 0 until tw) {
                layers.addAll(build(x, y, order, cCompatibility)!!)
            }
        }
        return layers
    }

    private fun buildMultiThread(
        order: ByteOrder,
        cCompatibility: Boolean,
        tw: Int,
        th: Int,
        threads: Int
    ): List<ByteArray> {
        val ec = Executors.newFixedThreadPool(threads)
        val partialResults = Array(th) { arrayOfNulls<List<ByteArray>>(tw) }
        for (y in 0 until th) {
            for (x in 0 until tw) {
                ec.submit { partialResults[y][x] = build(x, y, order, cCompatibility) }
            }
        }
        ec.shutdown()
        try {
            ec.awaitTermination(1000, TimeUnit.HOURS)
        } catch (_: InterruptedException) {
        }
        val layers: MutableList<ByteArray> = ArrayList()
        for (y in 0 until th) {
            for (x in 0 until tw) {
                layers.addAll(partialResults[y][x]!!)
            }
        }
        return layers
    }

    protected abstract fun build(tx: Int, ty: Int, order: ByteOrder, cCompatibility: Boolean): List<ByteArray>?
}