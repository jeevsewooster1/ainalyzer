package burp.model;

import burp.api.montoya.http.message.HttpRequestResponse;

import java.util.ArrayList;
import java.util.List;

public class ExecutionState {

  public enum State {
    IDLE,
    GENERATING_TASKS,
    AWAITING_STEP_EXECUTION,
    EXECUTING_STEP,
    GENERATING_SUMMARY
  }

  private State currentState;
  private final List<RequestThread> requestThreads;
  private RequestThread currentThread;
  private Task currentTask;
  private Step currentStep;

  public ExecutionState() {
    this.currentState = State.IDLE;
    this.requestThreads = new ArrayList<>();
  }

  public State getCurrentState() {
    return currentState;
  }

  public void setCurrentState(State state) {
    this.currentState = state;
  }

  public HttpRequestResponse getBaseRequestResponse() {
    return currentThread != null ? currentThread.getRequestResponse() : null;
  }

  public List<RequestThread> getRequestThreads() {
    return requestThreads;
  }

  public void addRequestThread(RequestThread requestThread) {
    this.requestThreads.add(requestThread);
  }

  public RequestThread getCurrentThread() {
    return currentThread;
  }

  public void setCurrentThread(RequestThread currentThread) {
    this.currentThread = currentThread;
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
