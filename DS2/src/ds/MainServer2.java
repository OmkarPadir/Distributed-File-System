/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ds;

/**
 *
 * @author Srini
 */
import Operations.FileObject;
import Operations.SqlOp;
import Operations.FileCodes;
import Operations.Client;
import java.net.*;
import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
public class MainServer2 {
    
    static ServerSocket serverSocket = null;
    
    static ArrayList<Client> client_list = new ArrayList<Client>();
    static ArrayList <Integer> LiveClientID = new ArrayList<Integer>();
    static int clientCount = 0;
    
    public MainServer2()
    {
        
    }
    
    public static Client getClient(int ClientID)
    {
        Client client = null;
        for(int i=0 ; i<client_list.size() ; i++)
        {
            client = client_list.get(i);
            if(ClientID == client.getClientID())
                break;
        }
        return client;
    }
    
    public static void init_fileTransfer(int source_clientID , int request_clientID, FileObject fileObject_choice, int backupFlag) throws IOException
    {
        Client source_client = getClient(source_clientID);
        Client request_client = getClient(request_clientID);
        
        Socket source_socket = source_client.getHiddenSocket();
        Socket request_socket = request_client.getSocket();
        
        DataOutputStream dos_source = new DataOutputStream(source_socket.getOutputStream());
        DataOutputStream dos_request = new DataOutputStream(request_socket.getOutputStream());
        
        dos_source.writeInt(FileCodes.DIFFERENT_REQUESTOR_SOURCE);
        dos_source.flush();
        dos_request.writeInt(FileCodes.DIFFERENT_REQUESTOR_SOURCE);
        dos_request.flush();
        
        dos_source.writeInt(FileCodes.FILE_SENDER_SERVER);
        dos_source.flush();
        dos_request.writeInt(FileCodes.FILE_RECEIVER_CLIENT);
        dos_request.flush();
        
        dos_source.writeInt(FileCodes.START_SERVER);
        dos_source.flush();
        dos_request.writeInt(FileCodes.START_CLIENT);
        dos_request.flush();
        
        dos_source.writeInt(source_client.getAssignedPort());
        dos_source.flush();
        
        dos_request.writeInt(source_client.getAssignedPort());
        dos_request.flush();
        dos_request.writeUTF(source_client.getIP());
        dos_request.flush();
        
        if(backupFlag==0)	dos_source.writeUTF(fileObject_choice.getFilePath());
        else	dos_source.writeUTF(fileObject_choice.getBackupPath());
        dos_source.flush();
        
        dos_request.writeUTF(fileObject_choice.getFileName());
        dos_request.flush();
    }
    
    public static void startServer() throws IOException
    {
        try
        {
            serverSocket = new ServerSocket(6664);
            
            SqlOp so = new SqlOp();
            while(true)
            {
                Socket s = serverSocket.accept();
                
                DataInputStream din = new DataInputStream(s.getInputStream());
                DataOutputStream dout = new DataOutputStream(s.getOutputStream());
                
                int ClientID = din.readInt();
                /*if(ClientID == -1)
                {
                    String ClientIP = s.getInetAddress().toString();
                    ClientIP = ClientIP.substring(1);
                    System.out.println(s.getPort() + " " + s.getLocalPort() + " " + ClientIP + " is connected");
                    ClientID = so.addClient(clientCount, ClientIP);
                    dout.writeInt(ClientID);
                    dout.flush();
                }
                */
                
                if(!so.checkClientExists(ClientID))
                {
                	  String ClientIP = s.getInetAddress().toString();
                      ClientIP = ClientIP.substring(1);
                	  ClientID = so.addClient(clientCount, ClientIP);
                }
                
                System.out.println(ClientID + " connected " + s);
                dout.writeInt(7000 + clientCount);
                dout.flush();
                ServerSocket hiddenServerSocket = new ServerSocket(7000 + clientCount);
                Socket hs = hiddenServerSocket.accept();
                Client client = new Client(ClientID , s.getInetAddress().toString() , so.getClientPort(ClientID) , s, hs);
                client_list.add(client);
                LiveClientID.add(ClientID);
                new Thread(new ClientHandler(ClientID, s, hs)).start();
                clientCount++;
            }
        }
        finally{
            if(serverSocket!=null) serverSocket.close();
        }
    }
    
    public static void sendMessage(int ClientID , int code) throws IOException
    {
        Client client = getClient(ClientID);
        System.out.println("sendMessage before sending code " + client.getClientID() + " " + code);
        Socket s = client.getSocket();
        System.out.println(s);
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        dos.writeInt(code);
        dos.flush();
        System.out.println("sendMessage after sending code " + client.getClientID() + " " + code);
    }
    
    public static void main(String args[]) throws IOException
    {
        startServer();
    }
    
    public static class ClientHandler implements Runnable
    {
        private final int ClientID;
        private final Socket socket;
        private final Socket hiddenSocket;
        
        ClientHandler(int ClientID , Socket socket, Socket hs)
        {
            this.ClientID = ClientID;
            this.socket = socket;
            this.hiddenSocket = hs;
        }
        
        public void init() throws SQLException
        {
            try {
                DataInputStream din = new DataInputStream(socket.getInputStream());
                
                int filesCount = din.readInt();
                
                FileObject[] fileObjects = new FileObject[filesCount];
                for(int i=0 ; i<filesCount ; i++)
                {
                    String fileName = din.readUTF();
                    String filePath = din.readUTF();
                    String fileAbsolutePath = din.readUTF();
                    fileObjects[i] = new FileObject(fileName , filePath , fileAbsolutePath);
                }
                
                SqlOp so = new SqlOp();
                so.addFiles(ClientID, fileObjects);
            } catch (IOException ex) {
                Logger.getLogger(MainServer2.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        public void execute_choices(int CHOICE_CODE)
        {
            try{
                SqlOp so = new SqlOp();
                switch(CHOICE_CODE)
                {
                    case FileCodes.LIST_ALL_FILES:
                    {
                        FileObject[] fileObjects = so.getFileObjects();
                
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        int filesCount = fileObjects.length;
                        dos.writeInt(filesCount);
                
                        for(FileObject fileObject : fileObjects)
                        {
                            dos.writeInt(fileObject.getSourceClientID());
                            dos.flush();
                            dos.writeUTF(fileObject.getFileName());
                            dos.flush();
                            dos.writeUTF(fileObject.getFilePath());
                            dos.flush();
                            dos.writeUTF(fileObject.getFileAbsolutePath());
                            dos.flush();
                        }
                        
                        DataInputStream din = new DataInputStream(socket.getInputStream());
                        int file_choice = din.readInt();
                        FileObject fileObject_choice = fileObjects[file_choice];
                        //System.out.println(fileObject_choice.getFileName());
                        int request_clientID = ClientID;
                        int source_clientID = fileObject_choice.getSourceClientID();
                        int backupFlag=0;
                      //  System.out.println("Current Client: "+ClientID+" Original source: "+source_clientID+!LiveClientID.contains((Integer)source_clientID));
                        if(!LiveClientID.contains((Integer)source_clientID))
                        {
                        	backupFlag=1;
                        	System.out.println(LiveClientID.toString());
                        	System.out.println("Taking from backup");
                        	source_clientID=so.getBackupClientID(fileObject_choice.getFileName());
                        	System.out.println("Backup ID is : "+source_clientID);
                        	String s= "backup"+(source_clientID+1)+"\\"+fileObject_choice.getFileName();
                        	fileObject_choice.setBackupPath(s);
                        	System.out.println("Backup path is: "+s);
                        }
                        if(request_clientID == source_clientID && backupFlag==0)
                        {
                            dos.writeInt(FileCodes.SAME_REQUESTOR_SOURCE);
                            dos.flush();
                        }
                        else
                        {
                            init_fileTransfer(source_clientID, request_clientID, fileObject_choice,backupFlag);
                        }
                    }
                    break;
                  /*  case FileCodes.LIST_ALL_FILES_CLIENTID:
                    {
                        DataInputStream din = new DataInputStream(socket.getInputStream());
                        int clientID = din.readInt();
                        
                        FileObject[] fileObjects = so.getFileObjects(clientID);
                        
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        int filesCount = fileObjects.length;
                        dos.writeInt(filesCount);
                        
                        for(FileObject fileObject : fileObjects)
                        {
                            dos.writeInt(fileObject.getSourceClientID());
                            dos.flush();
                            dos.writeUTF(fileObject.getFileName());
                            dos.flush();
                            dos.writeUTF(fileObject.getFilePath());
                            dos.flush();
                            dos.writeUTF(fileObject.getFileAbsolutePath());
                            dos.flush();
                        }
                        
                        int file_choice = din.readInt();
                        FileObject fileObject_choice = fileObjects[file_choice];
                        //System.out.println(fileObject_choice.getFileName());
                        int request_clientID = ClientID;
                        int source_clientID = fileObject_choice.getSourceClientID();
                        if(request_clientID == source_clientID)
                        {
                            dos.writeInt(FileCodes.SAME_REQUESTOR_SOURCE);
                            dos.flush();
                        }
                        else
                        {
                            init_fileTransfer(source_clientID, request_clientID, fileObject_choice);
                        }
                    }
                    break;
                    case FileCodes.LIST_ALL_FILES_FILENAME:
                    {
                        DataInputStream din = new DataInputStream(socket.getInputStream());
                        String name = din.readUTF();
                        
                        FileObject[] fileObjects = so.getFileObjects(name);
                        
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        int filesCount = fileObjects.length;
                        dos.writeInt(filesCount);
                        
                        for(FileObject fileObject : fileObjects)
                        {
                            dos.writeInt(fileObject.getSourceClientID());
                            dos.flush();
                            dos.writeUTF(fileObject.getFileName());
                            dos.flush();
                            dos.writeUTF(fileObject.getFilePath());
                            dos.flush();
                            dos.writeUTF(fileObject.getFileAbsolutePath());
                            dos.flush();
                        }
                        
                        int file_choice = din.readInt();
                        FileObject fileObject_choice = fileObjects[file_choice];
                        //System.out.println(fileObject_choice.getFileName());
                        int request_clientID = ClientID;
                        int source_clientID = fileObject_choice.getSourceClientID();
                        if(request_clientID == source_clientID)
                        {
                            dos.writeInt(FileCodes.SAME_REQUESTOR_SOURCE);
                            dos.flush();
                        }
                        else
                        {
                            init_fileTransfer(source_clientID, request_clientID, fileObject_choice);
                        }
                    }
                    break;*/
                    case FileCodes.EXIT_CODE:
                    {
                        socket.close();
                    }
                    break;
                }
            }catch(Exception ex){
                Logger.getLogger(MainServer2.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        @Override
        public void run()
        {
            try {
                init();
                try {
                    DataInputStream din = new DataInputStream(socket.getInputStream());
                    int choice;
                    while(true)
                    {
                    	 try {
                    		 choice = din.readInt();
           				
           			} catch (IOException e1) {
           				// TODO Auto-generated catch block
           				MainServer2.ClientDisconnected(ClientID);
           				//e1.printStackTrace();
           				break;
           			}
                        
                        execute_choices(choice);
                        if(choice == 4)
                            break;
                    }
                } catch (IOException ex) {
                    Logger.getLogger(MainServer2.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (SQLException ex) {
                Logger.getLogger(MainServer2.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

	public static void ClientDisconnected(int clientID) {
		// TODO Auto-generated method stub
		System.out.println("Client disconnected "+clientID);
		LiveClientID.remove((Integer)clientID);
		/*System.out.println("After removing :");
		System.out.println(LiveClientID.toString());*/
		
	}
    
}
