package com.shouvagya.redis.persistence;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class PersistenceManager {

    private static final String FILE_NAME = "data.db";

    public void save(PersistenceSnapshot snapshot) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            out.writeObject(snapshot);
        }
    }

    public PersistenceSnapshot load() throws IOException, ClassNotFoundException {
        File file = new File(FILE_NAME);

        if (!file.exists()) {
            return new PersistenceSnapshot(new ConcurrentHashMap<>(),new ConcurrentHashMap<>());
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (PersistenceSnapshot) in.readObject();
        }
    }
}
