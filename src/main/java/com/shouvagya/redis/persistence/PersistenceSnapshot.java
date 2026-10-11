package com.shouvagya.redis.persistence;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class PersistenceSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ConcurrentHashMap<String, String> store;
    private final ConcurrentHashMap<String, Long> expiryMap;

    public PersistenceSnapshot(ConcurrentHashMap<String, String> store, ConcurrentHashMap<String, Long> expiryMap) {
        this.store = new ConcurrentHashMap<>(store);
        this.expiryMap = new ConcurrentHashMap<>(expiryMap);
    }

    public ConcurrentHashMap<String, String> getStore() {
        return store;
    }

    public ConcurrentHashMap<String, Long> getExpiryMap() {
        return expiryMap;
    }
}