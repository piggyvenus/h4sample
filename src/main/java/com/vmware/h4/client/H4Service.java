package com.vmware.h4.client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.xml.ws.BindingProvider;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.vmware.h4.util.SslUtil;
import com.vmware.h4.util.VCenterUtilities;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.RuntimeFaultFaultMsg;

public class H4Service {

	private final int VC_PORT = 443;
	private final String MGR_PORT = "8044";
	private final String REPLICATOR_PORT = "8043";
	private String vc_ip;
	private String dc_name;
	private String cluster_name;
	private String old_passwd;
	private String mgr_passwd;
	private String replicator_paaswd;
	private String vc_user;
	private String vc_passwd;
	private String ova_url;
	private String vm_name;
	
	public H4Service(String vc_ip,String dc_name,String cluster_name,String old_passwd,String mgr_passwd,String replicator_paaswd,String vc_user, String vc_passwd,String ova_url,String vm_name)
	{
		System.out.println("H4Service...");
		this.setVc_ip(vc_ip);
		this.setDc_name(dc_name);
		this.setCluster_name(cluster_name);
		this.setOld_passwd(old_passwd);
		this.setMgr_passwd(mgr_passwd);
		this.setReplicator_paaswd(replicator_paaswd);
		this.setVc_user(vc_user);
		this.setVc_passwd(vc_passwd);
		this.setVm_name(vm_name);
		//String datastore_name = "SAP-3TB-3";
		//String replication_host = "10.156.78.53";
		//String target_name = "photon02"; //add to the command
		
	}
	
	public String getMgrPort() {
		return MGR_PORT;
	}


	public String getReplicatorPort() {
		return REPLICATOR_PORT;
	}

	public String getVc_ip() {
		return vc_ip;
	}


	public void setVc_ip(String vc_ip) {
		this.vc_ip = vc_ip;
	}


	public String getDc_name() {
		return dc_name;
	}


	public void setDc_name(String dc_name) {
		this.dc_name = dc_name;
	}


	public String getCluster_name() {
		return cluster_name;
	}


	public void setCluster_name(String cluster_name) {
		this.cluster_name = cluster_name;
	}


	public String getOld_passwd() {
		return old_passwd;
	}


	public void setOld_passwd(String old_passwd) {
		this.old_passwd = old_passwd;
	}


	public String getMgr_passwd() {
		return mgr_passwd;
	}


	public void setMgr_passwd(String mgr_passwd) {
		this.mgr_passwd = mgr_passwd;
	}


	public String getReplicator_paaswd() {
		return replicator_paaswd;
	}


	public void setReplicator_paaswd(String replicator_paaswd) {
		this.replicator_paaswd = replicator_paaswd;
	}


	public String getVc_user() {
		return vc_user;
	}


	public void setVc_user(String vc_user) {
		this.vc_user = vc_user;
	}


	public String getVc_passwd() {
		return vc_passwd;
	}


	public void setVc_passwd(String vc_passwd) {
		this.vc_passwd = vc_passwd;
	}


	public String getOva_url() {
		return ova_url;
	}


	public void setOva_url(String ova_url) {
		this.ova_url = ova_url;
	}


	public String getVm_name() {
		return vm_name;
	}


	public void setVm_name(String vm_name) {
		this.vm_name = vm_name;
	}


	
	
	/**
	 * 
	 * 1. H4 initial Config
	 * 2. Replicator initial Config
	 * 3. H4 mgr sso login
	 * 4. Replicator sso login
	 * 5. replicator get cert and thrumb print
	 * 6. Registering New Replicator with * as owner
	 * 
	 * @param site_name
	 */
	//public static void onboard(String vc_ip, String dc_name, String cluster_name, String old_passwd, String mgr_passwd, String replicator_passwd, String vm_name, String vc_user, String vc_passwd) 
	public void onboard(String site_name) throws FileNotFoundException, CertificateException, NoSuchAlgorithmException, MalformedURLException, IOException
	{
		System.out.println("onboard()...");
		//deploy ova
		
		String h4_ip = getH4ip();
		StringBuilder target_url = new StringBuilder("https://");
		target_url.append(vc_ip);
		target_url.append(":").append(VC_PORT);
		String vc_thumbprint = getServerThumbprint(target_url.toString());
		
		//1. initial config (checked)
		init(h4_ip, MGR_PORT, old_passwd, mgr_passwd, vc_thumbprint);
		
		//2. replicator initial config (checked)
		init(h4_ip, REPLICATOR_PORT, mgr_passwd, replicator_paaswd, vc_thumbprint);
		
		//replicator SSO login
		String replicator_xauthcookie = ssologin(h4_ip, REPLICATOR_PORT);
		System.out.println("onboard() - replicator xauth cookie: "+ replicator_xauthcookie);
		
		//3. replicator get cert and thrumb print
		String replicator_cert = getCert(h4_ip, REPLICATOR_PORT, replicator_xauthcookie);
		System.out.println("onboard() - replicator cert: "+ replicator_cert);
		
		//Manager SSO login
		String mgr_xauthcookie = ssologin(h4_ip, MGR_PORT);
		System.out.println("mgr xauth cookie: "+ mgr_xauthcookie);
		//site_name = "site1";
		
		//4. register replicator
		
		String desc = "Test Replicator";
		registerReplicator(h4_ip, mgr_xauthcookie,  replicator_cert, site_name, desc);
		//get site?
		//get replicator?
	}
	
	/**
	 * 1. Start new replication
	 * 2. Get replication status
	 * 
	 * @param h4_ip
	 * @param target_name
	 * @param datastore_name
	 * @param host_name
	 * @return
	 */
	//public static String replication(String h4_ip, String vc_ip, String vc_user, String vc_passwd, String target_name, String datastore_name, String host_name)
	public String replication(String h4_ip, String target_name, String datastore_name, String host_name, String site_name)
	{
		//Manager SSO login
		String mgr_xauthcookie = ssologin(h4_ip, MGR_PORT);
		System.out.println("replication() mgr xauth cookie: "+ mgr_xauthcookie);
		//String site_name = "site1";
				
		//-----------------------
		//5. list vms
		
		VmsInfo replicationInfo = listVms(h4_ip, mgr_xauthcookie, target_name);
		System.out.println("replication(): " + replicationInfo.getRepid());
		System.out.println("replication(): " + replicationInfo.getVcid());
		System.out.println("replication(): " + replicationInfo.getGvmid());
			
		//6.start new replication
		
		String vc_auth = vclogin();
		
		String dsid = findDataStore(datastore_name, vc_auth);
		System.out.println("replication(): datastore id: " + dsid);
		
		String hostid = getHostId(host_name, vc_auth);
		System.out.println("replication(): host id: " + hostid);
		
		String jsonResult = startNewReplication(h4_ip, mgr_xauthcookie, replicationInfo, site_name, dsid, hostid, target_name);
		System.out.println("replication(): jsonResult: " + jsonResult);
		String replicationStatus = null;
		String replicationId = null;
		try {
			JSONObject replication = new JSONObject(jsonResult);
			replicationStatus = replication.get("state").toString();
			JSONObject workflowinfo = (JSONObject) replication.get("workflowInfo");
			replicationId= workflowinfo.get("resourceId").toString();
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("replication(): replicationStatus: " + replicationStatus);
		System.out.println("replication(): replicationId: " + replicationId);
		
		return replicationId;
	}
	
	/**
	 * 1. start replication
	 * 2. manual sync
	 * 3. export instance
	 * 4. list instance details
	 * 
	 * @param h4_ip
	 * @param target_name
	 * @param datastore_name
	 * @param host_name
	 */
	//public static void export(String h4_ip, String vc_ip, String vc_user, String vc_passwd, String target_name, String datastore_name, String host_name, String ds_folder, String replication_id)
	public void export(String h4_ip,String target_name, String datastore_name, String host_name, String ds_folder, String replication_id)
	{
		//String replication_id = replication(h4_ip, vc_ip, vc_user, vc_passwd, target_name, datastore_name, host_name);
		//Manager SSO login
		String mgr_xauthcookie = ssologin(h4_ip, MGR_PORT);
		manualSync(h4_ip, replication_id, mgr_xauthcookie);
		String vc_auth = vclogin();
		String datastore_id = findDataStore(datastore_name, vc_auth);
		System.out.println("replication(): datastore id: " + datastore_id);
		
		String repliationJson = getReplicationDetails(h4_ip, mgr_xauthcookie, replication_id);
		/**
		String status = "RED";
		while (status != "GREEN")
		{
			String new_status = getDetailsStatus(repliationJson);
			status = new_status;
			if (new_status == "GREEN")
				break;
			
		}
		**/
		System.out.println("replication(): look for instance id ");
		String instance_id = null;
		if (repliationJson!= null)
		{
			try {
				JSONObject jsonData = new JSONObject(repliationJson);
				if (jsonData != null)
				{
					JSONObject dest = (JSONObject)jsonData.get("destinationState");
					JSONObject latestInstance = (JSONObject)dest.get("latestInstance");
					instance_id = latestInstance.getString("key");
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("replication(): export instance");
		//check replication 
		
		String exportStatus = exportInstance(h4_ip, mgr_xauthcookie, replication_id,  datastore_id, ds_folder, instance_id);
		String detailsJson = getInstanceDetails(h4_ip, mgr_xauthcookie, replication_id, instance_id);
		System.out.println("replication(): "+exportStatus+" getInstanceDetails " + detailsJson);
		
	}
	/**
	 * 
	 * @param h4_ip
	 * @param h4_port: indicates the call is to manager (8044) or replicator (8043)
	 * @param old_password
	 * @param new_password
	 * @param vc_thrumbprint
	 */
	//private static void init(String h4_ip, String h4_port, String vc_ip, String old_password, String new_password, String vc_thrumbprint)
	public void init(String h4_ip, String h4_port, String old_password, String new_password, String vc_thrumbprint)
	{
		System.out.println("initializing h4 ...");
		
		StringBuilder url = new StringBuilder("https://");
		url.append(h4_ip);
		url.append(":");
		url.append(h4_port);
		url.append("/config");
		
		final SSLConnectionSocketFactory sslsf;
		try {
		    sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(),
		            NoopHostnameVerifier.INSTANCE);
		} catch (NoSuchAlgorithmException e) {
		    throw new RuntimeException(e);
		}
		
		final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
		        .register("http", new PlainConnectionSocketFactory())
		        .register("https", sslsf)
		        .build();
		
		final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
		cm.setMaxTotal(100);

		
		CloseableHttpClient client = HttpClients.custom()
				.setSSLSocketFactory(sslsf)
	            .setConnectionManager(cm)
	            .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
	            .build();
		
	    HttpPost httpPost = new HttpPost(url.toString());
	    String jsonBody = "{\r\n \"rootPassword\" : \""+new_password+"\",\r\n \"lsUrl\" : \"https://"+ vc_ip +"/lookupservice/sdk\",\r\n  \"lsThumbprint\" : \"SHA-256:"+vc_thrumbprint+"\",\r\n  \"site\" : \"site1\"\r\n}";
	    StringEntity entity;
		try {
			entity = new StringEntity(jsonBody);
			httpPost.setEntity(entity);
		    httpPost.addHeader("Accept", "application/vnd.vmware.h4-v1+json");
		    httpPost.addHeader("Content-type", "application/json");
		    httpPost.addHeader("Config-Secret", old_password);
		    //SslUtil.trustAllHttpsCertificates();
			CloseableHttpResponse response = client.execute(httpPost);
			BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
		
			System.out.println(result.toString());
			client.close();
	
		    
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
		
	}
	
	/**
	 * 
	 * @param h4_ip: IP of the h4 applicance
	 * @param h4_port: indicates the call is to manager (8044) or replicator (8043)
	 * @return
	 */
	public String ssologin(String h4_ip, String h4_port)
	{   
		StringBuilder url = new StringBuilder("https://");
		url.append(h4_ip);
		url.append(":");
		url.append(h4_port);
		url.append("/sessions");
		
		final SSLConnectionSocketFactory sslsf;
		try {
		    sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(),
		            NoopHostnameVerifier.INSTANCE);
		} catch (NoSuchAlgorithmException e) {
		    throw new RuntimeException(e);
		}
		
		final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
		        .register("http", new PlainConnectionSocketFactory())
		        .register("https", sslsf)
		        .build();
		
		final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
		cm.setMaxTotal(100);

		
		CloseableHttpClient client = HttpClients.custom()
				.setSSLSocketFactory(sslsf)
	            .setConnectionManager(cm)
	            .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
	            .build();
		
	    HttpPost httpPost = new HttpPost(url.toString());
	    String jsonBody = "{\r\n \"type\" : \"ssoCredentials\",\r\n \"username\" : \""+vc_user+"\",\r\n \"password\" : \""+vc_passwd+"\"\r\n}";
	    StringEntity entity;
	    String vcavAuth = null;
		try {
			entity = new StringEntity(jsonBody);
			httpPost.setEntity(entity);
		    httpPost.addHeader("Content-type", "application/json");
		 
			CloseableHttpResponse response = client.execute(httpPost);
			Header[] cookies = response.getHeaders("X-VCAV-Auth");
			vcavAuth = cookies[0].toString();
			int spot = vcavAuth.indexOf(": ");
			
			vcavAuth = vcavAuth.substring(spot+1);
			client.close();
	
		    
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return vcavAuth;
	}
	
	public String getCert(String h4_ip, String h4_port, String xauthcookie)
	{
		System.out.println("getCert()...");
		StringBuilder url = new StringBuilder("https://");
		url.append(h4_ip);
		url.append(":");
		url.append(h4_port);
		url.append("/config/certificate");
		String thumbprint = null;

		
		String result = getH4Data(url.toString(), xauthcookie);
		try {
			JSONObject jsonData = new JSONObject(result);
			JSONObject certificate = (JSONObject)jsonData.get("certificate");
			thumbprint = certificate.get("thumbPrint").toString();
			System.out.println("thumbprint: "+thumbprint);
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		return thumbprint;
	}
	
	public void registerReplicator(String h4_ip, String mgr_xauth, String thumbprint, String site_name, String desc)
	{
		System.out.println("Registering replicator...");
		
		StringBuilder url = new StringBuilder("https://");
		url.append(h4_ip);
		url.append(":");
		url.append(MGR_PORT);
		url.append("/replicators");
		
		String jsonBody = "{\r\n  \"owner\" : \"*\",\r\n  \"site\" : \""+site_name+"\",\r\n  \"description\" : \""+desc+"\",\r\n  \"details\" : {\r\n    \"apiUrl\" : \"https://"+h4_ip+":8043\",\r\n    \"apiThumbprint\" : \""+thumbprint+"\",\r\n    \"rootPassword\" : \""+this.replicator_paaswd+"\",\r\n    \"ssoUser\" : \""+this.vc_user+"\",\r\n    \"ssoPassword\" : \""+this.vc_passwd+"\"\r\n  }\r\n}";
		System.out.println("Input"+jsonBody);
		String jsonData = postH4Data(url.toString(), mgr_xauth, jsonBody);
		System.out.println("Result"+jsonData);
		
	}
	
	public String startNewReplication(String h4_ip, String mgr_xauth, VmsInfo vms, String site_name, String datastore_id, String host_id, String target_name)
	{
		System.out.println("startNewReplication()...");
		StringBuilder url = new StringBuilder("https://");
		url.append(h4_ip);
		url.append(":");
		url.append(MGR_PORT);
		url.append("/replications");
		
		String jsonBody = "{\n  \"vcId\" : \""+vms.getVcid()+"\",\n  \"vmId\" : \""+vms.getGvmid()+"\",\n  \"description\" : \"replication description\",\n  \"externalKey\" : null,\n  \"excludedDiskKeys\" : [2000],\n  \"rpo\" : 15,\n  \"sourceLocation\" : {\n    \"site\" : \""+site_name+"\",\n    \"replicatorId\" : \""+vms.getRepid()+"\"\n  },\n  \"destinationLocation\" : {\n    \"site\" : \""+site_name+"\",\n    \"replicatorId\" : \""+vms.getRepid()+"\"\n  },\n  \"replicationPlacement\" : {\n    \"datastoreId\" : \""+datastore_id+"\",\n    \"datastoreFolder\" : \""+target_name+"\",\n    \"nfcHostId\" : \""+host_id+"\",\n    \"vcId\" : \""+vms.getVcid()+"\"\n  },\n  \"autopinInstances\" : true,\n  \"targetDiskType\" : \"THIN\"\n}\n\n";
		System.out.println("startNewReplication(): jsonBody: \n"+jsonBody);
		
		String jsonData = postH4Data(url.toString(), mgr_xauth, jsonBody);
		System.out.println("startNewReplication(): result: \n"+jsonData);
		return jsonData;
	}
	
	public String vclogin()
	{
		System.out.println("vclogin()...");
		String cookie = null;
		StringBuilder url = new StringBuilder("https://");
		url.append(vc_ip);
		url.append("/rest/com/vmware/cis/session");
		final SSLConnectionSocketFactory sslsf;
		try {
		    sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(),
		            NoopHostnameVerifier.INSTANCE);
		} catch (NoSuchAlgorithmException e) {
		    throw new RuntimeException(e);
		}
		
		final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
		        .register("http", new PlainConnectionSocketFactory())
		        .register("https", sslsf)
		        .build();
		
		final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
		cm.setMaxTotal(100);

		
		
	    //
		HttpHost httphost = new HttpHost(vc_ip, VC_PORT, "https");
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(AuthScope.ANY, 
				  new UsernamePasswordCredentials(vc_user, vc_passwd));
		AuthCache authCache = new BasicAuthCache();
		authCache.put(httphost, new BasicScheme());
		
		HttpClientContext context = HttpClientContext.create();
		context.setCredentialsProvider(credsProvider);
		context.setAuthCache(authCache);
		
		CloseableHttpClient client = HttpClients.custom()
				.setDefaultCredentialsProvider(credsProvider)
				.setSSLSocketFactory(sslsf)
	            .setConnectionManager(cm)
	            .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
	            .build();

		HttpPost httpPost = new HttpPost(url.toString());
		try {
			CloseableHttpResponse response = client.execute(httpPost);
			BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
		
			System.out.println("vclogin: "+result.toString());
			
			Header[] auth_cookie = response.getHeaders("Set-Cookie");
			String setcookie = auth_cookie[0].getValue();
			int start = setcookie.indexOf("=");
			int end = setcookie.indexOf(";Path=");
			cookie = setcookie.substring(start+1, end);
			System.out.println("vclogin() cookie: "+cookie);
			
			client.close();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cookie;
	}
	
	public String findDataStore(String datastore_name, String vc_xauth)
	{
		StringBuilder url = new StringBuilder("https://");
		url.append(vc_ip);
		url.append("/rest/vcenter/datastore?filter.names.1=");
		url.append(datastore_name);
		System.out.println("findDatastore(): "+ url.toString());
		
		String result = getVCData(url.toString(), vc_xauth);
		String ds_id = null;
		try {
			JSONObject jsonData = new JSONObject(result);
		
			JSONArray value = jsonData.getJSONArray("value");
			for (int i = 0; i< value.length(); i++)
			{
				JSONObject ds = (JSONObject) value.get(i);
				String dsname = ds.get("name").toString();
				if (dsname.equalsIgnoreCase(datastore_name))
				{
					ds_id = ds.getString("datastore");
				}
				
			}
		} catch (JSONException e) {
			
			e.printStackTrace();
		}
		System.out.println("findDataStore()- ds_id: "+ ds_id);
		return ds_id;
	}
	
	public String getHostId(String host_name, String vc_xauth)
	{
		String host_id = null;
		
		StringBuilder url = new StringBuilder("https://");
		url.append(vc_ip);
		url.append("/rest/vcenter/host");
		System.out.println("getHostId(): "+ url.toString());
		
		String result = getVCData(url.toString(), vc_xauth);
		System.out.println("getHostId() result: "+ result);
		
		try {
			JSONObject jsonData = new JSONObject(result);
		
			JSONArray value = jsonData.getJSONArray("value");
			for (int i = 0; i< value.length(); i++)
			{
				JSONObject ds = (JSONObject) value.get(i);
				String hostname = ds.get("name").toString();
				if (hostname.equalsIgnoreCase(host_name))
				{
					host_id = ds.getString("host");
					break;
				}
				
			}
		} catch (JSONException e) {
			
			e.printStackTrace();
		}
		
		System.out.println("getHostId()- returning host_id: "+ host_id);
		return host_id;
	}
	
	public String getReplicationDetails(String h4_ip, String vc_xauth, String replication_id)
	{
		System.out.println("getReplicationDetails()...");
		
		StringBuilder url = new StringBuilder("https://");
		url.append(h4_ip);
		url.append(":");
		url.append(MGR_PORT);
		url.append("/replications/");
		url.append(replication_id);
		System.out.println("url : "+url.toString());
		
		String result = getH4Data(url.toString(), vc_xauth);
		
		System.out.println("getReplicationDetails(): "+ result);
		return result;
	}
	
	
	private String getVCData(String url, String vc_xauth)
	{
		StringBuilder result = new StringBuilder();
		
		System.out.println("url : "+url);
		final SSLConnectionSocketFactory sslsf;
		try {
		    sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(),
		            NoopHostnameVerifier.INSTANCE);
		} catch (NoSuchAlgorithmException e) {
		    throw new RuntimeException(e);
		}
		
		final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
		        .register("http", new PlainConnectionSocketFactory())
		        .register("https", sslsf)
		        .build();
		
		final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
		cm.setMaxTotal(100);

		
		CloseableHttpClient client = HttpClients.custom()
				.setSSLSocketFactory(sslsf)
	            .setConnectionManager(cm)
	            .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
	            .build();
	    	
		HttpGet httpget = new HttpGet(url);
		httpget.addHeader("Content-type", "application/json");
		httpget.addHeader("vmware-api-session-id", vc_xauth);
		
		try { 
				CloseableHttpResponse httpResponse = client.execute(httpget);
	
						 
				System.out.println("----------------------------------------");
			    System.out.println(httpResponse.getStatusLine());
			    System.out.println("----------------------------------------");

			    HttpEntity entity = httpResponse.getEntity();
			    BufferedReader rd = new BufferedReader(
		                  new InputStreamReader(entity.getContent()));

				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
				client.close();
				System.out.println("RESULT: "+result.toString());
				
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		return result.toString();
	}
	
	
	private String getH4Data(String url, String xauthcookie)
	{
		StringBuilder result = new StringBuilder();
		
		final SSLConnectionSocketFactory sslsf;
		try {
		    sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(),
		            NoopHostnameVerifier.INSTANCE);
		} catch (NoSuchAlgorithmException e) {
		    throw new RuntimeException(e);
		}
		
		final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
		        .register("http", new PlainConnectionSocketFactory())
		        .register("https", sslsf)
		        .build();
		
		final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
		cm.setMaxTotal(100);

		
		CloseableHttpClient client = HttpClients.custom()
				.setSSLSocketFactory(sslsf)
	            .setConnectionManager(cm)
	            .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
	            .build();
	    	
		HttpGet httpget = new HttpGet(url);
		httpget.addHeader("Content-type", "application/json");
		httpget.addHeader("X-VCAV-Auth", xauthcookie);
		
		try { 
				CloseableHttpResponse httpResponse = client.execute(httpget);
	
						 
				System.out.println("----------------------------------------");
			    System.out.println(httpResponse.getStatusLine());
			    System.out.println("----------------------------------------");

			    HttpEntity entity = httpResponse.getEntity();
			    BufferedReader rd = new BufferedReader(
		                  new InputStreamReader(entity.getContent()));

				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
				client.close();
				System.out.println("RESULT: "+result.toString());
				
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		return result.toString();
	}
	
	public String getDetailsStatus(String details)
	{
		String status = null;
		//System.out.println("getDetailsStatus(): "+details);
		try {
			JSONObject replicationDetails = new JSONObject(details);
			status = replicationDetails.getString("overallHealth");
			
		} catch (JSONException e) {
			
			e.printStackTrace();
		}
		
		return status;
	}
	
	
	public VmsInfo listVms(String h4_ip, String mgr_xauth, String target_name)
	{
		
		//TO-DO add exception for not found guest vm
		System.out.println("listVms()...");
		//StringBuilder result = new StringBuilder();
		String replicator_id = null;
		String vc_id = null;
		String guestvm_id = null;
		VmsInfo vmsinfo = new VmsInfo();
		
		StringBuilder url = new StringBuilder("https://");
		url.append(h4_ip);
		url.append(":");
		url.append(MGR_PORT);
		url.append("/inventory/vms");
		
		System.out.println("listVms() - url : "+url.toString());
		
		String result = getH4Data(url.toString(), mgr_xauth);
		System.out.println("listVms() - result : "+result);
		try {	
			JSONArray jsonData = new JSONArray(result);
	
			for (int i=0; i<jsonData.length(); i++)
			{
				JSONObject obj = (JSONObject) jsonData.get(i);
				replicator_id = (String) obj.get("replicatorId").toString();
				JSONArray info = obj.getJSONArray("info");
				for (int j=0; j<info.length(); j++)
				{
					JSONObject infoObj = (JSONObject) info.get(j);
					vc_id = (String) infoObj.get("vcId").toString();
					JSONArray vms = infoObj.getJSONArray("vms");
					for (int k=0; k<vms.length(); k++)
					{
						JSONObject vmsObj = (JSONObject) vms.get(k);
						String gustname = vmsObj.get("name").toString();
						if (gustname.equalsIgnoreCase(target_name))
						{
							guestvm_id = vmsObj.get("id").toString();
							break;
						}
					}
				}
				
			}
					
			System.out.println("replicatorId: "+replicator_id);
			System.out.println("guestvm_id: "+guestvm_id);
			System.out.println("vc_id: "+vc_id);
			
			vmsinfo.setRepid(replicator_id);
			vmsinfo.setgvmid(guestvm_id);
			vmsinfo.setVcid(vc_id);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return vmsinfo;
	}
	
	public void manualSync(String h4_ip, String replication_id, String mgr_xauth)
	{
		System.out.println("manualSync()...");
		StringBuilder url = new StringBuilder("https://");
		url.append(h4_ip);
		url.append(":");
		url.append(MGR_PORT);
		url.append("/replications/");
		url.append(replication_id);
		url.append("/sync");
		String state = null;
		String jsonData = postH4Data(url.toString(), mgr_xauth, null);
		System.out.println(jsonData);
		
		try {
			JSONObject json = new JSONObject(jsonData);
			state = json.getString("state");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("manualSync() state: "+ state);

	}
	
	public String getInstanceDetails(String h4_ip, String vc_xauth, String replication_id, String instance_id)
	{
		System.out.println("getInstanceDetails()...");
		
		StringBuilder url = new StringBuilder("https://");
		url.append(h4_ip);
		url.append(":");
		url.append(MGR_PORT);
		url.append("/replications/");
		url.append(replication_id);
		url.append("/instances/");
		url.append(instance_id);
		System.out.println("url : "+url.toString());
		
		String result = getH4Data(url.toString(), vc_xauth);
		
		System.out.println("getReplicationDetails(): "+ result);
		return result;
		
		
	}
	
	public String postH4Data(String url, String xauthcookie, String jsonBody)
	{
		StringBuilder result = new StringBuilder();
		//System.out.println("url : "+url);
		final SSLConnectionSocketFactory sslsf;
		try {
		    sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(),
		            NoopHostnameVerifier.INSTANCE);
		} catch (NoSuchAlgorithmException e) {
		    throw new RuntimeException(e);
		}
		
		final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
		        .register("http", new PlainConnectionSocketFactory())
		        .register("https", sslsf)
		        .build();
		
		final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
		cm.setMaxTotal(100);

		
		CloseableHttpClient client = HttpClients.custom()
				.setSSLSocketFactory(sslsf)
	            .setConnectionManager(cm)
	            .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
	            .build();
		
	    HttpPost httpPost = new HttpPost(url);
	    //String jsonBody = "{\n  \"vcId\" : \""+vms.getVcid()+"\",\n  \"vmId\" : \""+vms.getGvmid()+"\",\n  \"description\" : \"replication description\",\n  \"externalKey\" : null,\n  \"excludedDiskKeys\" : [ ],\n  \"rpo\" : 15,\n  \"sourceLocation\" : {\n    \"site\" : \""+site_name+"\",\n    \"replicatorId\" : \""+vms.getRepid()+"\"\n  },\n  \"destinationLocation\" : {\n    \"site\" : \""+site_name+"\",\n    \"replicatorId\" : \""+vms.getRepid()+"\"\n  },\n  \"replicationPlacement\" : {\n    \"datastoreId\" : \""+datastore_id+"\",\n    \"datastoreFolder\" : \""+target_name+"\",\n    \"nfcHostId\" : \""+host_id+"\",\n    \"vcId\" : \""+vms.getVcid()+"\"\n  },\n  \"retentionPolicy\" : {\n    \"rules\" : [ {\n      \"numberOfInstances\" : 24,\n      \"distance\" : 5\n    } ]\n  },\n  \"targetDiskType\" : \"THIN\"\n}\n\n";
	    //if (jsonBody != null)
	    	//System.out.println("postH4Data(): jsonBody: \n"+jsonBody);
	    StringEntity entity;
		try {
			if (jsonBody != null)
			{
				entity = new StringEntity(jsonBody);
				httpPost.setEntity(entity);
			}
			//httpPost.addHeader("Accept", "application/vnd.vmware.h4-v1+json");
		    httpPost.addHeader("Content-type", "application/json");
		    httpPost.addHeader("X-VCAV-Auth", xauthcookie);
		 
			CloseableHttpResponse response = client.execute(httpPost);
			BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
		
			System.out.println("postH4Data(): result json: "+result.toString());
			
			client.close();
	
		    
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result.toString();
		
		
	}
	
	public void deleteReplication(String h4_ip,String mgr_xauth, String replication_id)
	{
		System.out.println("deleteReplication()...");
		
		StringBuilder url = new StringBuilder("https://");
		url.append(h4_ip);
		url.append(":");
		url.append(MGR_PORT);
		url.append("/replications/");
		url.append(replication_id);
		url.append("?diskCleanup=AUTO");
		
		final SSLConnectionSocketFactory sslsf;
		try {
		    sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(),
		            NoopHostnameVerifier.INSTANCE);
		} catch (NoSuchAlgorithmException e) {
		    throw new RuntimeException(e);
		}
		
		final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
		        .register("http", new PlainConnectionSocketFactory())
		        .register("https", sslsf)
		        .build();
		
		final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
		cm.setMaxTotal(100);

		
		CloseableHttpClient client = HttpClients.custom()
				.setSSLSocketFactory(sslsf)
	            .setConnectionManager(cm)
	            .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
	            .build();
		
	    HttpDelete httpDelete = new HttpDelete(url.toString());
	    
		try {
			httpDelete.addHeader("Content-type", "application/json");
			httpDelete.addHeader("X-VCAV-Auth", mgr_xauth);
		 
			CloseableHttpResponse response = client.execute(httpDelete);
			BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
		
			System.out.println(result.toString());
			client.close();
	
		    
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getH4ip()
	{
		System.out.println("getH4ip()");
		String h4ip = null;
	
		StringBuilder tmp = new StringBuilder("https://");
		tmp.append(vc_ip);
		tmp.append("/sdk/vimService");
		String url = tmp.toString();
		List<ObjectContent> objectContentList = new ArrayList<ObjectContent>();
		try {

			SslUtil.trustAllHttpsCertificates();
			com.vmware.vim25.VimService vimService = new com.vmware.vim25.VimService();
			com.vmware.vim25.VimPortType vimPort = vimService.getVimPort();
			
			ManagedObjectReference serviceInstance = new ManagedObjectReference();
			serviceInstance.setType("ServiceInstance");
			serviceInstance.setValue("ServiceInstance");
			Map<String, Object> ctxt = ((BindingProvider) vimPort).getRequestContext();
			ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
	        ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
	       
			com.vmware.vim25.ServiceContent serviceContent = vimPort.retrieveServiceContent(serviceInstance);
			vimPort.login(serviceContent.getSessionManager(), vc_user, vc_passwd, null);
			
			VCenterUtilities vcUtil = new VCenterUtilities();
			objectContentList = vcUtil.findAllObjects(vimPort, serviceContent, "VirtualMachine", "name", "guest.ipAddress");
			for(ObjectContent obj:  objectContentList)
			{
				List<DynamicProperty> props = obj.getPropSet();
				String guestip = null;
				for (DynamicProperty prop: props)
				{
					String name = prop.getName();
					String val = prop.getVal().toString();
					
					if (name.equalsIgnoreCase("guest.ipAddress"))
						guestip = prop.getVal().toString();
					if (val.equalsIgnoreCase(vm_name))
					{
						h4ip = guestip;
						break;
					}
					
				}
			}
			System.out.println("h4ip is: "+ h4ip);
			
		} catch (RuntimeFaultFaultMsg e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return h4ip;
			
	}
	
	public String exportInstance(String h4_ip, String mgr_xauth, String replication_id, String datastore_id, String ds_folder, String instance_id)
	{
		System.out.println("exportInstance()...");

		StringBuilder url = new StringBuilder("https://");
		url.append(h4_ip);
		url.append(":");
		url.append(MGR_PORT);
		url.append("/replications/");
		url.append(replication_id);
		url.append("/export-instance");
		
		String jsonBody = "{\n  \"instanceId\" : \""+instance_id+"\",\n  \"datastoreId\" : \""+datastore_id+"\",\n  \"datastoreFolder\" : \""+ds_folder+"\"\n}";
	    //To-do: check result
		System.out.println("exportInstance(): jsonBody: \n"+jsonBody);
		
		String exportStatus = null;
		String jsonData = postH4Data(url.toString(), mgr_xauth, jsonBody);
		System.out.println("exportInstance(): result: \n"+jsonBody);
		try {
			JSONObject replication = new JSONObject(jsonData);
			exportStatus = replication.getString("state");
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return exportStatus;
		
	}
	
	public String getServerThumbprint(String url) throws FileNotFoundException, CertificateException, NoSuchAlgorithmException, MalformedURLException, IOException {
    	
		String thumbprint = null;
    	URL destinationURL = new URL(url);
        HttpsURLConnection conn = (HttpsURLConnection) destinationURL.openConnection();
        conn.connect();
        Certificate[] certs = (X509Certificate[]) conn.getServerCertificates();
        for (Certificate cert : certs) {
            //System.out.println("Certificate is: " + cert);
            if(cert instanceof X509Certificate) {
                    thumbprint = getThumbprint((X509Certificate) cert);
                    System.out.println("\n THUMBPRINT: "+thumbprint);
            }
        }
        return thumbprint;
	}
	
	private String getThumbprint(X509Certificate cert)
            throws NoSuchAlgorithmException, CertificateEncodingException {
    	String sha256AsHex = DigestUtils.sha256Hex(cert.getEncoded());
    	sha256AsHex = sha256AsHex.toUpperCase();
        //System.out.println("\n get raw Thumbprint: "+sha256AsHex.toUpperCase());
        char[] hexStr = sha256AsHex.toCharArray();
        StringBuilder finger = new StringBuilder();
        finger.append(hexStr[0]);
        int counter = 0;
        for (int i=1; i< hexStr.length; i++)
        {
        	counter++;
        	if (counter ==2)
        	{
        		finger.append(":");
        		counter = 0;
        	}
        	finger.append(hexStr[i]);
        }
        
        return finger.toString();
    }
	
	
	public class VmsInfo {
		String repid;
		String gvmid;
		String vcid;
		
		public void setRepid(String rep_id)
		{
			this.repid = rep_id;
		}
		public String getRepid()
		{
			return this.repid;
		}
		public void setgvmid(String gvm_id)
		{
			this.gvmid = gvm_id;
		}
		public String getGvmid()
		{
			return this.gvmid;
		}
		public void setVcid(String vc_id)
		{
			this.vcid = vc_id;
		}
		public String getVcid()
		{
			return this.vcid;
		}
		
		
	}
	
}
