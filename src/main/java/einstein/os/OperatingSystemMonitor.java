package einstein.os;

import einstein.models.GerenciadorRecursos;
import einstein.controller.MainController;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OperatingSystemMonitor extends Thread {
    private final Long detectionIntervalSeconds;
    private final GerenciadorRecursos resourceManager;
    private MainController controller;
    private ScheduledExecutorService scheduler;

    public OperatingSystemMonitor(Long detectionIntervalSeconds, GerenciadorRecursos resourceManager, MainController controller) {
        this.detectionIntervalSeconds = detectionIntervalSeconds;
        this.resourceManager = resourceManager;
        this.controller = controller;
        setDaemon(true);
    }

    @Override
    public void run() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::performDeadlockCheck, detectionIntervalSeconds, detectionIntervalSeconds, TimeUnit.SECONDS);
    }

    private void performDeadlockCheck() {
        List<Integer> deadlockedProcessIds = resourceManager.detectDeadlock();

        if (deadlockedProcessIds.isEmpty()) {
            Platform.runLater(() -> controller.updateDeadlockStatus("Nenhum"));
        } else {
            String ids = deadlockedProcessIds.stream()
                    .map(String::valueOf)
                    .sorted()
                    .collect(Collectors.joining(", "));
            Platform.runLater(() -> {
                controller.updateDeadlockStatus(ids);
                controller.addLog("DEADLOCK DETECTADO! Processos envolvidos: " + ids);
            });
        }
    }

    public void stopMonitor() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            controller.addLog("Monitor de deadlock parado.");
        }
    }
}