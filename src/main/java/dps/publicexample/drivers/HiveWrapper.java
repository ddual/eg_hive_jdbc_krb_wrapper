package dps.publicexample.drivers;

import java.net.URLDecoder;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HiveWrapper implements Driver {
	
	private static final  Logger       log               = LogManager.getLogger(HiveWrapper.class);
	
	private static final String        DRIVER_TO_WRAP    = "org.apache.hive.jdbc.HiveDriver";		//< This and PREFIX_ACTUAL is all you'd need to change to wrap a different JDBC driver
	
	private static final  String 	   SEC_PROP_KRBDEBUG = "false";									//< Gets applied to sun.security.krb5.debug
	private static final  String 	   SEC_PROP_SUBJCRED = "false";									//< Gets applied to javax.security.auth.useSubjectCredsOnly
	
	public  static final  String       PREFIX 			 = "jdbc:hivecustom:";					    //< This the subprotocol this class expects
	public  static final  String       PREFIX_ACTUAL 	 = "jdbc:hive2:";							//< This is the subprotocol DRIVER_TO_WRAP expects
	
	private static final  String	   RES_PTH_LOGINCONF = "login.conf";							//< loaded from src/main/resources
	private static final  String	   RES_PTH_KRB5_CONF = "krb5.conf";								//< loaded from src/main/resources
	private static final  String	   SEC_PROP_KRREALM  = "YOUR.DOMAIN.COM";
	private static final  String       SEC_PROP_KRKDC    = "host.your.domain.com:host.your.domain.com";
	
	private static        LoginContext loginContext;												//< Everything is a JAAS privileged action...
	
	private static        Driver 	   driver; 														//< Let the driver pull this up in it's own static { }   ...
											   														//  but we are going to load an instance of org.apache.hive.jdbc.HiveDriver 
	
	//
	// CTRs
	//
	
	/**
	 * We need an explicit default CTR to avoid the MethodNotFound init()
	 * exception - but this class is pulled up by it's static initalizer
	 */
	public HiveWrapper() {
		
	}
	
    public HiveWrapper(Driver d) { 
    	log.info("[EN] HiveWrapper::HiveWrapper");
        driver = d; 
    } 
    
    //
    // java.sql.Driver Method implementation:
    //
    
    @Override
    public boolean acceptsURL(String url) {
        return (url != null 					  && 
        		url.toLowerCase().startsWith(PREFIX));
    }
    
    @Override 
    public Connection connect(String u, Properties p) throws SQLException { 
    	log.info("[EN] HiveWrapper::connect");
    	
    	//WARN: we are okay here because PREFIX & PREFIX_ACTUAL do not contain any regex chars
    	String hiveUrl = u.replaceAll("^" + PREFIX, PREFIX_ACTUAL);
    	
    	log.info("[II] Translated URL: " + hiveUrl);
    	
    	
		Subject 	 serviceSubject = loginContext.getSubject();
		Connection   conn 		    =
	            Subject.doAs(serviceSubject, new PrivilegedAction<Connection>() {
	            	Connection   conn  	= null;
	            	
	            	@Override
	                public Connection run() {
	                    try {
	                    	log.info("[II] Calling into parent driver...");
			            	conn 		   = driver.connect(hiveUrl, p);
				            
				            return conn;
	                    } 
	                    catch (SQLException ioe) {
	                        ioe.printStackTrace();
	                    } finally {
	                        // nothing to do here
	                    }
	                    return conn;
	            	}
	            });//end Subject.doAs
		
		return conn;
    } 
    
    @Override 
    public int getMajorVersion() { 
        return driver.getMajorVersion(); 
    } 
    
    @Override 
    public int getMinorVersion() { 
        return driver.getMinorVersion(); 
    } 
    
    @Override 
    public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException { 
        return driver.getPropertyInfo(u, p); 
    } 
    
    @Override 
    public boolean jdbcCompliant() { 
    	// Note: the HiveDriver returns false here
        return driver.jdbcCompliant();
    } 
 
    @Override 
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException { 
        throw new UnsupportedOperationException("Not supported yet."); 
    } 
    
    
    //
    // Static Initalizer
    //
    
    static {
		log.info("[EN] HiveWrapper::static_initalizer");
		
		try {

			//
			// Get externalized paths to our bundeled Kb5/JAAS resources
			ClassLoader classloader 	 = Thread.currentThread().getContextClassLoader();
			String	    pathLoginConfigt = classloader.getResource(RES_PTH_LOGINCONF).toExternalForm();
			String	    pathKrb5Configt  = classloader.getResource(RES_PTH_KRB5_CONF).toExternalForm();
			
			String      pathLoginConfig  = URLDecoder.decode(pathLoginConfigt, "UTF-8");
			String      pathKrb5Config   = URLDecoder.decode(pathKrb5Configt , "UTF-8");
			
			log.debug("[DD] Path of JAAS Conf: " + pathLoginConfig);
			log.debug("[DD] Path of krb5 Conf: " + pathKrb5Config);
			
			//
			// Kerberos init
			log.info("[II] Init krb...");
			System.setProperty("java.security.auth.login.config"		, pathLoginConfig);
			System.setProperty("java.security.krb5.conf"				, pathKrb5Config); 
			System.setProperty("java.security.krb5.realm"  				, SEC_PROP_KRREALM);
			System.setProperty("java.security.krb5.kdc"  				, SEC_PROP_KRKDC);
			System.setProperty("sun.security.krb5.debug"				, SEC_PROP_KRBDEBUG);
			System.setProperty("javax.security.auth.useSubjectCredsOnly", SEC_PROP_SUBJCRED);
		
			//
			// Hadoop Client init - 
			// WARNING: DO NOT use this if you want to use this as a driver for Teradata Studio
			//          you will get a FileNotFoundException for core-default.xml
			//          most of this stuff has zero effect anyway - just build your URL parameters correctly
			//org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
			//conf.set("hadoop.rpc.protection"		 , "privacy");  	// must match the server-side setting in - core-site.xml
			//conf.set("hadoop.security.authentication", "kerberos"); 	// must match the server-side setting in - hive-site.xml
			//UserGroupInformation.setConfiguration(conf);
	    
	    	String appName = "Login";
	    	loginContext = new LoginContext(appName);
			log.debug("[DD] Login in for appName=" + appName);
			loginContext.login();
			log.debug("[DD] Principals after login=" + loginContext.getSubject().getPrincipals());
			log.debug("[DD] UserGroupInformation.loginUserFromSubject(): appName="
																			+ appName
																			+ ", principals="
																			+ loginContext.getSubject().getPrincipals());
			
			loginContext.login();
	    	
	        DriverManager.registerDriver(new HiveWrapper((Driver) Class.forName(DRIVER_TO_WRAP).newInstance()));
	    	log.info("[EX] HiveWrapper::static_initalizer");
	    }
	    catch (Exception e) {
	      System.err.println(e.getLocalizedMessage());
	      log.error("[!!]" + e.getLocalizedMessage(), e);
	    }
	  }
    
}
