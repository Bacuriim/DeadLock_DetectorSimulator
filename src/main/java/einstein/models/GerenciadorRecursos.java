package einstein.models;

import einstein.controller.MainController;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

public class GerenciadorRecursos {
    private final Object lockObject = new Object();
    public int[] totalResources; // Recursos existentes (total de inst�ncias por tipo)
    public Semaphore[] availableResources; // Recursos dispon�veis (inst�ncias dispon�veis por tipo)
    public int[][] allocationMatrix; // Matriz de aloca��o (allocationMatrix[processId-1][resourceId] = inst�ncias alocadas)
    public int[][] requestMatrix; // Matriz de requisi��o (requestMatrix[processId-1][resourceId] = inst�ncias requisitadas)
    private Recurso[] resourceConfigurations; // Refer�ncia aos recursos configurados no MainController

    private MainController controller;

    public GerenciadorRecursos(Recurso[] resourceConfigurations, MainController controller) {
        this.resourceConfigurations = resourceConfigurations;
        this.controller = controller;

        totalResources = new int[10]; // 10 tipos de recursos
        availableResources = new Semaphore[10];
        for (int i = 0; i < 10; i++) {
            if (resourceConfigurations[i] != null) {
                totalResources[i] = resourceConfigurations[i].getTotalInstancesCount();
                availableResources[i] = new Semaphore(totalResources[i]);
            } else {
                totalResources[i] = 0;
                availableResources[i] = new Semaphore(0);
            }
        }

        allocationMatrix = new int[10][10]; // 10 processos x 10 recursos
        requestMatrix = new int[10][10]; // 10 processos x 10 recursos
    }

    public void setResourceConfiguration(int resourceId, Recurso resource) {
        synchronized (lockObject) {
            resourceConfigurations[resourceId] = resource;
            totalResources[resourceId] = resource.getTotalInstancesCount();
            availableResources[resourceId] = new Semaphore(totalResources[resourceId]);
        }
    }

    public List<Recurso> getAvailableResourceTypes() {
        List<Recurso> activeResourceTypes = new ArrayList<>();
        for (Recurso r : resourceConfigurations) {
            if (r != null) {
                activeResourceTypes.add(r);
            }
        }
        return activeResourceTypes;
    }


    public ArrayList<Integer> detectDeadlock() {
        synchronized (lockObject) {
            boolean[] activeProcesses = new boolean[10];
            for (int i = 0; i < 10; i++) {
                boolean hasAllocationOrRequest = false;
                for (int j = 0; j < 10; j++) {
                    if (allocationMatrix[i][j] > 0 || requestMatrix[i][j] > 0) {
                        hasAllocationOrRequest = true;
                        break;
                    }
                }
                activeProcesses[i] = hasAllocationOrRequest;
            }

            int[] Work = new int[10];
            for (int i = 0; i < 10; i++) {
                Work[i] = availableResources[i].availablePermits();
            }

            boolean[] Finish = new boolean[10];
            for (int i = 0; i < 10; i++) {
                Finish[i] = !activeProcesses[i];
            }

            boolean changed = true;
            while (changed) {
                changed = false;
                for (int i = 0; i < 10; i++) {
                    if (activeProcesses[i] && !Finish[i]) {
                        boolean canExecute = true;
                        for (int j = 0; j < 10; j++) {
                            if (requestMatrix[i][j] > Work[j]) {
                                canExecute = false;
                                break;
                            }
                        }
                        if (canExecute) {
                            for (int j = 0; j < 10; j++) {
                                Work[j] += allocationMatrix[i][j];
                            }
                            Finish[i] = true;
                            changed = true;
                        }
                    }
                }
            }

            ArrayList<Integer> deadlockedProcessIds = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                if (activeProcesses[i] && !Finish[i]) {
                    deadlockedProcessIds.add(i + 1);
                }
            }

            Collections.sort(deadlockedProcessIds);

            Platform.runLater(() -> controller.updateResourceStateDisplay(totalResources, getAvailableResourcesArray(), allocationMatrix, requestMatrix));
            return deadlockedProcessIds;
        }
    }

    public Recurso requestResource(Integer processId, Integer resourceId) {
        synchronized (lockObject) {
            if (resourceId < 0 || resourceId >= 10 || resourceConfigurations[resourceId] == null) {
                System.out.println("Processo " + processId + ": Tentativa de requisitar recurso inv�lido/inexistente ID " + resourceId);
                return null;
            }

            if ((allocationMatrix[processId - 1][resourceId] + requestMatrix[processId - 1][resourceId]) >= totalResources[resourceId]) {
                System.out.println("Processo " + processId + ": J� alocou/solicitou o m�ximo de inst�ncias de " + resourceConfigurations[resourceId].getResourceName());
                return null;
            }

            requestMatrix[processId - 1][resourceId]++;
            System.out.println("Processo " + processId + " requisitou 1 inst�ncia de " + resourceConfigurations[resourceId].getResourceName() + " (ID: " + resourceId + ")");
            Platform.runLater(() -> controller.updateResourceStateDisplay(totalResources, getAvailableResourcesArray(), allocationMatrix, requestMatrix));
        }

        try {
            availableResources[resourceId].acquire();

            synchronized (lockObject) {
                requestMatrix[processId - 1][resourceId]--;
                allocationMatrix[processId - 1][resourceId]++;
                System.out.println("Processo " + processId + " alocou 1 inst�ncia de " + resourceConfigurations[resourceId].getResourceName() + " (ID: " + resourceId + ")");
                Platform.runLater(() -> controller.updateResourceStateDisplay(totalResources, getAvailableResourcesArray(), allocationMatrix, requestMatrix));
                return resourceConfigurations[resourceId];
            }
        } catch (InterruptedException e) {
            synchronized (lockObject) {
                requestMatrix[processId - 1][resourceId]--;
                System.out.println("Processo " + processId + " teve sua requisi��o para " + resourceConfigurations[resourceId].getResourceName() + " desfeita devido a interrup��o.");
                Platform.runLater(() -> controller.updateResourceStateDisplay(totalResources, getAvailableResourcesArray(), allocationMatrix, requestMatrix));
            }
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            System.out.println("Erro durante a requisi��o/aloca��o de recurso para o Processo " + processId + ": " + e.getMessage());
            synchronized (lockObject) {
                requestMatrix[processId - 1][resourceId]--;
                Platform.runLater(() -> controller.updateResourceStateDisplay(totalResources, getAvailableResourcesArray(), allocationMatrix, requestMatrix));
            }
            return null;
        }
    }

    public void releaseResource(Integer processId, Integer resourceId) {
        synchronized (lockObject) {
            if (resourceId < 0 || resourceId >= 10 || resourceConfigurations[resourceId] == null) {
                System.out.println("Processo " + processId + ": Tentativa de liberar recurso inv�lido/inexistente ID " + resourceId);
                return;
            }
            if (allocationMatrix[processId - 1][resourceId] <= 0) {
                System.out.println("Processo " + processId + ": Tentativa de liberar recurso " + resourceConfigurations[resourceId].getResourceName() + " que n�o possui.");
                return;
            }

            allocationMatrix[processId - 1][resourceId]--;
            System.out.println("Processo " + processId + " liberou 1 inst�ncia de " + resourceConfigurations[resourceId].getResourceName() + " (ID: " + resourceId + ")");
            availableResources[resourceId].release();
            Platform.runLater(() -> controller.updateResourceStateDisplay(totalResources, getAvailableResourcesArray(), allocationMatrix, requestMatrix));
        }
    }

    public void clearRequests(int processId) {
        synchronized (lockObject) {
            for (int j = 0; j < 10; j++) {
                requestMatrix[processId - 1][j] = 0;
            }
            Platform.runLater(() -> controller.updateResourceStateDisplay(totalResources, getAvailableResourcesArray(), allocationMatrix, requestMatrix));
        }
    }

    public void clearAllocations(int processId) {
        synchronized (lockObject) {
            for (int j = 0; j < 10; j++) {
                if (allocationMatrix[processId - 1][j] > 0) {
                    int instancesToRelease = allocationMatrix[processId - 1][j];
                    allocationMatrix[processId - 1][j] = 0;
                    availableResources[j].release(instancesToRelease);
                    System.out.println("Processo " + processId + " teve " + instancesToRelease + " inst�ncias de " + resourceConfigurations[j].getResourceName() + " liberadas for�adamente.");
                }
            }
            Platform.runLater(() -> controller.updateResourceStateDisplay(totalResources, getAvailableResourcesArray(), allocationMatrix, requestMatrix));
        }
    }

    public int[] getAvailableResourcesArray() {
        int[] arr = new int[10];
        for (int i = 0; i < 10; i++) {
            arr[i] = (availableResources[i] != null) ? availableResources[i].availablePermits() : 0;
        }
        return arr;
    }
}