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

public class AiService {

  private final MontoyaApi api;
  private final Gson gson;
  private String apiEndpoint;
  private String model;

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

    String systemPrompt = "You are a senior penetration testing expert. Your goal is to analyze an HTTP request " +
        "and response to create a list of 3-5 high-impact security testing tasks to perform. " +
        "Focus on vulnerabilities like IDOR, SQL Injection, Broken Access Control, and Command Injection. " +
        "Your response must be a single, complete, raw HTTP request. " +
        "CRITICAL: All lines in the 'request' field MUST be separated by \\r\\n (CRLF) characters. " +
        "You MUST use \\r\\n." +
        "Respond *only* with the provided JSON schema.";

    String userPrompt = String.format(
        "Analyze the following HTTP interaction and generate the task list.\n\n" +
            "--- REQUEST ---\n%s\n\n" +
            "--- RESPONSE ---\n%s",
        requestStr, responseStr);

    List<JsonObject> messages = new ArrayList<>();
    messages.add(createMessage("system", systemPrompt));
    messages.add(createMessage("user", userPrompt));

    String aiResponse = callAi(messages, this.taskGenerationSchema);
    return parseTasks(aiResponse);
  }

  public Step generateStep(HttpRequestResponse baseReqResp, Task task, List<Step> previousSteps) throws Exception {

    String systemPrompt = "You are a senior penetration testing expert. Your goal is to execute a single " +
        "security test. You will be given a base request, a specific task, and the history of " +
        "all previous steps (your own actions and their results). " +
        "Analyze this history to determine the *next logical step*. Do not repeat previous steps. " +
        "Your response MUST be a JSON object adhering to the provided schema." +
        "\n\n" +
        "CRITICAL RULES FOR THE JSON FIELDS:\n" +
        "1. 'name' FIELD: This MUST be a short, human-readable title for the test (e.g., 'Test for SQLi in login'). " +
        "   It MUST NOT be a URL, an API endpoint, or a raw code snippet.\n" +
        "2. 'request' FIELD: This MUST be a single string containing the complete, raw HTTP request. " +
        "   All line breaks in this string MUST be represented as '\\r\\n' (CRLF). " +
        "   DO NOT use just '\\n'. YOU MUST USE '\\r\\n' for every line termination. " +
        "   Example of a correct line: 'Host: example.com\\r\\n'";

    List<JsonObject> messages = new ArrayList<>();
    messages.add(createMessage("system", systemPrompt));

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

    String systemPrompt = "You are a senior penetration testing expert. Your goal is to analyze an HTTP request " +
        "and response to create a list of 3-5 high-impact security testing tasks to perform. " +
        "Focus on vulnerabilities like IDOR, SQL Injection, Broken Access Control, and Command Injection. " +
        "Each task MUST have a short, descriptive name and a clear description of the goal. " +
        "Respond *only* with the provided JSON schema.";

    String userPrompt = String.format(
        "Analyze the following test step:\n\n" +
            "--- THOUGHTS ---\n%s\n\n" +
            "--- REQUEST ---\n%s\n\n" +
            "--- RESPONSE ---\n%s",
        step.getThoughtProcess(),
        request.toString(),
        response.toString());

    List<JsonObject> messages = new ArrayList<>();
    messages.add(createMessage("system", systemPrompt));
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

    requestBody.addProperty("temperature", 0.1);

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

    if (responseObj.has("choices") && responseObj.get("choices").isJsonArray()
        && !responseObj.getAsJsonArray("choices").isEmpty()) {
      JsonObject firstChoice = responseObj.getAsJsonArray("choices").get(0).getAsJsonObject();
      if (firstChoice.has("message") && firstChoice.get("message").isJsonObject()) {
        JsonObject message = firstChoice.getAsJsonObject("message");
        if (message.has("content") && message.get("content").isJsonPrimitive()) {
          return message.get("content").getAsString();
        }
      }
    }

    api.logging().logToError("Failed to parse AI response. Unexpected JSON structure: " + response);
    throw new RuntimeException("Failed to parse AI response: " + response);
  }

  private JsonObject createTaskSchema() {
    String schema = """
        {
          "name": "task_list",
          "strict": true,
          "schema": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "description": { "type": "string" }
              },
              "required": ["name", "description"]
            }
          }
        }
        """;
    return gson.fromJson(schema, JsonObject.class);
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
            "required": ["name", "thought_process", "request"]
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
            "required": ["summary"]
          }
        }
        """;
    return gson.fromJson(schema, JsonObject.class);
  }

  private List<Task> parseTasks(String aiJsonResponse) {
    try {
      JsonArray taskArray = gson.fromJson(aiJsonResponse, JsonArray.class);
      List<Task> tasks = new ArrayList<>();
      for (JsonElement element : taskArray) {
        JsonObject taskObj = element.getAsJsonObject();
        String name = taskObj.get("name").getAsString();
        String description = taskObj.get("description").getAsString();
        tasks.add(new Task(name, description));
      }
      return tasks;
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
      step.setRequest(stepObj.get("request").getAsString());
      return step;
    } catch (Exception e) {
      api.logging().logToError("Error parsing step JSON: " + e.getMessage());
      api.logging().logToError("Faulty JSON: " + aiJsonResponse);
      Step fallback = new Step("Error: Failed to parse AI response");
      fallback.setThoughtProcess("Check the extension error log for details.");
      fallback.setRequest("GET /error HTTP/1.1\r\nHost: parse.error\r\n\r\n");
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

  private JsonObject createMessage(String role, String content) {
    JsonObject message = new JsonObject();
    message.addProperty("role", role);
    message.addProperty("content", content);
    return message;
  }

}
