/*
 * Copyright (c) 2011-2020 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

import java.net.*;
import java.util.*;

/**
 * This is more or less the same code as DistributedObjectServer#start()
 * If this fails, there is no point in going any further.
 */
public class Test {
	public static void main(String args[]) throws Exception {
	   String host = args[0];

	    final InetAddress ip = InetAddress.getByName(host);
	    System.out.println("Local address is " + ip);
	    Enumeration<NetworkInterface> all = NetworkInterface.getNetworkInterfaces();
	    while(all.hasMoreElements()) {
 		System.out.println(all.nextElement());
	    }
	    System.out.println("Interface is " + NetworkInterface.getByInetAddress(ip));
	    if (!ip.isLoopbackAddress() && (NetworkInterface.getByInetAddress(ip) == null)) {
	      final String msg = "Unable to find local network interface for " + host;
	      System.out.println(msg);
	      System.exit(-1);
	    }

	}
}
