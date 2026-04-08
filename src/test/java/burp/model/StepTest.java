package burp.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StepTest {

  @Test
  void setRequestParsesValidHttpRequest() {
    Step step = new Step("Valid request");

    boolean parsed = step.setRequest("GET /health HTTP/1.1\r\nHost: example.com\r\n\r\n");

    assertTrue(parsed);
    assertNotNull(step.getRequest());
    assertNull(step.getRequestParseError());
  }

  @Test
  void setRequestFailsClosedForInvalidHttpRequest() {
    Step step = new Step("Invalid request");

    boolean parsed = step.setRequest("not a raw http request");

    assertFalse(parsed);
    assertNull(step.getRequest());
    assertNotNull(step.getRequestParseError());
    assertFalse(step.getRequestParseError().isBlank());
  }
}
