package com.shouvagya.redis.protocol;

import java.io.PrintWriter;

public class RespWriter {
    
    public void write(PrintWriter writer, String command){
        String[] parts= command.trim().split("\\s+");

        writer.println("*"+parts.length);

        for(String part : parts){
            writer.println("$"+part.length());
            writer.println(part);
        }
    }
}
