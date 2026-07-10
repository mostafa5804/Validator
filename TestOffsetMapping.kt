import kotlin.test.assertTrue

fun main() {
    val texts = listOf("123", "1234", "12345", "12345678", "123456789", "123456789012", "1234567890123456", "12345678901234567890")
    
    for (text in texts) {
        val trimmed = if (text.length >= 16) text.substring(0..15) else text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i != 15) out += "-"
        }
        
        for (offset in 0..text.length) {
            val trans = if (offset <= 3) offset
                else if (offset <= 7) offset + 1
                else if (offset <= 11) offset + 2
                else if (offset <= 16) offset + 3
                else 19
            if (trans > out.length) {
                println("CRASH in Card: text length ${text.length}, offset $offset, trans $trans, out length ${out.length}")
            }
        }
    }
}
