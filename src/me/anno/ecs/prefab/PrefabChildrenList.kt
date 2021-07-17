package me.anno.ecs.prefab

class PrefabChildrenList(val owner: PrefabEntity1, val get: () -> List<PrefabEntity1>) : MutableList<PrefabEntity1> {

    override val size: Int get() = get().size

    override fun add(element: PrefabEntity1): Boolean {
        owner.add(element)
        return true
    }

    override fun add(index: Int, element: PrefabEntity1) {
        owner.addListChange(PrefabEntity1.childrenPath, element, index)
    }

    override fun addAll(elements: Collection<PrefabEntity1>): Boolean {
        for (element in elements) {
            add(element)
        }
        return elements.isNotEmpty()
    }

    override fun addAll(index: Int, elements: Collection<PrefabEntity1>): Boolean {
        // todo how is this supposed to work?
        TODO("Not yet implemented")
    }

    override fun clear() {
        for (index in size - 1 downTo 0) {
            removeAt(index)
        }
    }

    override fun contains(element: PrefabEntity1): Boolean {
        return get().contains(element)
    }

    override fun containsAll(elements: Collection<PrefabEntity1>): Boolean {
        return get().containsAll(elements)
    }

    override fun get(index: Int): PrefabEntity1 {
        return get()[index]
    }

    override fun indexOf(element: PrefabEntity1): Int {
        return get().indexOf(element)
    }

    override fun lastIndexOf(element: PrefabEntity1): Int {
        return get().lastIndexOf(element)
    }

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): MutableIterator<PrefabEntity1> {
        return listIterator()
    }

    override fun listIterator(): MutableListIterator<PrefabEntity1> {
        return listIterator(0)
    }

    override fun listIterator(index: Int): MutableListIterator<PrefabEntity1> {
        return object : MutableListIterator<PrefabEntity1> {
            var index2 = index
            override fun add(element: PrefabEntity1) {
                // correct?? idk...
                add(index2, element)
                index2++
            }

            override fun remove() {
                removeAt(index2)
                index2--
            }

            override fun hasNext(): Boolean = index2 < size
            override fun hasPrevious(): Boolean = index2 > 0
            override fun next(): PrefabEntity1 = get(index2++)
            override fun previous(): PrefabEntity1 = get(--index2)
            override fun nextIndex(): Int = index2 + 1
            override fun previousIndex(): Int = index2 - 1
            override fun set(element: PrefabEntity1) {
                set(index2, element)
            }

        }
    }

    override fun remove(element: PrefabEntity1): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAt(index: Int): PrefabEntity1 {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<PrefabEntity1>): Boolean {
        var wasChanged = false
        for (element in elements) {
            wasChanged = remove(element) || wasChanged
        }
        return wasChanged
    }

    override fun retainAll(elements: Collection<PrefabEntity1>): Boolean {
        TODO("Not yet implemented")
    }

    override fun set(index: Int, element: PrefabEntity1): PrefabEntity1 {
        val removed = removeAt(index)
        add(index, element)
        return removed
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<PrefabEntity1> {
        TODO("Not yet implemented")
    }

}