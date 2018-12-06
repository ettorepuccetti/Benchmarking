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

	// I need a map instead of array for avoiding auto-resize and indexOutOfBound exception when I set request to null.
	HashMap<Integer,Request> requestMap;
	
	// I also need a single lock-object for every thread, for avoiding blocking all the process for a single thread.
	ArrayList<Object> lockArray;

    public Dispatcher (int nWorker, WoCoServer server) {
		this.nWorker = nWorker;
		this.server = server;
		this.requestQueue = server.requestQueue;
		this.threadArray = new ArrayList<>(nWorker);
		this.requestMap = new HashMap<>(nWorker);
		this.lockArray = new ArrayList<>(nWorker);
		for (int i=0; i<nWorker; i++) {
			Object lock = new Object();
			Counter counter = new Counter(this.server, requestMap, i, lock);
			Thread thread = new Thread(counter);
			thread.start();
			threadArray.add(i,thread);
			requestMap.put(i, null);
			lockArray.add(i, lock);
		}
    }

	public void processRequests() throws InterruptedException {
		while (!requestQueue.isEmpty()) {
			Request request = requestQueue.remove();
			int clientId = request.clientId;
			int indexThread = clientId%nWorker;
			Object lock = lockArray.get(indexThread);
			
			try {
				synchronized (lock) {
					// until thread has not processed his request, I have to wait, then I will receive a signal from the Thread 
					while (requestMap.get(indexThread) != null) {
						lock.wait();
					}
					// If I got out of the loop , it means that the thread had sent me a signal, or he wasn't busy
					// He is WAITING for new request. I can push him a new request.
					requestMap.put(indexThread, request);
					
					// Since he is waiting, I have also to wake him up!
					lock.notify();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void run () {
		while (true) {
			try {
				synchronized (requestQueue) {
					while (requestQueue.isEmpty()) {
						requestQueue.wait();
					}
					// I have been notified by the server's method, something in the queue has been added.
					// let's process them
					processRequests();
				}	
			} catch (InterruptedException e ){
				e.printStackTrace();
			}
		}
	}
}