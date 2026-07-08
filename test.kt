fun testOffset(offset: Int, len: Int): Int {
    val lastIndex = len - 1
    var spaces = 0
    for (i in 0 until offset) {
        if (i % 4 == 3 && i != lastIndex) spaces++
    }
    return offset + spaces
}
fun main() {
    println(testOffset(4, 4))
}
