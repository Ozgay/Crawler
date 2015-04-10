package com.guozhong.downloader.impl;

import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class HttpClientFactory {
	
	public static final int USE_DEFAULT_HTTPCLIENT = 0;
	public static final int USE_HTTPS_HTTPCLIENT = 1;
	
	/**
	 * ����ģʽĬ����USE_DEFAULT_HTTPCLIENT����Ҫ���ֶ�����ΪUSE_HTTPS_HTTPCLIENT
	 */
	public static int CREATE_MODE = USE_DEFAULT_HTTPCLIENT;
	
	public static final String defaultEncoding = "utf-8";
	private static HttpClientFactory instance = null;
	private PoolingHttpClientConnectionManager connManager=null;
	static{
		instance = new HttpClientFactory();
	}
	public static HttpClientFactory getInstance(){
		return instance;
	}
	
	/**
	 * ����һ����ͨ��httpClient��
	 * @return
	 */
	public  CloseableHttpClient buildDefaultHttpClient(BasicCookieStore cookieStore) {
		CloseableHttpClient client = null;
		switch(CREATE_MODE){
		case USE_DEFAULT_HTTPCLIENT:
			HttpClientBuilder builder = HttpClientBuilder.
	        create().setDefaultCookieStore(cookieStore);
	        client = builder.build();
			break;
		case USE_HTTPS_HTTPCLIENT:
			client = buildHttpsClient(cookieStore);
			break;
		default:
				throw new RuntimeException("��֧�ָ�����");
		}
		return client;
	}
	
	/**
	 * ��ȡHttpClientʵ��,http��https���ֽ���~.~������cookie�Ĳ�����ƣ�Ӧ����֤���û������ĵ�½����
	 */
	private CloseableHttpClient buildHttpsClient(BasicCookieStore cookieStore) {
        //�������Ӳ���
        ConnectionConfig connConfig = ConnectionConfig.custom().setCharset(Charset.forName(defaultEncoding)).build();
        SocketConfig socketConfig = SocketConfig.custom().build();
        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.<ConnectionSocketFactory>create();
        ConnectionSocketFactory plainSF = new PlainConnectionSocketFactory();
        registryBuilder.register("http", plainSF);
        try { 
            //ָ��������Կ�洢����������׽��ֹ���
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            SSLContext sslContext = SSLContexts.custom().useTLS().loadTrustMaterial(trustStore,
                    new AnyTrustStrategy()).build();
            LayeredConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(sslContext,
                    SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            registryBuilder.register("https", sslSF);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Registry<ConnectionSocketFactory> registry = registryBuilder.build();
        //�������ӹ�����
        if(connManager == null){
        	 connManager = new PoolingHttpClientConnectionManager(registry);
        }
        connManager.setDefaultConnectionConfig(connConfig);
        connManager.setDefaultSocketConfig(socketConfig);
        //LaxRedirectStrategy�����Զ��ض������е�HEAD��GET��POST���󣬽����http�淶��post�����ض�������ơ�
        //LaxRedirectStrategy redirectStrategy = new LaxRedirectStrategy();
        //�����ͻ���
        HttpClientBuilder builder = HttpClientBuilder.
        create().
        setDefaultCookieStore(cookieStore).
        setConnectionManager(connManager);
        CloseableHttpClient httpClient = builder.build();
        return httpClient;
    	}
	}
final class AnyTrustStrategy implements TrustStrategy{
	//�ƹ���ȫ֤�飡Ĭ�����ж�ͨ��
	@Override
	public boolean isTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		return true;
	}
	
}
