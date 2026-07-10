import kotlin.test.assertTrue

fun main() {
    val texts = listOf("123", "1234", "12345", "12345678", "123456789", "123456789012", "1234567890123456", "12345678901234567890")
    
    for (text in texts) {
        val trimmed = text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i != trimmed.lastIndex) out += "-"
        }
        
        for (offset in 0..text.length) {
            val trans = offset + (offset / 4) - if (offset > 0 && offset % 4 == 0) 1 else 0
            if (trans > out.length || trans < 0) {
                println("CRASH in Card: text length ${text.length}, offset $offset, trans $trans, out length ${out.length}")
            }
        }
        for (offset in 0..out.length) {
            val orig = offset - (offset / 5)
            if (orig > text.length || orig < 0) {
                println("CRASH in Card orig: out length ${out.length}, offset $offset, orig $orig, text length ${text.length}")
            }
        }
    }
}
