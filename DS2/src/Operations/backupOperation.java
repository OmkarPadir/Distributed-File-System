package Operations;

public class backupOperation {
	
	public int SenderID;
	public int RecieverID;
	public String Filename;
	public int senderRecieverFlag; // 0 Sender, 1Reciever
	
	public backupOperation(int flag,int s, int r, String f)
	{
		senderRecieverFlag=flag;
		SenderID=s;
		RecieverID=r;
		Filename=f;
	}
	public backupOperation()
	{}
		

}
