package burp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiServiceTest {

  @Test
  void extractsRawJsonObjectPayload() {
    AiService service = new AiService(null, null);

    String payload = service.extractJsonPayload("{\"summary\":\"ok\"}");

    assertEquals("{\"summary\":\"ok\"}", payload);
  }

  @Test
  void extractsFencedJsonPayload() {
    AiService service = new AiService(null, null);

    String payload = service.extractJsonPayload("""
        Here is the result:

        ```json
        {"summary":"ok"}
        ```
        """);

    assertEquals("{\"summary\":\"ok\"}", payload);
  }

  @Test
  void extractsJsonAfterAgentCommentary() {
    AiService service = new AiService(null, null);

    String payload = service.extractJsonPayload("Done.\n{\"summary\":\"ok\"}");

    assertEquals("{\"summary\":\"ok\"}", payload);
  }

  @Test
  void returnsNullForMalformedJsonPayload() {
    AiService service = new AiService(null, null);

    String payload = service.extractJsonPayload("Done.\n{\"summary\":");

    assertNull(payload);
  }
}
