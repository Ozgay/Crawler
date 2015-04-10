package com.guozhong.downloader.impl;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;


import org.apache.http.conn.ConnectTimeoutException;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;



import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.google.common.cache.Weigher;
import com.guozhong.CrawlTask;
import com.guozhong.Request;
import com.guozhong.Status;
import com.guozhong.component.PageProcessor;
import com.guozhong.component.PageScript;
import com.guozhong.downloader.PageDownloader;
import com.guozhong.downloader.driverpool.WebDriverPool;
import com.guozhong.exception.NormalContainException;
import com.guozhong.exception.ProxyIpLoseException;
import com.guozhong.model.Proccessable;
import com.guozhong.page.DefaultPageFactory;
import com.guozhong.page.Page;
import com.guozhong.page.PageFactory;
import com.guozhong.proxy.ProxyIp;
import com.guozhong.proxy.ProxyIpPool;
import com.guozhong.thread.CountableThreadPool;
import com.guozhong.util.ExceptionUtil;
import com.guozhong.util.StringUtil;
/**
 * ģ�������������
 * @author Administrator
 *
 */
public class SeleniumDownloader extends PageDownloader {
	private final Logger log = Logger.getLogger(SeleniumDownloader.class);

	private volatile WebDriverPool webDriverPool;
	
	
	private final PageFactory pageFactory;

    private Logger logger = Logger.getLogger(getClass());
    
    private HashMap<String,PageScript> scripts = new HashMap<String,PageScript>();
    
    /**
     * ��ʼҳ���ScriptKEY
     */
    private String startPageScriptKey ; 
    
    private DefaultPageDownloader defaultDownloader = new DefaultPageDownloader();
    
    private ProxyIpPool proxyIpPool;
    
    public SeleniumDownloader() {
    	pageFactory = new DefaultPageFactory();
    }
    
    
    public void addJavaScriptFunction(String tag,PageScript javaScript ){
    	PageScript func = scripts.get(tag);
    	if(func == null){
    		if(tag == null){
    			tag = UUID.randomUUID().toString();
    			startPageScriptKey = tag;
    		}
    		scripts.put(tag , javaScript);
    	}else{
    		throw new RuntimeException("�ű��Ѿ�����");
    	}
    }

    /**
     * �ܾ�mimeType�����
     */
	@Override
	public Page download(Request request,CrawlTask task) {
		checkInit();
		if(request.isDefaultDownload()){
			return defaultDownload(request, task);
		}
		
		Page page = null;
		
		if(proxyIpPool == null){
			try {
				page = go(request, task, null);
			} catch (ProxyIpLoseException e) {}
			return page;
		}
		int proxyIpRequestCount = 0 ;
		ProxyIp ip = null;
		while(true){
			if(proxyIpRequestCount >= this.maxProxyRequestCount&&!request.isStartURL()){
				log.error(request.getUrl()+"���ش�������"+maxProxyRequestCount+"������"); 
				break;
			}
			ip = proxyIpPool.pollProxyIp();//����ȥ�����µĴ���IPȥ����
			try {
				page = go(request, task, ip);
				if(ip.incrementRequestCount() >= proxyIpPool.getMaxValidCount()){
					//System.out.println(ip+"ʹ�ôﵽ"+proxyIpPool.getMaxValidCount()+"��");
				}else{
					ip.markCache();//����IP
					//System.out.println(ip+"ʹ�óɹ�");
				}
				return page;
			} catch (ProxyIpLoseException e) {
				proxyIpRequestCount++;
				log.info(request.getUrl()+" "+ip+">����ʧ��");
				continue;
			} 
		}
		return page;
	}
	
	private Page go(Request request,CrawlTask task,ProxyIp ip)throws ProxyIpLoseException{
		ProxyIpLoseException exception  = null;
		ExtendWebDriver webDriver;
		PageScript script = findPageScripts(request.getTag());
		try {
			webDriver = webDriverPool.get((script != null));
	    } catch (InterruptedException e) {
	           logger.error("interrupted", e);
	           e.printStackTrace();
	           return null;
	    }
	    setProxyIp(webDriver,ip);
		//logger.info("downloading page " + request.getUrl());
		Page page = null;
		try{
			if(!request.getHedaers().isEmpty()){
				setHeaders(webDriver,request.getHedaers());
			}
			webDriver.get(request.getUrl());
			if(script != null){
		     	List<Proccessable> pros= script.executeJS(webDriver);
		     	task.handleProccessable(pros);
		     	//Thread.sleep(800);//Ĭ��ִ��JS��ȴ�800����
		    }
			String pageSource = webDriver.getPageSource();
			Status status = Status.fromHttpCode(webDriver.getResponseCode());
			if(ip != null){//������֤�Ƿ���������ҳ
				PageProcessor pageProcessor = task.findPageProccess(request.getTag());
				Matcher matcher = pageProcessor.getNormalContain().matcher(pageSource);
				boolean containalNormal = matcher.find();
				if(!containalNormal){//��ҳ�����Ƿ��������������ַ���
					exception = new ProxyIpLoseException(ip.toString());
					throw exception;
				}
			}
			if(status.getBegin() >= 400 || status.equals(Status.UNSPECIFIED_ERROR)){
				page = pageFactory.buildErrorPage(request, status,webDriverPool , webDriver.getIndex());
			}else{
				WebElement root = null;
				if(pageSource.contains("<html")){//��htmlҳ��
					root = webDriver.findElement(By.xpath("//html"));
				}
				page = pageFactory.buildOkPage(request, status, pageSource,root,webDriverPool , webDriver.getIndex());
			}
		}catch(Exception e){
//			e.printStackTrace();
			//System.out.println("ExtendWebDriver����ҳ��URL:"+request.getUrl());
			if(e instanceof NullPointerException){
				throw new RuntimeException(e); 
			}
			if(ip != null ){//�ж��쳣�Ƿ��Ǵ���Ip�����µ�
				if(ExceptionUtil.existProxyException(e)){
					//e.printStackTrace();
					exception = new ProxyIpLoseException(ip.toString());
				}else{
					e.printStackTrace();
					System.out.println("ipppp:"+ip);
					page = pageFactory.buildRetryPage(request,webDriverPool , webDriver.getIndex());
				}
			}else{
				page = pageFactory.buildRetryPage(request,webDriverPool , webDriver.getIndex());
			}
		}finally{
			webDriverPool.returnToPool(webDriver);
			if(exception != null){//�׳�ipʧЧ�쳣
				throw exception;
			}
		}
		return page;
	}
	
	private void setHeaders(ExtendWebDriver webDriver,
			Map<String, String> hedaers) {
		Set<Entry<String, String>> sets = hedaers.entrySet();
		for (Entry<String, String> entry : sets) {
			webDriver.addRequestHeader(entry.getKey(), entry.getValue());
		}
	}


	public Page defaultDownload(Request request,CrawlTask task) {
		return defaultDownloader.download(request, task);
	} 
	
	private final void setProxyIp(ExtendWebDriver webDriver,ProxyIp ip){
		if(proxyIpPool == null){
			return ;
		}
		WebClient wc = webDriver.getClient();
		WebClientOptions o = wc.getOptions();
		ProxyConfig proxyConfig = new ProxyConfig(ip.getIp(),ip.getPort());
		o.setProxyConfig(proxyConfig);
	}

	
    public void setMaxDriverCount(int count) {
    	checkInit();
    	webDriverPool.setMaxDriverCount(count);
    }
    
    public void setMinDriverCount(int count) {
    	checkInit();
    	webDriverPool.setMinDriverCount(count);
    }

    public void close() throws IOException {
        webDriverPool.closeAll();
    }
    
    private void checkInit() {
        if (webDriverPool == null) {
            synchronized (this){
                webDriverPool = new WebDriverPool();
            }
        }
    }

	public void setTimeout(int second) {
		this.webDriverPool.setPageLoadTimeout(second);
	}

	
	/**
	 * ȡ�õ�ǰҳ����Ҫִ�е�javaScript����
	 * @param request
	 * @return
	 */
	public PageScript findPageScripts(String tag){
		if(tag == null){
			return scripts.get(startPageScriptKey);
		}
		Set<String> allTag = scripts.keySet();
		for (String urlTag : allTag) {
			if(urlTag.equals(tag)){
				return scripts.get(tag);
			}
		}
		return null;
	}


	public void open() { 
		checkInit();
		webDriverPool.open();
	}


	public final void setProxyIpPool(ProxyIpPool proxyIpPool) {
		this.proxyIpPool = proxyIpPool;
		defaultDownloader.setProxyIpPool(proxyIpPool);
	}


	@Override
	public void setMaxProxyRequestCount(int count) {
		super.setMaxProxyRequestCount(count);
		defaultDownloader.setMaxProxyRequestCount(count);
	}
	
}
