package io.whizi.modelname

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelNameTest {
    private fun check(input: String, vendor: String, family: String, version: String = "",
                      size: String = "", date: String = "", snapshot: String = "", tags: List<String> = emptyList()) {
        val m = ModelName.parse(input)
        assertEquals("$input vendor", vendor, m.vendor)
        assertEquals("$input family", family, m.family)
        assertEquals("$input version", version, m.version)
        assertEquals("$input size", size, m.size)
        assertEquals("$input date", date, m.date)
        assertEquals("$input snapshot", snapshot, m.snapshot)
        assertEquals("$input tags", tags.sorted(), m.tags.sorted())
    }

    @Test
    fun apiIds() {
        check("claude-3-5-sonnet-20241022", "anthropic", "claude-sonnet", "3.5", date = "2024-10-22")
        check("claude-sonnet-4-5", "anthropic", "claude-sonnet", "4.5")
        check("claude-opus-4-1-20250805", "anthropic", "claude-opus", "4.1", date = "2025-08-05")
        check("anthropic/claude-haiku-4.5", "anthropic", "claude-haiku", "4.5")
        check("gpt-4o-2024-08-06", "openai", "gpt", "4o", date = "2024-08-06")
        check("gpt-4.1-mini", "openai", "gpt-mini", "4.1")
        check("gpt-4-0613", "openai", "gpt", "4", snapshot = "0613")
        check("chatgpt-4o-latest", "openai", "chatgpt", "4o", tags = listOf("latest"))
        check("o3-mini", "openai", "o3-mini")
        check("gemini-2.0-flash-001", "google", "gemini-flash", "2.0", snapshot = "001")
        check("gemini-2.5-pro-exp-03-25", "google", "gemini-pro", "2.5", snapshot = "03-25", tags = listOf("exp"))
        check("x-ai/grok-4", "xai", "grok", "4")
        check("meta-llama/llama-3.3-70b-instruct:free", "meta", "llama", "3.3", size = "70b", tags = listOf("free", "instruct"))
        check("mistral-large-2411", "mistral", "mistral-large", snapshot = "2411")
        check("mistral-7b-instruct-v0.2", "mistral", "mistral", "0.2", size = "7b", tags = listOf("instruct"))
        check("deepseek/deepseek-v3.2", "deepseek", "deepseek", "3.2")
        check("deepseek-chat-v3-1", "deepseek", "deepseek-chat", "3.1")
        check("qwen/qwen3-235b-a22b", "alibaba", "qwen", "3", size = "235b-a22b")
        check("moonshotai/kimi-k2", "moonshot", "kimi", "k2")
        check("glm-4-6", "zhipu", "glm", "4.6")
    }

    @Test
    fun displayNames() {
        check("Claude Opus 4.8", "anthropic", "claude-opus", "4.8")
        check("GPT-5.4 Nano", "openai", "gpt-nano", "5.4")
        check("Gemini 3.1 Pro Preview", "google", "gemini-pro", "3.1", tags = listOf("preview"))
        check("Qwen3.5 Plus 2026-04-20", "alibaba", "qwen-plus", "3.5", date = "2026-04-20")
        check("Qwen3.6 35B A3B", "alibaba", "qwen", "3.6", size = "35b-a3b")
        check("Kimi K2.7 Code", "moonshot", "kimi-code", "k2.7")
        check("R1", "deepseek", "r1")
    }

    @Test
    fun titleAndFilter() {
        assertEquals("Anthropic · Claude Sonnet · 4.5", ModelName.title(ModelName.parse("claude-sonnet-4-5")))
        assertNull(ModelName.modelIn("step 1: build"))
        assertNull(ModelName.modelIn("src/app-2"))
        assertEquals("anthropic", ModelName.modelIn("openrouter/anthropic/claude-sonnet-4.5")?.vendor)
    }
}
