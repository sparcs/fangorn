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

import java.io.File;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DeleteIndex {
	private String dirname;
	private static final Logger logger = Logger.getLogger(DeleteIndex.class
			.getName());

	public DeleteIndex(String dirname) {
		this.dirname = dirname;
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java DeleteIndex <dir_name_to_delete>");
			System.exit(1);
		}
		DeleteIndex index = new DeleteIndex(args[0]);
		index.delete();
	}

	private void delete() {
		DB db = new DB();
		boolean error = false;
		if (db.loadDriver()) {
			try {
				if (db.noDirectoryByName(dirname)) {
					System.out.println("No index at dir " + dirname);
					db.shutdown();
					System.exit(0);
				} else {
					File file = new File("index" + File.separator + dirname);
					final File[] indexFiles = file.listFiles();
					for (File indexFile : indexFiles) {
						indexFile.delete();
					}
					file.delete();
					db.deleteCorpus(dirname);
				}
			} catch (SQLException e) {
				error = true;
				logger.warning(e.getMessage());
			} catch (Exception e) {
				error = true;
				logger.warning(e.getMessage());
			}
			if (!error) {
				System.out.println("Deleted index " + dirname
						+ " successfully.");
			} else {
				System.out.println("Error occurred while deleting index "
						+ dirname + ".");
			}
			db.shutdown();
		}
	}

}
