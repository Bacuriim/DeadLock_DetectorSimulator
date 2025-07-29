package einstein;

import einstein.controller.MainController;
import javafx.application.Platform;
import java.util.Random;

public class ProcessThread extends Thread {
    private final int id;
    private final int ts;
    private final int tu;
    private final ResourceManager resourceManager;
    private final MainController controller;
    private boolean running = true;

    public ProcessThread(int id, int ts, int tu, ResourceManager resourceManager, MainController controller) {
        this.id = id;
        this.ts = ts;
        this.tu = tu;
        this.resourceManager = resourceManager;
        this.controller = controller;
    }

    @Override
    public void run() {
        Random rand = new Random();
        while (running) {
            try {
                Thread.sleep(ts * 1000);
                Resource res = resourceManager.getAllResources().stream().toList()
                        .get(rand.nextInt(resourceManager.getAllResources().size()));

                log("solicitou recurso " + res.getName());

                synchronized (res) {
                    while (!res.allocate()) {
                        log("bloqueado aguardando " + res.getName());
                        res.wait();
                    }
                }

                log("utilizando " + res.getName());
                Thread.sleep(tu * 1000);
                synchronized (res) {
                    res.release();
                    res.notifyAll();
                }
                log("liberou " + res.getName());
            } catch (InterruptedException e) {
                log("foi interrompido");
                return;
            }
        }
    }

    public void stopProcess() {
        running = false;
        this.interrupt();
    }

    private void log(String msg) {
        Platform.runLater(() -> controller.addLog("Processo " + id + ": " + msg));
    }

    public int getProcessId() {
        return id;
    }
}

