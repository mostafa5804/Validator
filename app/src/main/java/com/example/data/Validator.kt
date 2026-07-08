package com.example.data

object Validator {

    fun validateCardNumber(cardNumber: String): Boolean {
        val sanitized = cardNumber.replace(Regex("[^0-9]"), "")
        if (sanitized.length != 16) return false
        var sum = 0
        for (i in 0 until 16) {
            val char = sanitized[i]
            if (!char.isDigit()) return false
            var digit = Character.getNumericValue(char)
            if (i % 2 == 0) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return sum % 10 == 0
    }

    fun validateShaba(shabaWithoutIR: String): Boolean {
        val sanitized = shabaWithoutIR.replace("IR", "").replace(Regex("[^0-9]"), "")
        if (sanitized.length != 24 || !sanitized.all { it.isDigit() }) return false
        
        val checkDigits = sanitized.substring(0, 2)
        val bban = sanitized.substring(2)
        val formattedShaba = bban + "1827" + checkDigits
        
        var remainder = 0
        for (i in formattedShaba.indices) {
            val digit = Character.getNumericValue(formattedShaba[i])
            remainder = (remainder * 10 + digit) % 97
        }
        
        return remainder == 1
    }

    fun validateNationalId(nationalId: String): Boolean {
        val sanitized = nationalId.replace(Regex("[^0-9]"), "")
        if (sanitized.length != 10) return false
        
        if (sanitized.all { it == sanitized[0] }) return false

        val controlDigit = Character.getNumericValue(sanitized[9])
        var sum = 0
        for (i in 0 until 9) {
            sum += Character.getNumericValue(sanitized[i]) * (10 - i)
        }

        val remainder = sum % 11
        return if (remainder < 2) {
            controlDigit == remainder
        } else {
            controlDigit == (11 - remainder)
        }
    }
}
