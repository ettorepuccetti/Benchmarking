package org.imdea.software;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Collections;

import org.imdea.software.Request;
import org.imdea.software.Dispatcher;

public class WoCoServer {
	
	public static final char SEPARATOR = '$';
	private static boolean CLEAN;
	private static int NTHREAD;
	private static int NCLIENT;

	public final double MLN = 1000000.0;
	
	private static int deletedKeys = 0;

	private HashMap<Integer, StringBuilder> buffer;

	public HashMap<Integer, ArrayList<Float>> readingTimes;
	public HashMap<Integer, ArrayList<Float>> cleaningTimes;
	public HashMap<Integer, ArrayList<Float>> countingTimes;
	public HashMap<Integer, ArrayList<Float>> serializingTimes;

	private FileWriter fileWriterReading;
	private FileWriter fileWriterCleaning;
	private FileWriter fileWriterCounting;
	private FileWriter fileWriterSerializing;

	private Dispatcher dispatcher;
	public LinkedList<Request> requestQueue;


	public void initStat () {
		readingTimes = new HashMap<>();
		cleaningTimes = new HashMap<>();
		countingTimes = new HashMap<>();
		serializingTimes = new HashMap<>();
	}

	public void printSingleStats (HashMap<Integer, ArrayList<Float>> stat, FileWriter fileWriter) {
		
		ArrayList<Float> respTime = new ArrayList<>();
		for (int clientId : stat.keySet()) {
			for (float value : stat.get(clientId))
				respTime.add(value);
		}
		Collections.sort(respTime);

		System.out.print("\n");
		System.out.println("total records: " + respTime.size());
		float sum = 0;
		for (float elem : respTime) {
			sum += elem;
		}
		float avg = sum/respTime.size();
		try {
			System.out.println("Average [ms]: " + avg + "\npercentiles [ms]: ");
			fileWriter.write("average," +avg+"\n");
			for (int p=1; p<=100; p++) {
				System.out.print(p+","+respTime.get(respTime.size()*p/100-1));
				fileWriter.write(p+","+respTime.get(respTime.size()*p/100-1));
				if (p!=100) {
					System.out.print("\n");
					fileWriter.write("\n");
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println();
	}

	public void printStats () {
		try {
			File fileReading = new File ("/Users/ettorepuccetti/log/readingtime.csv");
			if (!fileReading.exists()) {
				fileReading.createNewFile();
			}
			System.out.println("-----");
			System.out.print("Reading time");
			fileWriterReading = new FileWriter(fileReading.getAbsoluteFile(), false);
			printSingleStats(readingTimes, fileWriterReading);
			fileWriterReading.close();

			File fileCleaning = new File ("/Users/ettorepuccetti/log/cleaningtime.csv");
			if (!fileCleaning.exists()) {
				fileCleaning.createNewFile();
			}
			System.out.println("\n-----");
			System.out.print("Cleaning time");
			fileWriterCleaning = new FileWriter(fileCleaning.getAbsoluteFile(), false);
			printSingleStats(cleaningTimes, fileWriterCleaning);
			fileWriterCleaning.close();

			File fileCounting = new File ("/Users/ettorepuccetti/log/countingtime.csv");
			if (!fileCounting.exists()) {
				fileCounting.createNewFile();
			}
			System.out.println("\n-----");
			System.out.print("Counting time");
			fileWriterCounting = new FileWriter(fileCounting.getAbsoluteFile(), false);
			printSingleStats(countingTimes, fileWriterCounting);
			fileWriterCounting.close();

			File fileSerializing = new File ("/Users/ettorepuccetti/log/serializingtime.csv");
			if (!fileSerializing.exists()) {
				fileSerializing.createNewFile();
			}
			System.out.println("\n-----");
			System.out.print("Serializing time");
			fileWriterSerializing = new FileWriter(fileSerializing.getAbsoluteFile(), false);
			printSingleStats(serializingTimes, fileWriterSerializing);
			fileWriterSerializing.close();

		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Reads an (HTML) document as a String, it extract the plain text and return it as a String,
	 * removing any content between the angular parenteses.
	 * It does not make any assumption about starting from inside a tag block or not.
	 * @param line The document encoded as a string.
	 * @return the line cleaned from the HTML tags.
	 */
	public String deleteTag (String line, int clientId) {
		
		// statistics - start
		long startCleaningTime = System.nanoTime();

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
		// statistics - end
		if (!cleaningTimes.containsKey(clientId)) {
			cleaningTimes.put(clientId, new ArrayList<Float>());
		}
		ArrayList<Float> singleClientStatistic = cleaningTimes.get(clientId);
		singleClientStatistic.add( (float) ((System.nanoTime() - startCleaningTime) / MLN));

		return result.toString();
	}
	


	/**
	 * Constructor of the server.
	 */
	public WoCoServer() {
		buffer = new HashMap<Integer, StringBuilder>();
		requestQueue = new LinkedList<>();
		this.dispatcher = new Dispatcher(NTHREAD, this);
		Thread dispatcherThread = new Thread(this.dispatcher);
		dispatcherThread.setName("dispatcher worker thread");
		dispatcherThread.start();
		this.initStat();
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
	public boolean receiveData(int clientId, String dataChunk, long startReadingTime, SocketChannel client) {
		
		StringBuilder sb;
		
		if (!buffer.containsKey(clientId)) {
			sb = new StringBuilder();
			buffer.put(clientId, sb);
		} else {
			sb = buffer.get(clientId);
		}
		
		sb.append(dataChunk);
				
		if (dataChunk.indexOf(WoCoServer.SEPARATOR)>-1) {
			//we have at least one line
			
			// statistics - end
			if (!readingTimes.containsKey(clientId)) {
				readingTimes.put(clientId, new ArrayList<Float>());
			}
			ArrayList<Float> singleClientStatistic = readingTimes.get(clientId);
			singleClientStatistic.add( (float) ((System.nanoTime() - startReadingTime) / MLN));

			String bufData = sb.toString();
			
			int indexNL = bufData.indexOf(WoCoServer.SEPARATOR);
			
			String line = bufData.substring(0, indexNL);
			String rest = (bufData.length()>indexNL+1) ? bufData.substring(indexNL+1) : null;
			
			if (indexNL==0) {
				System.out.println("SEP@"+indexNL+" bufdata:\n"+bufData);
			}
			// TODO: ask what is he doing here... 
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
			String cleanLine = CLEAN ? deleteTag(line, clientId) : line;
			requestQueue.add(new Request(cleanLine, client, clientId));
			try {
				synchronized (requestQueue) {
					requestQueue.notify();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
			
		} else {
			return false;
		}
		
	}


	public static void main(String[] args) throws IOException {
		
		if (args.length!=4 && args.length !=5) {
			System.out.println("Usage: <listenaddress> <listenport> <cleaning> <threadcount> [<nclients> default:1]");
			System.exit(0);
		}
		
		String lAddr = args[0];
		int lPort = Integer.parseInt(args[1]);
		boolean cMode = Boolean.parseBoolean(args[2]);
		int threadCount = Integer.parseInt(args[3]);
		
		NCLIENT = (args.length == 5) ? Integer.parseInt(args[4]) : 1;
		CLEAN = cMode;
		NTHREAD = threadCount;

				
		WoCoServer server = new WoCoServer();

		Selector selector = Selector.open(); 
 		
		ServerSocketChannel serverSocket = ServerSocketChannel.open();
		InetSocketAddress myAddr = new InetSocketAddress(lAddr, lPort);
 
		serverSocket.bind(myAddr);
 
		serverSocket.configureBlocking(false);
 
		int ops = serverSocket.validOps();
		serverSocket.register(selector, ops, null);
 
		// Infinite loop..
		// Keep server running
		ByteBuffer bb = ByteBuffer.allocate(1024*1024);
		
		while (true) {
			
			if (deletedKeys == NCLIENT) {
				server.printStats();
			}

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

				} else if (key.isReadable()) {
					SocketChannel client = (SocketChannel) key.channel();
					int clientId = client.hashCode();
					
					bb.rewind();

					// statistics - start 	(it ends in receiveData() method.)
					long startReadingTime = System.nanoTime();

		            int readCnt = client.read(bb);
					
		            if (readCnt>0) {
						String result = new String(bb.array(),0, readCnt);       						
						boolean hasResult = server.receiveData(clientId, result, startReadingTime, client);
						
						//TODO: la risposta la deve rimandare il thread worker!!
						// 		quindi per ogni worker avrò un buffer, una hashmap per i risultati,
						//		un array per i tempi, ecc.
						//		quando il server riceve chiusure dai client, allora rimette insieme i tempi..
						//		ancora non ho capito come chiudere il server...
		            } else {
						key.cancel();
						deletedKeys++;
		            }	
				}
				iterator.remove();
			}
			
		}
	}

}
