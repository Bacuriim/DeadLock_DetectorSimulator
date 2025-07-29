package einstein;

import java.util.*;

public class ResourceManager {
    private final Map<Integer, Resource> resources = new HashMap<>();

    public void addResource(Resource resource) {
        resources.put(resource.getId(), resource);
    }

    public Resource getResource(int id) {
        return resources.get(id);
    }

    public Collection<Resource> getAllResources() {
        return resources.values();
    }

    public boolean allocateResource(int resourceId) {
        Resource res = resources.get(resourceId);
        return res != null && res.allocate();
    }

    public void releaseResource(int resourceId) {
        Resource res = resources.get(resourceId);
        if (res != null) res.release();
    }

    public int[] getAvailableVector() {
        return resources.values().stream().mapToInt(Resource::getAvailable).toArray();
    }
}

