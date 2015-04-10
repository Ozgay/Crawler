package com.guozhong;


import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.guozhong.CrawlManager.CrawlTimerTask;
import com.guozhong.backup.RequestQueueBackup;
import com.guozhong.backup.impl.RequestQueueBackupImpl;
import com.guozhong.component.DynamicEntrance;
import com.guozhong.component.PageProcessor;
import com.guozhong.component.PageScript;
import com.guozhong.component.Pipeline;
import com.guozhong.component.listener.ChromeDriverLifeListener;
import com.guozhong.component.listener.HttpClientLifeListener;
import com.guozhong.component.listener.TaskLifeListener;
import com.guozhong.downloader.PageDownloader;
import com.guozhong.downloader.impl.ChromeDownloader;
import com.guozhong.downloader.impl.DefaultFileDownloader;
import com.guozhong.downloader.impl.DefaultPageDownloader;
import com.guozhong.downloader.impl.SeleniumDownloader;
import com.guozhong.exception.EntranceException;
import com.guozhong.exception.PageProccessorException;
import com.guozhong.model.Proccessable;
import com.guozhong.page.ErrorPage;
import com.guozhong.page.OkPage;
import com.guozhong.page.Page;
import com.guozhong.page.RejectedMimeTypePage;
import com.guozhong.page.RetryPage;
import com.guozhong.queue.RequestPriorityBlockingQueue;
import com.guozhong.thread.CountableThreadPool;
import com.guozhong.util.ProccessableUtil;

/**
 * ����������
 * @author Administrator
 *
 */

public class CrawlTask implements Runnable{
	
	private static Logger logger = Logger.getLogger(CrawlTask.class);
	
	/**
	 * ����Ŀ¼
	 */
	public static String TASK_DIR = null;
	
	
	
	private static final int DEFAULT_MAX_PAGE_RETRY_COUNT = 1;
	
	private static final int DEFAULT_DOWNLOAD_FILE_THREAD = 3;
	
	private  String taskName ;
	
	
	private BlockingQueue<StartContext> startRequests = new LinkedBlockingQueue<StartContext>();
	
	/**
	 * ���ݳ�ʼURL
	 */
	private List<StartContext> allStartBackups = new ArrayList<StartContext>();
	
	/**
	 * Ĭ�������ӳ١����ȼ�����
	 */
	private BlockingQueue<Request> requestQueue = new RequestPriorityBlockingQueue();
	
	private PageDownloader downloader ;
	
	private DefaultFileDownloader defaultFileDownloader;
	
	private int download_file_thread = DEFAULT_DOWNLOAD_FILE_THREAD;
	
	private CountableThreadPool downloadThreadPool ;
	
	private CountableThreadPool offlineHandleThreadPool ; //���ߴ����߳�
	
	private int maxPageRetryCount ;
	
	private Map<String,PageProcessor> taskPageProccess = new HashMap<String,PageProcessor>();
	
	private static final String FIRST_KEY = "FIRST_PAGEPROCCESS";
	
	private Pipeline  pipeline ;
	
	/**
	 * �´����еĶ�ʱ����
	 */
	private CrawlTimerTask repetitive ;
	
	/**
	 * �µĶ�ʱ����
	 */
	private CrawlTimerTask newRepetitive;
	
	private CrawlManager spider ;
	
	private StartContext context;
	
	/**
	 * �������ڼ�����
	 */
	private TaskLifeListener taskLifeListener;
	
	/**
	 * ���һ������ʼ��ʱ��
	 */
	private long lastStartTime ;
	
	/*
	 * ��ǰ���е�״̬
	 */
	private String status ;
	
	private String timerInfo ; 
	
	/**
	 * ��̬���URL
	 */
	private DynamicEntrance dynamicEntrance ;
	
	public CrawlTask(String name){
		this(name,DEFAULT_MAX_PAGE_RETRY_COUNT);
	}
	
	public CrawlTask(String name,int maxPageRetryCount){
		name = name.replaceAll("[/\\\\*\\?<>|]", "_");//  /\*?<>|  �滻�ļ����Ƿ��ַ�
		this.taskName  =name;
		this.maxPageRetryCount = maxPageRetryCount;
	}
	
	/**
	 * ��������Ŀ¼
	 * @param taskId
	 */
	private void makeTaskDir(String taskId){
		TASK_DIR = CrawlManager.PROJECT_DIR+"/"+taskId;
		File task = new File(TASK_DIR);
		if(!task.exists()){
			task.mkdirs();
		}
	}
	
	public String getTaskName(){
		return taskName;
	}

	public TaskLifeListener getTaskLifeListener(){
		return taskLifeListener;
	}

	public void setTaskLifeListener(TaskLifeListener taskLifeListener) {
		this.taskLifeListener = taskLifeListener;
	}
	
	
	public DynamicEntrance getDynamicEntrance() {
		return dynamicEntrance;
	}

	
	public void setDynamicEntrance(DynamicEntrance dynamicEntrance) {
		this.dynamicEntrance = dynamicEntrance;
	}


	public void setDownloadFileThread(int download_file_thread){
		if(download_file_thread > 200){
			throw new RuntimeException("�����̲߳��ܴ���200��");
		}
		if(download_file_thread<3){
			this.download_file_thread = 3;
		}else{
			this.download_file_thread = download_file_thread;
		}
	}
	
	public void setDownloadFileDelayTime(int time){
		initDownloadFileThreadPool();
		defaultFileDownloader.setDelayTime(time);
	}

	public long getLastStartTime() {
		return lastStartTime;
	}

	public void addStartContxt(StartContext context)
	{
		context.setPipeline(pipeline);
		allStartBackups.add(context);
	}
	
	public void addStartURL(String url,Map<String,Object> extra){
		StartContext context = new StartContext(url, null, 0);
		if(extra != null){
			for (Map.Entry<String, Object> keyValuePair :  extra.entrySet()) {
				context.putGlobalAttribute(keyValuePair.getKey(), keyValuePair.getValue());
			}
		}
		addStartContxt(context);
	}
	
	/**
	 * �������URL���ø��Ӳ�����ҳ������ʽ
	 * @param url
	 * @param extra
	 * @param charSet
	 * @return
	 */
	public void addStartUrl(String url,Map<String,Object> extra,String charSet){
		StartContext context = null;
		if(charSet != null){
			context = new StartContext(url, null, 0, charSet);
		}else{
			context = new StartContext(url, null, 0);
		}
		if(extra != null){
			for (Map.Entry<String, Object> keyValuePair :  extra.entrySet()) {
				context.putGlobalAttribute(keyValuePair.getKey(), keyValuePair.getValue());
			}
		}
		addStartContxt(context);
	}
	
	/**
	 * ������г�ʼURL  
	 * �ڼ����������ʼ�����URLǰ���ø÷����ͷ�֮ǰ�����url
	 */
	private void clearStartRequest(){
		startRequests.clear();
		allStartBackups.clear();
	}
	
	public void pushRequest(Request request){
		if(request != null){
			this.requestQueue.add(request);
		}
	}
	
	public int getPageRetryCount() {
		return maxPageRetryCount;
	}
	
	public void setPageRetryCount(int pageRetryCount) {
		this.maxPageRetryCount = pageRetryCount;
	}
	
	public void setDownloader(PageDownloader downloader) {
		this.downloader = downloader;
	}
	
	public void setChromeDriverLifeListener(ChromeDriverLifeListener chromeDriverLifeListener) {
		if(downloader instanceof ChromeDownloader){
			ChromeDownloader d = (ChromeDownloader) downloader;
			d.setChromeDriverLifeListener(chromeDriverLifeListener);
		}else{
			throw new RuntimeException("setChromeDriverLifeListener()��Ҫʹ��  ChromeDownloader");
		}
	}
	
	public void setHttpClientLifeListener(HttpClientLifeListener httpClientLifeListener) {
		if(downloader instanceof DefaultPageDownloader){
			DefaultPageDownloader d = (DefaultPageDownloader) downloader;
			d.setHttpClientLifeListener(httpClientLifeListener);
		}else{
			throw new RuntimeException("setHttpClientLifeListener()��Ҫʹ��  DefaultPageDownloader");
		}
	}
	
	public Pipeline getPipeline() {
		return pipeline;
	}
	
	public void setPipeline(Pipeline pipeline) {
		this.pipeline = pipeline;
		for(StartContext context : allStartBackups){
			context.setPipeline(pipeline);
		}
	}
	
	public BlockingQueue<Request> getRequestQueue() {
		return requestQueue;
	}
	
	public void setRequestQueue(BlockingQueue<Request> q){
		this.requestQueue = q;
	}
	
	public void setThreadPool(CountableThreadPool threadPool) {
		if(this.downloadThreadPool != null && !this.downloadThreadPool.isShutdown()){
			this.downloadThreadPool.shutdown();
		}
		this.downloadThreadPool = threadPool;
		initOfflineThread();
	}

	/**
	 * ��ʼ�����ߴ����̳߳�
	 */
	private final void initOfflineThread() {
		if(offlineHandleThreadPool!=null){
			return;
		}
		if(downloadThreadPool.getThreadNum()<=20){
			 offlineHandleThreadPool = new CountableThreadPool(5);
		}else{
			int offlineThreadNum = downloadThreadPool.getThreadNum()/4;
			offlineHandleThreadPool = new CountableThreadPool(offlineThreadNum);
		}
	}
	
	public final Request poolRequest() throws InterruptedException{
		Request req = null;
		while (true) {
			if(downloadThreadPool.getIdleThreadCount() == 0){
				Thread.sleep(100);
				continue;//�ȴ����߳̿��Թ���
			}
			if ((!requestQueue.isEmpty() || !isSingleStartFinished())) {
				req = requestQueue.poll();
				if (req != null)
					break;
				else 
					Thread.sleep(100);
			} else {
				break;
			}
		}
		return req;
	}
	
	/**
	 * ÿ�����URL���Ӷ���ȫ��ץȡ����򷵻�true
	 * @return
	 */
	public boolean isSingleStartFinished(){
		int alive = downloadThreadPool.getThreadAlive();
		int offline = offlineHandleThreadPool.getThreadAlive();
		if(alive == 0 && requestQueue.isEmpty() && offline == 0){
			return true;
		}
		return false;
	}
	
	public boolean isRuning(){
		if(downloadThreadPool.isShutdown()){
			return false;
		}
		int alive = downloadThreadPool.getThreadAlive();
		int offline = offlineHandleThreadPool.getThreadAlive();
		if(alive > 0 || !requestQueue.isEmpty() || offline > 0)
			return true;
		return false;
	}
	
	public PageDownloader getDownloader(){
		return this.downloader;
	}
	
	@Override
	public void run() {
		logger.info("��ʼץȡ");
		initDynamicEntrance();
		try{
			initTask();
		}catch(EntranceException e){
			logger.warn("���URL�б�Ϊ��", e);
			spider.destoryCrawTask(taskName);//��������
			downloadThreadPool.shutdown();
			offlineHandleThreadPool.shutdown();
			//�����������ڻص�
			if(taskLifeListener != null){
				taskLifeListener.onFinished(this);
			}
		}
		
		if(context==null){
			throw new NullPointerException("���������ΪNull");
		}
		
		lastStartTime  = System.currentTimeMillis();
		status = "ץȡ��";
		downloader.open();//��������
		
		while(!Thread.currentThread().isInterrupted()){
			Request request ;
			try{
				request = poolRequest();
				if(request == null){
					if(isSingleStartFinished() && !nextStartUrlQueue()){//�����ǰ���URLץ�겢��û������һ�����URL���������
						destoryCrawlTask();
						break;
					}else{
						//seelp(0.2f);//ÿץ��һ�����URL  ��˯200ms
						continue;
					}
				}
			}catch(Exception e){
				e.printStackTrace();
				logger.error("��ѯ���г���",e);
				break;
			}
			final Request finalRequest = request;
			final StartContext finalContext  = context;
			invokeDownload(finalRequest, finalContext);
		}
		
	}

	/**
	 * ��ʼ������
	 */
	private void initTask() throws EntranceException{
		if(allStartBackups.isEmpty()){
			throw new EntranceException("����URL��������1��");
		}
		if(startRequests.isEmpty()){
			for (StartContext context: allStartBackups) {//��ʼ���м�������URL
				startRequests.add(context);
			}
		}
		if(taskLifeListener != null){
			taskLifeListener.onStart(this);
		}
		nextStartUrlQueue();
	}


	/**
	 * ��̬�������URL
	 */
	private final void initDynamicEntrance() {
		if(dynamicEntrance != null){
			if(dynamicEntrance.isClearLast()){
				clearStartRequest();//���֮ǰ�����URL
				logger.info("����ϸ��������URL");
			}
			dynamicEntrance.onStartLoad();
			
			String charSet = dynamicEntrance.getEntranceCharSet();
			
			List<String> urls = dynamicEntrance.load();
			if(urls != null){
				for (String url : urls) {
					addStartUrl(url, null, charSet);
				}
			}
			Map<String,Map<String,Object>> paramsUrls = dynamicEntrance.load2();
			if(paramsUrls != null){
				Set<Entry<String, Map<String, Object>>> keyValue = paramsUrls.entrySet();
				for (Entry<String, Map<String, Object>> entry : keyValue) {
					String url = entry.getKey();
					Map<String,Object> params = entry.getValue();
					addStartUrl(url, params, charSet);
				}
			}
			
			List<StartContext> startContexts = dynamicEntrance.getStartContext();
			if(startContexts != null){
				for (StartContext sc : startContexts) {
					addStartContxt(sc);
				}
			}
			logger.info("���������URL"+allStartBackups.size()+"��"); 
			dynamicEntrance.onLoadComplete();
		}
	}

	/**
	 * ��������
	 * @param finalRequest
	 * @param finalContext
	 */
	private final void invokeDownload(final Request finalRequest,
			final StartContext finalContext) {
		if(finalRequest.isBinary()){
			initDownloadFileThreadPool();
			defaultFileDownloader.downloadFile(finalRequest);
		}else{
			downloadThreadPool.execute(new Runnable() {
				
				@Override
				public void run() {
					PageProcessor pageProccess = findPageProccess(finalRequest.getTag());
					if(pageProccess == null)return;
					Page page = downloader.download(finalRequest,CrawlTask.this);
					if(page == null) return;//ȡ����page�򷵻�
					//System.out.println("ץȡ:"+finalRequest.getUrl()+"\tCode:"+page.getStatusCode());
					logger.info("ץȡ:"+finalRequest.getUrl()+"\tCode:"+page.getStatusCode());
					offlineHandle(pageProccess, page, finalContext);
				}
			});
		}
	}

	/**
	 * ��ʼ���ļ������̳߳�
	 */
	private void initDownloadFileThreadPool() {
		if(defaultFileDownloader == null){
			synchronized (this){
				if(defaultFileDownloader == null){
					defaultFileDownloader = new DefaultFileDownloader(download_file_thread);
				}
			}
		}
	}
	
	/**
	 * ���ߴ���
	 * @param pageProccess
	 * @param page
	 * @return
	 */
	private final void offlineHandle(final PageProcessor pageProccess ,final Page page,final StartContext finalContext){
		offlineHandleThreadPool.execute(new Runnable() {
			
			@Override
			public void run() {
				if(page instanceof RejectedMimeTypePage){
				}else if(page instanceof ErrorPage){
					try {
						pageProccess.proccessErrorPage(page,finalContext);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}else if(page instanceof RetryPage){
					RetryPage retryPage = (RetryPage) page;
					if(retryPage.getRetryCount() < maxPageRetryCount){
						retryPage.record();
						pushRequest(retryPage.getRequest());
						logger.warn("��������URL��"+retryPage.getRequest().getUrl());
					}else{
						logger.error("���ش�������"+maxPageRetryCount+":"+retryPage.getRequest().getUrl()+" ������");
					}
				}else if(page instanceof OkPage){
					List<Proccessable> proccessables = ProccessableUtil.buildProcceableList();
					try {
					    pageProccess.process((OkPage) page,finalContext,proccessables);
						handleProccessable(proccessables);
					} catch (Exception e) {
						if(page.isNeedPost()){
							page.handleComplete();
						}
						e.printStackTrace();
						logger.error("���ߴ����쳣URL:"+page.getRequest().getUrl(),e);
					}
					RetryPage.clearCount(page.getRequest().getUrl());
				}
			}
		});
	}
	
	/**
	 * �����URL����ȡ��һ��URL
	 * @return
	 */
	private boolean nextStartUrlQueue() {
		if(context != null){//�����һ�������ĵ���ʱ��Ϣ
			context.clearTempAttribute();
		}
		context = startRequests.poll();
		if(context != null){
			logger.debug("startRequests : "+context.getStartRequest().getUrl());
			pushRequest(context.getStartRequest());
			List<Request> subRequest = context.getSubRequest();
			for (Request sub : subRequest) {
				pushRequest(sub);//��Ӹ���url
			}
		}
		return context!=null;
	}

	/**
	 * �Ҷ�Ӧ�Ľ�����
	 * @param requestTag
	 * @return
	 */
	public final PageProcessor findPageProccess(String requestTag) {
		PageProcessor proccess = null;
		if(requestTag == null){
			proccess = taskPageProccess.get(FIRST_KEY);
		}else{
			proccess = taskPageProccess.get(requestTag);
		}
		return proccess;
	}
	
	private final void seelp(float second) throws InterruptedException{
			Thread.sleep((long) (second * 1000));
	}
	
	public void handleProccessable(List<Proccessable> proccessables){
		Map<String,List<Proccessable>> data = new HashMap<String,List<Proccessable>>();
		List<Request> genjinRequest  = new ArrayList<Request>();
		for (Proccessable procdata : proccessables) {
			if(!(procdata instanceof Request)){
				String className = procdata.getClass().getName();
				if(!data.containsKey(className)){
					data.put(className, new ArrayList<Proccessable>());
				}
				List<Proccessable> list = data.get(className);
				list.add(procdata);
			}else{
				Request follow = (Request) procdata;
				if(!genjinRequest.contains(follow)){
					genjinRequest.add(follow);
				}
			}
		}
		if(pipeline != null){//Ϊ��֤��Ϣ�ȴ洢
			for (List<Proccessable> datalist: data.values()) {
				pipeline.proccessData(datalist);
			}
		}
		//����URL�������
		for (Request req : genjinRequest) {
			pushRequest(req);
		}
	}
	
	public void addPageProccess(PageProcessor proccess){
		if(!taskPageProccess.containsValue(proccess)){
			if(proccess.getTag() == null){
				if(!taskPageProccess.containsKey(FIRST_KEY)){
					taskPageProccess.put(FIRST_KEY,proccess);
				}else{
					throw new PageProccessorException("�Ѿ�����һ��FIRST_KEY PageProccessor");
				}
			}else{
				taskPageProccess.put(proccess.getTag(),proccess);
			}
			PageScript javaScript = proccess.getJavaScript();
			if(javaScript != null){
				if(downloader == null || !(downloader instanceof SeleniumDownloader)){
					throw new RuntimeException("����ʵ����SeleniumDownloader");
				}
				SeleniumDownloader seleniumDownloader = (SeleniumDownloader) downloader;
				seleniumDownloader.addJavaScriptFunction(proccess.getTag(),javaScript);
			}
		}else{
			throw new RuntimeException("��������ظ���PageProccesor");
		}
	}
	
	
	protected CrawlTimerTask getRepetitive() {
		return repetitive;
	}
	
	protected void setRepetitive(CrawlTimerTask repetitive) {
		this.repetitive = repetitive;
	}
	
	/**
	 * �����µĶ�ʱ
	 * @param newRepetitive
	 */
	public void setNewRepetitive(CrawlTimerTask newRepetitive){
		if(newRepetitive != null){
			this.newRepetitive = newRepetitive;
			int periodMinute = (int) (newRepetitive.getPeriod()/1000/60);
			this.timerInfo = newRepetitive.getHour()+"�㿪ʼ��"+newRepetitive.getEndHour()+"�����"+",ÿ"+periodMinute+"��������һ��";
		}
	}
	
	protected void ownerSpider(CrawlManager spider){
		this.spider = spider;
	}
	
	public String getStatus() {
		return status;
	}

	public String getTimerInfo() {
		return timerInfo;
	}

	public void setTimerInfo(String timerInfo) {
		this.timerInfo = timerInfo;
	}

	/**
	 * ���������������
	 */
	private final void destoryCrawlTask(){
		logger.info(taskName+"���������������");
		//�ͷ�������
		try {
			downloader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//��ʱ����
		if(repetitive == null){
			spider.destoryCrawTask(taskName);//��������
			downloadThreadPool.shutdown();
			offlineHandleThreadPool.shutdown();
		}else{
			if(newRepetitive != null){
				spider.redeployTimerTask(newRepetitive);//����������
				newRepetitive = null;
			}else{
				spider.redeployTimerTask(repetitive);//��������
			}
			status = "������";
		}
		//�����������ڻص�
		if(taskLifeListener != null){
			taskLifeListener.onFinished(this);
		}
	}
	
	
}
