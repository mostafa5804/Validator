package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.R

data class BankInfo(
    val bankName: String,
    val shabaCode: String,      // 2-digit IBAN code as per user's list
    val cardPrefixes: List<String>, // 6-digit Card prefixes
    val color: Color,
    val logoResId: Int? = null
)

object BankRepository {
    val banks = listOf(
        BankInfo("آینده", "62", listOf("636214"), Color(0xFF8D6E63), R.drawable.ic_bank_ayandeh),
        BankInfo("اقتصاد نوین", "55", listOf("627412"), Color(0xFF6A1B9A), R.drawable.ic_bank_eghtesad_novin),
        BankInfo("ایران زمین", "69", listOf("505785"), Color(0xFF00897B), R.drawable.ic_bank_iran_zamin),
        BankInfo("پارسیان", "54", listOf("622106"), Color(0xFFC2185B), R.drawable.ic_bank_parsian),
        BankInfo("پاسارگاد", "57", listOf("502229"), Color(0xFFFFB300), R.drawable.ic_bank_pasargad),
        BankInfo("تجارت", "18", listOf("627353"), Color(0xFF0097A7), R.drawable.ic_bank_tejarat),
        BankInfo("توسعه تعاون", "22", listOf("502908"), Color(0xFF1565C0), R.drawable.ic_bank_tosee_taavon),
        BankInfo("توسعه صادرات ایران", "20", listOf("207177"), Color(0xFF4527A0), R.drawable.ic_bank_export_development),
        BankInfo("خاورمیانه", "80", listOf("585983"), Color(0xFFFFA000), R.drawable.ic_bank_middle_east),
        BankInfo("دی", "66", listOf("502938"), Color(0xFFD81B60), R.drawable.ic_bank_dey),
        BankInfo("رفاه کارگران", "13", listOf("589463"), Color(0xFF1976D2), R.drawable.ic_bank_refah),
        BankInfo("سامان", "56", listOf("621986"), Color(0xFF0288D1), R.drawable.ic_bank_saman),
        BankInfo("سپه", "15", listOf("589210"), Color(0xFF388E3C), R.drawable.ic_bank_sepah),
        BankInfo("سرمایه", "58", listOf("639607"), Color(0xFF5C6BC0), R.drawable.ic_bank_sarmayeh),
        BankInfo("سینا", "59", listOf("639346"), Color(0xFF00BCD4), R.drawable.ic_bank_sina),
        BankInfo("شهر", "61", listOf("502806"), Color(0xFFEF5350), R.drawable.ic_bank_shahr),
        BankInfo("صادرات ایران", "19", listOf("603769"), Color(0xFF283593), R.drawable.ic_bank_saderat),
        BankInfo("صنعت و معدن", "11", listOf("627961"), Color(0xFF9E9E9E), R.drawable.ic_bank_industry_and_mine),
        BankInfo("قرض الحسنه رسالت", "70", listOf("504172"), Color(0xFF3949AB), R.drawable.ic_bank_resalat),
        BankInfo("قرض الحسنه مهر ایران", "60", listOf("606373"), Color(0xFF43A047), R.drawable.ic_bank_mehr_iran),
        BankInfo("کارآفرین", "53", listOf("627488"), Color(0xFFFBC02D), R.drawable.ic_bank_karafarin),
        BankInfo("کشاورزی", "16", listOf("603770"), Color(0xFF2E7D32), R.drawable.ic_bank_keshavarzi),
        BankInfo("گردشگری", "64", listOf("505416"), Color(0xFFE53935), R.drawable.ic_bank_tourism),
        BankInfo("مسکن", "14", listOf("628023"), Color(0xFFF57C00), R.drawable.ic_bank_maskan),
        BankInfo("ملت", "12", listOf("610433"), Color(0xFFD32F2F), R.drawable.ic_bank_mellat),
        BankInfo("ملی ایران", "17", listOf("603799"), Color(0xFF0277BD), R.drawable.ic_bank_melli),
        BankInfo("پست بانک ایران", "21", listOf("627760"), Color(0xFF4CAF50), R.drawable.ic_bank_post_bank),
        BankInfo("موسسه اعتباری ملل", "75", listOf("606256"), Color(0xFF7CB342), R.drawable.ic_bank_melal),
        BankInfo("موسسه اعتباری نور", "75", listOf("207177"), Color(0xFF009688), R.drawable.ic_bank_noor)
    )

    fun findBankByCard(cardNumber: String): BankInfo? {
        val sanitized = cardNumber.replace("-", "").replace(" ", "")
        if (sanitized.length < 6) return null
        val prefix = sanitized.substring(0, 6)
        return banks.find { it.cardPrefixes.contains(prefix) }
    }

    fun findBankByShaba(shabaNumber: String): BankInfo? {
        val sanitized = shabaNumber.replace("IR", "").replace(" ", "").trim()
        if (sanitized.length < 5) return null // Need enough characters to extract bank code safely.
        // For IR Shaba, if IR is removed, the format is CCBBB...
        // CC (2 digits) + BBB (3 digit bank code usually starting with 0).
        // e.g., IR12017... -> 12017... -> BBB = "017" -> matching with user list "17" (shabaCode)
        val bbb = sanitized.substring(2, 5) // "017"
        val code = bbb.trimStart('0') // "17"
        return banks.find { it.shabaCode == code }
    }
}
