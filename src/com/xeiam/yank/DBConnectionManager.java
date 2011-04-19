/**
 * Copyright 2011 Xeiam LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xeiam.yank;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a Singleton that provides access to one or many connection pools defined in a Property file. A client gets access to the single instance and can then check-out and check-in connections from a pool. When the client shuts down it
 * should call the release() method to close all open connections and do other clean up.
 * 
 * @author timmolter
 */
public class DBConnectionManager {

    /** slf4J logger wrapper */
    static Logger logger = LoggerFactory.getLogger(DBConnectionManager.class);

    private final Map<String, DBConnectionPool> pools = new HashMap<String, DBConnectionPool>();

    private Driver jdbcDriver;

    private Properties sqlProperties;

    // The singleton instance
    public static final DBConnectionManager INSTANCE = new DBConnectionManager();

    /**
     * A private constructor since this is a Singleton
     */
    private DBConnectionManager() {
    }

    public void init(Properties pDBProps, Properties pSQLProps) {

        logger.info("Initializing DBConnectionManager...");

        if (pDBProps == null) {
            logger.error("DB PROPS NULL!!!");
        }
        if (pSQLProps == null) {
            logger.warn("SQL PROPS NULL!!!");
        }
        sqlProperties = pSQLProps;

        loadDriver(pDBProps);
        createPools(pDBProps);
    }

    private boolean loadDriver(Properties dbProperties) {

        String jdbcDriverClassName = dbProperties.getProperty("driverclassname");
        try {
            jdbcDriver = (Driver) Class.forName(jdbcDriverClassName).newInstance();
            DriverManager.registerDriver(jdbcDriver);
            logger.info("Registered JDBC driver " + jdbcDriverClassName);
        } catch (Exception e) {
            logger.error("Can't register JDBC driver: " + jdbcDriverClassName + ". ", e);
            return false;
        }
        return true;
    }

    /**
     * Creates instances of DBConnectionPool objects based on the properties. A DBConnectionPool can be defined with the following properties:
     * 
     * <PRE>
     * poolname.url         The JDBC URL for the database
     * poolname.user        A database user (optional)
     * poolname.password    A database user password (if user specified)
     * poolname.maxconn     The maximal number of connections (optional)
     * </PRE>
     * 
     * @param props The connection pool properties
     */
    private void createPools(Properties dbProperties) {

        Enumeration propNames = dbProperties.propertyNames();
        while (propNames.hasMoreElements()) {
            String name = (String) propNames.nextElement();
            if (name.endsWith(".url")) {
                String poolName = name.substring(0, name.lastIndexOf("."));
                String url = dbProperties.getProperty(poolName + ".url");
                if (url == null) {
                    logger.warn("No URL specified for " + poolName);
                    continue;
                }
                String user = dbProperties.getProperty(poolName + ".user");
                String password = dbProperties.getProperty(poolName + ".password");
                String maxconn = dbProperties.getProperty(poolName + ".maxconn", "0");
                int max;
                try {
                    max = Integer.valueOf(maxconn).intValue();
                } catch (NumberFormatException e) {
                    logger.warn("Invalid maxconn value " + maxconn + " for " + poolName);
                    max = 0;
                }

                DBConnectionPool pool = new DBConnectionPool(poolName, url, user, password, max);
                pools.put(poolName, pool);
                logger.info("Initialized pool '" + poolName + "'");
            }
        }
    }

    /**
     * Returns an open connection. If no one is available, and the max number of connections has not been reached, a new connection is created.
     * 
     * @param poolName The pool name as defined in the properties file
     * @return Connection, the connection or null
     */
    public Connection getConnection(String poolName) {

        DBConnectionPool pool = pools.get(poolName);
        if (pool != null) {
            return pool.getConnection();
        }
        return null;
    }

    /**
     * Returns a connection to the named pool.
     * 
     * @param poolName The pool name as defined in the properties file
     * @param con The Connection
     */
    public void freeConnection(String poolName, Connection con) {

        DBConnectionPool pool = pools.get(poolName);
        if (pool != null) {
            pool.freeConnection(con);
        }
    }

    /**
     * Closes all open connections and deregisters all drivers.
     */
    public synchronized void release() {

        logger.info("Releasing DBConnectionManager...");

        // DBConnectionPool p = pools.get("local");

        Set<String> allPools = pools.keySet();

        for (Iterator<String> iterator = allPools.iterator(); iterator.hasNext();) {

            String poolName = iterator.next();
            logger.debug("Releasing pool: " + poolName + "...");

            pools.get(poolName).release();
        }

        try {
            DriverManager.deregisterDriver(jdbcDriver);
        } catch (SQLException e) {
            logger.error("ExceptionDeregistered JDBC driver " + jdbcDriver.getClass().getName(), e);

        }
        logger.info("Deregistered JDBC driver " + jdbcDriver.getClass().getName());
    }

    /**
     * @return the sqlProperties
     */
    public Properties getSqlProperties() {
        return sqlProperties;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

}
