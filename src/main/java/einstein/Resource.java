package einstein;

public class Resource {
    private final String name;
    private final int id;
    private int total;
    private int available;

    public Resource(String name, int id, int total) {
        this.name = name;
        this.id = id;
        this.total = total;
        this.available = total;
    }

    public synchronized boolean allocate() {
        if (available > 0) {
            available--;
            return true;
        }
        return false;
    }

    public synchronized void release() {
        available++;
    }

    public String getName() { return name; }
    public int getId() { return id; }
    public int getAvailable() { return available; }
    public int getTotal() { return total; }
}

