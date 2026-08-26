package com.mochi.tl

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class MochiViewModelTest {
    private lateinit var viewModel: MochiViewModel

    @Before
    fun setUp() {
        viewModel = MochiViewModel(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun testSaveAndDeleteProject() {
        val project = TranslationProject(
            id = "proj_test_1",
            name = "Test Project",
            description = "Project Description",
            promptTemplateId = "builtin_novel",
            providerId = "openai",
            glossaryIds = listOf("g1", "g2"),
            targetLanguage = "Indonesia"
        )

        viewModel.saveProject(project)
        assertTrue(viewModel.projects.value.any { it.id == "proj_test_1" })

        viewModel.selectProject(project)
        assertEquals("proj_test_1", viewModel.activeProject.value?.id)
        assertEquals("builtin_novel", viewModel.activePrompt.value.id)
        assertEquals("openai", viewModel.activeProvider.value.id)

        viewModel.deleteProject("proj_test_1")
        assertFalse(viewModel.projects.value.any { it.id == "proj_test_1" })
        assertNull(viewModel.activeProject.value)
    }

    @Test
    fun testPromptManagement() {
        val customPrompt = PromptTemplate(
            id = "custom_1",
            name = "Custom Prompt",
            content = "Gunakan bahasa gaul",
            category = "custom",
            description = "Desc",
            isBuiltIn = false
        )

        viewModel.savePrompt(customPrompt)
        assertTrue(viewModel.prompts.value.any { it.id == "custom_1" })

        viewModel.selectPrompt(customPrompt)
        assertEquals("custom_1", viewModel.activePrompt.value.id)

        viewModel.deletePrompt("custom_1")
        assertFalse(viewModel.prompts.value.any { it.id == "custom_1" })
        assertNotEquals("custom_1", viewModel.activePrompt.value.id)
    }

    @Test
    fun testGlossaryManagement() {
        val glossaryItem = GlossaryEntry(
            id = "glossary_1",
            source = "Mochi",
            target = "Kue Mochi",
            note = "Makanan"
        )

        viewModel.saveGlossaryItem(glossaryItem)
        assertTrue(viewModel.glossary.value.any { it.id == "glossary_1" })

        val exportedJson = viewModel.exportGlossaryJson()
        assertTrue(exportedJson.contains("Kue Mochi"))

        viewModel.deleteGlossaryItem("glossary_1")
        assertFalse(viewModel.glossary.value.any { it.id == "glossary_1" })

        val importResult = viewModel.importGlossaryJson(exportedJson)
        assertTrue(importResult.isSuccess)
        assertEquals(1, importResult.getOrNull())
        assertTrue(viewModel.glossary.value.any { it.id == "glossary_1" })
    }

    @Test
    fun testOpenAiCompatibleProviderExists() {
        val providers = viewModel.providers.value
        val openAiCompat = providers.find { it.id == "openaicompatible" }
        assertNotNull(openAiCompat)
        assertEquals("OpenAI Compatible", openAiCompat?.name)
        assertNotEquals("openai", openAiCompat?.id)
    }

    @Test
    fun testModelSelectionAndPersistence() {
        val openAiCompat = viewModel.providers.value.first { it.id == "openaicompatible" }
        viewModel.selectProvider(openAiCompat)

        viewModel.setModelForActiveProvider("gpt-4o-custom")
        assertEquals("gpt-4o-custom", viewModel.customModel)
        assertEquals("gpt-4o-custom", viewModel.storageModelFor("openaicompatible"))

        viewModel.setModelForActiveProvider("")
        assertNull(viewModel.customModel)
    }

    @Test
    fun testBuiltInPromptsAreNaturalLanguageStyleRules() {
        val prompts = BuiltIns.prompts
        assertTrue("Prompts list should not be empty", prompts.isNotEmpty())

        for (prompt in prompts) {
            assertFalse("Prompt ${prompt.id} should NOT contain technical {target} variable", prompt.content.contains("{target}"))
            assertFalse("Prompt ${prompt.id} should NOT contain technical <source_text> tag", prompt.content.contains("<source_text>"))
        }
    }

    @Test
    fun testBuildSystemPromptSystemStructureAndGlossary() {
        val prompt = PromptTemplate("p1", "Test", "Gunakan bahasa gaul", "cat")
        val glossaryEntries = listOf(GlossaryEntry("g1", "Mochi", "Kue Mochi", "snack"))

        val resultWithoutProject = PromptBuilder.buildSystemPrompt(
            prompt = prompt,
            sourceLanguage = "Jepang",
            targetLanguage = "Indonesia",
            glossaryList = glossaryEntries,
            project = null
        )

        assertTrue(resultWithoutProject.contains("from Jepang into natural, accurate Indonesia"))
        assertTrue(resultWithoutProject.contains("<source_text>"))
        assertTrue(resultWithoutProject.contains("RAW DATA"))
        assertTrue(resultWithoutProject.contains("Additional Style & Preference Rules (Strictly Follow):\nGunakan bahasa gaul"))
        assertTrue(resultWithoutProject.contains("Glossary Mapping"))
        assertTrue(resultWithoutProject.contains("- Mochi -> Kue Mochi (snack)"))

        val project = TranslationProject("proj1", "Project", promptTemplateId = "p1", glossaryIds = listOf("other_glossary_id"))
        val resultWithEmptyProjectGlossary = PromptBuilder.buildSystemPrompt(
            prompt = prompt,
            sourceLanguage = LanguageOptions.AUTO_DETECT,
            targetLanguage = "Inggris",
            glossaryList = glossaryEntries,
            project = project
        )

        assertTrue(resultWithEmptyProjectGlossary.contains("from auto-detected source language into natural, accurate Inggris"))
        assertFalse(resultWithEmptyProjectGlossary.contains("Glossary Mapping"))
    }

    @Test
    fun testFormatChunkTextSourceWrapper() {
        val chunk = "Hello world!"
        val formatted = PromptBuilder.formatChunkText(chunk)
        assertEquals("<source_text>\nHello world!\n</source_text>", formatted)
    }

    @Test
    fun testChunkShortTextReturnsSingleChunk() {
        val text = "Paragraf pertama.\n\nParagraf kedua."
        assertEquals(listOf(text), chunkByParagraphs(text))
    }

    @Test
    fun testChunkBlankTextReturnsEmpty() {
        assertTrue(chunkByParagraphs("   \n  ").isEmpty())
    }

    @Test
    fun testChunkLongTextRespectsMaxCharsAndLineBoundaries() {
        val expectedLines = (1..300).map { "Baris ke-$it berisi kalimat yang cukup panjang untuk pengujian." }
        val longText = expectedLines.joinToString("\n")
        val chunks = chunkByParagraphs(longText, 1000)

        assertTrue("Harus terpecah menjadi beberapa chunk", chunks.size > 1)
        assertTrue(chunks.all { it.length <= 1000 })

        // Tidak boleh memotong di tengah baris/paragraf.
        chunks.forEachIndexed { index, chunk ->
            if (index < chunks.lastIndex) {
                assertTrue(
                    "Chunk harus berakhir di batas baris",
                    chunk.endsWith("\n") || chunk.endsWith(".")
                )
            }
        }

        // Setiap baris harus utuh dan muncul tepat satu kali.
        assertEquals(expectedLines, chunks.flatMap { it.split("\n") })
    }

    @Test
    fun testChunkSingleOversizedParagraphIsForceSplit() {
        val single = "y".repeat(5000)
        val chunks = chunkByParagraphs(single, 4000)
        assertEquals(listOf("y".repeat(4000), "y".repeat(1000)), chunks)
    }
}
