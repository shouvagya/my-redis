package com.shouvagya.redis.datastore;

import java.util.concurrent.ConcurrentHashMap;
import com.shouvagya.redis.persistence.PersistenceManager;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.io.IOException;
import com.shouvagya.redis.persistence.PersistenceSnapshot;


public class DataStore{

    private ConcurrentHashMap<String, String> store;

    private final PersistenceManager persistenceManager = new PersistenceManager();

    private final ConcurrentHashMap<String, Long> expiryMap=new ConcurrentHashMap<>();

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();


    public DataStore(){
        try{
            PersistenceSnapshot snapShot=persistenceManager.load();

            store=new ConcurrentHashMap<>(snapShot.getStore());
            expiryMap.putAll(snapShot.getExpiryMap());
        }
        catch(Exception e){
            e.printStackTrace();
            store = new ConcurrentHashMap<>();
        }
    }

    private void persistSnapshot(){
        //The calling method must already hold writeLock
        try{
            PersistenceSnapshot snapshot=new PersistenceSnapshot(store,expiryMap);
            persistenceManager.save(snapshot);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    public void set(String key, String value){
        writeLock.lock();
        try{
            store.put(key,value);
            //A normal SET removes the key's previous TTL
            expiryMap.remove(key);

            try{
                persistSnapshot();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        finally{
            writeLock.unlock();
        }
    }

    public String get(String key){
        readLock.lock();

        try{
            String value = store.get(key);
            if(value==null){
                return null;
            }
            Long expiryTime = expiryMap.get(key);

            if(expiryTime == null || System.currentTimeMillis()<expiryTime){
                return value;
            }
        }
        finally{
            readLock.unlock();
        }

        //The key appeared expired. Recheck under the write lock
        //because another thread might have updated it meanwhile.
        writeLock.lock();
        try{
            removeIfExpired(key);
            return store.get(key);
        }
        finally{
            writeLock.unlock();
        }
    }

    public boolean delete(String key){
        writeLock.lock();
        try{
            boolean removedValue = store.remove(key) != null;
            boolean removedExpiry = expiryMap.remove(key)!=null;

            if(removedValue || removedExpiry){
                persistSnapshot();
            }
            return removedValue;
        }
        finally{
            writeLock.unlock();
        }
    }

    public boolean exists(String key){
        readLock.lock();

        try{
            if(!store.containsKey(key)){
                return false;
            }

            Long expiryTime = expiryMap.get(key);
            if(expiryTime==null || System.currentTimeMillis() < expiryTime){
                return true;
            }
        }
        finally{
            readLock.unlock();
        }

        writeLock.lock();
        try{
            removeIfExpired(key);
            return store.containsKey(key);
        }
        finally{
            writeLock.unlock();
        }
    }

    public boolean expire(String key, long seconds){
        writeLock.lock();
        try{
            if(!store.containsKey(key)){
                return false;
            }

            long expiryTime = System.currentTimeMillis() + seconds*1000;

            expiryMap.put(key,expiryTime);
            persistSnapshot();
            return true;
        }
        finally{
            writeLock.unlock();
        }
    }

    public long ttl(String key){
        //may use readLock later for optimization
        writeLock.lock();
        try{
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
        finally{
            writeLock.unlock();
        }
        
    }

    private void removeIfExpired(String key){
        //The caller must already hold the write lock
        Long expiryTime = expiryMap.get(key);

        if(expiryTime == null){
            return;
        }

        if(System.currentTimeMillis() < expiryTime){
            return;
        }

        String removedValue = store.remove(key);
        expiryMap.remove(key,expiryTime);

        //Persist the deletion so the so the expired key
        //does not reappear after a restart
        
        if(removedValue!=null){
            try{
                persistSnapshot();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public void cleanupExpiredKeys(){
        writeLock.lock();
        boolean changed=false;

        try{
            long now=System.currentTimeMillis();

            for(String key:expiryMap.keySet()){

                Long expiryTime=expiryMap.get(key);

                if(expiryTime!=null && expiryTime<=now){
                    String removedValue=store.remove(key);
                    expiryMap.remove(key,expiryTime);

                    if(removedValue!=null){
                        changed=true;
                    }

                }
            }

            //Save at once, if changed
            if(changed){
                try{
                    persistSnapshot();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }

        }
        finally{
            writeLock.unlock();
        }
    }
    
}