// Parses AI model IDs and display names into parts you can compare.
// Port of the Go package github.com/BoNour/modelname. Same rules, same tests.
package io.whizi.modelname

data class Model(
    val id: String,
    var vendor: String = "",
    var family: String = "",
    var version: String = "",
    var size: String = "",
    var date: String = "",
    var snapshot: String = "",
    val tags: MutableList<String> = mutableListOf(),
)

object ModelName {
    private val tagWords = setOf(
        "preview", "latest", "exp", "experimental",
        "instruct", "thinking", "beta", "free",
    )

    // First word of a model name, and who makes it.
    val vendors: Map<String, String> = mapOf(
        "aya" to "cohere", "chatgpt" to "openai", "claude" to "anthropic",
        "codestral" to "mistral", "codex" to "openai", "command" to "cohere",
        "deepseek" to "deepseek", "devstral" to "mistral", "ernie" to "baidu",
        "gemini" to "google", "gemma" to "google", "glm" to "zhipu",
        "gpt" to "openai", "granite" to "ibm", "grok" to "xai",
        "hunyuan" to "tencent", "hy" to "tencent", "jamba" to "ai21",
        "kimi" to "moonshot", "ling" to "inclusionai", "llama" to "meta",
        "longcat" to "meituan", "magistral" to "mistral", "mercury" to "inception",
        "mimo" to "xiaomi", "minimax" to "minimax", "ministral" to "mistral",
        "mistral" to "mistral", "mixtral" to "mistral", "muse" to "meta",
        "nemotron" to "nvidia", "nova" to "amazon", "o1" to "openai",
        "o3" to "openai", "o4" to "openai", "phi" to "microsoft",
        "pixtral" to "mistral", "qwen" to "alibaba", "qwq" to "alibaba",
        "r1" to "deepseek", "ring" to "inclusionai", "seed" to "bytedance",
        "solar" to "upstage", "sonar" to "perplexity", "step" to "stepfun",
    )

    // OpenRouter and friends put the vendor in front, spelled their own way.
    val prefixes: Map<String, String> = mapOf(
        "ibm-granite" to "ibm", "meta-llama" to "meta", "mistralai" to "mistral",
        "moonshotai" to "moonshot", "qwen" to "alibaba", "x-ai" to "xai", "z-ai" to "zhipu",
    )

    /** Splits a model ID or display name into its parts. Never fails. */
    fun parse(id: String): Model {
        val m = Model(id)
        var s = id.trim().lowercase()

        // OpenRouter style suffixes, e.g. ":free" or ":thinking".
        val colon = s.indexOf(':')
        if (colon >= 0) {
            s.substring(colon + 1).split(':').filter { it.isNotEmpty() }.forEach { m.tags.add(it) }
            s = s.substring(0, colon)
        }

        var prefix = ""
        val slash = s.lastIndexOf('/')
        if (slash >= 0) {
            prefix = s.substring(0, slash)
            s = s.substring(slash + 1)
        }

        val toks = tokenize(s)
        val words = mutableListOf<String>()
        var i = 0
        while (i < toks.size) {
            val t = toks[i]
            if (i + 2 < toks.size && isYear(t) && isDigits(toks[i + 1], 2) && isDigits(toks[i + 2], 2)) {
                val d = parseDate(t + toks[i + 1] + toks[i + 2])
                if (d.isNotEmpty()) {
                    m.date = d
                    i += 3
                    continue
                }
                m.snapshot = t
            } else if (isDigits(t, 8)) {
                val d = parseDate(t)
                if (d.isNotEmpty()) m.date = d else m.snapshot = t
            } else if (isSize(t)) {
                m.size = t
                if (i + 1 < toks.size && isActive(toks[i + 1])) {
                    m.size += "-" + toks[i + 1]
                    i++
                }
            } else if (t in tagWords) {
                m.tags.add(t)
            } else if (m.version.isEmpty() && startsWithDigit(t) && !isLongNumber(t)) {
                m.version = t
                i = absorb(m, toks, i)
            } else if (m.version.isEmpty() && i == 0 && splitGlued(t).isNotEmpty()) {
                // qwen3.5, hy3
                val v = splitGlued(t)
                words.add(t.substring(0, t.length - v.length))
                m.version = v
                i = absorb(m, toks, i)
            } else if (m.version.isEmpty() && i > 0 && isLetterVersion(t)) {
                // v3.2, k2, m2, mk1
                m.version = t
                if (t[0] == 'v' && startsWithDigit(t.substring(1))) m.version = t.substring(1)
                i = absorb(m, toks, i)
            } else if (isLongNumber(t)) {
                m.snapshot = t
            } else if (m.version.isNotEmpty() && isDigits(t, 1, 2)) {
                // stray numbers after the name, like the 03-25 in gemini-2.5-pro-exp-03-25
                if (m.snapshot.isNotEmpty()) m.snapshot += "-"
                m.snapshot += t
            } else {
                words.add(t)
            }
            i++
        }

        m.family = words.joinToString("-")
        m.vendor = when {
            prefix.isNotEmpty() -> prefixes[prefix] ?: prefix
            words.isNotEmpty() -> vendors[words[0]] ?: ""
            else -> ""
        }
        return m
    }

    // absorb joins dash-separated version parts, so claude-opus-4-1 reads as 4.1.
    private fun absorb(m: Model, toks: List<String>, start: Int): Int {
        var i = start
        while (i + 1 < toks.size && isDigits(toks[i + 1], 1, 2)) {
            m.version += "." + toks[i + 1]
            i++
        }
        return i
    }

    private fun tokenize(s: String): List<String> =
        s.split(Regex("[-_ ()]"))
            .map { it.trim('.', ',') }
            .filter { it.isNotEmpty() }

    // Real month, real day, or "".
    private fun parseDate(s: String): String {
        if (!Regex("^[0-9]{8}$").matches(s)) return ""
        val y = s.substring(0, 4).toInt()
        val mo = s.substring(4, 6).toInt()
        val d = s.substring(6, 8).toInt()
        if (mo < 1 || mo > 12 || d < 1) return ""
        val leap = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
        val days = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)[mo - 1]
        if (d > days) return ""
        return "${s.substring(0, 4)}-${s.substring(4, 6)}-${s.substring(6, 8)}"
    }

    private fun isDigits(t: String, vararg lengths: Int): Boolean =
        t.isNotEmpty() && t.all { it in '0'..'9' } && t.length in lengths

    private fun isYear(t: String) = isDigits(t, 4) && t.startsWith("20")

    // Three or more plain digits is a date code, not a version.
    private fun isLongNumber(t: String) = isDigits(t, 3, 4, 5, 6, 7)

    private fun startsWithDigit(t: String) = t.isNotEmpty() && t[0] in '0'..'9'

    // Parameter counts: 8b, 70b, 2.4t, 135m.
    private fun isSize(t: String): Boolean {
        val n = t.length
        if (n < 2 || t[n - 1] !in "bmt") return false
        return isNumber(t.substring(0, n - 1))
    }

    // The active-parameter half of a MoE size, like a3b.
    private fun isActive(t: String) =
        t.length > 2 && t[0] == 'a' && t.last() == 'b' && isNumber(t.substring(1, t.length - 1))

    private fun isNumber(t: String): Boolean {
        if (t.isEmpty() || t[0] == '.' || t.last() == '.') return false
        if (!t.all { it in '0'..'9' || it == '.' }) return false
        return t.count { it == '.' } <= 1
    }

    // Trailing version of a name like qwen3.5 or hy3. Single letters are left
    // alone so o3 and r1 stay names.
    private fun splitGlued(t: String): String {
        val i = t.indexOfFirst { it in '0'..'9' }
        if (i < 2 || !isVersionish(t.substring(i))) return ""
        if (!t.substring(0, i).all { it in 'a'..'z' }) return ""
        return t.substring(i)
    }

    // v2.5, k2, m2, n2, mk1.
    private fun isLetterVersion(t: String): Boolean {
        val i = t.indexOfFirst { it in '0'..'9' }
        if (i < 1 || i > 2) return false
        if (!t.substring(0, i).all { it in 'a'..'z' }) return false
        return isVersionish(t.substring(i))
    }

    private fun isVersionish(t: String): Boolean {
        if (!isNumber(t.replace(".", ""))) return false
        return t.last() != '.'
    }

    // ---- display ----

    private val vendorNames = mapOf(
        "ai21" to "AI21", "alibaba" to "Alibaba", "amazon" to "Amazon", "anthropic" to "Anthropic",
        "baidu" to "Baidu", "bytedance" to "ByteDance", "cohere" to "Cohere", "deepseek" to "DeepSeek",
        "google" to "Google", "ibm" to "IBM", "inception" to "Inception", "inclusionai" to "inclusionAI",
        "meituan" to "Meituan", "meta" to "Meta", "microsoft" to "Microsoft", "minimax" to "MiniMax",
        "mistral" to "Mistral", "moonshot" to "Moonshot AI", "nvidia" to "NVIDIA", "openai" to "OpenAI",
        "perplexity" to "Perplexity", "stepfun" to "StepFun", "tencent" to "Tencent", "upstage" to "Upstage",
        "xai" to "xAI", "xiaomi" to "Xiaomi", "zhipu" to "Zhipu",
    )

    private val wordNames = mapOf(
        "chatgpt" to "ChatGPT", "deepseek" to "DeepSeek", "glm" to "GLM", "gpt" to "GPT", "kat" to "KAT",
        "mimo" to "MiMo", "minimax" to "MiniMax", "o1" to "o1", "o3" to "o3", "o4" to "o4", "qwq" to "QwQ",
    )

    private val knownVendors: Set<String> = vendors.values.toSet() + prefixes.values.toSet()

    private fun cap(s: String) = s.replaceFirstChar { it.uppercaseChar() }

    fun vendorName(v: String) = vendorNames[v] ?: cap(v)

    fun familyName(f: String) =
        f.split('-').filter { it.isNotEmpty() }.joinToString(" ") { wordNames[it] ?: cap(it) }

    /** "Anthropic · Claude Sonnet · 4.5" */
    fun title(m: Model): String =
        listOf(vendorName(m.vendor), familyName(m.family), cap(m.version))
            .filter { it.isNotEmpty() }
            .joinToString(" · ")

    /** "size 70b · snapshot 0613 · free, instruct", or "" if there is nothing extra. */
    fun details(m: Model): String {
        val out = mutableListOf<String>()
        if (m.size.isNotEmpty()) out.add("size ${m.size}")
        if (m.date.isNotEmpty()) out.add("date ${m.date}")
        if (m.snapshot.isNotEmpty()) out.add("snapshot ${m.snapshot}")
        if (m.tags.isNotEmpty()) out.add(m.tags.joinToString(", "))
        return out.joinToString(" · ")
    }

    private val allowed = Regex("^[A-Za-z0-9 ._:/()+-]+$")

    /** The parsed model if text looks like a real model ID, else null. */
    fun modelIn(text: String): Model? {
        val s = text.trim()
        if (s.length < 2 || s.length > 80 || !allowed.matches(s) || s.none { it in '0'..'9' }) return null
        val colon = s.indexOf(':')
        if (colon >= 0 && s.substring(colon).contains(' ')) return null
        var m = parse(s)
        // "openrouter/anthropic/claude-sonnet-4.5": try the last part on its own.
        if (m.vendor !in knownVendors && s.contains('/')) m = parse(s.substring(s.lastIndexOf('/') + 1))
        return if (m.family.isNotEmpty() && m.vendor in knownVendors) m else null
    }
}
