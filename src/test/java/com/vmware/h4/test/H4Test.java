package com.vmware.h4.test;

import com.vmware.h4.client.*;
import com.vmware.h4.util.*;

import java.util.Iterator;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

public class H4Test {
	static String vc_ip = null;
	static String dc_name = null;
	static String cluster_name = null;
	static String old_passwd = null;
	static String mgr_passwd = null;
	static String replicator_paaswd = null;
	static String vc_user = null;
	static String vc_passwd = null;
	static String ova_url = null;
	static String vm_name = null;
	static String target_name = null;
	static String datastore_name = null;
	static String replication_host = null;
	static String site_name = null;

	public static void main(String[] args)  throws Exception
	{

		
		 CompositeConfiguration config = new CompositeConfiguration();
		 
        // add config sources.
        // add SystemConfiguration first below we need to override properties
        // using java system properties
        config.addConfiguration(new SystemConfiguration());
        config.addConfiguration(new PropertiesConfiguration("./sample.properties"));
        System.out.println("----------------------------");
        System.out.println("Listing composite properties");
        System.out.println("----------------------------");
        Iterator<String> keys = config.getKeys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.equalsIgnoreCase("vc_ip"))
            	vc_ip = (String) config.getProperty(key);
            else if (key.equalsIgnoreCase("dc_name"))
            	dc_name = (String) config.getProperty(key);
            else if (key.equalsIgnoreCase("cluster_name"))
            	cluster_name = (String) config.getProperty(key);
            else if (key.equalsIgnoreCase("old_passwd"))
            	old_passwd = (String) config.getProperty(key);
            else if (key.equalsIgnoreCase("mgr_passwd"))
            	mgr_passwd = (String) config.getProperty(key);
            else if (key.equalsIgnoreCase("replicator_paaswd"))
            	replicator_paaswd = (String) config.getProperty(key);
            else if (key.equalsIgnoreCase("vc_user"))
            	vc_user = (String) config.getProperty(key);
            else if (key.equalsIgnoreCase("vc_passwd"))
            	vc_passwd = (String) config.getProperty(key);
            else if (key.equalsIgnoreCase("ova_url"))
            	ova_url = (String) config.getProperty(key);
            else if (key.equalsIgnoreCase("vm_name"))
            	vm_name = (String) config.getProperty(key);
            else if (key.equalsIgnoreCase("target_name"))
            	target_name = (String) config.getProperty(key);
            else if (key.equalsIgnoreCase("datastore_name"))
            	datastore_name = (String) config.getProperty(key);
            else if (key.equalsIgnoreCase("replication_host"))
            	replication_host = (String) config.getProperty(key);
            else if (key.equalsIgnoreCase("site_name"))
            	site_name = (String) config.getProperty(key);
            System.out.println(key + " = " + config.getProperty(key));
        }
        
        //we still need to import the cert via keytool
		SslUtil.trustAllHttpsCertificates();
		H4Service svc = new H4Service(vc_ip,dc_name,cluster_name,old_passwd,mgr_passwd,replicator_paaswd,vc_user, vc_passwd,ova_url,vm_name);
		/*
		 * Step #1: on board only once 
		 */
		svc.onboard(site_name); 
		String h4_ip = svc.getH4ip();

		/*
		 * Step 2: start a new replication
		 */
		String h4_port = svc.getMgrPort();
		System.out.println("main - h4 info: "+ h4_ip + "," + h4_port);
		String replication_id = svc.replication(h4_ip, target_name, datastore_name, replication_host, site_name);
		
		//this is mock-up when replication has started.
		//String replication_id ="H4-f5979daf-ea35-45c3-959c-0da621b303ed";
		
		String vc_auth = svc.ssologin(h4_ip, h4_port);
		String replicationJson = svc.getReplicationDetails(h4_ip, vc_auth, replication_id);
		System.out.println("main()- getReplicationDetails: "+ replicationJson);
		String status = svc.getDetailsStatus(replicationJson);
		System.out.println("main - getDetailsStatus(): "+ status);
	
		/*
		 * Step 3: export: only one export per host at a time
		 */
		//Test export H4-6905b6d8-7567-400b-85c7-5d8ed485c0ec
		//get the replication id
		//svc.export(h4_ip, target_name, datastore_name, replication_host, "photon03-export-b5",replication_id);
		/*
		 * Step 4. Delete replication
		 */
		//String mgr_xauthcookie = svc.ssologin(h4_ip, h4_port);
		//svc.deleteReplication(h4_ip, mgr_xauthcookie, replication_id);

			
		
	}
	

}
