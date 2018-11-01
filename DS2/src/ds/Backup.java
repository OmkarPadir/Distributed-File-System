package ds;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import Operations.Client;
import Operations.FileCodes;
import Operations.FileObject;
import Operations.SqlOp;
import Operations.backupOperation;
import ds.MainServer.ClientHandler;

import java.lang.Math;
import java.net.ServerSocket;
import java.net.Socket; 



class BackupServerProcess{
	static ServerSocket serverSocket = null;
    
    static ArrayList<Client> client_list = new ArrayList<Client>();
    static ArrayList<Integer> Old_client_id_list = new ArrayList<Integer>();
    
    static int clientCount = 0;
    static ArrayList<ArrayList<FileObject>> ClientFilesList; 
      
    static SqlOp so = new SqlOp();
    public static void startServer() throws IOException
    {
    	Old_client_id_list = so.getOldClientDetails();    	
    	
        try
        {
            serverSocket = new ServerSocket(6677);
            
            
            while(true)
            {
                Socket s = serverSocket.accept();
                clientCount++;
               
                DataInputStream din = new DataInputStream(s.getInputStream());
                DataOutputStream dout = new DataOutputStream(s.getOutputStream());
                              
                int ClientID = din.readInt();
                System.out.println(" Client connected; ClientID: " +ClientID);
                
                if(Old_client_id_list.contains(ClientID))
                {
                System.out.println("Client Reconnected");  
                }
                else {
                	System.out.println("New Client detected");
                	Old_client_id_list.add(ClientID);
                	try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                	
                }
                if(clientCount>1)
                	redistribute();
                Client client = new Client(ClientID , s.getInetAddress().toString() , so.getClientPort(ClientID) , s, null);
                client_list.add(client);     
                
                Thread t = new BackupClientHandler(s, din, dout, ClientID); 
                
                // Invoking the start() method 
                t.start(); 
                  
            }
        }
        finally{
            if(serverSocket!=null) serverSocket.close();
        }
    }
    
    public static void ClientDisconnected(int clientID2) {
		// TODO Auto-generated method stub
    	
    	System.out.println("Client Disconnected "+ clientID2);
    	clientCount--;
		
	} 
	private static void redistribute() {
		// TODO Auto-generated method stub
		try {
			 ClientFilesList = new ArrayList<ArrayList<FileObject>>();
			FileObject [] fileobjects = so.getAllFilesforBackup();
			int currentClientId = fileobjects[0].getSourceClientID();
			ArrayList<FileObject> folist = new ArrayList<FileObject>();
			for( int i=0; i< fileobjects.length; i++)
			{
				// System.out.println("first For"+ fileobjects[i].getFileName());
			  if( fileobjects[i].getSourceClientID() == currentClientId)
			  {
				  folist.add(fileobjects[i]);			  			  
			  }
			  else
			  {
				  ClientFilesList.add(folist);
				  //System.out.println("CurrClient & size" + currentClientId + folist.size());
				  folist = new ArrayList<FileObject>();
				 					 
				  currentClientId++;
				  i--;
			  }
			 
			}
			ClientFilesList.add(folist);
			
			System.out.println("Files wrt Clients");
			for(int i=0 ; i<ClientFilesList.size(); i++)
			{
				System.out.println("Client : " + i );
				ArrayList<FileObject> ffffflist = ClientFilesList.get(i);
				for (int j=0;j<ffffflist.size(); j++)
				{
					System.out.println(ffffflist.get(j).getFileName());
				}					
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		  int partitions = ClientFilesList.size()-1;
		  
		for(int currClient=0 ; currClient<ClientFilesList.size(); currClient++)
		{
			int senderClientId= currClient;
			int recieverClientId = ((currClient+1)%ClientFilesList.size());
			
			ArrayList<FileObject> currFilelist = ClientFilesList.get(currClient);
			
			int partition_size= (int) Math.ceil((double)currFilelist.size()/partitions);
			
		
			int currFilePtr =0 ;
			
			for(int x=0 ; x<partitions; x++)
			{
				for(int y=0; (y<partition_size) && (currFilelist.size()>(y+currFilePtr)) ; y++)
				{
					System.out.print("Sender: "+ senderClientId);
					System.out.print(" Reciever: "+ recieverClientId);
					System.out.println(" File: "+ currFilelist.get(y+currFilePtr).getFileName());
					so.addRequiredBackupDistribution(currFilelist.get(y+currFilePtr).getFileName(),senderClientId,recieverClientId);
				}
				
				currFilePtr= currFilePtr+partition_size;
				recieverClientId = ((recieverClientId+1)%ClientFilesList.size());
			}
			
			
		}
	}
   
}
class BackupClientHandler extends Thread  
{ 
   
    final DataInputStream din; 
    final DataOutputStream dout; 
    final Socket s; 
    final int ClientID;
    SqlOp sop =new SqlOp();  
  
    // Constructor 
    public BackupClientHandler(Socket s, DataInputStream dis, DataOutputStream dos, int c)  
    { 
        this.s = s; 
        this.din = dis; 
        this.dout = dos; 
        ClientID=c;
    } 
  
    @Override
    public void run()  
    {     	
    	try {
			Thread.sleep(5000);
		
    	int oldclientCount = BackupServerProcess.clientCount;
    	startBackupAction();  
    	System.out.println("BackupActionfinished Once");
    	while(true) {
    		
    		synchronized (din) 
    		{
    			if(oldclientCount<BackupServerProcess.clientCount)
        		{
        			System.out.println("running backup action again");
        			Thread.sleep(5000);
        	        startBackupAction();  
        	        oldclientCount=BackupServerProcess.clientCount;
        		}
			}
    	}
    	} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }

	private void startBackupAction() {
		// TODO Auto-generated method stub
		int i=0;
		int requiredcountOfActions = sop.getBackupActionCount(ClientID);
		System.out.println("ActionCount= "+requiredcountOfActions);
	      while(i<requiredcountOfActions)
	      {
	    	  try {
	    		  din.readBoolean();
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				BackupServerProcess.ClientDisconnected(ClientID);
				//e1.printStackTrace();
				break;
			}
	    	  backupOperation bo= new backupOperation();
	    	  bo=sop.getBackupAction(ClientID);
	          if(bo != null && bo.senderRecieverFlag == 1)
	          {
	        	  try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	        	  System.out.println("Client "+ClientID+" Created Reciever");
	        	  try {
	      			dout.writeUTF("CreateReciever");
	      			
	      			dout.writeInt(bo.SenderID);
	      			dout.writeUTF(bo.Filename);
	      			dout.writeUTF(sop.getClientIP(bo.SenderID));
	      			int v=din.readInt();
	      			if(v==1)
	      			{
	      				sop.addtoCurrentDistribution(bo.SenderID, ClientID, bo.Filename);
	      				System.out.println("Reciever action complete of "+ClientID);
	      			}
	      			
	      			int x=din.readInt();
	      			
	      			
	      		} catch (IOException e) {
	      			// TODO Auto-generated catch block
	      			e.printStackTrace();
	      		}
	        	  
	          }
	          else if(bo != null && bo.senderRecieverFlag == 0)
	          {
	        	  try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	        	  System.out.println("Client "+ClientID+" Created Server");
	        	  try {
	    			dout.writeUTF("CreateSender");
	    			
	    			dout.writeUTF(bo.Filename);
	    			int x=din.readInt();
	    			System.out.println("Sender action complete of "+ClientID);
	    			
	    		} catch (IOException e) {
	    			// TODO Auto-generated catch block
	    			e.printStackTrace();
	    		}
	          }
	        /*  else if(bo==null){
	        	 
	        	 // continue;
	          }*/
	    	  
	          i++;
	      }
		
	}

	
} 


public class Backup {
	 public static void main(String args[]) throws IOException
	    {		 
		 BackupServerProcess.startServer();
	    }

	
    
	
	
	
}
