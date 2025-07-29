package einstein.models;

public class Recurso {
    private final Integer resourceId;
    private final String resourceName;
    private final Integer totalInstancesCount;

    public Recurso(Integer resourceId, String resourceName, Integer totalInstancesCount) {
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.totalInstancesCount = totalInstancesCount;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public Integer getTotalInstancesCount() {
        return totalInstancesCount;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "id=" + resourceId +
                ", name='" + resourceName + '\'' +
                ", totalInstances=" + totalInstancesCount +
                '}';
    }
}
