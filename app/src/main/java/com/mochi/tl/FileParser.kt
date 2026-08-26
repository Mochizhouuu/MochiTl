package com.mochi.tl

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URLDecoder
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Pembaca teks dari berbagai format dokumen:
 * - TXT  : baca langsung sebagai UTF-8.
 * - DOCX : ZIP berisi word/document.xml — teks diekstrak dari <w:t>,
 *          baris baru mengikuti batas paragraf <w:p>.
 * - EPUB : ZIP berisi XHTML; urutan baca mengikuti spine di OPF
 *          (container.xml -> content.opf), fallback urutan nama file.
 * - PDF  : via PDFBox Android (pdfbox-android).
 */
object FileParser {

    enum class Format { TXT, EPUB, DOCX, PDF }

    fun readText(context: Context, uri: Uri): String = try {
        when (detectFormat(context, uri)) {
            Format.TXT -> context.contentResolver.openInputStream(uri)?.use(::txtText).orEmpty()
            Format.DOCX -> context.contentResolver.openInputStream(uri)?.use(::docxText)
                ?: throw IOException("Tidak dapat membuka file")
            Format.EPUB -> context.contentResolver.openInputStream(uri)?.use(::epubText)
                ?: throw IOException("Tidak dapat membuka file")
            Format.PDF -> {
                PDFBoxResourceLoader.init(context.applicationContext)
                context.contentResolver.openInputStream(uri)?.use(::pdfText)
                    ?: throw IOException("Tidak dapat membuka file")
            }
        }
    } catch (e: Exception) {
        "Gagal membaca file: ${e.message ?: "format tidak didukung"}"
    }

    /** Deteksi format dari ekstensi nama file, fallback ke TXT. */
    fun detectFormat(context: Context, uri: Uri): Format {
        var name: String? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(0)
            }
        }
        return when (name?.lowercase()?.substringAfterLast('.')?.takeIf { it.isNotEmpty() }) {
            "epub" -> Format.EPUB
            "docx" -> Format.DOCX
            "pdf" -> Format.PDF
            else -> Format.TXT
        }
    }

    // ===== TXT =====

    private fun txtText(input: InputStream): String =
        BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()

    // ===== ZIP helpers =====

    /**
     * Iterasi seluruh entri ZIP dari salinan byte [data]; callback dipanggil
     * untuk setiap entri dan boleh mengembalikan hasil non-null untuk
     * menghentikan pencarian.
     */
    private inline fun <T> forEachZipEntry(data: ByteArray, onEntry: (ZipEntry, InputStream) -> T?): T? {
        ZipInputStream(ByteArrayInputStream(data)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                onEntry(entry, zip)?.let { return it }
                entry = zip.nextEntry
            }
        }
        return null
    }

    private fun readZipEntryBytes(input: InputStream): ByteArray =
        input.readBytes()

    // ===== DOCX =====

    private fun docxText(input: InputStream): String {
        val data = input.readBytes()
        val documentXml = forEachZipEntry(data) { entry, stream ->
            if (entry.name.equals("word/document.xml", ignoreCase = true)) readZipEntryBytes(stream) else null
        } ?: throw IOException("document.xml tidak ditemukan — bukan file DOCX valid")

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(documentXml), null)

        val sb = StringBuilder()
        var insideTextRun = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "t" -> insideTextRun = true
                    "p", "br" -> sb.append('\n')
                    "tab" -> sb.append('\t')
                }
                XmlPullParser.TEXT -> if (insideTextRun) sb.append(parser.text)
                XmlPullParser.END_TAG -> if (parser.name == "t") insideTextRun = false
            }
            event = parser.next()
        }
        return normalizeParagraphs(sb.toString())
    }

    // ===== EPUB =====

    private fun epubText(input: InputStream): String {
        val data = input.readBytes()

        // Pass 1: META-INF/container.xml -> path file OPF.
        val opfPath = forEachZipEntry(data) { entry, stream ->
            if (entry.name.equals("META-INF/container.xml", ignoreCase = true)) {
                parseContainerRootFile(readZipEntryBytes(stream))
            } else null
        } ?: throw IOException("container.xml tidak ditemukan — bukan file EPUB valid")

        // Pass 2: OPF -> daftar href dokumen konten sesuai urutan spine.
        val opfDir = opfPath.substringBeforeLast('/', "")
        val spineHrefs = forEachZipEntry(data) { entry, stream ->
            if (entry.name == opfPath) parseOpfSpine(readZipEntryBytes(stream)) else null
        } ?: throw IOException("OPF tidak ditemukan — bukan file EPUB valid")

        val normalizedHrefs = spineHrefs.mapNotNull { href ->
            val decoded = URLDecoder.decode(href.trim(), "UTF-8")
            if (decoded.isBlank()) return@mapNotNull null
            val fullPath = buildString {
                if (!decoded.startsWith('/')) {
                    if (opfDir.isNotEmpty()) append(opfDir).append('/')
                }
                append(decoded.substringBefore('#'))
            }
            fullPath.replace("//", "/").removePrefix("/")
        }.filter { it.isNotBlank() }

        // Pass 3: ekstrak teks tiap dokumen sesuai urutan spine.
        val sb = StringBuilder()
        for (href in normalizedHrefs) {
            val text = forEachZipEntry(data) { entry, stream ->
                if (entry.name == href && isHtmlEntry(entry.name)) xhtmlText(readZipEntryBytes(stream)) else null
            }
            if (!text.isNullOrBlank()) {
                sb.append(text).append("\n\n")
            }
        }
        val result = sb.toString().trim()
        if (result.isEmpty()) throw IOException("Tidak ada dokumen teks ditemukan di EPUB")
        return result
    }

    private fun isHtmlEntry(name: String): Boolean =
        name.lowercase().substringAfterLast('.').let { it == "xhtml" || it == "html" || it == "htm" }

    /** container.xml → nilai atribut full-path dari rootfile. */
    private fun parseContainerRootFile(data: ByteArray): String? {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(data), null)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "rootfile") {
                for (i in 0 until parser.attributeCount) {
                    if (parser.getAttributeName(i) == "full-path") return parser.getAttributeValue(i)
                }
            }
            event = parser.next()
        }
        return null
    }

    /**
     * OPF → daftar href dokumen konten mengikuti <spine>. Fallback: semua
     * manifest item ber-ekstensi html jika elemen spine tidak ada.
     */
    private fun parseOpfSpine(data: ByteArray): List<String> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(data), null)

        val manifest = mutableMapOf<String, String>() // id -> href
        val spineIds = mutableListOf<String>()
        var insideManifest = false
        var insideSpine = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "manifest" -> insideManifest = true
                    "spine" -> insideSpine = true
                    "item" -> if (insideManifest) {
                        val id = (0 until parser.attributeCount)
                            .firstOrNull { parser.getAttributeName(it) == "id" }
                            ?.let { parser.getAttributeValue(it) }
                        val href = (0 until parser.attributeCount)
                            .firstOrNull { parser.getAttributeName(it) == "href" }
                            ?.let { parser.getAttributeValue(it) }
                        if (id != null && href != null) manifest[id] = href
                    }
                    "itemref" -> if (insideSpine) {
                        (0 until parser.attributeCount)
                            .firstOrNull { parser.getAttributeName(it) == "idref" }
                            ?.let { spineIds.add(parser.getAttributeValue(it)) }
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "manifest" -> insideManifest = false
                    "spine" -> insideSpine = false
                }
            }
            event = parser.next()
        }

        val fromSpine = spineIds.mapNotNull { manifest[it] }
        if (fromSpine.isNotEmpty()) return fromSpine
        return manifest.values.filter { isHtmlEntry(it) }
    }

    /** Ekstrak teks dari XHTML/HTML: baris baru pada blok p/div/h/li/br/tr. */
    private fun xhtmlText(data: ByteArray): String {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(data), null)

        val sb = StringBuilder()
        val blockTags = setOf("p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li", "tr")
        var skipContent = 0 // kedalaman <head>/<style>/<script>

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase()) {
                        "head", "style", "script" -> skipContent++
                        "br" -> sb.append('\n')
                    }
                }
                XmlPullParser.TEXT -> if (skipContent == 0) sb.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name.lowercase()) {
                    "head", "style", "script" -> if (skipContent > 0) skipContent--
                    in blockTags -> sb.append('\n')
                }
            }
            event = parser.next()
        }
        return normalizeParagraphs(sb.toString())
    }

    private fun normalizeParagraphs(text: String): String =
        text.replace(Regex("[ \\t]*\\n[ \\t]*"), "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

    // ===== PDF =====

    private fun pdfText(input: InputStream): String =
        PDDocument.load(BufferedInputStream(input)).use { document ->
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            stripper.getText(document).trim()
        }
}
