package com.shouvagya.redis.persistence;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class PersistenceManager {
    private static final String FILE_NAME = "data.db";

    public void save(ConcurrentHashMap<String, String> store) throws IOException{

        try( ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE_NAME)) ){
            out.writeObject(store);
        }
        
    }

    @SuppressWarnings("unchecked")
    public ConcurrentHashMap<String, String> load() throws IOException, ClassNotFoundException{

        File file = new File(FILE_NAME);

        if(!file.exists()){
            return new ConcurrentHashMap<>();
        }

        try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))){

            return (ConcurrentHashMap<String, String>)in.readObject();
        }
    }
}
