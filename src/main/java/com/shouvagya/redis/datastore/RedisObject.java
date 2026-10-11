package com.shouvagya.redis.datastore;

import java.io.Serializable;
import java.util.List;

public class RedisObject implements Serializable{
    private static final long serialVersionUID = 1L;

    private final RedisType type;
    private final Object value;

    public RedisObject(RedisType type, Object value){
        if (type == null || value == null) {
            throw new IllegalArgumentException("Type and value cannot be null");
        }

        if (type == RedisType.STRING && !(value instanceof String)) {
            throw new IllegalArgumentException("STRING type requires a String value");
        }

        if (type == RedisType.LIST && !(value instanceof List<?>)) {
            throw new IllegalArgumentException("LIST type requires a List value");
        }
        
        this.type=type;
        this.value=value;
    }

    public RedisType getType(){
        return type;
    }

    public Object getValue(){
        return value;
    }
}
