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

class TileCacheLayer : TileCacheLayerHeader() {

    /**
     * region count
     * */
    var regCount = 0

    private lateinit var heights: ByteArray // char
    private lateinit var areas: ByteArray // char
    private lateinit var cons: ByteArray // char
    private lateinit var regs: ByteArray // char

    fun init(gridSize: Int) {
        heights = ByteArray(gridSize)
        areas = ByteArray(gridSize)
        cons = ByteArray(gridSize)
        regs = ByteArray(gridSize)
    }

    fun getHeight(i: Int): Int {
        return heights[i].toInt() and 0xff
    }

    fun setHeight(i: Int, v: Int) {
        heights[i] = v.toByte()
    }

    fun getArea(i: Int): Int {
        return areas[i].toInt() and 0xff
    }

    fun setArea(i: Int, v: Int) {
        areas[i] = v.toByte()
    }

    fun getCon(i: Int): Int {
        return cons[i].toInt() and 0xff
    }

    fun setCon(i: Int, v: Int) {
        cons[i] = v.toByte()
    }

    fun getReg(i: Int): Int {
        return regs[i].toInt() and 0xff
    }

    fun setReg(i: Int, v: Int) {
        regs[i] = v.toByte()
    }

    fun setBytes(src: ByteArray, offset: Int, dst: ByteArray) {
        System.arraycopy(src, offset, dst, 0, dst.size)
    }

    fun setHeights(values: ByteArray, offset: Int) {
        setBytes(values, offset, heights)
    }

    fun setAreas(values: ByteArray, offset: Int) {
        setBytes(values, offset, areas)
    }

    fun setCons(values: ByteArray, offset: Int) {
        setBytes(values, offset, cons)
    }

    fun fillRegs(value: Int){
        regs.fill(value.toByte())
    }
}