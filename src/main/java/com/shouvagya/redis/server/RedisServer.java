package com.shouvagya.redis.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class RedisServer {
    private static final int PORT=6379;

    public void start(){
        try{
            ServerSocket serverSocket= new ServerSocket(PORT);
            System.out.println("My Redis started on port "+PORT);

            while(true){
                System.out.println("Waiting for a client..");

                Socket clientSocket = serverSocket.accept();

                System.out.println("Client connected: "+clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);

                Thread thread = new Thread(clientHandler);

                thread.start();
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        
    }
}
