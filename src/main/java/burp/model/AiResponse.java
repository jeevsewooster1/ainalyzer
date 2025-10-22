package burp.model;

public class AiResponse {
  private final String reasoningContent;
  private final String content;

  public AiResponse(String reasoningContent, String content) {
    this.reasoningContent = reasoningContent != null ? reasoningContent : "";
    this.content = content;
  }

  /**
   * @return The model's internal chain-of-thought reasoning (empty for
   *         non-reasoning models)
   */
  public String getReasoningContent() {
    return reasoningContent;
  }

  /**
   * @return The final structured output content (e.g., JSON response)
   */
  public String getContent() {
    return content;
  }

  /**
   * @return Combined reasoning and content for display purposes
   */
  public String getCombined() {
    if (reasoningContent != null && !reasoningContent.isEmpty()) {
      return "=== REASONING PROCESS ===\n" + reasoningContent +
          "\n\n=== FINAL OUTPUT ===\n" + content;
    }
    return content;
  }

  /**
   * @return True if this response contains reasoning content
   */
  public boolean hasReasoning() {
    return reasoningContent != null && !reasoningContent.isEmpty();
  }
}
