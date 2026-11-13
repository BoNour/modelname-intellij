# Model Name for JetBrains IDEs

Put the caret on an AI model ID, right click, **Parse AI Model ID**.

```
claude-opus-4-1-20250805   ->  Anthropic · Claude Opus · 4.1, date 2025-08-05
meta-llama/llama-3.3-70b-instruct:free  ->  Meta · Llama · 3.3, size 70b, free, instruct
```

Same parser as the Go package [github.com/BoNour/modelname](https://github.com/BoNour/modelname)
and the VS Code extension, ported to Kotlin. No network calls.

Build with `gradle buildPlugin` (JDK 21). The zip lands in `build/distributions/`.

MIT licensed.
