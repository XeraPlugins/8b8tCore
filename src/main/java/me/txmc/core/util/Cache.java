package me.txmc.core.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Cache<S, T> {

    protected class Cachebox {

        protected T cached;
        protected int time;

        protected Cachebox(T cached, int time) {
            this.cached = cached;
            this.time = time;
        }

        protected boolean tick() {
            time--;
            return time <= 0;
        }
    }

    private final int cachetime;
    private final Map<S, Cachebox> cachestorage = new HashMap<>();

    public Cache(int cachetime) {
        this.cachetime = cachetime;
    }

    public void putCached(S key, T cached) {
        cachestorage.put(key, new Cachebox(cached, cachetime));
    }

    public boolean isCached(S key) {
        return cachestorage.containsKey(key);
    }

    public T getCached(S key) {
        if (isCached(key)) {
            return cachestorage.get(key).cached;
        }
        return null;
    }

    public void tick() {
        Set<S> removables = new HashSet<S>();

        for (S key : cachestorage.keySet()) {
            Cachebox box = cachestorage.get(key);

            if (!box.tick()) {
                continue;
            }

            removables.add(key);
        }

        cachestorage.keySet().removeAll(removables);
    }

    public void clear() {
        cachestorage.clear();
    }

    public boolean remove(S key) {
        return cachestorage.remove(key) != null;
    }

}
