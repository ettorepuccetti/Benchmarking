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
import java.lang.Thread.State;
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
import java.util.Queue;
import java.util.Set;
import java.util.Collections;

import org.imdea.software.Request;

public class Dispatcher implements Runnable{

	public int nWorker;
	private WoCoServer server;
	LinkedList<Request> requestQueue;
	ArrayList<Thread> threadArray;
	HashMap<Integer,Request> requestMap;

    public Dispatcher (int nWorker, WoCoServer server) {
		this.nWorker = nWorker;
		this.server = server;
		this.requestQueue = server.requestQueue;
		this.threadArray = new ArrayList<>(nWorker);
		this.requestMap = new HashMap<>();
		for (int i=0; i<nWorker; i++) {
			Counter counter = new Counter(this.server, requestMap, i);
			Thread thread = new Thread(counter);
			thread.start();
			threadArray.add(thread);
			requestMap.put(i, null);
		}
    }

	public void processRequests() throws InterruptedException {
		while (!requestQueue.isEmpty()) {
			Request request = requestQueue.remove();
			int clientId = request.clientId;
			int indexThread = clientId%nWorker;
			//Thread thread = threadArray.get(clientId%nWorker);

			// until thread has not processed his request, I have to wait, then I will receive a signal from the Thread 
			try {
				synchronized (requestMap.get(indexThread)) {
					while (requestMap.get(indexThread) != null) {
						requestMap.wait();
					}
					// once I'm out of the while-wait loop, I can push him a new request.
					requestMap.put(indexThread, request);
					// requestMap had sent me a signal, means it is WAITING for new request. I have to wake him up !
					requestMap.notifyAll();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void run () {
		while (true) {
			try {
				while (requestQueue.isEmpty()) {
					synchronized (requestQueue) { 
						requestQueue.wait();
					}
				}
				// I have been notified by the method server, something in the queue has been added.
				processRequests();
			} catch (InterruptedException e ){
				e.printStackTrace();
			}
		}
	}
}