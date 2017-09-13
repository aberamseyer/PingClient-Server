import java.util.Random;
import java.io.*;
import java.net.*;
import java.lang.NumberFormatException;

class PingServer {
	private static final double LOSS_RATE = 0.25;
	private static final int AVERAGE_DELAY = 150;
	public static void main(String args[]) throws Exception {
		// check for correct number of arguments
		if (args.length < 2 || args.length > 3) {
			System.out.println("Usage: java PingServer port protocol [seed]\n\tprotocol - {TCP, UDP}");
			System.exit(1);
		}

		String protocol = args[1];
		int port = 0;
		long seed = -1;

		// check arguments for errors
		try {
			port = Integer.parseInt(args[0]);	
			if (port > 65535) {
				System.err.println("ERR - arg 1");
				System.exit(1);
			}
		} catch (NumberFormatException e) {
			System.err.println("ERR - arg 1");
			System.exit(1);
		}
		if (!protocol.equals("TCP") && !protocol.equals("UDP")) {
			System.err.println("ERR - arg 2");	
			System.exit(1);
		}
		if (args.length == 3) {
			try {
				seed = Long.parseLong(args[2]);
			} catch (NumberFormatException e) {
				System.err.println("ERR - arg 3");
				System.exit(1);
			}
		}

		Random gen = (seed == -1) ? new Random(System.currentTimeMillis()) : new Random(seed);
		String clientMessage = "";
		String action = "";
		int delay = -1;
		
		// TCP Connection
		if (protocol.equals("TCP")) {
			ServerSocket welcomeSocket = new ServerSocket(port);
			System.out.println("Listening for connections...");
			while(true) {
				delay = (int) (gen.nextDouble() * 2 * AVERAGE_DELAY);
				Socket connectionSocket = welcomeSocket.accept();
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				clientMessage = inFromClient.readLine();
				
				Thread.sleep(delay); // simulate delay
				
				outToClient.writeBytes(clientMessage);
				String address = connectionSocket.getRemoteSocketAddress().toString().substring(1);
				System.out.println(address + "> " + clientMessage + "  ACTION: " + action + " " + delay + " ms");	
				connectionSocket.close();	
			}	
		}

		// UDP Connectioon
		else {
			DatagramSocket serverSocket = new DatagramSocket(port);
			byte[] receiveData = new byte[1024];
			byte[] sendData = new byte[1024];

			System.out.println("Listening for connections...");
			while(true) {
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				clientMessage = new String(receivePacket.getData());
				String address = receivePacket.getSocketAddress().toString().substring(1);
				// Determine whether to send a reply or not
				if (gen.nextDouble() < LOSS_RATE) {
					action = "not sent";
					System.out.println(address + "> " + clientMessage + "  ACTION: " + action); 
				}
				else {
					action = "delayed";
					delay = (int) (gen.nextDouble() * 2 * AVERAGE_DELAY);
					InetAddress IPAddress = receivePacket.getAddress();
					int returnPort = receivePacket.getPort();
					sendData = clientMessage.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, returnPort);
					Thread.sleep(delay);
					serverSocket.send(sendPacket); 
					System.out.println(address + "> " + clientMessage + "  ACTION: " + action + " " + delay + " ms");
				} 
			
					
			} 
		} 
	}
}
