package burp.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;

import burp.service.StateManager;

import javax.swing.*;
import java.awt.*;

public class ExecutionPanel extends JPanel {

  private final MontoyaApi api;
  private final StateManager stateManager;

  private final JTextArea thoughtProcessArea;
  private final HttpRequestEditor requestEditor;
  private final HttpResponseEditor responseEditor;

  public ExecutionPanel(MontoyaApi api, StateManager stateManager) {
    this.api = api;
    this.stateManager = stateManager;

    setLayout(new BorderLayout());
    setBorder(BorderFactory.createTitledBorder("Execution"));

    JPanel thoughtPanel = new JPanel(new BorderLayout());
    thoughtPanel.setBorder(BorderFactory.createTitledBorder("AI Thought Process"));
    thoughtProcessArea = new JTextArea(5, 40);
    thoughtProcessArea.setEditable(false);
    thoughtProcessArea.setLineWrap(true);
    thoughtProcessArea.setWrapStyleWord(true);
    thoughtPanel.add(new JScrollPane(thoughtProcessArea), BorderLayout.CENTER);

    JPanel reqRespPanel = new JPanel(new GridLayout(1, 2, 5, 0));

    requestEditor = api.userInterface().createHttpRequestEditor();
    responseEditor = api.userInterface().createHttpResponseEditor();

    JPanel requestPanel = new JPanel(new BorderLayout());
    requestPanel.setBorder(BorderFactory.createTitledBorder("Request"));
    requestPanel.add(requestEditor.uiComponent(), BorderLayout.CENTER);

    JPanel responsePanel = new JPanel(new BorderLayout());
    responsePanel.setBorder(BorderFactory.createTitledBorder("Response"));
    responsePanel.add(responseEditor.uiComponent(), BorderLayout.CENTER);

    reqRespPanel.add(requestPanel);
    reqRespPanel.add(responsePanel);

    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        thoughtPanel, reqRespPanel);
    splitPane.setDividerLocation(150);
    splitPane.setResizeWeight(0.2);

    add(splitPane, BorderLayout.CENTER);
  }

  public void setThoughtProcess(String thought) {
    thoughtProcessArea.setText(thought);
    thoughtProcessArea.setCaretPosition(0);
  }

  public void setRequest(HttpRequest request) {
    requestEditor.setRequest(request);
  }

  public void setResponse(HttpResponse response) {
    responseEditor.setResponse(response);
  }

  public void clear() {
    thoughtProcessArea.setText("");
    requestEditor.setRequest(null);
    responseEditor.setResponse(null);
  }
}
