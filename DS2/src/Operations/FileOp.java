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
import java.io.*;
import java.net.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileOp {
    
    private OutputStream socket_outputStream = null;
    private InputStream socket_inputStream = null;
    
    private static final int FILE_SIZE = 6022386;
    
    public FileOp()
    {
        
    }
    
    public FileOp(OutputStream os)
    {
        this.socket_outputStream = os;
    }
    
    public FileOp(InputStream is)
    {
        this.socket_inputStream = is;
    }
    
    public boolean sendFile(File toSend) throws IOException
    {
        if(toSend == null)
            return false;
        if(toSend.exists()==false)
            return false;
        if(toSend.isDirectory())
            return false;
        
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        
        try
        {
            fis = new FileInputStream(toSend);
            bis = new BufferedInputStream(fis);
            
            DataOutputStream dos = new DataOutputStream(this.socket_outputStream);
            dos.writeInt((int)toSend.length());
            dos.flush();
            
            byte[] BUFFER = new byte[(int)toSend.length()];
            
            bis.read(BUFFER, 0, BUFFER.length);
            
            this.socket_outputStream.write(BUFFER, 0, BUFFER.length);
            this.socket_outputStream.flush();
        }finally{
            boolean rvalue = false;
            if(fis!=null){
                rvalue = true;
                fis.close();
            }
            if(bis!=null){
                rvalue = true;
                bis.close();
            }
            return rvalue;
        }
    }
    
    public boolean receiveFile(File toReceive) throws IOException
    {
        if(toReceive == null)
            return false;
        if(toReceive.isDirectory())
            return false;
        
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        
        int bytesCount = 0;
        int bytesRead;
        
        try
        {
            byte[] BUFFER = new byte[FILE_SIZE];
            
            fos = new FileOutputStream(toReceive);
            bos = new BufferedOutputStream(fos);
            
            DataInputStream din = new DataInputStream(this.socket_inputStream);
            int size = din.readInt();
            
            bytesRead = this.socket_inputStream.read(BUFFER, 0, BUFFER.length);
            bytesCount = bytesRead;
            //System.out.println("BUFFER: " + BUFFER.length + "  Count: " + bytesCount);
            while(bytesRead>-1)
            {
                if(bytesCount>=size)
                    break;
                //System.out.println("Before reading again " + bytesRead + " " + bytesCount);
                bytesRead = this.socket_inputStream.read(BUFFER , bytesCount , (BUFFER.length - bytesCount));
                //System.out.println("Before condition checking " + bytesRead + " " + bytesCount);
                if(bytesRead>=0)
                    bytesCount+=bytesRead;
                else
                    break;
                //System.out.println();
            }
            //System.out.println("BUFFER: " + BUFFER.length + "  Count: " + bytesCount);
            bos.write(BUFFER, 0, bytesCount);
            //System.out.println("BUFFER: " + BUFFER.length + "  Count: " + bytesCount);
            bos.flush();
        }
        finally{
            boolean rvalue = false;
            if(fos!=null){
                rvalue = true;
                fos.close();
            }
            if(bos!=null){
                rvalue = true;
                bos.close();
            }
            return rvalue;
        }
    }
    
    public boolean copyFile(String source , String receive) throws IOException
    {
        /*FileInputStream fis = null;
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        
        try
        {
            fis = new FileInputStream(source);
            bis = new BufferedInputStream(fis);
            
            byte[] BUFFER = new byte[(int)source.length()];
            
            bis.read(BUFFER, 0, BUFFER.length);
            
            fos = new FileOutputStream(receive);
            bos = new BufferedOutputStream(fos);
            
            bos.write(BUFFER, 0, BUFFER.length);
        }
        finally
        {
            if(fis!=null) fis.close();
            if(bis!=null) bis.close();
            if(fos!=null) fos.close();
            if(bos!=null) bos.close();
        }*/
        Path temp = null;
        try{
            temp = Files.copy(Paths.get(source), Paths.get(receive)); 
        }catch(FileAlreadyExistsException ex)
        {
            File file = new File(receive);
            file.delete();
            temp = Files.copy(Paths.get(source), Paths.get(receive)); 
        }
  
        if(temp != null) 
        { 
            return true;
        } 
        else
        { 
            return false;
        } 
    }
    
}
