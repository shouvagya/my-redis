package com.shouvagya.redis.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import com.shouvagya.redis.datastore.DataStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisServer {
    private static final int PORT=6379;

    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public void start(){
        try{

            final DataStore dataStore = new DataStore();
            
            ServerSocket serverSocket= new ServerSocket(PORT);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down My Redis...");
                try {
                    serverSocket.close();
                } 
                catch (IOException e) {
                    e.printStackTrace();
                }
                threadPool.shutdown();
                System.out.println("Server stopped successfully.");
            }));
            

            System.out.println("My Redis started on port "+PORT);

            Thread cleaner = new Thread(() -> {
                while(true){
                    try{
                        Thread.sleep(1000);
                        dataStore.cleanupExpiredKeys();
                    }
                    catch(InterruptedException e){
                        break;
                    }
                }
            },"Expiry-cleaner");

            cleaner.setDaemon(true);//marking it as a daemon thread
            cleaner.start();


            while(true){
                System.out.println("Waiting for a client..");

                Socket clientSocket = serverSocket.accept();

                System.out.println("Client connected: "+clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, dataStore);

                threadPool.submit(clientHandler);
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        
    }
}
