package com.vidreanaurel.licenta.models

class CircularBuffer<T>(private val capacity: Int) {
    private val buffer: Array<Any?> = arrayOfNulls<Any?>(capacity)
    private var head: Int = 0
    private var tail: Int = 0
    private var size: Int = 0

    fun add(element: T) {
        buffer[tail] = element
        tail = (tail + 1) % capacity
        if (size == capacity) {
            head = (head + 1) % capacity
        } else {
            size++
        }
    }

    fun clear() {
        size = 0
        head = 0
        tail = 0
        for (i in buffer.indices) {
            buffer[i] = null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun toList(): List<T> {
        val list = mutableListOf<T>()
        var i = head
        repeat(size) {
            list.add(buffer[i] as T)
            i = (i + 1) % capacity
        }
        return list
    }
}