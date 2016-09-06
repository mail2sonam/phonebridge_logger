package com.phonebridge.main;
import com.mongodb.MongoClient;
import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import phonebridge.util.LogClass;
import phonebridge.util.util;
import phonebridgelogger.dao.EventsDAO;
import phonebridgelogger.dao.ServerDAO;
import phonebridgelogger.db.EventsDB;
import phonebridgelogger.db.ServerDB;
import phonebridgelogger.model.Server;

class phonebridge{
    private String lastReadLine;
    private List<String> lastReadBlock;
    private boolean continueConnection;
    private Socket socketConn;
    private Date lastEvent;
    private boolean queried;
    private PrintWriter socketWrite;
    private BufferedReader socketRead;
    private MongoClient mongoConn;
    private util utilObj;
    private Server serverDetails;
    private ServerDAO serverDao;
    private ServerDB serverDb;
    private EventsDAO eventsDao;
    private EventsDB eventsDb;
   
    
    TimerTask task = new TimerTask(){
        public void run(){
            serverDao.updateLastEventTime(serverDetails.getServerID(),lastEvent,serverDb,mongoConn);
            double seconds = utilObj.returnDiffBtwDateInSecsOrMinsOrHrs(new Date(), lastEvent, "secs");//(new Date().getTime()-lastEvent.getTime())/1000;
            if( seconds>=30 ){
                String stopRequestID = serverDao.checkForStopRequest(serverDetails.getServerID(), serverDb,"logger",mongoConn);
                if(stopRequestID!=null){
                    String cmd = "Action: Logoff\r\n\r\n";
                    socketWrite.write(cmd);
                    serverDao.updateRequestStartedOn(stopRequestID, mongoConn);
                    serverDao.updateRequestCompletedOn(stopRequestID, mongoConn);
                    gracefulExit("Stop Request Made",true);
                }
                //check if already queried
                else if(!queried){
                    //if no make a query
                    String cmd = "Action: CoreShowChannels\r\n\r\n";
                    System.out.println(cmd);
                    socketWrite.write(cmd);
                    socketWrite.flush();
                    queried = true;
                }
                else{
                    System.out.println( "Read Socket Stuck. exiting..." );
                    gracefulExit("Socket Disconnected",true);
                }
            }
        }
    };
    
    TimerTask coreShow = new TimerTask(){
        public void run(){
            putCoreShowChannelCommand();
        }
    };
    
    public void putCoreShowChannelCommand(){
        try{
            String cmd = "Action: CoreShowChannels\r\n\r\n";
            System.out.println(cmd);
            socketWrite.write(cmd);
            socketWrite.flush();
        }
        catch(Exception ex){
            LogClass.logMsg("phonebridge", "putCoreShowChannelCommand", ex.getMessage());
        }
    }
    
    private void gracefulExit(String reason,boolean logInDb){
        if(logInDb)
            serverDao.insertOrUpdateUpTime(serverDetails.getServerID(), "Disconnect", reason, mongoConn);
        continueConnection = false;
        this.mongoConn.close();
        System.out.println("Closed Mongo connection");
        System.out.println("Socket Close");
        System.exit(0);
        System.out.println("Closed Everything, now finished System.Exit(0)");
        /*try {
            System.out.println("GOING TO CLOSE SOCKET READ CONNECTION");
            socketRead.close();
        } catch (IOException ex) {
            System.out.println("INSIDE SOCKET READ CLOSE CATCH BLOCK "+ex.getMessage());
            LogClass.logMsg("phonebridge", "gracefulExit", ex.getMessage());
        }
        System.out.println("Socket Read Close");
        try {
            socketWrite.close();
         } catch (Exception ex) {
            LogClass.logMsg("phonebridge", "gracefulExit", ex.getMessage());
        }
        System.out.println("Socket Write Close");
        try {
            socketConn.close();
        } catch (IOException ex) {
            LogClass.logMsg("phonebridge", "gracefulExit", ex.getMessage());
        }
        System.out.println("Socket Close");
        System.exit(0);
        System.out.println("Closed Everything, now finished System.Exit(0)");
        //close any existing mongo connection*/
    }

    

    public static void main(String[] args){
        try{
            phonebridge listernerObj = new phonebridge(args[0]);
            ArrayList<String> macAddress = new ArrayList<>();
            macAddress = listernerObj.getMACAddress();
            boolean startLogger=false;
            for(String mac:macAddress){
                //if(mac.equals("F0:DE:F1:4D:19:91"))
                    startLogger=true;
            }
            if(startLogger)
                listernerObj.amiprocess();
        } 
        catch (IOException ex){
            LogClass.logMsg("phonebridge", "main", ex.getMessage());
        }
    }
    
    public ArrayList getMACAddress(){
        ArrayList<String> macAddress = new ArrayList<>();
        Enumeration e = null;
        try {
            e = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            Logger.getLogger(ServerDAO.class.getName()).log(Level.SEVERE, null, ex);
        }

        while(e.hasMoreElements()) 
        {
            NetworkInterface n = (NetworkInterface) e.nextElement();
            Enumeration ee = n.getInetAddresses();
            while(ee.hasMoreElements()) 
            {
                InetAddress i = (InetAddress) ee.nextElement();
                if(!i.isLoopbackAddress() && !i.isLinkLocalAddress() && i.isSiteLocalAddress()){
                    macAddress.add(buildString(i));
                }
            }
        }
        return macAddress;
    }
    
    public static String buildString(InetAddress ip)
    {
        NetworkInterface network = null;
        try {
            network = NetworkInterface.getByInetAddress(ip);
        } catch (SocketException ex) {
            Logger.getLogger(ServerDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] mac_byte = null;
        try {
            mac_byte = network.getHardwareAddress();
        } catch (SocketException ex) {
            Logger.getLogger(ServerDAO.class.getName()).log(Level.SEVERE, null, ex);
        }

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < mac_byte.length; i++) 
        {
            sb.append(String.format("%02X%s", mac_byte[i], (i < mac_byte.length -1) ? ":" : ""));
        }
        return sb.toString();
    }
    
    public phonebridge(String serverID){
        lastReadLine = "";
        lastReadBlock = new ArrayList<String>();
        this.socketConn = null;
        this.continueConnection = true;
        lastEvent = new Date();
        this.queried = false;
        this.mongoConn = LoggerDBClass.getInstance().getConnection();
        this.serverDao = new ServerDAO();
        this.serverDb = new ServerDB();
        this.utilObj = new util();
        this.eventsDao = new EventsDAO();
        this.eventsDb = new EventsDB();
        this.serverDetails = new ServerDAO().getServerByID(serverID);
        System.out.println("Try to Connect to AMI with "+this.serverDetails.getServerIP()+" and Port "+this.serverDetails.getAmiPort());
        
    }
    
    private void connectSocket(){ 
        
        try{
            serverDb.updateServerStatus(this.serverDetails.getServerID(),"starting",mongoConn);
            socketConn = new Socket(this.serverDetails.getServerIP(),this.serverDetails.getAmiPort());
            socketRead = new BufferedReader(new InputStreamReader(socketConn.getInputStream()));
            socketWrite = new PrintWriter(socketConn.getOutputStream());
        }
        catch (IOException ex) {
            LogClass.logMsg("phonebridge", "connectSocket", ex.getMessage());
        }
    }
    
    private void amiprocess() throws IOException{
        Timer timer = new Timer();
        timer.schedule( task, 1000,10*1000 );
        Timer timer_coreshow = new Timer();
        timer_coreshow.schedule( coreShow, 1000,5*1000 );
        this.connectSocket();
        if(socketRead==null){
            serverDb.updateServerStatus(this.serverDetails.getServerID(),"stopped",mongoConn);
            System.out.println("Not able to Connect!");
            gracefulExit("Not able to connect",false);
        }
        while(continueConnection){
            lastReadLine = socketRead.readLine();
            System.out.println(lastReadLine);
            if(lastReadLine.contains("Asterisk Call Manager")){
                String cmd = "Action: login\r\nUsername: "+this.serverDetails.getAmiUserName()+"\r\nSecret: "+
                    this.serverDetails.getAmiPassword()+"\r\nEvents: call,cdr,agent,dtmf,system\r\n\r\n";
                    socketWrite.write(cmd);
                    socketWrite.flush();
                    System.out.println(cmd);
            }
            if(lastReadLine.contains("Message: Authentication accepted")){
                eventsDao.deleteOriginateCallsForServer(serverDetails.getServerNamePrefix(), eventsDb, mongoConn);
                serverDao.insertOrUpdateUpTime(serverDetails.getServerID(), "Connect", null, mongoConn);
            }
            if(lastReadLine.length()==0){
                //new Thread(
                new SaveAMIBlock(lastReadBlock,mongoConn,serverDetails,utilObj,eventsDao,eventsDb).run();
                lastReadBlock.clear();
            }
            else{
                queried = false;
                lastEvent = new Date();
                lastReadBlock.add(lastReadLine);
            }
            this.lastReadLine = "";
        }//end out while loop
    }

    public static void thread(Runnable runnable, boolean daemon) {
        Thread brokerThread = new Thread(runnable);
        brokerThread.setDaemon(daemon);
        brokerThread.start();
    }
} 