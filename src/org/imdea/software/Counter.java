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
import java.lang.Object;
import org.imdea.software.Request;


public class Counter implements Runnable {
    
    WoCoServer server;
    private HashMap<Integer, HashMap<String, Integer>> results;
    public HashMap<Integer,Request> requestMap;
    private int index;
    public Object lock;


    public Counter (WoCoServer server, HashMap<Integer,Request> requestMap, int index, Object lock) {
        this.server = server;
        //this.requestQueue = requestQueue;
        this.results = new HashMap<>();
        this.requestMap = requestMap;
        this.index = index;
        this.lock = lock;
    }

    public void run () {

        while (true) {
            try {
                synchronized(lock) {
                    while (requestMap.get(index) == null) {
                        lock.wait();
                    }
                    processRequest();            
                    lock.notify();
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void processRequest() {
        
        Request request = requestMap.get(index);
        
        if (request != null) {

            int clientId = request.clientId;
            String line = request.line;
            SocketChannel client = request.clientChannel;

            if (!results.containsKey(clientId)) {
                results.put(clientId, new HashMap<String, Integer>());
            }
            HashMap<String, Integer> wc = results.get(clientId);

            // statistics - start
            long startCountingTime = System.nanoTime();

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

            // statistics - end
            if (!server.countingTimes.containsKey(clientId)) {
                server.countingTimes.put(clientId, new ArrayList<Float>());
            }
            ArrayList<Float> singleClientStatistic = server.countingTimes.get(clientId);
            singleClientStatistic.add( (float) ((System.nanoTime() - startCountingTime) / server.MLN));

            ByteBuffer ba;
            ba = ByteBuffer.wrap(serializeResultForClient(clientId).getBytes());
            try {
                client.write(ba);
            } catch (IOException e ) {
                e.printStackTrace();
            }
            requestMap.remove(index);
        } else {
            System.out.println(" richiesta nulla !!");
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
			
			// statistics - start
			long startSerializingTime = System.nanoTime();

			StringBuilder sb = new StringBuilder();
			HashMap<String, Integer> hm = results.get(clientId);
			for (String key : hm.keySet()) {
				sb.append(key+",");
				sb.append(hm.get(key)+",");
			}
			results.remove(clientId);
			sb.append("\n");

			// statistics - end
			if (!server.serializingTimes.containsKey(clientId)) {
				server.serializingTimes.put(clientId, new ArrayList<Float>());
			}
			ArrayList<Float> singleClientStatistic = server.serializingTimes.get(clientId);
			singleClientStatistic.add( (float) ((System.nanoTime() - startSerializingTime) / server.MLN));

			return sb.substring(0);
		} else {
			return "";
		}
	}


}