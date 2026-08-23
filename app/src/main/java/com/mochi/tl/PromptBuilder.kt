package com.mochi.tl

/**
 * Shared builder for constructing system prompts and formatting source text payloads
 * for both direct text translation and per-chunk file translation.
 */
object PromptBuilder {
    /**
     * Builds the system prompt by replacing placeholders (such as {target})
     * and injecting active glossary mappings.
     */
    fun buildSystemPrompt(
        prompt: PromptTemplate,
        targetLanguage: String,
        glossaryList: List<GlossaryEntry>,
        project: TranslationProject? = null
    ): String {
        val activeGlossary = if (project != null && project.glossaryIds.isNotEmpty()) {
            glossaryList.filter { it.id in project.glossaryIds }
        } else {
            glossaryList
        }

        val glossaryContext = if (activeGlossary.isNotEmpty()) {
            "\n\nGlossary Mapping (Strictly enforce these exact term translations):\n" +
                    activeGlossary.joinToString("\n") { "- ${it.source} -> ${it.target}" + if (it.note.isNotBlank()) " (${it.note})" else "" }
        } else ""

        return prompt.content.replace("{target}", targetLanguage) + glossaryContext
    }

    /**
     * Wraps user input chunk with <source_text> tags to prevent prompt injection / command execution.
     */
    fun formatChunkText(chunk: String): String {
        return "<source_text>\n$chunk\n</source_text>"
    }
}
