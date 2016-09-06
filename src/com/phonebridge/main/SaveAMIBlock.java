package com.phonebridge.main;

import com.mongodb.MongoClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import phonebridge.util.LogClass;
import phonebridge.util.util;
import phonebridgelogger.dao.EventsDAO;
import phonebridgelogger.db.EventsDB;
import phonebridgelogger.model.Server;

public class SaveAMIBlock{ //implements Runnable{
    
    private final List<String> lastReadBlock;
    private final MongoClient mongoConn;
    private final Server serverDetails;
    private final util utilObj;
    private final EventsDAO eventsDao;
    private final EventsDB eventsDb;
    public SaveAMIBlock(List<String> lastReadBlock,MongoClient mongoConn,Server serverDetails,util utilObj,
            EventsDAO eventsDao,EventsDB eventsDb){
        this.lastReadBlock = lastReadBlock;
        this.mongoConn = mongoConn;
        this.serverDetails = serverDetails;
        this.utilObj = utilObj;
        this.eventsDao = eventsDao;
        this.eventsDb = eventsDb;
    }
    
    private void printOutEventDetails(HashMap<String, String> h){
        Set<String> keys = h.keySet();
        for(String key: keys){
            System.out.println(key+" : "+h.get(key));
        }
        System.out.println("\n");
    }

    
    private HashMap<String,String> convertListToHashMap(){
        HashMap<String, String> h = new HashMap<String,String>();
        try{
            for (String line : this.lastReadBlock){
                String[] splititems = line.split(":");
                String remaining = "";
                for(int i=1;i<splititems.length;i++){
                    remaining+=splititems[i].trim();
                    if((i+1)!=splititems.length)
                        remaining+=":";
                    }
                h.put(splititems[0].trim(), remaining);
            }
        }
        catch(Exception ex){
            LogClass.logMsg("ProcessAMIBlock", "convertListToHashMap", ex.getMessage());
        }
        return h;
    }
    

    //@Override
    public void run(){
        HashMap<String,String> h = this.convertListToHashMap();
        doSaveEvents(h);
    } 

    private void doSaveEvents(HashMap<String, String> h) {
        if(h.containsKey("Event") && !(h.get("Event").equalsIgnoreCase("CoreShowChannel")
                || h.get("Event").equalsIgnoreCase("CoreShowChannelsComplete")))
        printOutEventDetails(h);
        if(!h.containsKey("Event"))
            return;
        switch(h.get("Event")){
                case "Dial":
                    logIntoDB(h);
                    break;
                case "Cdr":
                    logIntoDB(h);
                    break;
                case "Bridge":
                    logIntoDB(h);
                    break;
                case "Join":
                    logIntoDB(h);
                    break;
                case "Leave":
                    logIntoDB(h);
                    break;
                case "Newstate":
                    logIntoDB(h);
                    break;
                case "Newchannel":
                    logIntoDB(h);
                    break;
                case "DTMF":
                    logIntoDB(h);
                    break;
                case "ExtensionStatus":
                    logIntoDB(h);
                    break;
                case "Hangup":
                    logIntoDB(h);
                    break;
                case "Transfer":
                    logIntoDB(h);
                    break;
                case "MusicOnHold":
                    logIntoDB(h);
                    break;
                case "QueueCallerAbandon":
                    logIntoDB(h);
                    break;
                case "MeetmeJoin":
                    logIntoDB(h);
                    break;
                case "MeetmeLeave":
                    logIntoDB(h);
                    break;
                case "ParkedCall":
                    logIntoDB(h);
                    break;
                case "CoreShowChannel":
                    logIntoDB(h);
                    break;
                case "CoreShowChannelsComplete":
                    logIntoDB(h);
                    break;
                case "PeerStatus":
                    logIntoDB(h);
                    break;
            }
    }

    private void logIntoDB(HashMap<String, String> h){
        Document doc = new Document();
        for (Map.Entry<String, String> entry : h.entrySet()){
            String value="";
            if(doc.containsKey(entry.getKey().toLowerCase())){
                value=doc.getString(entry.getKey().toLowerCase());
                if(value.length()>0 && entry.getValue().length()>0)
                    value=value.concat(",");
                doc.remove(entry.getKey().toLowerCase());
            }
            doc.append(entry.getKey().toLowerCase(),value.concat(entry.getValue()));
        }
        eventsDao.logEventsInDB(doc, mongoConn, eventsDb, serverDetails, utilObj);
    }
}