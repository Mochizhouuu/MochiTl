package com.mochi.tl

import org.junit.Assert.*
import org.junit.Test

class LanguageOptionsTest {

    @Test
    fun testAutoDetectConstant() {
        assertEquals("Deteksi Otomatis", LanguageOptions.AUTO_DETECT)
    }

    @Test
    fun testTargetLanguagesList() {
        val expectedLanguages = listOf(
            "Indonesia",
            "Inggris",
            "Jepang",
            "Korea",
            "Mandarin Simplified",
            "Mandarin Traditional",
            "Spanyol",
            "Prancis",
            "Jerman",
            "Vietnam",
            "Thailand",
            "Melayu"
        )
        assertEquals(expectedLanguages, LanguageOptions.TARGET_LANGUAGES)
        assertFalse(LanguageOptions.TARGET_LANGUAGES.contains(LanguageOptions.AUTO_DETECT))
    }

    @Test
    fun testSourceLanguagesList() {
        assertEquals(LanguageOptions.AUTO_DETECT, LanguageOptions.SOURCE_LANGUAGES.first())
        assertEquals(LanguageOptions.TARGET_LANGUAGES.size + 1, LanguageOptions.SOURCE_LANGUAGES.size)
        assertEquals(
            listOf(LanguageOptions.AUTO_DETECT) + LanguageOptions.TARGET_LANGUAGES,
            LanguageOptions.SOURCE_LANGUAGES
        )
    }
}
