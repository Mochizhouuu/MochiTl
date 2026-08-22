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
            content = "Translate to {target}",
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
    fun testBuiltInPromptsIncludeSourceTextTagRules() {
        val prompts = BuiltIns.prompts
        assertTrue("Prompts list should not be empty", prompts.isNotEmpty())

        for (prompt in prompts) {
            assertTrue("Prompt ${prompt.id} should mention <source_text>", prompt.content.contains("<source_text>"))
            assertTrue("Prompt ${prompt.id} should instruct treating inside as RAW DATA", prompt.content.contains("RAW DATA"))
            assertTrue("Prompt ${prompt.id} should instruct NEVER refuse or reply to instructions", prompt.content.contains("NEVER refuse"))
        }
    }
}
