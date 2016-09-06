/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.phonebridge.main;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

/**
 *
 * @author sonamuthu
 */
public class LoggerDBClass {
    private static LoggerDBClass singleton = new LoggerDBClass();
    private MongoClient mongoClient;
    public static final String MDATABASE = "phonebridge_basic";
    
    private LoggerDBClass() {
    }

    public static LoggerDBClass getInstance() {
        return singleton;
    }

    public MongoClient getConnection(){
        String mongoIpAddress = "localhost";//"192.168.10.197";
        Integer mongoPort = 27017;
        try{
            if(mongoClient==null)
                mongoClient = new MongoClient(mongoIpAddress, mongoPort);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return mongoClient;
    } 
}
