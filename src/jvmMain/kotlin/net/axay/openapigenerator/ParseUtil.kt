package net.axay.openapigenerator

infix fun <E> E.singleIn(map: Map<E, *>): Boolean {
    return if (this in map) {
        if (map.size == 1) {
            true
        } else {
            error("Did only expect '$this' in the following object, but there were other keys as well: $map")
        }
    } else false
}
