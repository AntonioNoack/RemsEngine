package me.anno.utils.structures.maps

// performance tests vs normal hashmap
// not really faster, but contains errors...
// and a hashmap, where you place the elements after one-another was extremely slow at checking for elements (100x slower)
@Deprecated("contains errors, and isn't really better; the Java devs did a great job 😄")
class HashMap2<K, V>(capacity0: Int = 16) : MutableMap<K, V> {

    var capacity = toPowerOf2(capacity0)
    var capacityM1 = capacity - 1

    var hashes = IntArray(capacity)
    var keyStore = arrayOfNulls<Any>(capacity)
    var valStore = arrayOfNulls<Any>(capacity)
    var overflow = arrayOfNulls<Node<K, V>>(capacity)

    class Node<K, V>(var key: K, var hash: Int, var value: V) {
        var nextNode: Node<K, V>? = null
    }

    override var size = 0

    override fun isEmpty(): Boolean = size == 0

    private fun upsize() {
        val oldCapacity = capacity
        val oldHashes = hashes
        val oldKeys = keyStore
        val oldValues = valStore
        val oldOverflow = overflow
        val newCapacity = oldCapacity shl 1
        hashes = IntArray(newCapacity)
        keyStore = arrayOfNulls(newCapacity)
        valStore = arrayOfNulls(newCapacity)
        overflow = arrayOfNulls(newCapacity)
        capacity = newCapacity
        capacityM1 = newCapacity - 1
        // size doesn't change, only density halves
        // reenter all values
        for (i in 0 until oldCapacity) {
            val ki = oldKeys[i]
            if (ki != null) {
                putNear(ki, oldHashes[i], oldValues[i])
                var node = oldOverflow[i]
                while (node != null) {
                    putNear(node.key!!, node.key.hashCode(), node.value)
                    node = node.nextNode
                }
            }
        }
    }

    override fun toString(): String {
        return "keys: ${keyStore.joinToString()}\n," +
                "values: ${valStore.joinToString()}\n," +
                "overflow: ${overflow.joinToString()}"
    }

    private fun putNear(key: Any, hash: Int, value: Any?): Any? {
        val index = hash and capacityM1
        val oldValue = remove(key as K)
        size++
        if (keyStore[index] == null) {
            // done, just place it here
            keyStore[index] = key
            valStore[index] = value
            hashes[index] = hash
        } else {
            // find the next free slot
            // what if we already contain that key? we need to check the whole list...
            val node = Node(key as K, hash, value as V)
            node.nextNode = overflow[index]
            overflow[index] = node
        }
        return oldValue
    }

    private fun ensureCapacity(size: Int) {
        if (size > capacity shr 1) {
            upsize()
        }
    }

    override fun put(key: K, value: V): V? {
        if (key == null) throw IllegalArgumentException("Key must not be null!")
        ensureCapacity(size + 1)
        return putNear(key, key.hashCode(), value) as V?
    }

    override fun containsKey(key: K): Boolean {
        if (key == null) return false
        val hash = key.hashCode()
        val index = hash and capacityM1
        val key0 = keyStore[index] ?: return false
        if (hashes[index] == hash && key0 == key) return true
        // else check the overflow
        var node = overflow[index]
        while (node != null) {
            if (node.hash == hash && node.key == key) return true
            node = node.nextNode
        }
        return false
    }

    override fun get(key: K): V? {
        if (key == null) return null
        val hash = key.hashCode()
        val index = hash and capacityM1
        val key0 = keyStore[index] ?: return null
        if (hashes[index] == hash && key0 == key) {
            return valStore[index] as V
        }
        // else check the overflow
        var node = overflow[index]
        while (node != null) {
            if (node.hash == hash && node.key == key) {
                return node.value
            }
            node = node.nextNode
        }
        return null
    }

    override fun remove(key: K): V? {
        if (key == null) return null
        val hash = key.hashCode()
        val index = hash and capacityM1
        val key0 = keyStore[index] ?: return null
        if (hashes[index] == hash && key0 == key) {
            // found it :)
            val node = overflow[index]
            if (node != null) {
                // copy the first overflow item into this
                hashes[index] = node.hash
                keyStore[index] = node.key
                valStore[index] = node.value
                overflow[index] = node.nextNode
            }
            hashes[index] = 0
            keyStore[index] = null
            val v = valStore[index]
            valStore[index] = null
            size--
            return v as V
        }
        var node = overflow[index]
        var previousNode: Node<K, V>? = null
        while (node != null) {
            if (node.hash == hash && node.key == key) {
                // found it :)
                // kill the link
                if (previousNode == null) {
                    overflow[index] = node.nextNode
                } else {
                    previousNode.nextNode = node.nextNode
                }
                size--
                return node.value
            }
            previousNode = node
            node = node.nextNode
        }
        return null
    }

    override fun putAll(from: Map<out K, V>) {
        for ((k, v) in from) {
            put(k, v)
        }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = object : MutableSet<MutableMap.MutableEntry<K, V>> {

            override val size: Int get() = this@HashMap2.size

            override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
                return put(element.key, element.value) != null
            }

            override fun addAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
                ensureCapacity(size + elements.size)
                var wasChanged = false
                for (element in elements) {
                    wasChanged = add(element) || wasChanged
                }
                return wasChanged
            }

            override fun clear() {
                this@HashMap2.clear()
            }

            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
                throw RuntimeException("Not yet implemented")
            }

            override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean {
                // correct?, check the value as well?
                return this@HashMap2.remove(element.key) != null
            }

            override fun removeAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
                throw RuntimeException("Not yet implemented")
            }

            override fun retainAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
                throw RuntimeException("Not yet implemented")
            }

            override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean {
                return this@HashMap2[element.key] == element.value
            }

            override fun containsAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
                throw RuntimeException("Not yet implemented")
            }

            override fun isEmpty(): Boolean = size == 0

        }

    override fun containsValue(value: V): Boolean {
        for (i in 0 until capacity) {
            if (keyStore[i] != null) {
                val v = valStore[i]
                if (value == v) return true
                var node = overflow[i]
                while (node != null) {
                    if (node.value == value) return true
                    node = node.nextNode
                }
            }
        }
        return false
    }

    override fun clear() {
        size = 0
        hashes.fill(0)
        keyStore.fill(null)
        valStore.fill(null)
        overflow.fill(null)
    }

    override val values: MutableCollection<V>
        get() = object : MutableCollection<V> {
            override val size: Int = this@HashMap2.size

            override fun contains(element: V): Boolean {
                return containsValue(element)
            }

            override fun containsAll(elements: Collection<V>): Boolean {
                for (element in elements) {
                    if (!containsValue(element)) return false
                }
                return true
            }

            override fun isEmpty(): Boolean = size == 0

            override fun add(element: V): Boolean {
                throw NotImplementedError()
            }

            override fun addAll(elements: Collection<V>): Boolean {
                throw NotImplementedError()
            }

            override fun clear() {
                this@HashMap2.clear()
            }

            override fun iterator(): MutableIterator<V> {
                throw RuntimeException("Not yet implemented")
            }

            override fun remove(element: V): Boolean {
                throw RuntimeException("Not yet implemented")
            }

            override fun removeAll(elements: Collection<V>): Boolean {
                throw RuntimeException("Not yet implemented")
            }

            override fun retainAll(elements: Collection<V>): Boolean {
                throw RuntimeException("Not yet implemented")
            }

        }

    override val keys: MutableSet<K>
        get() = object : MutableSet<K> {
            override val size: Int get() = this@HashMap2.size
            override fun add(element: K): Boolean {
                throw NotImplementedError()
            }

            override fun addAll(elements: Collection<K>): Boolean {
                throw NotImplementedError()
            }

            override fun clear() {
                this@HashMap2.clear()
            }

            override fun iterator(): MutableIterator<K> {
                throw RuntimeException("Not yet implemented")
            }

            override fun remove(element: K): Boolean {
                return this@HashMap2.remove(element) != null
            }

            override fun removeAll(elements: Collection<K>): Boolean {
                throw RuntimeException("Not yet implemented")
            }

            override fun retainAll(elements: Collection<K>): Boolean {
                throw RuntimeException("Not yet implemented")
            }

            override fun contains(element: K): Boolean {
                return this@HashMap2.contains(element)
            }

            override fun containsAll(elements: Collection<K>): Boolean {
                for (e in elements) if (e !in this@HashMap2) return false
                return true
            }

            override fun isEmpty(): Boolean = size == 0

        }

    companion object {
        fun toPowerOf2(x: Int): Int {
            if (x.and(x - 1) == 0) return x
            return 1024
        }
    }

}