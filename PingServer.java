/*
 * IT383 programming assignment 1 part 2
 * September 12, 2017
 * author: Abe Ramseyer
 */
import java.util.Random;
import java.io.*;
import java.net.*;
import java.lang.NumberFormatException;

/**
 * creates a simple ping server that echos back whatever is sent to it
 */
class PingServer {
	private static final double LOSS_RATE = 0.25; // the rate at which packets are dropped from the server
	private static final int AVERAGE_DELAY = 150; // delay number from which our random delays will deviate
	private static Random gen; // Random object for creating delays
	private static String clientMessage = ""; // the message received from the client
	private static String action = ""; // "delayed" or "not sent", depending on the server's choice
	private static int delay = -1; // length in milliseconds the packet response was delayed
	private static String protocol; // "TCP" or "UDP", specified as argument
	private static int port = 0; // port to run on, specified as argument
	private static long seed = -1; // seed for the Random object, specified as optional argument
		
	public static void main(String args[]) {
		// check for correct number of arguments
		if (args.length < 2 || args.length > 3) {
			System.out.println("Usage: java PingServer port protocol [seed]\n\tprotocol - {TCP, UDP}");
			System.exit(1);
		}

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

		protocol = args[1];
	
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
		// create random gen with seed or nothing if not given
		gen  = (seed == -1) ? new Random(System.currentTimeMillis()) : new Random(seed);
		
		try { 
			if (protocol.equals("TCP")) {
				serveTCP();
			}
			else {
				serveUDP();
			} 
		} catch (Exception e) {
			System.err.println("Exception while pinging server");
			System.exit(1);
		}
	}

	/**
 	* creates the TCP server
 	*/
	static void serveTCP() throws Exception {
		ServerSocket welcomeSocket = new ServerSocket(port);
		System.out.println("Listening for connections...");
		while(true) {
			delay = (int) (gen.nextDouble() * 2 * AVERAGE_DELAY);
			Socket connectionSocket = welcomeSocket.accept();
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			clientMessage = inFromClient.readLine();
			
			Thread.sleep(delay); // simulate delay
			
			outToClient.writeBytes(clientMessage); // echo back same message that was received
			String address = connectionSocket.getRemoteSocketAddress().toString().substring(1);
			System.out.println(address + "> " + clientMessage + "  ACTION: " + action + " " + delay + " ms");	
			connectionSocket.close();	
		}	
	}

	/**
 	* creates the UDP server
 	*/
	static void serveUDP() throws Exception {
		DatagramSocket serverSocket = new DatagramSocket(port);
	
		// create byte arrays to set and receive data
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
				delay = (int) (gen.nextDouble() * 2 * AVERAGE_DELAY); // calculate delay
				InetAddress IPAddress = receivePacket.getAddress();
				int returnPort = receivePacket.getPort();
				sendData = clientMessage.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, returnPort);

				Thread.sleep(delay); // simulate delay 

				serverSocket.send(sendPacket); 
				System.out.println(address + "> " + clientMessage + "  ACTION: " + action + " " + delay + " ms");
			} 
		} 
	}
}
