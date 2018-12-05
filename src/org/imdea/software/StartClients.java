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

public class StartClients {

	private static int NCLIENT = 4;

    /**
	 * Function to generate a document based on the hardcoded example file. 
	 * @param length Length of the document in bytes.
	 * @param seed This random seed is used to start reading from different offsets
	 * in the file every time a new document is generated. Could be useful for debugging
	 * to return to a problematic seed.
	 * @return Returns the document which is encoded as a String 
	 * @throws IOException
	 */
	private static String generateDocument(int length, int seed) throws IOException {
		
        String fileName = "input.html";
        String line = null;
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(fileName));

        while((line = br.readLine()) != null) {
            sb.append(line.trim()+" ");
        }   

        br.close();
                
        String ref = sb.toString();
		
		sb = new StringBuilder(length);
		int i;
		
		for (i=0; i<length; i++) {
			sb.append(ref.charAt((i+seed)%ref.length()));								
		}
		
		//we need to remove all occurences of this special character! 
		return sb.substring(0).replace(WoCoServer.SEPARATOR, '.');
		
    }
    

    public static void main (String[] args) throws UnknownHostException, IOException, InterruptedException {
        //reading in parameters
        if (args.length<4) {
            System.out.println("Usage: <servername> <serverport> <documentsize(KiB)> <opcount(x1000)> [<seed>]");
            System.exit(0);
        }
        
        String sName = args[0];
        int sPort = Integer.parseInt(args[1]);
        float dSize = Float.parseFloat(args[2])*1024;
        int ops = Integer.parseInt(args[3])*1000;
        int seed = (args.length==5) ? Integer.parseInt(args[4]) : (int) (Math.random()*10000);

        //We generate one document for the entire runtime of this client
        //Otherwise the client would spend too much time generating new inputs.
        String docu = generateDocument((int) (dSize), seed);

        for( int i=0; i<NCLIENT; i++) {
            WoCoClient client = new WoCoClient(sName, sPort, docu, ops);
            Thread clientThread = new Thread(client);
            clientThread.start();
        }
    }
}