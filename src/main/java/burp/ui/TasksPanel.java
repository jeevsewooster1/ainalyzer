package burp.ui;

import burp.api.montoya.MontoyaApi;
import burp.model.Task;
import burp.service.StateManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TasksPanel extends JPanel {

  private MontoyaApi api;
  private StateManager stateManager;
  private DefaultListModel<Task> taskListModel;
  private JList<Task> taskList;
  private List<Task> tasks;

  public TasksPanel(MontoyaApi api, StateManager stateManager) {
    this.api = api;
    this.stateManager = stateManager;
    this.tasks = new ArrayList<>();

    setLayout(new BorderLayout());

    taskListModel = new DefaultListModel<>();
    taskList = new JList<>(taskListModel);
    taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    taskList.setCellRenderer(new TaskCellRenderer()); // Uses the updated renderer

    taskList.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        Task selectedTask = taskList.getSelectedValue();
        if (selectedTask != null) {
          stateManager.selectTask(selectedTask);
        }
      }
    });

    add(new JScrollPane(taskList), BorderLayout.CENTER);
  }

  public void setTasks(List<Task> tasks) {
    this.tasks = tasks;
    taskListModel.clear();
    for (Task task : tasks) {
      taskListModel.addElement(task);
    }
  }

  public void addTask(Task task) {
    tasks.add(task);
    taskListModel.addElement(task);
  }

  public Task getSelectedTask() {
    return taskList.getSelectedValue();
  }

  private static class TaskCellRenderer extends JPanel implements ListCellRenderer<Task> {

    private JLabel nameLabel;
    // private JLabel descLabel;
    private JTextArea descArea;

    public TaskCellRenderer() {
      setLayout(new BorderLayout(5, 2));
      setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

      nameLabel = new JLabel();
      nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));

      descArea = new JTextArea();
      descArea.setLineWrap(true);
      descArea.setWrapStyleWord(true);
      descArea.setEditable(false);
      descArea.setFont(descArea.getFont().deriveFont(20f));
      descArea.setForeground(Color.GRAY);

      add(nameLabel, BorderLayout.NORTH);
      add(descArea, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends Task> list,
        Task task,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {

      nameLabel.setText(task.getName());
      descArea.setText(task.getDescription());

      Insets listInsets = list.getInsets();
      Insets panelInsets = getInsets();
      int availableWidth = list.getWidth() - listInsets.left - listInsets.right - panelInsets.left - panelInsets.right;

      if (availableWidth > 0) {
        descArea.setSize(availableWidth, Short.MAX_VALUE);
      }

      if (isSelected) {
        setBackground(new Color(184, 207, 229));
        nameLabel.setForeground(Color.BLACK);
        // descLabel.setForeground(Color.DARK_GRAY);
        descArea.setForeground(Color.DARK_GRAY);
        descArea.setBackground(new Color(184, 207, 229));
      } else {
        setBackground(list.getBackground());
        nameLabel.setForeground(list.getForeground());
        // descLabel.setForeground(Color.GRAY);
        descArea.setForeground(Color.GRAY);
        descArea.setBackground(list.getBackground());
      }

      return this;
    }
  }
}
