cat << 'INNEREOF' > ShabaPatch.txt
class ShabaTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val trimmed = if (text.text.length >= 24) text.text.substring(0..23) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i != 23) out += " "
        }
        val offsetMapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = if (offset > 24) 24 else offset
                return clamped + (clamped / 4) - (if (clamped == 24) 1 else 0)
            }
            override fun transformedToOriginal(offset: Int): Int {
                val orig = offset - (offset / 5)
                return if (orig > 24) 24 else orig
            }
        }
        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString("IR" + out), offsetMapping) // Wait, prepend IR?
    }
}
INNEREOF
