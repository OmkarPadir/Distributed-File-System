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
import Operations.FileCodes;
import Operations.FileOp;
import Operations.SqlOp;
import java.io.*;
import java.net.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
public class Slave_Client2 {
    
    private static int ClientID=1;
    private static int config=1;
    private static final String CLIENT_DIRECTORY = "clientfiles" + Integer.toString(config+1);
    private static final String CLIENT_TEMP_DIRECTORY = "temp" + Integer.toString(config+1);
    private static final String CLIENT_BACKUP_DIRECTORY = "backup" + Integer.toString(config+1);
    private static Socket socket = null;
    private static Socket Backupsocket = null;
    private static final String SERVER_IP = "192.168.43.115";
    private static final int SERVER_PORT = 6665;
    private static final int BACKUP_SERVER_PORT = 6677;
    private static final String SERVER_IP_2 = "192.168.43.115";
    private static final int SERVER_PORT_2 = 6664;
    private static Socket hiddenSocket = null;
    
    private static Thread t = null;
    
    public static void init(Socket socket) throws IOException
    {
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        DataInputStream din = new DataInputStream(socket.getInputStream());
        
        SqlOp so = new SqlOp();
       // ClientID = so.getClientID(config);
        dos.writeInt(ClientID);
        dos.flush();
       /* if(ClientID == -1)
        {
            ClientID = din.readInt();
            so.writeClientID(ClientID);
            config = ClientID;
        }*/
        //System.out.println(ClientID);
        
        
       
        
       
        
        int port = din.readInt();
        hiddenSocket = new Socket(SERVER_IP , port);
        System.out.println("Hidden Back Channel Connected");
        
        File[] filesList = (new File(CLIENT_DIRECTORY)).listFiles();
        int filesCount = filesList.length;
        
        dos.writeInt(filesCount);
        dos.flush();
        for(File file : filesList)
        {
            //if(file.isDirectory())
                //continue;
            dos.writeUTF(file.getName());
            dos.flush();
            dos.writeUTF(file.getPath());
            dos.flush();
            dos.writeUTF(file.getAbsolutePath());
            dos.flush();
        }
    }
    
    public static void execute_choices(int choice)
    {
        try
        {
            switch(choice)
            {
                case FileCodes.LIST_ALL_FILES:
                {
                    DataInputStream din = new DataInputStream(socket.getInputStream());
                    int filesCount = din.readInt();
        
                    FileObject[] fileObjects = new FileObject[filesCount];
                    for(int i=0 ; i<filesCount ; i++)
                    {
                        int clientID = din.readInt();
                        String fileName = din.readUTF();
                        String filePath = din.readUTF();
                        String fileAbsolutePath = din.readUTF();
                        fileObjects[i] = new FileObject(fileName,filePath,fileAbsolutePath,clientID);
                        System.out.println(i + "\t" + fileName);
                    }
                    Scanner sc = new Scanner(System.in);
                    System.out.println("Enter file choice:");
                    int file_choice;
                    do{
                        file_choice = sc.nextInt();
                        if(file_choice>filesCount)
                            System.out.println("Invalid file choice");
                    }while(file_choice>filesCount);
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    dos.writeInt(file_choice);
                    int return_code = din.readInt();
                    if(return_code == FileCodes.SAME_REQUESTOR_SOURCE)
                    {
                        FileOp fo = new FileOp();
                        String source = fileObjects[file_choice].getFilePath();
                        String receive = CLIENT_TEMP_DIRECTORY + "/" + fileObjects[file_choice].getFileName();
                        if(fo.copyFile(source, receive))
                            System.out.println("File copied successfully!");
                        else
                            System.out.println("ERROR WHILE COPYING FILE");
                    }
                    else if(return_code == FileCodes.DIFFERENT_REQUESTOR_SOURCE)
                    {
                        System.out.println("Different requestor source");
                        int code = din.readInt();
                        switch(code)
                        {
                            case FileCodes.FILE_RECEIVER_CLIENT:
                            {
                                System.out.println("FILE_RECEIVER_CLIENT");
                                
                                code = din.readInt();
                                if(code == FileCodes.START_CLIENT)
                                {
                                    int port = din.readInt();
                                    String ip = din.readUTF();
                                    ip = ip.substring(1);
                                    System.out.println("Port: " + port + " IP: " + ip);
                                    Socket fileClient = new Socket(ip , port);
                                    System.out.println("File client connected " + fileClient);
                                    
                                    String fileName = din.readUTF();
                                    System.out.println("File name: " + fileName);
                                    
                                    FileOp fo = new FileOp(fileClient.getInputStream());
                                    if(fo.receiveFile(new File(CLIENT_TEMP_DIRECTORY+"/"+fileName)))
                                        System.out.println("File received successfully!");
                                    else
                                        System.out.println("ERROR WHILE RECEIVING FILE");
                                    fileClient.close();
                                }
                            }
                            break;
                            case FileCodes.FILE_SENDER_SERVER:
                            {
                                System.out.println("FILE_SENDER_SERVER");
                            }
                            break;
                        }
                    }
                }
              }
        }             
            
        catch(Exception ex)
        {
            Logger.getLogger(MainServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void startClient(int port , String ip)
    {
    	
    	
    	
        try {
            Scanner sc = new Scanner(System.in);
            socket = new Socket(ip , port);
            System.out.println(socket);
            init(socket);
            
            
            Backupsocket = new Socket(SERVER_IP,BACKUP_SERVER_PORT);
            new Thread(new BackupClientProcess(Backupsocket)).start();
            
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            t = new Thread(new hiddenReceiveThread(new DataInputStream(hiddenSocket.getInputStream())));
            t.start();
            
            new Thread(new CheckThread()).start();
            
            while(true){
                System.out.println("Enter 1 for refresh");
                int ch = sc.nextInt();
                if(ch==1)
                    continue;
                int choice = FileCodes.LIST_ALL_FILES;
                dos.writeInt(choice);
                dos.flush();
                execute_choices(choice);
                if(choice == 4)
                    break;
            }
        } catch (IOException ex) {
            //Logger.getLogger(Slave_Client2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void reconnectServer()
    {
        t.stop();
        System.out.println("Reconnecting to another server....");
        startClient(SERVER_PORT_2, SERVER_IP_2);
    }
    
    public static void main(String args[]) throws IOException
    {
        startClient(SERVER_PORT , SERVER_IP);
    }
    
    private static class CheckThread implements Runnable
    {
        @Override
        public void run()
        {
            while(true)
            {
                if(t.isAlive())
                {
                    
                }
                else
                {
                    reconnectServer();
                    return;
                }
            }
        }
    }
    
    public static class BackupClientProcess implements Runnable
    {
    	 Socket backupSocket;
    	 public BackupClientProcess(Socket s) {
			// TODO Auto-generated constructor stub
    		 backupSocket =s ;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			 try {
				 
				 DataInputStream din = new DataInputStream(backupSocket.getInputStream());
				 DataOutputStream dout = new DataOutputStream(backupSocket.getOutputStream());
				 
				 dout.writeInt(ClientID);  
				 
				 SqlOp so = new SqlOp();
				 while (true) 
				 {
					 dout.writeBoolean(true);
					 try {
				 String Action=din.readUTF();
				 
				
				 if(Action.equals("CreateReciever"))
				 {
					 System.out.println("Backup file reciever client Created");
					 byte []b=new byte[2000];
					 int SenderID=din.readInt();
					 String FileName=din.readUTF();
					 System.out.println("SenderID is "+SenderID);
					 Socket sr;
					 String SenderIP=din.readUTF();
					 System.out.println("Sender IP is "+SenderIP);
					 System.out.println("File name: " + FileName);
					 int port=9000+SenderID;
					 System.out.println("port is"+port);
					 sr = new Socket(SenderIP,port);
					 
					 
                     
                     FileOp fo = new FileOp(sr.getInputStream());
                     boolean rvalue = fo.receiveFile(new File(CLIENT_BACKUP_DIRECTORY+"/"+FileName));
                     int sendValue = 0;
                     if(rvalue)
                         sendValue = 1;
                     dout.writeInt(sendValue);
                     if(rvalue)
                     {
                         System.out.println("File received successfully!");
                         //so.addtoCurrentDistribution(SenderID,ClientID,FileName);
                         
                     }
                     else
                         System.out.println("ERROR WHILE RECEIVING FILE");
                     sr.close();
                     /*
					 InputStream is=sr.getInputStream();
					 
					 File file = new File("\\backup"+ClientID+"\\"+FileName);
					 System.out.println(file.getAbsolutePath());
					 FileOutputStream fr=new FileOutputStream(file.getAbsolutePath());
					 is.read(b,0,b.length);
					 fr.write(b, 0, b.length);*/
				 }
				 else if(Action.equals("CreateSender"))
				 {
					 
					System.out.println("Starting sender");
					 String FileName= din.readUTF();
					 int port = 9000+ClientID;
					 System.out.println("port is "+port);
					 ServerSocket s=new ServerSocket(port);
					 System.out.println("Backup file sender Server Created at port "+port+"for File"+FileName);
					 Socket sr=s.accept();
					 System.out.println("reciver connected");
					 
					 FileOp fo = new FileOp(sr.getOutputStream());
					 
					 //fo.sendFile(new File(CLIENT_DIRECTORY+"\\"+FileName));
                     if(fo.sendFile(new File(CLIENT_DIRECTORY+"\\"+FileName)))
                         System.out.println("File sent successfully!");
					
                     else
                         System.out.println("ERROR WHILE SENDING FILE!");
                     
                     Thread.sleep(100);
                     sr.close();
                     s.close();
                    
				 }
		
				 dout.writeInt(1);
				 }
				 catch (ConnectException e) {
					    // TODO Auto-generated catch block
						
					   // e.printStackTrace();
					} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}  
				 }
				 
				 
				 
			}
			 catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
    	
    }
   

	public static class hiddenReceiveThread implements Runnable
    {
        DataInputStream din = null;
        String filePath = "";
        
        hiddenReceiveThread(DataInputStream din)
        {
            this.din = din;
        }
        
        public void setFilePath(String fp)
        {
            filePath = fp;
        }

        @Override
        public void run() {
            A:
            while(true)
            {
                try {
                    int code = this.din.readInt();
                    
                    if(code == FileCodes.DIFFERENT_REQUESTOR_SOURCE)
                    {
                        System.out.println("DIFFERENT REQUESTOR SOURCE");
                        
                        code = this.din.readInt();
                        B:
                        switch(code)
                        {
                            case FileCodes.FILE_RECEIVER_CLIENT:
                            {
                                System.out.println("FILE RECEIVER CLIENT");
                            }
                            break B;
                            case FileCodes.FILE_SENDER_SERVER:
                            {
                                System.out.println("FILE SENDER SERVER");
                                
                                code = din.readInt();
                                if(code == FileCodes.START_SERVER)
                                {
                                    int port = din.readInt();
                                    System.out.println("Port received: " + port);
                                    ServerSocket fileServer = new ServerSocket(port);
                                    System.out.println("File server created waiting to connect!");
                                    Socket fileSocket = fileServer.accept();
                                    System.out.println("Client connected " + fileSocket);
                                    
                                    String filePath = din.readUTF();
                                    System.out.println("File path: " + filePath);
                                    
                                    FileOp fo = new FileOp(fileSocket.getOutputStream());
                                    if(fo.sendFile(new File(filePath)))
                                        System.out.println("File sent successfully!");
                                    else
                                        System.out.println("ERROR WHILE SENDING FILE!");
                                    fileSocket.close();
                                    fileServer.close();
                                }
                            }
                            break B;
                        }
                    }
                } catch (IOException ex) {
                    if(ex instanceof SocketException)
                    {
                        System.out.println(ex.toString());
                        return;
                    }
                }
            }
        }
        
        
    }
    
}
