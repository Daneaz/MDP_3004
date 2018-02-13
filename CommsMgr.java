package utilities;

import java.net.*;
import java.io.*;
import java.util.concurrent.TimeUnit;

public class CommsMgr {
	
	

	public static void main(String[] args) throws Exception {
            // Create TCP Socket
		try {
			
			String server_address = "192.168.13.1";
			int r_port = 5182;
			String msg = "HPF";
			Socket tcpClientSocket = new Socket(server_address, r_port);
			while (true) {
				
				TimeUnit.SECONDS.sleep(2);
				sendMessage(r_port, msg, server_address, tcpClientSocket);
				
				receiveMessage(r_port, server_address, tcpClientSocket);
			}
			
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public static void sendMessage(int r_port, String msg, String server_address, Socket tcpClientSocket) throws Exception {
		String message = msg;
		
		DataOutputStream outToServer = new DataOutputStream(tcpClientSocket.getOutputStream());
		outToServer.writeChars(message);
		System.out.print("Message sent: " + message + "\n");
		
	}
	
	public static void receiveMessage(int r_port, String server_address, Socket tcpClientSocket) throws Exception{
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(tcpClientSocket.getInputStream()));
		String msg = inFromServer.readLine();
		System.out.println("CLIENT_RCV_MSG = " + msg);
	}

}
