package com.shouvagya.redis.datastore;

import java.util.concurrent.ConcurrentHashMap;
import com.shouvagya.redis.persistence.PersistenceManager;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.io.IOException;
import com.shouvagya.redis.persistence.PersistenceSnapshot;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class DataStore{

    private ConcurrentHashMap<String, RedisObject> store;

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
            store.put(key,new RedisObject(RedisType.STRING,value));
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

    private String getStringValue(String key){
        RedisObject object = store.get(key);

        if(object == null){
            return null;
        }

        if(object.getType() != RedisType.STRING){
            throw new IllegalStateException("WRONGTYPE: key does not contain a string");
        }

        return (String) object.getValue();
    }

    public String get(String key){
        readLock.lock();

        try{
            String value = getStringValue(key);
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
            return getStringValue(key);
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

        RedisObject removedValue = store.remove(key);
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
                    RedisObject removedValue=store.remove(key);
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

    public long lpush(String key, String... values){
        if(values == null || values.length==0){
            throw new IllegalStateException("LPUSH requires at least one value");
        }

        writeLock.lock();
        try{
            //Remove the key if first if its TTL has expired
            removeIfExpired(key);

            RedisObject object = store.get(key);
            LinkedList<String> list;

            if(object == null){
                //Create a new list when the key doesn't exist
                list = new LinkedList<>();
                store.put(key, new RedisObject(RedisType.LIST, list));
            }
            else{
                if(object.getType() != RedisType.LIST){
                    throw new IllegalStateException("WRONGTYPE: key does not contain a list");
                }

                list= (LinkedList<String>) object.getValue();
            }

            //Inserting each argument at the beginning
            for(String value:values){
                list.addFirst(value);
            }

            persistSnapshot();
            return list.size();
        }
        finally{
            writeLock.unlock();
        }
    }

    
    public long rpush(String key, String... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("RPUSH requires at least one value");
        }

        writeLock.lock();
        try{
            removeIfExpired(key);

            RedisObject object = store.get(key);
            LinkedList<String> list;

            if (object == null) {
                list = new LinkedList<>();
                store.put(key, new RedisObject(RedisType.LIST, list));
            } 
            else {
                if (object.getType() != RedisType.LIST) {
                    throw new IllegalStateException("WRONGTYPE: key does not contain a list");
                }

                list = (LinkedList<String>) object.getValue();
            }

            for (String value : values) {
                list.addLast(value);
            }

            persistSnapshot();

            return list.size();
        } 
        finally{
            writeLock.unlock();
        }
    }

    
    public String lpop(String key) {
        writeLock.lock();
        try {
            removeIfExpired(key);

            RedisObject object = store.get(key);

            // Return null if the key doesn't exist.
            if (object == null) {
                return null;
            }

            if (object.getType() != RedisType.LIST) {
                throw new IllegalStateException("WRONGTYPE: key does not contain a list");
            }

            LinkedList<String> list = (LinkedList<String>) object.getValue();

            String value = list.pollFirst();

            // Redis removes the key when its list becomes empty.
            if (list.isEmpty()) {
                store.remove(key);
                expiryMap.remove(key);
            }

            persistSnapshot();

            return value;

        } 
        finally {
            writeLock.unlock();
        }
    }

    
    public String rpop(String key) {
        writeLock.lock();
        try {
            removeIfExpired(key);

            RedisObject object = store.get(key);

            if (object == null) {
                return null;
            }

            if (object.getType() != RedisType.LIST) {
                throw new IllegalStateException("WRONGTYPE: key does not contain a list");
            }

            LinkedList<String> list = (LinkedList<String>) object.getValue();

            String value = list.pollLast();

            // Remove the key if the list becomes empty.
            if (list.isEmpty()) {
                store.remove(key);
                expiryMap.remove(key);
            }

            persistSnapshot();

            return value;

        }
        finally {
            writeLock.unlock();
        }
    }

    public long llen(String key) {
        writeLock.lock();
        try {
            removeIfExpired(key);

            RedisObject object = store.get(key);

            // Missing or expired key has length zero.
            if (object == null) {
                return 0;
            }

            if (object.getType() != RedisType.LIST) {
                throw new IllegalStateException("WRONGTYPE: key does not contain a list");
            }

            LinkedList<String> list = (LinkedList<String>) object.getValue();
            return list.size();
        } 
        finally {
            writeLock.unlock();
        }
    }

    
    public List<String> lrange(String key, int start, int stop) {
        writeLock.lock();
        try{
            removeIfExpired(key);

            RedisObject object = store.get(key);

            if (object == null) {
                return new ArrayList<>();
            }

            if (object.getType() != RedisType.LIST) {
                throw new IllegalStateException("WRONGTYPE: key does not contain a list");
            }

            LinkedList<String> list = (LinkedList<String>) object.getValue();

            int size = list.size();

            // Convert negative indices to positive indices.
            int from = start < 0 ? size + start : start;
            int to = stop < 0 ? size + stop : stop;

            // Clamp the range to valid list boundaries.
            from = Math.max(from, 0);
            to = Math.min(to, size - 1);

            // Return an empty list if the range is invalid.
            if (from >= size || to < 0 || from > to) {
                return new ArrayList<>();
            }

            // Stop is inclusive, so to + 1.
            return new ArrayList<>(list.subList(from, to + 1));

        } 
        finally {
            writeLock.unlock();
        }
    }










    
}