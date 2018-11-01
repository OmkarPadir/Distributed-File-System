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
public class FileObject {
    
    private final String FileName;
    private final String FilePath;
    
    private final String FileAbsolutePath;
    private int SourceClientID;
    private String FileBackupPath;
    
    public FileObject(String fileName , String filePath , String fileAbsolutePath)
    {
        this.FileName = fileName;
        this.FilePath = filePath;
        this.FileAbsolutePath = fileAbsolutePath;
    }
    
    public FileObject(String fileName, String filePath, String fileAbsolutePath, int sourceClientID)
    {
        this.FileName = fileName;
        this.FilePath = filePath;
        this.FileAbsolutePath = fileAbsolutePath;
        this.SourceClientID = sourceClientID;
    }
    
    public String getFileName(){
        return this.FileName;
    }
    
    public String getFilePath(){
        return this.FilePath;
    }
    
    public String getFileAbsolutePath(){
        return this.FileAbsolutePath;
    }
    
    public int getSourceClientID(){
        return this.SourceClientID;
    }
    
    public void setBackupPath(String s)
    {
    	FileBackupPath=s;
    }
    
    public String getBackupPath()
    {
    	return this.FileBackupPath;
    }
    
}
