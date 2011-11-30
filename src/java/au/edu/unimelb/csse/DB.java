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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

public class DB {
	private static final String driver = "org.apache.derby.jdbc.EmbeddedDriver";
	private static final String protocol = "jdbc:derby:";
	private static final String dbName = "db";
	private static final SimpleDateFormat df = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm:ss");
	private static final Logger logger = Logger.getLogger(DB.class.getName());

	public static void main(String[] args) {
		int arg = 0;
		if (args.length != 1) {
			printUsage();
			System.exit(1);
		}
		try {
			arg = Integer.parseInt(args[0]);
		} catch (NumberFormatException nfe) {
			printUsage();
			System.exit(1);
		}
		DB db = new DB();
		if (db.loadDriver()) {
			try {
				if (arg == 0) {
					db.create();
				} else if (arg == 1) {
					db.printAllIndexDetails();
				}
			} catch (SQLException e) {
				db.printSQLException(e);
			}
			db.shutdown();
		}
	}

	private static void printUsage() {
		System.out.println("Usage: java DB <int-choice> [<additional_param>]");
		System.out
				.println("Where <int-choice> value 0 creates the DB; value 1 prints INDEX table contents; value 2 deletes INDEX entry with dir mentioned.");
	}

	public boolean loadDriver() {
		try {
			Class.forName(driver).newInstance();
			return true;
		} catch (ClassNotFoundException cnfe) {
			// System.err.println("Please check your CLASSPATH.");
			logger.severe("\nUnable to load the JDBC driver " + driver);
			cnfe.printStackTrace(System.err);
		} catch (InstantiationException ie) {
			logger.severe("\nUnable to instantiate the JDBC driver " + driver);
			ie.printStackTrace(System.err);
		} catch (IllegalAccessException iae) {
			logger.severe("\nNot allowed to access the JDBC driver " + driver);
			iae.printStackTrace(System.err);
		}
		return false;
	}

	private void create() throws SQLException {
		Connection conn = DriverManager.getConnection(protocol + dbName
				+ ";create=true", new Properties());
		System.out.println("Connected to and created database " + dbName);

		Statement s = conn.createStatement();
		s
				.execute("create table index(id int generated always as identity, dir varchar(40), name varchar(40), sentences int, createddate varchar(20))");
		s.execute("create table defaultselection(id int)");

		conn.commit();
		conn.close();
		System.out.println("Created tables for operation");
	}

	public void shutdown() {
		try {
			DriverManager.getConnection("jdbc:derby:;shutdown=true");
		} catch (SQLException se) {

			if (!((se.getErrorCode() == 50000) && ("XJ015".equals(se
					.getSQLState())))) {
				System.err.println("Database did not shut down normally.");
				printSQLException(se);
			}
		}
	}

	public void insert(String dir, String name, int sentences)
			throws SQLException {
		Connection conn = DriverManager.getConnection(protocol + dbName,
				new Properties());

		Statement s = conn.createStatement();
		ResultSet rs = s.executeQuery("select count(*) from index");
		rs.next();
		int number = rs.getInt(1);
		rs.close();
		PreparedStatement ps = conn
				.prepareStatement("insert into index(dir, name, sentences, createddate) values (?, ?, ?, ?)");
		ps.setString(1, dir);
		ps.setString(2, name);
		ps.setInt(3, sentences);
		ps.setString(4, df.format(new Date()));
		ps.executeUpdate();

		if (number == 0) {
			ps = conn.prepareStatement("select id from index where dir = ?");
			ps.setString(1, dir);

			rs = ps.executeQuery();
			rs.next();
			int id = rs.getInt(1);
			rs.close();
			ps = conn
					.prepareStatement("insert into defaultselection values (?)");
			ps.setInt(1, id);
			ps.executeUpdate();
		}
		conn.commit();
		conn.close();
	}

	public void printAllIndexDetails() throws SQLException {
		Connection conn = DriverManager.getConnection(protocol + dbName,
				new Properties());

		Statement s = conn.createStatement();
		ResultSet rs = s.executeQuery("select * from index");
		System.out
				.println("ID\tDirName\tDisplayName\tNumSentences\tCreatedDate");
		while (rs.next()) {
			System.out.println(rs.getInt(1) + "\t" + rs.getString(2) + "\t"
					+ rs.getString(3) + "\t" + rs.getInt(4) + "\t"
					+ rs.getString(5));
		}
		rs.close();
		conn.close();
	}

	public boolean noDirectoryByName(String dir) throws SQLException {
		Connection conn = DriverManager.getConnection(protocol + dbName,
				new Properties());
		PreparedStatement s = conn
				.prepareStatement("select count(*) from index where dir=?");
		s.setString(1, dir);
		ResultSet rs = s.executeQuery();
		rs.next();
		int number = rs.getInt(1);
		rs.close();
		conn.close();
		return number == 0;
	}

	public boolean isIndexEmpty() throws SQLException {
		Connection conn = DriverManager.getConnection(protocol + dbName,
				new Properties());
		Statement s = conn.createStatement();
		ResultSet rs = s.executeQuery("select count(*) from index");
		rs.next();
		int number = rs.getInt(1);
		rs.close();
		conn.close();
		return number == 0;
	}

	public void loadCorporaInformation() throws SQLException {
		Connection conn = DriverManager.getConnection(protocol + dbName,
				new Properties());
		Statement s = conn.createStatement();
		ResultSet rs = s.executeQuery("select * from index");
		while (rs.next()) {
			int id = rs.getInt(1);
			String dir = rs.getString(2);
			String name = rs.getString(3);
			int sents = rs.getInt(4);
			String date = rs.getString(5);
			Corpora.INSTANCE.addCorpus(id, dir, name, sents, date);
		}
		rs.close();
		rs = s.executeQuery("select * from defaultselection");
		rs.next();
		Corpora.INSTANCE.setDefaultSelection(rs.getInt(1));
		rs.close();
		conn.close();
	}

	public void deleteCorpus(String dir) throws SQLException {
		Connection conn = DriverManager.getConnection(protocol + dbName,
				new Properties());
		PreparedStatement s = conn
				.prepareStatement("select count(*) from defaultselection where id = (select id from index where dir = ?)");
		s.setString(1, dir);
		ResultSet rs = s.executeQuery();
		rs.next();
		int number = rs.getInt(1);
		s = conn.prepareStatement("delete from index where dir = ?");
		s.setString(1, dir);
		s.executeUpdate();
		if (number != 0) {
			s = conn
					.prepareStatement("select id from index order by createddate");
			rs = s.executeQuery();
			if (rs.next()) {
				int newid = rs.getInt(1);
				s = conn.prepareStatement("update defaultselection set id = "
						+ newid + " where id = " + number);
				s.executeUpdate();
			} else {
				s = conn.prepareStatement("delete from defaultselection");
				s.executeUpdate();
			}
		}
		rs.close();
		conn.close();
	}

	public void printSQLException(SQLException e) {
		while (e != null) {
			System.err.println("\n----- SQLException -----");
			System.err.println("  SQL State:  " + e.getSQLState());
			System.err.println("  Error Code: " + e.getErrorCode());
			System.err.println("  Message:    " + e.getMessage());
			e = e.getNextException();
		}
	}
}
