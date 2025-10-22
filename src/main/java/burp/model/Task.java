
package burp.model;

import java.util.ArrayList;
import java.util.List;

public class Task {
  private String name;
  private String description;
  private List<Step> steps;

  public Task(String name, String description) {
    this.name = name;
    this.description = description;
    this.steps = new ArrayList<>();
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public List<Step> getSteps() {
    return steps;
  }

  public void addStep(Step step) {
    steps.add(step);
  }

  public int getStepCount() {
    return steps.size();
  }

  @Override
  public String toString() {
    return name;
  }
}
