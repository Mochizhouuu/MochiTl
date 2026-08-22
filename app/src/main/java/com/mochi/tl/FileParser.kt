package com.mochi.tl

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

/** Only TXT is advertised until additional native parsers are implemented and tested. */
object FileParser {
    fun readText(context: Context, uri: Uri): String = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        }.orEmpty()
    }.getOrElse { "Gagal membaca file: ${it.message ?: "format tidak didukung"}" }
}
