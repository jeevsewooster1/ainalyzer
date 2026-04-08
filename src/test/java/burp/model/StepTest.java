package burp.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StepTest {

  @Test
  void setRequestStoresRawHttpRequestUntilServiceIsAvailable() {
    Step step = new Step("Valid request");

    boolean accepted = step.setRequest("GET /health HTTP/1.1\r\nHost: example.com\r\n\r\n");

    assertTrue(accepted);
    assertEquals("GET /health HTTP/1.1\r\nHost: example.com\r\n\r\n", step.getRawRequest());
    assertNull(step.getRequest());
    assertNull(step.getRequestParseError());
  }

  @Test
  void parseRequestFailsClosedWhenServiceIsMissing() {
    Step step = new Step("Invalid request");

    boolean accepted = step.setRequest("GET /health HTTP/1.1\r\nHost: example.com\r\n\r\n");
    boolean parsed = step.parseRequest(null);

    assertTrue(accepted);
    assertFalse(parsed);
    assertNull(step.getRequest());
    assertEquals("HTTP service is not set.", step.getRequestParseError());
  }

  @Test
  void setRequestRejectsEmptyInput() {
    Step step = new Step("Empty request");

    boolean accepted = step.setRequest("   ");

    assertFalse(accepted);
    assertNull(step.getRequest());
    assertEquals("Request text is empty.", step.getRequestParseError());
  }
}
