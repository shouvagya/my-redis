package com.shouvagya.redis.server;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.shouvagya.redis.commands.CommandProcessor;
import com.shouvagya.redis.datastore.DataStore;
import com.shouvagya.redis.protocol.RespParser;

import java.io.PrintWriter;

public class ClientHandler implements Runnable{
    
    private final Socket socket;
    private final CommandProcessor commandProcessor;

    public ClientHandler(Socket socket, DataStore dataStore){
        this.socket=socket;
        this.commandProcessor = new CommandProcessor(dataStore);
    }

    @Override
    public void run(){

        try{

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(),true);

            RespParser parser = new RespParser();

            while(true){
                
                String line = parser.parse(reader);

                if(line==null){
                    break;
                }

                System.out.println("Received: "+line);

                String response = commandProcessor.process(line);

                writer.println(response);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }

        
    }
}
