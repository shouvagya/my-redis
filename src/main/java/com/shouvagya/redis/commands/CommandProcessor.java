package com.shouvagya.redis.commands;

import com.shouvagya.redis.datastore.DataStore;

public class CommandProcessor {
    
    private final DataStore dataStore;

    public CommandProcessor(DataStore dataStore){
        this.dataStore = dataStore;
    }

    public String process(String command){
        
        command = command.trim();

        if(command.isEmpty()){
            return "ERROR: Empty command";
        }

        String[] parts = command.split("\\s+");

        String cmd = parts[0].toUpperCase();

        switch(cmd){
            case "PING":
                return "PONG";

            case "SET":
                if(parts.length !=3){
                    return "ERROR: Usage -> SET key value";
                }
                dataStore.set(parts[1],parts[2]);
                return "OK";

            case "GET":
                if(parts.length!=2){
                    return "ERROR: Usage -> GET key";
                }

                String value= dataStore.get(parts[1]);

                if(value==null){
                    return "(nil)";
                }
                return value;

            case "DEL":
                if(parts.length!=2){
                    return "ERROR: Usage -> DEL key";
                }
                boolean isDeleted = dataStore.delete(parts[1]);
                return (isDeleted)?"1":"0";
            
            case "EXISTS":
                if(parts.length!=2){
                    return "ERROR: Usage -> EXISTS key";
                }
                boolean exists=dataStore.exists(parts[1]);
                return (exists)?"1":"0";

            case "EXPIRE":
                if(parts.length!=3){
                    return "ERROR: Usage -> EXPIRE key seconds";
                }

                try{
                    long seconds = Long.parseLong(parts[2]);

                    boolean success = dataStore.expire(parts[1], seconds);

                    return success? "1":"0";
                }
                catch(NumberFormatException e){
                    return "ERROR: Seconds must be a number";
                }
            
            case "TTL":
                if(parts.length!=2){
                    return "ERROR: Usage -> TTL key";
                }

                long ttl = dataStore.ttl(parts[1]);
                
                return String.valueOf(ttl);

            default:
                return "ERROR: Unknown command";

        }
    }
}
