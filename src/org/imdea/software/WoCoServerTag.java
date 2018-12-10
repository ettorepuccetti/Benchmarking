package org.imdea.software;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class WoCoServerTag {
	
	public static final char SEPARATOR = '$';
	
	private HashMap<Integer, StringBuilder> buffer;
    private HashMap<Integer, HashMap<String, Integer>> results;
    private static boolean CLEAN;
    

    /**
	 * Reads an (HTML) document as a String, it extract the plain text and return it as a String,
	 * removing any content between the angular parenteses.
	 * It does not make any assumption about starting from inside a tag block or not.
	 * @param line The document encoded as a string.
	 * @return the line cleaned from the HTML tags.
	 */
	public String deleteTag (String line) {

		StringBuilder result = new StringBuilder();
		
		// since the input from clients start at random position in the document, we don't know
		// if we are starting reading from inside a tag or not, until the first angular parenteses.
		boolean tagOpened;
		int j = 0;
		char cstart = line.charAt(j);
		while (cstart != '<' && cstart != '>' && j<line.length()) {
			j++;
			cstart = line.charAt(j);
		}
		// if I've read a '>' it means a tag block is closing, so I'm starting reading from inside a tag block
		if (cstart == '>') tagOpened = true;
		else tagOpened = false;
		
		//starting again, now we know if we are in a tag block or not.
		for( int i=0; i<line.length(); i++) {
			char cc = line.charAt(i);
			if (cc == '<') {
				tagOpened = true;
				continue;
			}
			if (cc == '>') { 
				tagOpened = false;
				continue;
			}
			if (!tagOpened) {
				result.append(cc);
			}
		}
		return result.toString();
	}



	/**
	 * Performs the word count on a document. It first converts the document to 
	 * lower case characters and then extracts words by considering "a-z" english characters
	 * only (e.g., "alpha-beta" become "alphabeta"). The code breaks the text up into
	 * words based on spaces.
	 * @param line The document encoded as a string.
	 * @param wc A HashMap to store the results in.
	 */
	public static void doWordCount(String line, HashMap<String, Integer> wc) {
        String ucLine = line.toLowerCase();
		StringBuilder asciiLine = new StringBuilder();
		
		char lastAdded = ' ';
		for (int i=0; i<line.length(); i++) {
			char cc = ucLine.charAt(i);
			if ((cc>='a' && cc<='z') || (cc==' ' && lastAdded!=' ')) {
				asciiLine.append(cc);
				lastAdded = cc;
			}
		}
		
		String[] words = asciiLine.toString().split(" ");
		for (String s : words) {
			
			
			if (wc.containsKey(s)) {
				wc.put(s, wc.get(s)+1);
			} else {
				wc.put(s, 1);
			}
		}
	}
	
	/**
	 * Constructor of the server.
	 */
	public WoCoServerTag() {
		buffer = new HashMap<Integer, StringBuilder>();	
		results = new HashMap<Integer, HashMap<String, Integer>>();
	}
	
	/**
	 * This function handles data received from a specific client (TCP connection).
	 * Internally it will check if the buffer associated with the client has a full
	 * document in it (based on the SEPARATOR). If yes, it will process the document and
	 * return true, otherwise it will add the data to the buffer and return false
	 * @param clientId
	 * @param dataChunk
	 * @return A document has been processed or not.
	 */
	public boolean receiveData(int clientId, String dataChunk) {
		
		StringBuilder sb;
		
		if (!results.containsKey(clientId)) {
			results.put(clientId, new HashMap<String, Integer>());
		}
		
		if (!buffer.containsKey(clientId)) {
			sb = new StringBuilder();
			buffer.put(clientId, sb);
		} else {
			sb = buffer.get(clientId);
		}
		
		sb.append(dataChunk);
				
		if (dataChunk.indexOf(WoCoServerTag.SEPARATOR)>-1) {
			//we have at least one line
			
			String bufData = sb.toString();
			
			int indexNL = bufData.indexOf(WoCoServerTag.SEPARATOR);
			
			String line = bufData.substring(0, indexNL);
			String rest = (bufData.length()>indexNL+1) ? bufData.substring(indexNL+1) : null;
			
			if (indexNL==0) {
				System.out.println("SEP@"+indexNL+" bufdata:\n"+bufData);
			}
			
			if (rest != null) {
				System.out.println("more than one line: \n"+rest);
				try {
					System.in.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
				buffer.put(clientId, new StringBuilder(rest));
			} else {
				buffer.put(clientId, new StringBuilder());
			}
			
			
			//word count in line
            HashMap<String, Integer> wc = results.get(clientId);
            String cleanLine = CLEAN ? deleteTag(line) : line;
			doWordCount(cleanLine, wc);
			
			
			return true;
			
		} else {
			return false;
		}
		
	}
	
	/**
	 * Returns a serialized version of the word count associated with the last
	 * processed document for a given client. If not called before processing a new
	 * document, the result is overwritten by the new one.
	 * @param clientId
	 * @return
	 */
	public String serializeResultForClient(int clientId) {
		if (results.containsKey(clientId)) {
			StringBuilder sb = new StringBuilder();
			HashMap<String, Integer> hm = results.get(clientId);
			for (String key : hm.keySet()) {
				sb.append(key+",");
				sb.append(hm.get(key)+",");
			}
			results.remove(clientId);
			sb.append("\n");
			return sb.substring(0);
		} else {
			return "";
		}
	}
	

	public static void main(String[] args) throws IOException {
		
		if (args.length!=4) {
			System.out.println("Usage: <listenaddress> <listenport> <cleaning> <threadcount>");
			System.exit(0);
		}
		
		String lAddr = args[0];
		int lPort = Integer.parseInt(args[1]);
		boolean cMode = Boolean.parseBoolean(args[2]);
		int threadCount = Integer.parseInt(args[3]);
		
		CLEAN = cMode;
		
		if (threadCount>1) {
			//TODO: will have to implement multithreading
			System.out.println("FEATURE NOT IMPLEMENTED");
			System.exit(0);

		}
		
		
		WoCoServerTag server = new WoCoServerTag();
		
		Selector selector = Selector.open(); 
 		
		ServerSocketChannel serverSocket = ServerSocketChannel.open();
		InetSocketAddress myAddr = new InetSocketAddress(lAddr, lPort);
 
		serverSocket.bind(myAddr);
 
		serverSocket.configureBlocking(false);
 
		int ops = serverSocket.validOps();
		SelectionKey selectKey = serverSocket.register(selector, ops, null);
 
		// Infinite loop..
		// Keep server running
		ByteBuffer bb = ByteBuffer.allocate(1024*1024);
		ByteBuffer ba;
		int connectedClient = 0;
		int disconnectedClient = 0;
		
		while (true) {
 			
			selector.select();
 
			Set<SelectionKey> readyKeys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = readyKeys.iterator();
 
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
 
				if (key.isAcceptable()) {
					SocketChannel client = serverSocket.accept();
 
					client.configureBlocking(false);
 
					client.register(selector, SelectionKey.OP_READ);
					System.out.println("Connection Accepted: " + client.getLocalAddress() + "\n");
					connectedClient++;
 
				} else if (key.isReadable()) {										
					SocketChannel client = (SocketChannel) key.channel();
					int clientId = client.hashCode();
					
					bb.rewind();
		            int readCnt = client.read(bb);
		            
		            if (readCnt>0) {
		            	String result = new String(bb.array(),0, readCnt);		            
		            		         						
						boolean hasResult = server.receiveData(clientId, result);
						
						if (hasResult) {
							
							ba = ByteBuffer.wrap(server.serializeResultForClient(clientId).getBytes());
							client.write(ba);
						}
		            } else {
						key.cancel();
						disconnectedClient++;
						if (connectedClient == disconnectedClient) {
							System.exit(0);
						}
		            }
 
					
				}
				iterator.remove();
			}
		}
	}

}

