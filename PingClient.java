/*
 * IT383 programming assignment 1 part 2
 * September 12, 2017
 * author: Abe Ramseyer
 */
import java.net.*;
import java.io.*;
import java.lang.NumberFormatException;
import java.text.DecimalFormat;

/**
 * This class represents a client that can ping a server. It sends 10 packets and records the round trip time of each,
 * printing a summary at the end
 */
class PingClient {
	private static String[] pings = new String[10]; //holds the result of all the pings
	private static long endTime = -1; // the time a packet returns to the client
	private static long startTime = -1; // the time a packet is sent from the client
	private static int dropped = 0; // the number of dropped packets
	private static long[] delays = new long[10]; // the length of each delay for our packets, -1 indicates a dropped packet
	private static String host; // the host to connect to
	private static String protocol; // the protocol to use when pinging
	private static int port = 0; // the port to connect to on the server	
		
	public static void main(String args[]) throws Exception {
		
		// Check for correct number of arguments
		if(args.length != 3) {
			System.out.println("Usage: java PingClient hostname port protocol\n\tprotocol - {TCP, UDP}");
			System.exit(1);
		}
		
		// initialize from command line	
		host = args[0];
		protocol = args[2];

		// Check arguments for errors
		try {
			InetAddress.getByName(host);		
		} catch (UnknownHostException e) {
			System.err.println("ERR - arg 1");
			System.exit(1);
		}
		try {
			port = Integer.parseInt(args[1]);
			if (port > 65535) {
				System.err.println("ERR - arg 2");
				System.exit(1);
			}
		} catch (NumberFormatException e) {
			System.err.println("ERR - arg 2");
			System.exit(1);
		}
		if (!protocol.equals("UDP") && !protocol.equals("TCP")) {
			System.err.println("ERR - arg 3");
			System.exit(1);
		}
		
		try {
			if(protocol.equals("TCP")) {
				pingTCP();
			} 
			else {
				pingUDP();	
			}
		} catch (Exception e) {
			System.err.println("Exception while pinging server");
		}
		
		printResults();
	}
	
	/**
 	* pings the server over TCP protocol
 	*/
	static void pingTCP() throws Exception {
		for(int i = 0; i < pings.length; i++) {
			Socket clientSocket = new Socket(host, port);
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));	
			startTime = System.currentTimeMillis();
			pings[i] = "PING " + (i+1) + " " + startTime;
			outToServer.writeBytes(pings[i] + "\n");
			inFromServer.readLine();
			endTime = System.currentTimeMillis();
			delays[i] = endTime - startTime;
			pings[i] += "   RTT: " + delays[i] + " ms"; 
			System.out.println(pings[i]);		
		}	
	}

	/**
 	* pings the server over udp protocol
 	*/
	static void pingUDP() throws Exception {
		DatagramSocket clientSocket = new DatagramSocket();
		clientSocket.setSoTimeout(1000);
		InetAddress address = InetAddress.getByName(host);
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		
		for(int i = 0; i < pings.length; i++) {
			startTime = System.currentTimeMillis();
			pings[i] = "PING " + (i+1) + " " + startTime;
			sendData = pings[i].getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);	
			clientSocket.send(sendPacket);
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			// Using setSoTimeout() throws an exception upon the delay reached, so handle exception
			// accordingly
			try {
				clientSocket.receive(receivePacket);
			} catch (SocketTimeoutException e) { }
			endTime = System.currentTimeMillis();	
			
			// check for dropped packet
			if((endTime - startTime) >= 1000) {
				pings[i] += "   RTT: *";
				dropped++;
				delays[i] = -1;
			}	
			else {
				delays[i] = endTime - startTime;
				pings[i] += "   RTT: " + delays[i] + " ms";
			}
			System.out.println(pings[i]);
		}	
	}

	/**
 	* prints a summary of the results of the 10 pings
 	*/
	static void printResults() {
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		int sum = 0;
		double avg = 0;
		for(int i = 0; i < delays.length; i++) { // calculate min, max, and average of delays,
			if(delays[i] == -1) continue;    // not counting dropped packets
			min = (min < delays[i]) ? min : delays[i];
			max = (max > delays[i]) ? max : delays[i];
			sum += delays[i];
		} 
		avg = (double)sum / (10-dropped);

		DecimalFormat formatPercent = new DecimalFormat("##%");
		DecimalFormat formatDecimal = new DecimalFormat("###.#");
		System.out.println("---- PING STATISTICS ----");
		System.out.println("10 packets transmitted, " + (10-dropped) + " received, " + formatPercent.format(dropped/10.0) + " packet loss");
		System.out.println("round-trip (ms) min/avg/max = " + min + "/" + formatDecimal.format(avg) + "/" + max); 
	}	
}
