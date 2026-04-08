# AInalyzer

An open-source Burp Suite extension that uses an AI assistant to propose and execute iterative security tests against HTTP endpoints with analyst oversight.

![AInalyzer interface](images/image_ainalyzer.png)

## Features

- Generate 3 to 5 follow-up security tasks from a captured HTTP request/response pair
- Execute one AI-proposed test step at a time with analyst review between steps
- Show task history, generated requests, server responses, and AI summaries in one tab
- Keep prior step context for the selected task so the model can refine the next request
- Work with OpenAI-compatible chat completion APIs

## Requirements

- Burp Suite Professional
- JDK 21 or newer
- An OpenAI-compatible API endpoint and model

## Build

```bash
./gradlew clean build
```

The extension JAR is produced under `build/libs/`.

## Load In Burp

1. Open Burp Suite Professional.
2. Go to `Extensions` -> `Installed` -> `Add`.
3. Choose extension type `Java`.
4. Select the built JAR from `build/libs/`.
5. Confirm the extension loads without errors.

## Configure

1. Open the `AInalyzer` tab in Burp.
2. Choose a provider preset.
3. Confirm or adjust the endpoint and model.
4. Enter an API key when using OpenAI.
5. Save the settings.

Provider presets:

- `Local / OpenAI-compatible`
  - for Ollama, LM Studio, and similar local servers
  - default endpoint: `http://localhost:11434/v1/chat/completions`
- `OpenAI`
  - default endpoint: `https://api.openai.com/v1/chat/completions`
  - requires bearer-token auth

Example models:

- Ollama: `llama3.1`, `qwen2.5`
- OpenAI: `gpt-4.1-mini`, `gpt-4.1`, `o4-mini`

## Usage

1. Capture or open a request in Burp.
2. Right-click and choose `Send to AInalyzer`.
3. Wait for the extension to generate tasks.
4. Select a task and click `Next` to execute the next proposed step.
5. Review the generated request, response, and summary before continuing.

## Development Notes

- The project currently targets Java 21.
- Tests use JUnit 5.
- Some provider-specific chat completion quirks may require prompt or response-format adjustments.
