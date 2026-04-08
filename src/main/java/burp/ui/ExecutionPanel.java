package burp.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import burp.model.ThreadMessage;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class ExecutionPanel extends JPanel {

  private final JLabel statusLabel;
  private final DefaultListModel<ThreadMessage> conversationModel;
  private final JList<ThreadMessage> conversationList;
  private final JTextArea composerArea;
  private final JButton sendButton;
  private final HttpRequestEditor requestEditor;
  private final HttpResponseEditor responseEditor;
  private Consumer<String> conversationSendListener;

  public ExecutionPanel(MontoyaApi api) {
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createTitledBorder("Execution"));

    JPanel conversationPanel = new JPanel(new BorderLayout(0, 5));
    conversationPanel.setBorder(BorderFactory.createTitledBorder("Conversation"));

    statusLabel = new JLabel(" ");
    conversationPanel.add(statusLabel, BorderLayout.NORTH);

    conversationModel = new DefaultListModel<>();
    conversationList = new JList<>(conversationModel);
    conversationList.setCellRenderer(new ConversationCellRenderer());
    conversationPanel.add(new JScrollPane(conversationList), BorderLayout.CENTER);

    composerArea = new JTextArea(3, 40);
    composerArea.setLineWrap(true);
    composerArea.setWrapStyleWord(true);

    sendButton = new JButton("Send");
    sendButton.addActionListener(e -> sendComposerMessage());

    JPanel composerPanel = new JPanel(new BorderLayout(5, 0));
    composerPanel.add(new JScrollPane(composerArea), BorderLayout.CENTER);
    composerPanel.add(sendButton, BorderLayout.EAST);
    conversationPanel.add(composerPanel, BorderLayout.SOUTH);

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
        conversationPanel, reqRespPanel);
    splitPane.setDividerLocation(300);
    splitPane.setResizeWeight(0.5);

    add(splitPane, BorderLayout.CENTER);
  }

  public void setThoughtProcess(String thought) {
    statusLabel.setText(thought != null && !thought.isBlank() ? thought : " ");
  }

  public void setRequest(HttpRequest request) {
    requestEditor.setRequest(request);
  }

  public void setResponse(HttpResponse response) {
    responseEditor.setResponse(response);
  }

  public void setConversation(List<ThreadMessage> conversation) {
    conversationModel.clear();
    if (conversation != null) {
      for (ThreadMessage message : conversation) {
        conversationModel.addElement(message);
      }
      if (!conversation.isEmpty()) {
        conversationList.ensureIndexIsVisible(conversationModel.size() - 1);
      }
    }
  }

  public void setConversationEnabled(boolean enabled) {
    composerArea.setEnabled(enabled);
    sendButton.setEnabled(enabled);
  }

  public void setConversationSendListener(Consumer<String> conversationSendListener) {
    this.conversationSendListener = conversationSendListener;
  }

  public void clear() {
    statusLabel.setText(" ");
    requestEditor.setRequest(null);
    responseEditor.setResponse(null);
  }

  private void sendComposerMessage() {
    String content = composerArea.getText().trim();
    if (!content.isBlank() && conversationSendListener != null) {
      conversationSendListener.accept(content);
      composerArea.setText("");
      composerArea.requestFocusInWindow();
    }
  }

  private static class ConversationCellRenderer extends JPanel implements ListCellRenderer<ThreadMessage> {
    private final JLabel headerLabel;
    private final JTextArea messageArea;

    public ConversationCellRenderer() {
      setLayout(new BorderLayout(0, 4));
      setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

      headerLabel = new JLabel();
      headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 12f));

      messageArea = new JTextArea();
      messageArea.setEditable(false);
      messageArea.setLineWrap(true);
      messageArea.setWrapStyleWord(true);
      messageArea.setOpaque(false);

      add(headerLabel, BorderLayout.NORTH);
      add(messageArea, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends ThreadMessage> list,
        ThreadMessage message,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {

      headerLabel.setText(message.getDisplayRole() + "  " + message.getDisplayTimestamp());
      messageArea.setText(message.getContent());

      Insets listInsets = list.getInsets();
      Insets panelInsets = getInsets();
      int availableWidth = list.getWidth() - listInsets.left - listInsets.right - panelInsets.left - panelInsets.right;
      if (availableWidth > 0) {
        messageArea.setSize(availableWidth, Short.MAX_VALUE);
      }

      Color userBackground = new Color(225, 239, 255);
      Color aiBackground = new Color(242, 242, 242);
      Color background = message.getRole() == ThreadMessage.Role.USER ? userBackground : aiBackground;

      setBackground(background);
      headerLabel.setForeground(Color.BLACK);
      messageArea.setForeground(Color.BLACK);

      return this;
    }
  }
}
