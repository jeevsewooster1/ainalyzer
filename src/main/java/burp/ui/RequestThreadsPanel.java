package burp.ui;

import burp.api.montoya.MontoyaApi;
import burp.model.RequestThread;
import burp.service.StateManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RequestThreadsPanel extends JPanel {

  private final StateManager stateManager;
  private final DefaultListModel<RequestThread> requestListModel;
  private final JList<RequestThread> requestList;

  public RequestThreadsPanel(MontoyaApi api, StateManager stateManager) {
    this.stateManager = stateManager;

    setLayout(new BorderLayout());

    requestListModel = new DefaultListModel<>();
    requestList = new JList<>(requestListModel);
    requestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    requestList.setCellRenderer(new RequestThreadCellRenderer());
    requestList.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        RequestThread selectedThread = requestList.getSelectedValue();
        if (selectedThread != null) {
          stateManager.selectRequestThread(selectedThread);
        }
      }
    });

    add(new JScrollPane(requestList), BorderLayout.CENTER);
  }

  public void setRequestThreads(List<RequestThread> requestThreads) {
    requestListModel.clear();
    if (requestThreads != null) {
      for (RequestThread requestThread : requestThreads) {
        requestListModel.addElement(requestThread);
      }
    }
  }

  public void selectRequestThread(RequestThread requestThread) {
    requestList.setSelectedValue(requestThread, true);
  }

  private static class RequestThreadCellRenderer extends JPanel implements ListCellRenderer<RequestThread> {
    private final JLabel titleLabel;
    private final JTextArea subtitleArea;

    public RequestThreadCellRenderer() {
      setLayout(new BorderLayout(5, 2));
      setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

      titleLabel = new JLabel();
      titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

      subtitleArea = new JTextArea();
      subtitleArea.setLineWrap(true);
      subtitleArea.setWrapStyleWord(true);
      subtitleArea.setEditable(false);
      subtitleArea.setFont(subtitleArea.getFont().deriveFont(11f));
      subtitleArea.setForeground(Color.GRAY);

      add(titleLabel, BorderLayout.NORTH);
      add(subtitleArea, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends RequestThread> list,
        RequestThread requestThread,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {

      titleLabel.setText(requestThread.getDisplayTitle());
      subtitleArea.setText(requestThread.getDisplaySubtitle());

      Insets listInsets = list.getInsets();
      Insets panelInsets = getInsets();
      int availableWidth = list.getWidth() - listInsets.left - listInsets.right - panelInsets.left - panelInsets.right;
      if (availableWidth > 0) {
        subtitleArea.setSize(availableWidth, Short.MAX_VALUE);
      }

      if (isSelected) {
        setBackground(new Color(184, 207, 229));
        titleLabel.setForeground(Color.BLACK);
        subtitleArea.setForeground(Color.DARK_GRAY);
        subtitleArea.setBackground(new Color(184, 207, 229));
      } else {
        setBackground(list.getBackground());
        titleLabel.setForeground(list.getForeground());
        subtitleArea.setForeground(Color.GRAY);
        subtitleArea.setBackground(list.getBackground());
      }

      return this;
    }
  }
}
