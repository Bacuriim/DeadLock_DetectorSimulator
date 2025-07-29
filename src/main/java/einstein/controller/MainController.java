package einstein.controller;

import einstein.models.GerenciadorRecursos;
import einstein.models.ProcessThread;
import einstein.models.Recurso;
import einstein.os.OperatingSystemMonitor;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MainController {

    // Control Variables
    private Recurso[] configuredResourceTypes = new Recurso[10];
    private Map<Integer, ProcessThread> activeProcesses = new ConcurrentHashMap<>();
    private OperatingSystemMonitor operatingSystemThread;
    private GerenciadorRecursos resourceManager;

    // FXML Components - Register Resource
    @FXML private Label resourceLimitMessage;
    @FXML private TextField resourceNameInput;
    @FXML private TextField resourceIdInput;
    @FXML private TextField resourceInstanceCountInput;

    // FXML Components - Create Process
    @FXML private TextField processIdInput;
    @FXML private TextField requestIntervalInput;
    @FXML private TextField usageIntervalInput;

    // FXML Components - Detector
    @FXML private TextField detectionIntervalInput;

    // FXML Components - General UI
    @FXML private TextArea logArea;
    @FXML private Label deadlockProcessesLabel;
    @FXML private TextArea resourcesStatusArea;
    @FXML private TextArea processesStatusArea;
    @FXML private TextArea allocationRequestStatusArea;

    // FXML Components - Eliminar Processo (new)
    @FXML private TextField processIdToDeleteInput;


    public void initialize() {
        resourceManager = new GerenciadorRecursos(configuredResourceTypes, this);
        updateAllUIStatus();
        setupLogRedirection();
    }

    @FXML
    public void handleAddResource() {
        try {
            String name = resourceNameInput.getText();
            int id = Integer.parseInt(resourceIdInput.getText());
            int count = Integer.parseInt(resourceInstanceCountInput.getText());

            if (id < 0 || id >= 10) {
                addLog("Erro: ID do recurso deve ser entre 0 e 9.");
                return;
            }
            if (configuredResourceTypes[id] != null) {
                addLog("Erro: Recurso com ID " + id + " já existe.");
                return;
            }
            if (count <= 0) {
                addLog("Erro: O número de instâncias deve ser positivo.");
                return;
            }

            configuredResourceTypes[id] = new Recurso(id, name, count);
            resourceManager.setResourceConfiguration(id, configuredResourceTypes[id]);
            addLog("Recurso '" + name + "' (ID: " + id + ", Qtd: " + count + ") adicionado.");
            clearResourceFields();
            updateAllUIStatus();
        } catch (NumberFormatException e) {
            addLog("Erro ao adicionar recurso: Verifique o ID e a Quantidade (devem ser números inteiros).");
        }
    }

    @FXML
    public void handleCreateProcess() {
        try {
            int id = Integer.parseInt(processIdInput.getText());
            if (id < 1 || id > 10) {
                addLog("Erro: ID do processo deve ser entre 1 e 10.");
                return;
            }
            if (activeProcesses.containsKey(id)) {
                addLog("Erro: Processo com ID " + id + " já existe.");
                return;
            }
            if (activeProcesses.size() >= 10) {
                addLog("Erro: Número máximo de processos (10) atingido.");
                return;
            }

            int ts = Integer.parseInt(requestIntervalInput.getText());
            int tu = Integer.parseInt(usageIntervalInput.getText());
            if (ts <= 0 || tu <= 0) {
                addLog("Erro: Intervalos de Requisição e Utilização devem ser positivos.");
                return;
            }

            ProcessThread pt = new ProcessThread(id, ts, tu, resourceManager, this);
            activeProcesses.put(id, pt);
            pt.start();
            addLog("Processo " + id + " criado (Intervalo de Requisição: " + ts + "s, Intervalo de Utilização: " + tu + "s).");
            clearProcessFields();
            updateAllUIStatus();
        } catch (NumberFormatException e) {
            addLog("Erro ao criar processo: Verifique o ID, Intervalo de Requisição e Intervalo de Utilização (devem ser números inteiros).");
        }
    }

    @FXML
    public void handleStartDetector() {
        try {
            long interval = Long.parseLong(detectionIntervalInput.getText());
            if (interval <= 0) {
                addLog("Erro: O intervalo do detector deve ser um número positivo.");
                return;
            }

            if (operatingSystemThread != null && operatingSystemThread.isAlive()) {
                operatingSystemThread.stopMonitor();
                addLog("Detector anterior parado.");
            }

            operatingSystemThread = new OperatingSystemMonitor(interval, resourceManager, this);
            operatingSystemThread.start();
            addLog("Detector de deadlock iniciado com intervalo de " + interval + " segundos.");
            detectionIntervalInput.clear();
        } catch (NumberFormatException e) {
            addLog("Erro ao iniciar detector: Verifique o intervalo (deve ser um número inteiro).");
        }
    }

    @FXML
    public void handleEliminateProcess() {
        try {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Parar Processo");
            dialog.setHeaderText("Informe o ID do processo a ser parado:");
            dialog.setContentText("ID:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(idStr -> {
                try {
                    int idToStop = Integer.parseInt(idStr);
                    ProcessThread pt = activeProcesses.get(idToStop);
                    if (pt != null) {
                        pt.stopProcessExecution();
                        activeProcesses.remove(idToStop);
                        resourceManager.clearAllocations(idToStop);
                        resourceManager.clearRequests(idToStop);
                        addLog("Processo " + idToStop + " parado e removido.");
                        updateAllUIStatus();
                    } else {
                        addLog("Erro: Processo com ID " + idToStop + " não encontrado.");
                    }
                } catch (NumberFormatException e) {
                    addLog("Erro: ID inválido.");
                }
            });
        } catch (Exception e) {
            addLog("Erro ao parar processo: " + e.getMessage());
        }
    }


    public void addLog(String log) {
        Platform.runLater(() -> logArea.appendText(log + "\n"));
    }

    public void updateDeadlockStatus(String status) {
        Platform.runLater(() -> deadlockProcessesLabel.setText("Processos em Deadlock: " + status));
    }

    public void updateCountdownTimer(double seconds) {
        // Implement if you have a label for countdown
    }


    public void updateResourceStateDisplay(int[] totalRes, int[] availableRes, int[][] allocMatrix, int[][] reqMatrix) {
        updateAllUIStatus();
    }


    private void clearResourceFields() {
        resourceNameInput.clear();
        resourceIdInput.clear();
        resourceInstanceCountInput.clear();
    }

    private void clearProcessFields() {
        processIdInput.clear();
        requestIntervalInput.clear();
        usageIntervalInput.clear();
    }

    public void shutdown() {
        if (operatingSystemThread != null) {
            operatingSystemThread.stopMonitor();
        }
        for (ProcessThread pt : activeProcesses.values()) {
            pt.stopProcessExecution();
        }
        activeProcesses.clear();
    }

    private void updateAllUIStatus() {
        Platform.runLater(() -> {
            resourcesStatusArea.setText(getDisplayedResourceStatus());
            processesStatusArea.setText(getDisplayedProcessStatus());
            allocationRequestStatusArea.setText(getDisplayedAllocationAndRequest());
        });
    }

    public String getDisplayedProcessStatus() {
        return activeProcesses.values().stream()
                .map(p -> "P" + p.getProcessId() + ": " + p.getProcessStatus())
                .collect(Collectors.joining("\n"));
    }

    public String getDisplayedResourceStatus() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (configuredResourceTypes[i] != null) {
                Recurso res = configuredResourceTypes[i];
                sb.append("Recurso '").append(res.getResourceName()).append("' (ID: ").append(res.getResourceId()).append("):\n");
                sb.append("  Total: ").append(resourceManager.totalResources[i])
                        .append(", Disponível: ").append(resourceManager.availableResources[i].availablePermits()).append("\n");
                List<Integer> holders = new ArrayList<>();
                for (Map.Entry<Integer, ProcessThread> entry : activeProcesses.entrySet()) {
                    int processId = entry.getKey();
                    if (resourceManager.allocationMatrix[processId - 1][i] > 0) {
                        holders.add(processId);
                    }
                }
                if (!holders.isEmpty()) {
                    sb.append("  Usado por: P").append(holders.stream().map(String::valueOf).collect(Collectors.joining(", P"))).append("\n");
                } else {
                    sb.append("  Não usado por nenhum processo.\n");
                }
            }
        }
        return sb.toString();
    }

    public String getDisplayedAllocationAndRequest() {
        StringBuilder sb = new StringBuilder();
        for (ProcessThread pt : activeProcesses.values()) {
            sb.append("Processo P").append(pt.getProcessId()).append(":\n");
            List<Integer> heldByProcess = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                if (configuredResourceTypes[j] != null && resourceManager.allocationMatrix[pt.getProcessId() - 1][j] > 0) {
                    heldByProcess.add(configuredResourceTypes[j].getResourceId());
                }
            }
            if (!heldByProcess.isEmpty()) {
                sb.append("  Alocado (Em posse): R").append(heldByProcess.stream().map(String::valueOf).collect(Collectors.joining(", R"))).append("\n");
            } else {
                sb.append("  Em posse: Nenhum\n");
            }

            List<Integer> waitingForProcess = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                if (configuredResourceTypes[j] != null && resourceManager.requestMatrix[pt.getProcessId() - 1][j] > 0) {
                    waitingForProcess.add(configuredResourceTypes[j].getResourceId());
                }
            }
            if (!waitingForProcess.isEmpty()) {
                sb.append("  Aguardando (Requisitado): R").append(waitingForProcess.stream().map(String::valueOf).collect(Collectors.joining(", R"))).append("\n");
            } else {
                sb.append("  Aguardando: Nenhum\n");
            }
        }
        return sb.toString();
    }

    public void setupLogRedirection() {
        PrintStream printStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                Platform.runLater(() -> appendLogText(String.valueOf((char) b)));
            }
            @Override
            public void write(byte[] b, int off, int len) {
                String texto = new String(b, off, len);
                Platform.runLater(() -> appendLogText(texto));
            }
            private void appendLogText(String texto) {
                double scrollTop = logArea.getScrollTop();
                int caretPosition = logArea.getCaretPosition();
                logArea.appendText(texto);
                logArea.positionCaret(caretPosition);
                logArea.setScrollTop(scrollTop);
            }
        }, true, java.nio.charset.StandardCharsets.UTF_8);

        System.setOut(printStream);
        System.setErr(printStream);
    }
}