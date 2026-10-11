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

            case "LPUSH":
                if(parts.length < 3){
                    //LPUSH requires a key and atleast one value
                    return "ERROR: Usage -> LPUSH key value";
                }

                String key = parts[1];
                
                String[] values = new String[parts.length-2];
                for(int i=2;i<parts.length;i++) values[i-2]=parts[i];

                try{
                    long length = dataStore.lpush(key, values);
                    return String.valueOf(length);
                }
                catch(IllegalStateException e){
                    return "ERROR: " + e.getMessage();
                }

            case "RPUSH":
                if(parts.length < 3){
                    //RPUSH requires a key and atleast one value
                    return "ERROR: Usage -> LPUSH key value";
                }

                key = parts[1];
                
                values = new String[parts.length-2];
                for(int i=2;i<parts.length;i++) values[i-2]=parts[i];

                try{
                    long length = dataStore.rpush(key, values);
                    return String.valueOf(length);
                }
                catch(IllegalStateException e){
                    return "ERROR: " + e.getMessage();
                }
            
            
            case "LPOP":
                if(parts.length != 2) {
                    return "ERROR: Usage -> LPOP key";
                }

                try{
                    String poppedValue = dataStore.lpop(parts[1]);
                    return poppedValue == null ? "(nil)" : poppedValue;
                }
                catch (IllegalStateException e) {
                    return "ERROR: " + e.getMessage();
                }
            
            
            case "RPOP":
                if(parts.length != 2) {
                    return "ERROR: Usage -> RPOP key";
                }

                try{
                    String poppedValue = dataStore.rpop(parts[1]);
                    return poppedValue == null ? "(nil)" : poppedValue;
                }
                catch (IllegalStateException e) {
                    return "ERROR: " + e.getMessage();
                }
            
            case "LLEN":
                if(parts.length != 2) {
                    return "ERROR: Usage -> LLEN key";
                }

                try {
                    return String.valueOf(dataStore.llen(parts[1]));
                }
                catch (IllegalStateException e) {
                    return "ERROR: " + e.getMessage();
                }
            
            
            case "LRANGE":
                if(parts.length != 4) {
                    return "ERROR: Usage -> LRANGE key start stop";
                }

                try{
                    int start = Integer.parseInt(parts[2]);
                    int stop = Integer.parseInt(parts[3]);

                    return String.valueOf(dataStore.lrange(parts[1], start, stop));
                } 
                catch (NumberFormatException e) {
                    return "ERROR: start and stop must be integers";
                } 
                catch (Exception e) {
                    return "ERROR: " + e.getMessage();
                }

            default:
                return "ERROR: Unknown command";

        }
    }
}
