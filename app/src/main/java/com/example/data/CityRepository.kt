package com.example.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class CityRepository(private val context: Context) {
    private val cityMap = HashMap<String, String>()

    init {
        loadCityCodes()
    }

    private fun loadCityCodes() {
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("city_codes.csv")
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val tokens = line!!.split(",")
                if (tokens.size >= 2) {
                    val code = tokens[0].trim().padStart(3, '0') // استانداردسازی به ۳ رقم
                    val name = tokens[1].trim()
                    cityMap[code] = name
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCityByNationalId(nationalId: String): String {
        if (nationalId.length < 3) return "نامشخص"
        val prefix = nationalId.substring(0, 3)
        return cityMap[prefix] ?: "کد شهر ناشناخته"
    }

    fun getAllCities(): Map<String, String> {
        return cityMap
    }
}
