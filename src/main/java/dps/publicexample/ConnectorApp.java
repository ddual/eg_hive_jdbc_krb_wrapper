package dps.publicexample;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * THIS IS JUST AN EXAMPLE OF USING THE DRIVER PROGRAMATICALLY
 *     - the class:
 *            dps.publicexamples.drivers.HiverWrapper
 *       is the interesting part, and is what you would usually import into your SQL client
 *       (see the README for more info on how to do this)
 *
 * This is just a very basic example of using the HiveWrapper in the context of a 
 * Java application. In reality the HiveWrapper implements the java.sql.Driver Interface,
 * so you just use it like you would any other JDBC Driver implementation, except you do not
 * need to worry about the Kerberos details.
 */
public class ConnectorApp  {
	
	private static final  Logger    log   			  = LogManager.getLogger(ConnectorApp.class);
	
	private static final  String    JDBC_DRIVER       = "HiveWrapper";
	private static final  String 	CONN_URL_PCB_DEV  = "jdbc:hivecustom://hivehost.your.domain.com:10000/;AuthMech=1;principal=hive/hivehost.your.domain.com@YOUR.DOMAIN.COM;saslQop=auth-conf";
	
	
    public static void main(String[] args)
            throws ClassNotFoundException, SQLException {
        log.info("[II] Starting inside main() - basic connectivity test");
        
        //
        // Load driver
        log.info("[II] Registering driver");
        Class.forName(JDBC_DRIVER);
        
        // Print out all loaded JDBC drivers.
        log.info("[II] Drivers registered:");
        Enumeration<Driver> e = DriverManager.getDrivers();
        while (e.hasMoreElements()) {
          Object driverAsObject = e.nextElement();
          log.info("[II] JDBC Driver=" + driverAsObject);
        }
        
        //
        // Establish connection
        log.info("[II] Attempting connection...");
        Connection conn = DriverManager.getConnection(CONN_URL_PCB_DEV);
        log.info("[II] Connected.");
        
        //
        // Try to run a query
        log.info("[II] Running basic show DBs query...");
        Statement stmt = conn.createStatement();
        ResultSet rs   = stmt.executeQuery("show databases");
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnsNumber = rsmd.getColumnCount();                     

        while (rs.next()) { //print rows
	        for(int i = 1 ; i <= columnsNumber; i++) {//print cols
	              System.out.print(rs.getString(i) + " "); 
	        }
	        System.out.println(); //next row           
        }

        // you can try this too: ResultSet rs   = stmt.executeQuery("SELECT * FROM test_db.test_table LIMIT 10");
    }
}
