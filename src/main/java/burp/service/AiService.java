package burp.service;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.model.Step;
import burp.model.Task;
import com.google.gson.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AiService {

  private static final String TASK_GENERATION_PROMPT = """
      You are a senior penetration testing expert.
      Analyze the provided HTTP request and response and propose 3 to 5 high-impact security testing tasks.
      Focus on concrete follow-up tests such as IDOR, SQL injection, broken access control, SSRF, XSS, CSRF, and command injection when relevant.
      Return only JSON matching the provided schema.
      Each item must contain:
      - name: a short human-readable task title
      - description: one concise sentence describing the test objective
      Do not return raw HTTP requests in this task-generation response.
      """;

  private static final String STEP_GENERATION_PROMPT = """
      You are a senior penetration testing expert.
      You will be given a base request, a specific task, and the history of previous steps for that task.
      Determine the next logical single test to execute without repeating prior steps.
      Return only JSON matching the provided schema.

      Field requirements:
      1. name: a short human-readable title for this test. It must not be just a URL, endpoint path, or code snippet.
      2. thought_process: concise reasoning for why this request is the right next step.
      3. request: one complete raw HTTP request string using literal \\r\\n line endings throughout.
      """;

  private static final String SUMMARY_GENERATION_PROMPT = """
      You are a senior penetration testing expert.
      Analyze the provided test request, response, and reasoning, then summarize the security significance of the result.
      Return only JSON matching the provided schema.
      The summary must be concise, factual, and useful for deciding the next follow-up test.
      """;

  private final MontoyaApi api;
  private final Gson gson;

  private final JsonObject taskGenerationSchema;
  private final JsonObject stepGenerationSchema;
  private final JsonObject summaryGenerationSchema;

  private final SettingsService settingsService;

  public AiService(MontoyaApi api, SettingsService settingsService) {
    this.api = api;
    this.settingsService = settingsService; // Store the injected service
    this.gson = new GsonBuilder().create();
    this.taskGenerationSchema = createTaskSchema();
    this.stepGenerationSchema = createStepSchema();
    this.summaryGenerationSchema = createSummarySchema();
  }

  public List<Task> generateTasks(HttpRequestResponse requestResponse) throws Exception {
    String requestStr = requestResponse.request().toString();
    String responseStr = requestResponse.response() != null ? requestResponse.response().toString()
        : "No response provided.";

    String userPrompt = String.format(
        "Analyze the following HTTP interaction and generate the task list.\n\n" +
            "--- REQUEST ---\n%s\n\n" +
            "--- RESPONSE ---\n%s",
        requestStr, responseStr);

    List<JsonObject> messages = new ArrayList<>();
    messages.add(createMessage("system", TASK_GENERATION_PROMPT));
    messages.add(createMessage("user", userPrompt));

    String aiResponse = callAi(messages, this.taskGenerationSchema);
    return parseTasks(aiResponse);
  }

  public Step generateStep(HttpRequestResponse baseReqResp, Task task, List<Step> previousSteps) throws Exception {

    List<JsonObject> messages = new ArrayList<>();
    messages.add(createMessage("system", STEP_GENERATION_PROMPT));

    messages.add(createMessage("user",
        "Start of test.\n" +
            "Task: " + task.getName() + " (" + task.getDescription() + ")\n\n" +
            "--- BASE REQUEST ---\n" + baseReqResp.request().toString()));

    if (previousSteps != null) {
      for (Step prevStep : previousSteps) {
        messages.add(createMessage("assistant", gson.toJson(prevStep.toStepGenerationJson())));

        String resultSummary = prevStep.getSummary() != null ? prevStep.getSummary() : "No summary was generated.";
        String responseStr = prevStep.getResponse() != null ? prevStep.getResponse().toString()
            : "No response was recorded.";

        messages.add(createMessage("user",
            String.format(
                "I executed your request. Here is the result:\n\n" +
                    "--- RESPONSE ---\n%s\n\n" +
                    "--- YOUR ANALYSIS ---\n%s",
                responseStr, resultSummary)));
      }
    }

    String finalPrompt = String.format(
        "Based on all the information above, what is your next step to test *only* for the task: '%s' (%s)?",
        task.getName(),
        task.getDescription());
    messages.add(createMessage("user", finalPrompt));

    String aiResponse = callAi(messages, this.stepGenerationSchema);
    return parseStep(aiResponse);
  }

  public String generateSummary(Step step) throws Exception {
    HttpRequest request = step.getRequest();
    HttpResponse response = step.getResponse();

    if (request == null || response == null) {
      return "Error: Step is missing request or response.";
    }

    String userPrompt = String.format(
        "Analyze the following test step:\n\n" +
            "--- THOUGHTS ---\n%s\n\n" +
            "--- REQUEST ---\n%s\n\n" +
            "--- RESPONSE ---\n%s",
        step.getThoughtProcess(),
        request.toString(),
        response.toString());

    List<JsonObject> messages = new ArrayList<>();
    messages.add(createMessage("system", SUMMARY_GENERATION_PROMPT));
    messages.add(createMessage("user", userPrompt));

    String aiResponse = callAi(messages, this.summaryGenerationSchema);
    return parseSummary(aiResponse);
  }

  private String callAi(List<JsonObject> messages, JsonObject schema) throws Exception {

    String currentApiEndpoint = settingsService.getApiEndpoint();
    String currentModel = settingsService.getModelName();

    if (currentApiEndpoint == null || currentApiEndpoint.trim().isEmpty()) {
      api.logging().logToError("AI API endpoint is not configured.");
      throw new Exception("AI API endpoint is not set. Please configure it in the AInalyzer tab.");
    }
    if (currentModel == null || currentModel.trim().isEmpty()) {
      api.logging().logToError("AI model name is not configured.");
      throw new Exception("AI model name is not set. Please configure it in the AInalyzer tab.");
    }

    URL url = new URL(currentApiEndpoint);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    applyConfiguredHeaders(conn);
    conn.setConnectTimeout(10000); // 10 seconds
    conn.setReadTimeout(6000000); // 100 minutes because my local llms are slow
    conn.setDoOutput(true);

    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("model", currentModel); // Use the variable
    requestBody.add("messages", gson.toJsonTree(messages));

    JsonObject responseFormat = new JsonObject();
    responseFormat.addProperty("type", "json_schema");
    responseFormat.add("json_schema", schema);
    requestBody.add("response_format", responseFormat);

    if (settingsService.getProviderType() == SettingsService.ProviderType.LOCAL_OPENAI_COMPATIBLE) {
      requestBody.addProperty("temperature", 0.1);
    }

    try (OutputStream os = conn.getOutputStream()) {
      byte[] input = gson.toJson(requestBody).getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    StringBuilder response = new StringBuilder();
    try (BufferedReader br = new BufferedReader(
        new InputStreamReader(
            conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream(),
            StandardCharsets.UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) {
        response.append(line.trim());
      }
    }

    if (conn.getResponseCode() >= 400) {
      api.logging().logToError("AI API Error. Code: " + conn.getResponseCode() + ". Response: " + response);
      throw new RuntimeException("AI API Error: " + response);
    }

    JsonObject responseObj = gson.fromJson(response.toString(), JsonObject.class);

    String content = extractResponseContent(responseObj);
    if (content != null && !content.isBlank()) {
      return content;
    }

    api.logging().logToError("Failed to parse AI response. Unexpected JSON structure: " + response);
    throw new RuntimeException("Failed to parse AI response: " + response);
  }

  private void applyConfiguredHeaders(HttpURLConnection conn) {
    String apiKey = settingsService.getApiKey();
    if (apiKey != null && !apiKey.isBlank()) {
      conn.setRequestProperty("Authorization", "Bearer " + apiKey);
    }

    Map<String, String> extraHeaders = settingsService.parseExtraHeaders(settingsService.getExtraHeaders());
    for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
      conn.setRequestProperty(entry.getKey(), entry.getValue());
    }
  }

  private JsonObject createTaskSchema() {
    String schema = """
        {
          "name": "task_list",
          "strict": true,
          "schema": {
            "type": "object",
            "properties": {
              "tasks": {
                "type": "array",
                "items": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" },
                    "description": { "type": "string" }
                  },
                  "required": ["name", "description"],
                  "additionalProperties": false
                }
              }
            },
            "required": ["tasks"],
            "additionalProperties": false
          }
        }
        """;
    return gson.fromJson(schema, JsonObject.class);
  }

  private List<Task> parseTaskArray(JsonArray taskArray) {
    List<Task> tasks = new ArrayList<>();
    for (JsonElement element : taskArray) {
      JsonObject taskObj = element.getAsJsonObject();
      String name = taskObj.get("name").getAsString();
      String description = taskObj.get("description").getAsString();
      tasks.add(new Task(name, description));
    }
    return tasks;
  }

  private List<Task> parseTasks(String aiJsonResponse) {
    try {
      JsonElement root = gson.fromJson(aiJsonResponse, JsonElement.class);

      if (root.isJsonArray()) {
        return parseTaskArray(root.getAsJsonArray());
      }

      JsonObject responseObj = root.getAsJsonObject();
      JsonArray taskArray = responseObj.getAsJsonArray("tasks");
      if (taskArray == null) {
        throw new IllegalArgumentException("Missing 'tasks' array in AI response.");
      }

      return parseTaskArray(taskArray);
    } catch (Exception e) {
      api.logging().logToError("Error parsing tasks JSON: " + e.getMessage());
      api.logging().logToError("Faulty JSON: " + aiJsonResponse);
      return List.of(new Task("Error parsing tasks", "Check extension error log"));
    }
  }

  private Step parseStep(String aiJsonResponse) {
    try {
      JsonObject stepObj = gson.fromJson(aiJsonResponse, JsonObject.class);
      Step step = new Step(stepObj.get("name").getAsString());
      step.setThoughtProcess(stepObj.get("thought_process").getAsString());
      String requestText = stepObj.get("request").getAsString();
      if (!step.setRequest(requestText)) {
        throw new IllegalArgumentException("AI returned an invalid HTTP request: " + step.getRequestParseError());
      }
      return step;
    } catch (Exception e) {
      api.logging().logToError("Error parsing step JSON: " + e.getMessage());
      api.logging().logToError("Faulty JSON: " + aiJsonResponse);
      Step fallback = new Step("Error: Failed to parse AI response");
      fallback.setThoughtProcess("The AI response could not be parsed into a valid HTTP request. Check the extension error log for details.");
      return fallback;
    }
  }

  private String parseSummary(String aiJsonResponse) {
    try {
      JsonObject summaryObj = gson.fromJson(aiJsonResponse, JsonObject.class);
      return summaryObj.get("summary").getAsString();
    } catch (Exception e) {
      api.logging().logToError("Error parsing summary JSON: " + e.getMessage());
      api.logging().logToError("Faulty JSON: " + aiJsonResponse);
      return "Error: Failed to parse AI summary.";
    }
  }

  private String extractResponseContent(JsonObject responseObj) {
    if (!responseObj.has("choices") || !responseObj.get("choices").isJsonArray()
        || responseObj.getAsJsonArray("choices").isEmpty()) {
      return null;
    }

    JsonObject firstChoice = responseObj.getAsJsonArray("choices").get(0).getAsJsonObject();
    if (!firstChoice.has("message") || !firstChoice.get("message").isJsonObject()) {
      return null;
    }

    JsonObject message = firstChoice.getAsJsonObject("message");
    if (!message.has("content")) {
      return null;
    }

    JsonElement content = message.get("content");
    if (content.isJsonPrimitive()) {
      return content.getAsString();
    }

    if (!content.isJsonArray()) {
      return null;
    }

    StringBuilder combinedText = new StringBuilder();
    for (JsonElement element : content.getAsJsonArray()) {
      if (!element.isJsonObject()) {
        continue;
      }

      JsonObject part = element.getAsJsonObject();
      if (part.has("text") && part.get("text").isJsonPrimitive()) {
        combinedText.append(part.get("text").getAsString());
      }
    }

    return combinedText.toString();
  }

  private JsonObject createStepSchema() {
    String schema = """
        {
          "name": "test_step",
          "strict": true,
          "schema": {
            "type": "object",
            "properties": {
              "name": {
                "type": "string",
                "description": "Short, human-readable explanation of this test's purpose. e.g., 'Test Incremental IDOR on /api/users'. MUST NOT be a URL or API path by itself."
              },
              "thought_process": {
                "type": "string",
                "description": "Your step-by-step reasoning for building this specific request, based on the history and task."
              },
              "request": {
                "type": "string",
                "description": "The complete, raw HTTP request string. CRITICAL: All line breaks MUST be the literal string '\\r\\n' (CRLF). Example: 'GET / HTTP/1.1\\r\\nHost: example.com\\r\\n\\r\\n'"
              }
            },
            "required": ["name", "thought_process", "request"],
            "additionalProperties": false
          }
        }
        """;
    return gson.fromJson(schema, JsonObject.class);
  }

  private JsonObject createSummarySchema() {
    String schema = """
        {
          "name": "analysis_summary",
          "strict": true,
          "schema": {
            "type": "object",
            "properties": {
              "summary": { "type": "string", "description": "Concise analysis of the test result (2-3 sentences)." }
            },
            "required": ["summary"],
            "additionalProperties": false
          }
        }
        """;
    return gson.fromJson(schema, JsonObject.class);
  }


  private JsonObject createMessage(String role, String content) {
    JsonObject message = new JsonObject();
    message.addProperty("role", role);
    message.addProperty("content", content);
    return message;
  }

}
