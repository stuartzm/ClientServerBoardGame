package Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;

public class ServerMain 
{
	
	// PORT 42069 WILL BE FOR THE HUB
	// All OTHER PORTS AER FREEGAME
	
    private static HashSet<String> usernames = new HashSet<String>();
    private static HashSet<ObjectOutputStream> outputStreams = new HashSet<ObjectOutputStream>();
    private static ArrayList<GameRoom> gameRoomsList = new ArrayList<GameRoom>();
    
	public static void main(String[] args) throws IOException
	{
		// SERVER START //
		Player.getAccounts();
		ServerMain server = new ServerMain();
		ServerSocket ss = new ServerSocket(42069);
		System.out.println("Server is running on Port: " + 42069);
		
		try
		{
			while(true)
			{
				server.new ServerConnection(ss.accept()).start();
			}
		}
		finally
		{
			ss.close();
		}
	}
	
	private class ServerConnection extends Thread
	{
		// Variables
		
		private Player account;
		private Socket socket;
		private ObjectInputStream ois;
		private ObjectOutputStream oos;
		
		// Constructor
		
		public ServerConnection(Socket socket)
		{
			this.socket = socket;
		}
		
		// Thread Runner
		
		public void run()
		{
			System.out.println("New Thread Started");
			try
			{
				oos = new ObjectOutputStream(socket.getOutputStream());
				ois = new ObjectInputStream(socket.getInputStream());
				
				// Get the account of this player by rerunning until a correct response is sent
				while (true)
				{
					boolean received = false;
					// Send a request to the client for a Name Object
					sendPacketToClient(new ServerObject("NAMEREQUEST", "Server", null));
					
					ServerObject packetIn = (ServerObject)ois.readObject();
					
					// If the response is to register
					if (packetIn.getHeader().equals("REGISTER"))
					{
						if (Player.checkForAccount((Player)packetIn.getPayload()) == true) // If the name is already registered
						{
							ServerObject outPacket = new ServerObject("INVALID", null, null);
							sendPacketToClient(outPacket);
							
							System.out.println("Account invalid");
						}
						else // if the name is not registered already
						{
							Player.putNewAccount((Player)packetIn.getPayload());
							Player.saveAccounts();
							
							ServerObject outPacket = new ServerObject("VALID", null, null);
							sendPacketToClient(outPacket);
							
							System.out.println("New user " + account.username + "has connected with " + account.wins + " wins and " + account.loses + "loses!");
							account = (Player)packetIn.getPayload();
							received = true;
						}
					}
					
					// If the response is to login
					else if (packetIn.getHeader().equals("LOGIN"))
					{
						
						if (Player.checkForAccount((Player)packetIn.getPayload()) == true && Player.checkPassword((Player)packetIn.getPayload()) == true) // if the name does exist and the password is right
						{
							ServerObject outPacket = new ServerObject("VALID", null, null);
							sendPacketToClient(outPacket);
							
							account = (Player)packetIn.getPayload();
							
							System.out.println("User " + account.username + "has connected with " + account.wins + " wins and " + account.loses + "loses!");
							received = true;
						}
						else // the name either doesn't exist or had the wrong password
						{
							ServerObject outPacket = new ServerObject("INVALID", null, null);
							sendPacketToClient(outPacket);
							
							System.out.println("Account invalid");
						}
					}
					
					// Since we are updating usernames across different threads, we need to use synchronized
					if (received == true)
					{		
						synchronized (usernames)
						{
							if (!usernames.contains(packetIn.getPayload().toString()))
							{
								usernames.add(packetIn.getPayload().toString());
								break;
							}
						}
					}
				}
				
				// Since the Name is Accepted, send an okay to the server.
				sendPacketToClient(new ServerObject("NAMEACCEPTED", "Server", null));
				outputStreams.add(oos);
				
				// Notify all users of someone joining the server.
				sendPacketToAllClients(new ServerObject("MESSAGE", "Server", "Challenger " + account.username + " has joined the server!"));
								
				//The while loop that takes all requests
				while(true)
				{
					ServerObject packetIn = (ServerObject)ois.readObject();
					
					// Chat Message to be sent to all other clients
					if (packetIn.getHeader().equals("MESSAGE"))
					{
						sendPacketToAllClients(new ServerObject("MESSAGE", packetIn.getSender(), packetIn.getPayload()));
					}
					
					// The Request to make a room
					else if(packetIn.getHeader().equals("MAKEROOM"))
					{
						
					}
					
					// The Request to join a room
					else if(packetIn.getHeader().equals("JOINROOM"))
					{
						
					}
				}	
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			} 
			catch (ClassNotFoundException e) 
			{
				e.printStackTrace();
			}
			finally
			{
				usernames.remove(account.username);
				outputStreams.remove(oos);
				
				try 
				{
					socket.close();
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
		// Send packet to the user
		public void sendPacketToClient(Object packet) throws IOException
		{
			oos.writeObject(packet);
		}
		
		// Send packet to all users
		public void sendPacketToAllClients(Object packet) throws IOException
		{
			for (ObjectOutputStream ooss : outputStreams)
			{
				ooss.writeObject(packet);
			}
		}
	}
}