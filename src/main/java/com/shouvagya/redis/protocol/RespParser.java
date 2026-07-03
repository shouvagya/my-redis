package com.shouvagya.redis.protocol;

import java.io.BufferedReader;
import java.io.IOException;

public class RespParser {

    public String parse(BufferedReader reader) throws IOException {

        String firstLine = reader.readLine();

        if(firstLine == null){
            return null;
        }

        //Plain text support (old protocol)
        if(!firstLine.startsWith("*")){
            return firstLine;
        }

        int numberOfParts = Integer.parseInt(firstLine.substring(1));

        StringBuilder command = new StringBuilder();

        for(int i=0;i<numberOfParts; i++){
            //Read $<length>
            reader.readLine();

            //Read actual value
            String value = reader.readLine();

            command.append(value);

            if(i != numberOfParts - 1){
                command.append(" ");
            }
        }

        return command.toString();

    }

}