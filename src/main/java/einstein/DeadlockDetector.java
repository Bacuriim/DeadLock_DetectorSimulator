package einstein;

import einstein.controller.MainController;

public class DeadlockDetector extends Thread {
    private final ResourceManager resourceManager;
    private final MainController controller;
    private final int interval;
    private boolean running = true;

    public DeadlockDetector(ResourceManager rm, MainController ctrl, int interval) {
        this.resourceManager = rm;
        this.controller = ctrl;
        this.interval = interval;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(interval * 1000);
                detect();
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void detect() {
        controller.showDeadlockStatus("Não há deadlock detectado (simulado)");
    }

    public void stopDetector() {
        running = false;
        this.interrupt();
    }
}

