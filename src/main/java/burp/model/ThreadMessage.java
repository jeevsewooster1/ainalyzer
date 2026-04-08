package burp.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ThreadMessage {
  public enum Role {
    USER,
    AI
  }

  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

  private final Role role;
  private final String content;
  private final Instant createdAt;

  public ThreadMessage(Role role, String content) {
    this.role = role;
    this.content = content != null ? content : "";
    this.createdAt = Instant.now();
  }

  public Role getRole() {
    return role;
  }

  public String getContent() {
    return content;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getDisplayRole() {
    return role == Role.USER ? "You" : "AI";
  }

  public String getDisplayTimestamp() {
    return TIME_FORMATTER.format(createdAt);
  }
}
