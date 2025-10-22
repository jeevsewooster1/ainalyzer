package burp.model;

import burp.api.montoya.http.message.HttpRequestResponse;

public class ExecutionState {

  public enum State {
    IDLE,
    GENERATING_TASKS,
    AWAITING_STEP_EXECUTION,
    EXECUTING_STEP,
    GENERATING_SUMMARY
  }

  private State currentState;
  private HttpRequestResponse baseRequestResponse;
  private Task currentTask;
  private Step currentStep;

  public ExecutionState() {
    this.currentState = State.IDLE;
  }

  public State getCurrentState() {
    return currentState;
  }

  public void setCurrentState(State state) {
    this.currentState = state;
  }

  public HttpRequestResponse getBaseRequestResponse() {
    return baseRequestResponse;
  }

  public void setBaseRequestResponse(HttpRequestResponse reqResp) {
    this.baseRequestResponse = reqResp;
  }

  public Task getCurrentTask() {
    return currentTask;
  }

  public void setCurrentTask(Task task) {
    this.currentTask = task;
  }

  public Step getCurrentStep() {
    return currentStep;
  }

  public void setCurrentStep(Step step) {
    this.currentStep = step;
  }
}
