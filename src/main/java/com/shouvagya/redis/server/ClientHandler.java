package com.shouvagya.redis.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ClientHandler implements Runnable{
    
    private final Socket socket;

    public ClientHandler(Socket socket){
        this.socket=socket;
    }

    @Override
    public void run(){

        try{
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line = reader.readLine();//waits till receives a terminating character, functions just like accept()

            //OutputStream output = socket.getOutputStream();
            // System.out.println("Client connected : "+ socket.getInetAddress());

            System.out.println("Received: "+line);
        }
        catch(Exception e){
            e.printStackTrace();
        }

        
    }
}
