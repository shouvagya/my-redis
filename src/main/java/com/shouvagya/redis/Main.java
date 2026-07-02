package com.shouvagya.redis;

import com.shouvagya.redis.server.RedisServer;

public class Main{
    public static void main(String[] args) {
        System.out.println("Starting My Redis....");

        RedisServer server = new RedisServer();
        server.start();
    }
}