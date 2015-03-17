import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class DSNResolver {
	
	private final static int serverPort = 9876;
	private final static int rootNameServerPort = 4569;
	
	private static Map<String,String> cacheMap = new HashMap<String, String>();
	
	public static void loadCache(){
		System.out.println("Loading Cache....");
		try {
			Scanner in = new Scanner(new FileReader("cache.txt"));			
			while (in.hasNext()){
				String hostname = in.next();
				String ip_address = in.next();				
				cacheMap.put(hostname, ip_address);				
			}			
			in.close();
			System.out.println("Loading cache completed!");
		}
		catch (IOException e){
			System.out.println(e.getMessage());
		}
	}
	
	public static void writeToCache(){
		
		try {			            
				
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("cache.txt")));
			
			for (Map.Entry<String, String> entry : cacheMap.entrySet()) {
			    String hostname = entry.getKey();
			    String ipAddress = entry.getValue();
			    out.println(hostname+" "+ipAddress);
			}           			  
			out.close();
		}
		catch (IOException e){
			System.out.println(e.getMessage());
		}
	}
	
	
	public static boolean findHost(String hostname){
		if(cacheMap.get(hostname) != null){
			return true;
		}
		return false;
	}
	
	public static String getDomain(String host){
		if(host.endsWith(".com")){
			return ".com";
		}else if(host.endsWith(".tt")){
			return ".tt";
		}
		return "Host Not found";
	}
	
	public static String callServer(String host, int ipAddress) throws Exception {
		
		byte[] sendData = (host+","+ipAddress).getBytes();
		byte[] receiveData = new byte[1024];
		String modifiedSentence;
		InetAddress IPAddress;
		DatagramSocket clientSocket = new DatagramSocket();		
		IPAddress = InetAddress.getByName("localhost");
		
		// send packet 
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, ipAddress);
		clientSocket.send(sendPacket);
		System.out.println("Data sent to "+ipAddress);
		
		// received packet
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		System.out.println("Data received from "+ipAddress);
		clientSocket.receive(receivePacket);
		
		modifiedSentence = new String(receivePacket.getData());

		System.out.println("FROM SERVER:" + modifiedSentence);
		clientSocket.close();
		System.out.println("Connection closed");
		
		return modifiedSentence;
	}
	
	public static void main(String args[]) throws Exception
    {
		
		
		DatagramSocket serverSocket = new DatagramSocket(serverPort);

		
		System.out.println("DNS Server started...");
		loadCache();
		System.out.println("\n");
		String host;
		while(true)
        {
			
			byte[] receiveData = new byte[1024];
			byte[] sendData  = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			serverSocket.receive(receivePacket);
			System.out.println("Request received from client");
			InetAddress IPAddress = receivePacket.getAddress();
			int port = receivePacket.getPort();
						
			host = new String(receivePacket.getData()).trim().toLowerCase();			
			System.out.println("Clent sent "+host);
			System.out.println("Searching cache...");
			
			if(findHost(host)){
				System.out.println("Host found in cache");
				String response ="Local DNS:"+host+" : "+cacheMap.get(host); //sentence.toUpperCase();

	            sendData = response.getBytes();

	            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);

	            serverSocket.send(sendPacket);
			}else{
				System.out.println("Host not in cache, calling root name server...");				

				String res[] = callServer(host,rootNameServerPort).trim().toLowerCase().split(",");
				String hostname = res[0];
				String ip_address = res[1];				
				cacheMap.put(hostname, ip_address);
				writeToCache();
				String response = "Local DNS:"+hostname+" : "+ip_address; //sentence.toUpperCase();
				System.out.println(response);
				sendData = response.getBytes();

	            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);

	            serverSocket.send(sendPacket);
	        	
			}	
			System.out.println("\n\n");
        }
    }
	
	
}
