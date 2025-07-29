package einstein.controller;

import einstein.DeadlockDetector;
import einstein.ProcessThread;
import einstein.Resource;
import einstein.ResourceManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.*;

public class MainController {
    @FXML private TextField resourceNameField, resourceIdField, resourceCountField;
    @FXML private TextField processIdField, tsField, tuField;
    @FXML private TextField intervalField;
    @FXML private TextArea logArea;

    private final ResourceManager resourceManager = new ResourceManager();
    private final Map<Integer, ProcessThread> processThreads = new HashMap<>();
    private DeadlockDetector detector;

    @FXML
    public void addResource() {
        String name = resourceNameField.getText();
        int id = Integer.parseInt(resourceIdField.getText());
        int count = Integer.parseInt(resourceCountField.getText());
        resourceManager.addResource(new Resource(name, id, count));
        addLog("Recurso " + name + " adicionado.");
    }

    @FXML
    public void createProcess() {
        int id = Integer.parseInt(processIdField.getText());
        int ts = Integer.parseInt(tsField.getText());
        int tu = Integer.parseInt(tuField.getText());

        ProcessThread pt = new ProcessThread(id, ts, tu, resourceManager, this);
        processThreads.put(id, pt);
        pt.start();
        addLog("Processo " + id + " criado.");
    }

    @FXML
    public void startDetector() {
        int interval = Integer.parseInt(intervalField.getText());
        detector = new DeadlockDetector(resourceManager, this, interval);
        detector.start();
        addLog("Detector de deadlocks iniciado.");
    }

    public void addLog(String log) {
        logArea.appendText(log + "\n");
    }

    public void showDeadlockStatus(String status) {
        addLog("Deadlock Check: " + status);
    }
}

