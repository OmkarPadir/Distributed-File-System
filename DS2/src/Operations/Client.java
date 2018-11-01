/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Operations;

import java.net.Socket;

/**
 *
 * @author Srini
 */
public class Client {
    
    private final int ClientID;
    private final int AssignedPort;
    private final String IP;
    private final Socket socket;
    private Socket hiddenSocket;
    
    public Client(int clientID , String ip , int assignedPort , Socket s)
    {
        this.ClientID = clientID;
        this.IP = ip.substring(1);
        this.AssignedPort = assignedPort;
        this.socket = s;
    }
    
    public Client(int clientID , String ip , int assignedPort , Socket s, Socket hs)
    {
        this.ClientID = clientID;
        this.IP = ip;
        this.AssignedPort = assignedPort;
        this.socket = s;
        this.hiddenSocket = hs;
    }
    
    public void setHiddenSocket(Socket hiddenSocket)
    {
        this.hiddenSocket = hiddenSocket;
    }
    
    public int getClientID()
    {
        return this.ClientID;
    }
    
    public int getAssignedPort()
    {
        return this.AssignedPort;
    }
    
    public String getIP()
    {
        return this.IP;
    }
    
    public Socket getSocket()
    {
        return this.socket;
    }
    
    public Socket getHiddenSocket()
    {
        return this.hiddenSocket;
    }
    
}
