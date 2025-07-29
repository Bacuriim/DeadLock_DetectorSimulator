package einstein.models;

import einstein.controller.MainController;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ProcessThread extends Thread {
    private final int processId;
    private final int requestIntervalSeconds;
    private final int usageIntervalSeconds;
    private final GerenciadorRecursos resourceManager;
    private final MainController controller;
    private volatile boolean isRunning = true;
    private volatile Recurso waitingForResource = null;
    private final List<Recurso> allocatedResourcesList = new ArrayList<>();

    public ProcessThread(int processId, int requestIntervalSeconds, int usageIntervalSeconds, GerenciadorRecursos resourceManager, MainController controller) {
        this.processId = processId;
        this.requestIntervalSeconds = requestIntervalSeconds;
        this.usageIntervalSeconds = usageIntervalSeconds;
        this.resourceManager = resourceManager;
        this.controller = controller;
    }

    @Override
    public void run() {
        Random random = new Random();
        List<Recurso> allAvailableResourceTypes = new ArrayList<>(resourceManager.getAvailableResourceTypes());

        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(requestIntervalSeconds * 1000L);

                if (allAvailableResourceTypes.isEmpty()) {
                    log("Nenhum recurso configurado no sistema.");
                    Thread.sleep(5000);
                    allAvailableResourceTypes = new ArrayList<>(resourceManager.getAvailableResourceTypes());
                    continue;
                }

                Recurso requestedResource = allAvailableResourceTypes.get(random.nextInt(allAvailableResourceTypes.size()));

                log("solicitou recurso " + requestedResource.getResourceName() + " (ID: " + requestedResource.getResourceId() + ")");
                setWaitingForResource(requestedResource);

                Recurso acquiredResource = resourceManager.requestResource(this.processId, requestedResource.getResourceId());

                if (acquiredResource != null) {
                    setWaitingForResource(null);
                    allocatedResourcesList.add(acquiredResource);

                    log("alocou recurso " + acquiredResource.getResourceName() + " (ID: " + acquiredResource.getResourceId() + ")");
                    Thread.sleep(usageIntervalSeconds * 1000L);

                    resourceManager.releaseResource(this.processId, acquiredResource.getResourceId());
                    allocatedResourcesList.remove(acquiredResource);
                    log("liberou recurso " + acquiredResource.getResourceName() + " (ID: " + acquiredResource.getResourceId() + ")");
                } else {
                    log("falhou ao alocar recurso " + requestedResource.getResourceName() + ". Processo pode ter sido interrompido ou recurso indisponível.");
                    setWaitingForResource(null);
                }

            } catch (InterruptedException e) {
                log("foi interrompido.");
                Thread.currentThread().interrupt();
                for (Recurso res : new ArrayList<>(allocatedResourcesList)) {
                    resourceManager.releaseResource(this.processId, res.getResourceId());
                }
                allocatedResourcesList.clear();
                resourceManager.clearRequests(this.processId);
                resourceManager.clearAllocations(this.processId);
                setWaitingForResource(null);
                return;
            } catch (Exception e) {
                log("Erro inesperado para o Processo " + processId + ": " + e.getMessage());
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void stopProcessExecution() {
        isRunning = false;
        this.interrupt();
    }

    private void log(String message) {
        Platform.runLater(() -> controller.addLog("Processo " + processId + ": " + message));
    }

    public int getProcessId() {
        return processId;
    }

    public Recurso getWaitingForResource() {
        return waitingForResource;
    }

    private void setWaitingForResource(Recurso resource) {
        this.waitingForResource = resource;
    }

    public String getProcessStatus() {
        return (waitingForResource != null) ? "Bloqueado (aguardando R" + waitingForResource.getResourceId() + ")" : "Rodando";
    }

    public List<Integer> getAllocatedResourceIds() {
        return allocatedResourcesList.stream().map(Recurso::getResourceId).toList();
    }
}