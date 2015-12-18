package eu.fbk.dkm.pikes.tintop.server;

import javax.annotation.Nullable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by alessio on 16/12/15.
 */

public abstract class ResourcePool<T> {

    abstract protected T createResource();

    static public int MAX_RESOURCES = 10;

    private Semaphore sem;
    private final Queue<T> resources = new ConcurrentLinkedQueue<T>();

    public ResourcePool() {
        this(MAX_RESOURCES);
    }

    public ResourcePool(@Nullable Integer numResources) {
        if (numResources == null) {
            numResources = MAX_RESOURCES;
        }
        sem = new Semaphore(numResources, true);
    }

    public T getResource(long maxWaitMillis) throws Exception {

        // First, get permission to take or create a resource
        sem.tryAcquire(maxWaitMillis, TimeUnit.MILLISECONDS);

        // Then, actually take one if available...
        T res = resources.poll();
        if (res != null) {
            return res;
        }

        // ...or create one if none available
        try {
            return createResource();
        } catch (Exception e) {
            // Don't hog the permit if we failed to create a resource!
            sem.release();
            throw new Exception(e);
        }
    }

    public void returnResource(T res) {
        resources.add(res);
        sem.release();
    }
}
