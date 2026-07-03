package com.shouvagya.redis.client;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import com.shouvagya.redis.protocol.RespWriter;

public class RedisClient {

    public static void main(String[] args) {

        try{
            Socket socket = new Socket("localhost", 6379);

            System.out.println("Connected to server..");

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(),true);

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

            RespWriter respWriter = new RespWriter();

            while(true){
                System.out.println("redis> ");

                String command = console.readLine();

                if(command.equalsIgnoreCase("exit")){
                    break;
                }

                respWriter.write(writer,command);
                String response = reader.readLine();

                System.out.println(response);
            }

            socket.close();

        }
        catch(Exception e){
            e.printStackTrace();
        }

    }

}