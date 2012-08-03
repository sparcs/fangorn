/*******************************************************************************
 * Copyright 2011 The fangorn project
 * 
 *        Author: Sumukh Ghodke
 * 
 *        Licensed to the Apache Software Foundation (ASF) under one
 *        or more contributor license agreements.  See the NOTICE file
 *        distributed with this work for additional information
 *        regarding copyright ownership.  The ASF licenses this file
 *        to you under the Apache License, Version 2.0 (the
 *        "License"); you may not use this file except in compliance
 *        with the License.  You may obtain a copy of the License at
 * 
 *          http://www.apache.org/licenses/LICENSE-2.0
 * 
 *        Unless required by applicable law or agreed to in writing,
 *        software distributed under the License is distributed on an
 *        "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *        KIND, either express or implied.  See the License for the
 *        specific language governing permissions and limitations
 *        under the License.
 ******************************************************************************/
package au.edu.unimelb.csse;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.thread.QueuedThreadPool;

public class StartServer {
    public static void main(String[] args) throws Exception {
		int port = 8080;
		if (args.length != 1) {
			printMessage();
		} else {
			int argPort = port;
			try {
				argPort = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				printMessage();
			}
			if (argPort < 0) {
				System.err
						.println("Port number cannot be negative. Reverting to default port 8080.");
				argPort = port;
			}
			port = argPort;
		}
		Server server = new Server(port);
    	
    	DB db = new DB();
    	
    	db.loadDriver();
    	if (db.isIndexEmpty()) {
    		System.out.println("No indexes found. Please create an index before starting the application.");
    		db.shutdown();
    		System.exit(0);
    	}
    	db.loadCorporaInformation();
    	db.shutdown();
    	
        WebAppContext context = new WebAppContext();
        context.setDescriptor("./server/WEB-INF/web.xml");
        context.setResourceBase("./server");
        context.setContextPath("/");
        context.setParentLoaderPriority(true);

        server.setHandler(context);
        server.setThreadPool(new QueuedThreadPool(500));

        server.start();
        server.join();
    }
    
	private static void printMessage() {
		System.out
				.println("Expected port number as argument. Reverting to default port 8080.");
	}
}
