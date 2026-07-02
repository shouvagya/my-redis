package com.shouvagya.redis.client;
import java.io.PrintWriter;
import java.net.Socket;

public class RedisClient {

    public static void main(String[] args) {

        try{
            Socket socket = new Socket("localhost", 6379);

            PrintWriter writer = new PrintWriter(socket.getOutputStream(),true);

            writer.println("PING");

            System.out.println("PING sent!");
        }
        catch(Exception e){
            e.printStackTrace();
        }

    }

}