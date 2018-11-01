/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Operations;

/**
 *
 * @author Srini
 */
import Operations.FileObject;
import Operations.Client;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqlOp {
    
    private Connection connection = null;
    
    public SqlOp()
    {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/FileSystem" , "root" , "");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void clearFileDetails() throws SQLException
    {
        Statement stmt = this.connection.createStatement();
        stmt.executeUpdate("truncate FILE_DETAILS");
    }
    
    private void clearClientDetails() throws SQLException
    {
        Statement stmt = this.connection.createStatement();
        stmt.executeUpdate("truncate CLient_Details");
    }
    
    private void clearDatabase() throws SQLException
    {
        this.clearClientDetails();
        this.clearFileDetails();
    }
    
    public boolean addFiles(int ClientID , FileObject[] filesList) throws SQLException
    {
        if(ClientID<0)
            return false;
        if(filesList.length==0)
            return false;
        
        //clearFileDetails();
        
        try {
            PreparedStatement ps = this.connection.prepareStatement("INSERT INTO FILE_DETAILS VALUES(?,?,?,?)");
            
            for(FileObject file : filesList)
            {
                String fileName = file.getFileName();
                String filePath = file.getFilePath();
                String fileAbsolutePath = file.getFileAbsolutePath();
                
                ps.setInt(1, ClientID);
                ps.setString(2, fileName);
                ps.setString(3, filePath);
                ps.setString(4, fileAbsolutePath);
                
                try{
                    ps.executeUpdate();
                }catch(SQLIntegrityConstraintViolationException e){
                    //System.out.println("Unique Violation " + fileName + " " + filePath + " " + fileAbsolutePath);
                    continue;
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }
    
    public ResultSet getFiles()
    {
        try {
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM FILE_DETAILS ORDER BY(FileName)");
            return rs;
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public FileObject[] getFileObjects() throws SQLException
    {
        ResultSet rs = this.getFiles();
        rs.last();
        int count = rs.getRow();
        FileObject[] fileObjects = new FileObject[count];
        rs.beforeFirst();
        for(int i=0; rs.next() ; i++)
        {
            fileObjects[i] = new FileObject(rs.getString("FileName"),rs.getString("FilePath"),rs.getString("FileAbsolutePath"),rs.getInt("ClientID"));
        }
        return fileObjects;
    }
    public FileObject[] getAllFilesforBackup() throws SQLException
    {
    	 Statement stmt = this.connection.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT * FROM FILE_DETAILS");
        rs.last();
        int count = rs.getRow();
        FileObject[] fileObjects = new FileObject[count];
        rs.beforeFirst();
        for(int i=0; rs.next() ; i++)
        {
            fileObjects[i] = new FileObject(rs.getString("FileName"),rs.getString("FilePath"),rs.getString("FileAbsolutePath"),rs.getInt("ClientID"));
        }
        return fileObjects;
    }
   
    
    
    public ResultSet getFiles(int ClientID)
    {
        try {
            PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM FILE_DETAILS WHERE ClientID = ?");
            ps.setInt(1, ClientID);
            ResultSet rs = ps.executeQuery();
            return rs;
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public FileObject[] getFileObjects(int ClientID) throws SQLException
    {
        ResultSet rs = this.getFiles(ClientID);
        rs.last();
        int count = rs.getRow();
        FileObject[] fileObjects = new FileObject[count];
        rs.beforeFirst();
        for(int i=0; rs.next() ; i++)
        {
            fileObjects[i] = new FileObject(rs.getString("FileName"),rs.getString("FilePath"),rs.getString("FileAbsolutePath"),rs.getInt("ClientID"));
        }
        return fileObjects;
    }
	
    
    public ResultSet getFiles(String fileName)
    {
        try {
            PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM FILE_DETAILS WHERE FileName = ?");
            ps.setString(1, fileName);
            ResultSet rs = ps.executeQuery();
            return rs;
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public FileObject[] getFileObjects(String fileName) throws SQLException
    {
        ResultSet rs = this.getFiles(fileName);
        rs.last();
        int count = rs.getRow();
        FileObject[] fileObjects = new FileObject[count];
        rs.beforeFirst();
        for(int i=0; rs.next() ; i++)
        {
            fileObjects[i] = new FileObject(rs.getString("FileName"),rs.getString("FilePath"),rs.getString("FileAbsolutePath"),rs.getInt("ClientID"));
        }
        return fileObjects;
    }
    
    public boolean checkClientExists(int ClientID)
    {
        try {
            PreparedStatement ps = this.connection.prepareStatement("SELECT count(ClientID) as count FROM Client_Details WHERE ClientID = ?");
            ps.setInt(1, ClientID);
            ResultSet rs = ps.executeQuery();
            rs.next();
            int count = rs.getInt("count");
            if(count==0)
                return false;
            
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }
    
    public int addClient(int ClientID , String ClientIP)
    {
        int port = 6767;
        
        try {
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT count(*) as count FROM Client_Details");
            rs.next();
            int count = rs.getInt("count");
            
            port = port + count;
            PreparedStatement ps = this.connection.prepareStatement("INSERT INTO Client_Details VALUES(?,?,?)");
            ps.setInt(1, ClientID);
            ps.setString(2, ClientIP);
            ps.setInt(3, port);
            try{
                ps.executeUpdate();
                return ClientID;
            }catch(SQLIntegrityConstraintViolationException e)
            {
                return ClientID;
            }
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    public int getClientID(int id)
    {
        try {
            String table = "client_config" + Integer.toString(id);
            Statement s = this.connection.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM " + table);
            if(rs.next())
                return rs.getInt("ClientID");
            else 
                return -1;
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    public int getClientPort(int id)
    {
        try {
            PreparedStatement ps = this.connection.prepareStatement("SELECT ClientPort FROM Client_Details WHERE ClientID = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            rs.next();
            int port = rs.getInt("ClientPort");
            return port;
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
    }
    
    public int writeClientID(int id)
    {
        try {
            String table = "client_config" + Integer.toString(id);
            PreparedStatement ps = this.connection.prepareStatement("INSERT INTO " + table + " VALUES(?)");
            ps.setInt(1,id);
            ps.executeUpdate();
            return id;
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    public ResultSet getClientDetails(int ClientID)
    {
        try {
            PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM Client_Details WHERE ClientID = ?");
            ps.setInt(1 , ClientID);
            ResultSet rs = ps.executeQuery();
            return rs;
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public ArrayList<Integer> getOldClientDetails()
    {
    	 ArrayList<Integer> Old_client_list = new ArrayList<Integer>();
        try {
            PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM Client_Details");
            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
            	Old_client_list.add(rs.getInt("ClientID"));
            }
            
            return Old_client_list;
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

	public void addRequiredBackupDistribution(String fileName, int senderClientId, int recieverClientId) {
		// TODO Auto-generated method stub
		 
	       
	        
	        try {
	        	PreparedStatement ps1 = this.connection.prepareStatement("SELECT count(*) as count FROM CURRENT_FILE_DISTRIBUTION  WHERE SenderClientID="+senderClientId+ 
	        			            		" AND BackupClientID="+recieverClientId+" AND FileName='"+fileName+"'");
	        	 ResultSet rs = ps1.executeQuery();
	        	 rs.next();
	        	if(rs.getInt("count")==0) {
	        		PreparedStatement ps2 = this.connection.prepareStatement("DELETE FROM CURRENT_FILE_DISTRIBUTION  WHERE FileName='"+fileName+"'");
                   ps2.executeUpdate();	
                   PreparedStatement ps3 = this.connection.prepareStatement("DELETE FROM REQUIRED_FILE_DISTRIBUTION  WHERE FileName='"+fileName+"'");
                   ps3.executeUpdate();	
	        		
	            PreparedStatement ps = this.connection.prepareStatement("INSERT INTO REQUIRED_FILE_DISTRIBUTION VALUES("+senderClientId+","+recieverClientId+",'"+fileName+"')");
	            ps.executeUpdate();
	        	}
	            
	        } catch (SQLException ex) {
	            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
	           
	        }
	      
		
	}

	public backupOperation getBackupAction(int ClientID) {
		// 0: Do nothing, 1 sendFile, 2 recieve file
		
		backupOperation bo ;
		try {
            PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM REQUIRED_FILE_DISTRIBUTION");
            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
            	if(rs.getInt("SenderClientID")==ClientID) {
            		bo=new backupOperation(0,rs.getInt("SenderClientID"),rs.getInt("RecieverClientID"),rs.getString("FileName"));
            		return bo;
            	}
            	else if(rs.getInt("RecieverClientID")==ClientID)
            	{	
            		bo=new backupOperation(1,rs.getInt("SenderClientID"),rs.getInt("RecieverClientID"),rs.getString("FileName"));
        		    return bo;
            	}
            }
            
           
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
        }
		return null;
	}

	public String getClientIP(int clientID) {
		// TODO Auto-generated method stub
		 try {
	            PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM Client_Details WHERE ClientID = ?");
	            ps.setInt(1 , clientID);
	            ResultSet rs = ps.executeQuery();
	            rs.next();
	            System.out.println(rs.getString("ClientIP"));
	            return rs.getString("ClientIP");
	        } catch (SQLException ex) {
	            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
	        }
		return null;
	}

	public void addtoCurrentDistribution(int senderID, int clientID, String fileName) {
		// TODO Auto-generated method stub
		try {
            PreparedStatement ps = this.connection.prepareStatement("INSERT INTO CURRENT_FILE_DISTRIBUTION VALUES("+senderID+","+clientID+",'"+fileName+"')");
            ps.executeUpdate();
            
            PreparedStatement ps1 = this.connection.prepareStatement("DELETE FROM REQUIRED_FILE_DISTRIBUTION WHERE SenderClientID="+senderID
            		+" AND RecieverClientID="+clientID+" AND FileName='"+fileName+"'");
            ps1.executeUpdate();
            
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
           
        }
	}

	public int getBackupActionCount(int clientID) {
		// TODO Auto-generated method stub
		try {
            PreparedStatement ps = this.connection.prepareStatement("SELECT count(*) as count FROM REQUIRED_FILE_DISTRIBUTION  WHERE SenderClientID="+clientID+
            		" OR RecieverClientID="+clientID);
            ResultSet rs=ps.executeQuery();
            
            rs.next();
            return rs.getInt("count");
            
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
           
        }
		return 0;
	}

	public int getBackupClientID(String fileName) {
		// TODO Auto-generated method stub
		try {
            PreparedStatement ps = this.connection.prepareStatement("Select BackupClientID as bid FROM CURRENT_FILE_DISTRIBUTION WHERE FileName='"+fileName+"'");
            ResultSet rs=ps.executeQuery();
            
            rs.next();
            return rs.getInt("bid");
            
        } catch (SQLException ex) {
            Logger.getLogger(SqlOp.class.getName()).log(Level.SEVERE, null, ex);
           
        }
		return -1;
	}


	
}

/*

create table FILE_DETAILS(ClientID int not null , FileName varchar(50) not null , FilePath varchar(100) not null, FileAbsolutePath varchar(150) not null);
alter table FILE_DETAILS add constraint unique_file unique(FileName,FilePath,FileAbsolutePath);

create table REQUIRED_FILE_DISTRIBUTION(SenderClientID int not null ,RecieverClientID int not null , FileName varchar(50) not null );
alter table REQUIRED_FILE_DISTRIBUTION add constraint unique_file unique(FileName);

create table CURRENT_FILE_DISTRIBUTION(SenderClientID int not null ,BackupClientID int not null , FileName varchar(50) not null );
alter table CURRENT_FILE_DISTRIBUTION add constraint unique_file unique(FileName);



create table Client_Details(ClientID int not null , ClientIP varchar(20) not null , ClientPort int not null);
alter table Client_Details add constraint ID_Key primary key(ClientID);

*/