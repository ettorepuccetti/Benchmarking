package org.imdea.software;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.plaf.SliderUI;

public class SingleClient implements Runnable{
	
	private Socket sHandle;
	private BufferedReader sInput;
	private BufferedWriter sOutput;
	private ArrayList<Float> respTime;
	private int cntSincePrint;
	private long timeLastPrint;
	private long timeCreate;
	private String docu;
	private int ops;
	private static boolean DEBUG = true;	
	
	/**
	 * Instantiates the client.
	 * @param serverAddress IP address or hostname of the WoCoServer.
	 * @param serverPort Port number of the server.
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public SingleClient (String serverAddress, int serverPort, String docu, int ops) throws UnknownHostException, IOException {
        this.sHandle = new Socket(serverAddress, serverPort);
        this.sInput = new BufferedReader(new InputStreamReader(sHandle.getInputStream()));
        this.sOutput = new BufferedWriter(new OutputStreamWriter(sHandle.getOutputStream()));
		this.docu = docu;
		this.ops = ops;
		this.initStats();

	}

	/**
	 * Initializes the data structure that holds statistical information on requests. 
	 */
	private void initStats(){
		respTime = new ArrayList<Float>();
        cntSincePrint = 0;
        timeLastPrint = System.nanoTime();
        timeCreate = timeLastPrint;
	}
	
	/**
	 * Sends a document to the server and waits for a response. The response is an
	 * ASCII serialized version of the <word, count> map.
	 * @param doc 
	 * @return
	 * @throws IOException
	 */
	private String sendToServer(String doc) throws IOException {
		long startTime = System.nanoTime();    
		sOutput.write(doc);
		sOutput.write(WoCoServer.SEPARATOR);
		sOutput.flush();
	
		
		String response = null;
		response = sInput.readLine();

		long endTime = System.nanoTime();
		respTime.add((float) ((endTime-startTime)/1000000.0));
		cntSincePrint ++;
		return response;
	}
	
	/**
	 * Sends a document to the server and returns the map of <word,count> pairs.
	 * If DEBUG is set to false, it returns null!
	 * @param doc
	 * @return Empty hashmap if DEBUG is false, a proper one otherwise.
	 * @throws IOException
	 */
	public HashMap<String,Integer> getWordCount(String doc) throws IOException {
		
		String response = sendToServer(doc);

		// Parsing this text into a data structure takes time, we only do it 
		// if we are in debug mode. Otherwise we'll assume that everything went
		// alright and we'd have a correct answer.
		if (DEBUG==true) {			
			HashMap<String, Integer> wordMap = new HashMap<String,Integer>();			
			String[] rParts = response.split(",");
			for (int i=0; i<rParts.length; i+=2) {
				wordMap.put(rParts[i], new Integer(rParts[i+1]));
			}
			return wordMap;
		} else {
			return new HashMap<String,Integer>();
		}				
	}
	
	
	/**
	 * Prints out statistical information since the last printStats invocation. 
	 * If called multiple times in a quick succession, if will only print out values
	 * if at least a second has passed since the last call.
	 * @param withPercentiles if true, not only averages of response time are printed
	 * but also the percentiles.
	 */
	public void printStats(boolean withPercentiles) {
		long currTime = System.nanoTime();
		
		float elapsedSeconds = (float) ((currTime-timeLastPrint)/1000000000.0);
		
		if (elapsedSeconds<1 && withPercentiles==false) {
			return;
		}
		
		float tput = cntSincePrint/elapsedSeconds;
		System.out.println("Interval time [s], Throughput [ops/s]: "+elapsedSeconds + ", "+ tput);
		timeLastPrint = currTime;
		cntSincePrint = 0;
		
		
		if (withPercentiles) {
			//sorting for pctiles
			Collections.sort(respTime);
			
			System.out.println("-----");
			
			elapsedSeconds = (float) ((currTime-timeCreate)/1000000000.0);
			tput = respTime.size()/elapsedSeconds;
			System.out.println("Total time [s], Throughput [ops/s]: "+elapsedSeconds + ", "+ tput);
			
			System.out.print("Response time percentiles [ms]: ");
			System.out.print("\n");
			for (int p=1; p<=100; p++) {
				System.out.print(p+","+respTime.get(respTime.size()*p/100-1));
				if (p!=100) {
					System.out.print("\n");
				}
			}
			System.out.println();
		}
		
	}
	
	/**
	 * Closes the connection to the server gracefully.
	 */
	public void shutDown() {
		try {
			this.sHandle.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

	public void run () {
		try {    	
			//send requests to the server in a loop.
			HashMap<String, Integer> result = new HashMap<>();  	
			for (int rep=0; rep<ops; rep++) {
				result = this.getWordCount(docu);
				
				if (DEBUG==true) {
					//System.out.println(result);
				}
				
				if (rep%25 == 0) {
					//reduce the overhead of printing statistics by calling this less often
					this.printStats(false);
				}
			}
			System.out.println(result);
			//final printout with percentiles
			this.printStats(true);
			this.shutDown();
			//System.exit(0);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}