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
import java.util.Set;
import java.util.Collections;

public class WorkerCount {

    public int nWorker;

    public WorkerCount (int nWorker) {
        this.nWorker = nWorker;
    }

    /**
	 * Reads an (HTML) document as a String, it extract the plain text and return it as a String,
	 * removing any content between the angular parenteses.
	 * It does not make any assumption about starting from inside a tag block or not.
	 * @param line The document encoded as a string.
	 * @return the line cleaned from the HTML tags.
	 */
	public static String deleteTag (String line, int clientId) {
		
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
	 * Performs the word count on a document. It first converts the document to 
	 * lower case characters and then extracts words by considering "a-z" english characters
	 * only (e.g., "alpha-beta" become "alphabeta"). The code breaks the text up into
	 * words based on spaces.
	 * @param line The document encoded as a string.
	 * @param wc A HashMap to store the results in.
	 */
	public static void doWordCount(String line, HashMap<String, Integer> wc, int clientId) {


		String cleanLine = CLEAN ? deleteTag(line, clientId) : line;

		//from the WordCountTime I have to exclude the time spent cleaning the document from tags.
		// statistics - start
		long startCountingTime = System.nanoTime();

		String ucLine = cleanLine.toLowerCase();
		StringBuilder asciiLine = new StringBuilder();
		
		char lastAdded = ' ';
		for (int i=0; i<cleanLine.length(); i++) {
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
		if (!countingTimes.containsKey(clientId)) {
			countingTimes.put(clientId, new ArrayList<Float>());
		}
		ArrayList<Float> singleClientStatistic = countingTimes.get(clientId);
		singleClientStatistic.add( (float) ((System.nanoTime() - startCountingTime) / MLN));
	}
}