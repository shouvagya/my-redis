package com.shouvagya.redis.datastore;

import java.util.concurrent.ConcurrentHashMap;
import com.shouvagya.redis.persistence.PersistenceManager;

public class DataStore{

    private ConcurrentHashMap<String, String> store;

    private final PersistenceManager persistenceManager = new PersistenceManager();

    private final ConcurrentHashMap<String, Long> expiryMap=new ConcurrentHashMap<>();

    public DataStore(){
        try{
            store = persistenceManager.load();
        }
        catch(Exception e){
            store = new ConcurrentHashMap<>();
        }
    }

    public void set(String key, String value){
        store.put(key,value);
        try{
            persistenceManager.save(store);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public String get(String key){
        removeIfExpired(key);
        return store.get(key);
    }

    public boolean delete(String key){
        expiryMap.remove(key);
        try{
            persistenceManager.save(store);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return store.remove(key) != null;
    }

    public boolean exists(String key){
        removeIfExpired(key);
        return store.containsKey(key);
    }

    public boolean expire(String key, long seconds){

        if(!store.containsKey(key)){
            return false;
        }

        long expiryTime = System.currentTimeMillis() + seconds*1000;

        expiryMap.put(key,expiryTime);
        return true;
    }

    public long ttl(String key){
        if(!store.containsKey(key)){
            return -2;
        }

        Long expiryTime = expiryMap.get(key);

        if(expiryTime == null){
            return -1;
        }

        long remaining = (expiryTime - System.currentTimeMillis())/1000;

        return Math.max(remaining,0);
    }

    private void removeIfExpired(String key){
        Long expiryTime = expiryMap.get(key);

        if(expiryTime == null){
            return;
        }

        if(System.currentTimeMillis() >= expiryTime){
            store.remove(key);
            expiryMap.remove(key);
        }
    }

    public void cleanupExpiredKeys(){
        long now= System.currentTimeMillis();

        for(String key: expiryMap.keySet()){
            Long expiry = expiryMap.get(key);

            if(expiry != null && now>=expiry){
                store.remove(key);
                expiryMap.remove(key);
            }
        }
    }
    
}