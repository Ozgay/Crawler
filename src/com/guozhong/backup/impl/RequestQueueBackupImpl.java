package com.guozhong.backup.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.log4j.Logger;
import org.cyberneko.html.filters.Writer;

import com.guozhong.CrawlManager;
import com.guozhong.CrawlTask;
import com.guozhong.Request;
import com.guozhong.StartContext;
import com.guozhong.backup.RequestQueueBackup;
import com.guozhong.queue.RequestPriorityBlockingQueue;
import com.guozhong.util.FileRW;

public class RequestQueueBackupImpl implements RequestQueueBackup{
	
	private static Logger logger = Logger.getLogger(RequestQueueBackupImpl.class);
		
	/**
	 * ��ʼURL����
	 */
	private BlockingQueue<StartContext> startRequests = null;
	
	/**
	 * ���ݳ�ʼURL
	 */
	private List<StartContext> allStartBackups = null;
	
	/**
	 */
	private BlockingQueue<Request> requestQueue = null;
	
	private long backupPeriod = 10 * 60 * 1000;//Ĭ�ϱ���ʱ��ʱ10����
	
	/**
	 * ���ݵļƻ�����
	 */
	private Timer timer = null;
	
	/**
	 *  ���ݵ�Ŀ¼
	 */
	private String path = null;
	
	/**
	 * ����log
	 */
	private File logFile = null;
	
	/**
	 * ��һ�α����Ƿ������˳�
	 */
	private boolean lastBackupStoped;
	
	/**
	 * ��ǰ�ı���Ŀ¼
	 */
	public final static String CURRENT_IMAGE = "current_image";
	
	/**
	 * ��һ�εı��� Ŀ¼
	 */
	public final static String LAST_IMAGE = "last_image";


	/**
	 * 
	 * @param path ���ݵ�Ŀ¼
	 */
	public RequestQueueBackupImpl(String path) {
		super();
		this.path = path;
		File dir = new File(path);
		if(!dir.exists()){
			dir.mkdirs();
		}
		logFile = new File(path+"/buckup.log");
		if(logFile.exists()){
			String lastLog = FileRW.readFile(logFile.getAbsolutePath());
			if(lastLog.contains("startBackup")&&lastLog.contains("stopBackup")){
				lastBackupStoped = true;
			}else{
				lastBackupStoped = false;
			}
			logFile.delete();
		}else{
			lastBackupStoped = true;
		}
	}

	@Override
	public void startBackup() {
		String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		FileRW.writeFile(path+" --- startBackup --- "+date, logFile.getAbsolutePath(), false);
		if(timer == null){
			timer = new Timer();
		}
		logger.info("��ʼ���ݶ���");
		initFileEnvironment();
		synchronized (this){
			backUpAllTheEntrance();
		}
		timer.schedule(new BackupHandler(), backupPeriod, backupPeriod);
	}
	
	private final class BackupHandler extends TimerTask{

		@Override
		public void run() {
			initFileEnvironment();
			System.out.println("Ŀ¼��ʼ����ϸ��±���...");
			synchronized (RequestQueueBackupImpl.this) {//���������ļ���Ҫͬ��
				if(startRequests!=null){
					File current = new File(path+"/"+CURRENT_IMAGE+"/startRequests");
					FileOutputStream out = null;
					ObjectOutputStream oos = null;
					try{
						out = new FileOutputStream(current);
						oos = new ObjectOutputStream(out);
						oos.writeObject(startRequests);
						oos.flush();
						logger.info("startRequests�ѱ���");
					}catch(Exception e){
						e.printStackTrace();
						logger.error("���ݶ��г���",e);
					}finally{
						if(oos != null){
							try {
								oos.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						if(out != null){
							try {
								out.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
				
				if(requestQueue!=null){
					File current = new File(path+"/"+CURRENT_IMAGE+"/requestQueue");
					FileOutputStream out = null;
					ObjectOutputStream oos = null;
					try{
						out = new FileOutputStream(current);
						oos = new ObjectOutputStream(out);
						oos.writeObject(requestQueue);
						oos.flush();
						logger.error("requestQueue�ѱ���");
					}catch(Exception e){
						e.printStackTrace();
						logger.error("���ݶ��г���",e);
					}finally{
						if(oos != null){
							try {
								oos.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						if(out != null){
							try {
								out.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}

	/**
	 * ��ʼ��Ŀ¼����
	 */
	private void initFileEnvironment() {
		File last = new File(path+"/"+LAST_IMAGE);
		File current = new File(path+"/"+CURRENT_IMAGE);
		if(!last.exists()){//�����ڽ���Ŀ¼
			last.mkdirs();
		}else{//����ɾ���ļ�������ļ�
			last.delete();
		}
		if(current.exists()){
			current.renameTo(last);
		}
		current.mkdirs();//������ǰ����Ŀ¼
	}
	
	/**
	 * �����������
	 */
	private void backUpAllTheEntrance() {
		if(allStartBackups!=null){
			File current = new File(path+"/"+CURRENT_IMAGE+"/allStartBackups");
			FileOutputStream out = null;
			ObjectOutputStream oos = null;
			try{
				out = new FileOutputStream(current);
				oos = new ObjectOutputStream(out);
				oos.writeObject(allStartBackups);
				oos.flush();
				logger.info("allStartBackups�ѱ���");
			}catch(Exception e){
				e.printStackTrace();
				logger.error("���ݶ��г���",e);
			}finally{
				if(oos != null){
					try {
						oos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if(out != null){
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	@Override
	public void setBackupPeriod(long millisecond) {
		if(millisecond >= 60 * 1000){
			this.backupPeriod = millisecond;
		}
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public synchronized void stopBackup() {
		String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		FileRW.writeFile(path+" --- stopBackup --- "+date, logFile.getAbsolutePath(), true);
		timer.cancel();
		timer.purge();
		timer = null;
	}
	


	@Override
	public void clearBackup() {
		File current = new File(path+"/"+CURRENT_IMAGE);
		if(current.exists()){
			current.delete();
		}
		File last = new File(path+"/"+LAST_IMAGE);
		if(last.exists()){
			last.delete();
		}
	}
	
	@Override
	public void commitRequestQueue(BlockingQueue<Request> req) {
		this.requestQueue = req;
	}
	
	@Override
	public void commitStartRequestQueue(BlockingQueue<StartContext> startRequests) {
		this.startRequests = startRequests;
	}

	@Override
	public void commitStartBackups(List<StartContext> allStartBackups) {
		this.allStartBackups = allStartBackups;
	}

	@Override
	public synchronized BlockingQueue<StartContext> getLastStartRequestQueue()throws Exception {
		BlockingQueue<StartContext> startRequests = null;
		File current = new File(path+"/"+CURRENT_IMAGE+"/startRequests");
		File last = new File(path+"/"+LAST_IMAGE+"/startRequests");
		for (int i = 0; i < 2; i++) {
			FileInputStream in = null;
			ObjectInputStream ois = null;
			try{
				switch(i){
				case 0:
					in = new FileInputStream(current);
					break;
				case 1:
					in = new FileInputStream(last);
					break;
				}
				ois = new ObjectInputStream(in);
				startRequests = (BlockingQueue<StartContext>) ois.readObject();
				break;
			}catch(Exception e){
				//e.printStackTrace();
				if(i==1){
					throw e;
				}
			}finally{
				if(ois!=null){
					try {
						ois.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if(in!=null){
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return startRequests;
	}

	@Override
	public synchronized List<StartContext> getLastStartBackups() throws Exception{
		List<StartContext> allStartBackups = null;
		File current = new File(path+"/"+CURRENT_IMAGE+"/allStartBackups");
		File last = new File(path+"/"+LAST_IMAGE+"/startRequests");
		for (int i = 0; i < 2; i++) {
			FileInputStream in = null;
			ObjectInputStream ois = null;
			try{
				switch(i){
				case 0:
					in = new FileInputStream(current);
					break;
				case 1:
					in = new FileInputStream(last);
					break;
				}
				ois = new ObjectInputStream(in);
				allStartBackups = (List<StartContext>) ois.readObject();
				break;
			}catch(Exception e){
				//e.printStackTrace();
				if(i == 1){
					throw e;
				}
			}finally{
				if(ois!=null){
					try {
						ois.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if(in!=null){
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return allStartBackups;
	}
	
	@Override
	public synchronized BlockingQueue<Request> getLastRequestQueue() throws Exception {
		BlockingQueue<Request> requestQueue = null;
		File current = new File(path+"/"+CURRENT_IMAGE+"/requestQueue");
		File last = new File(path+"/"+LAST_IMAGE+"/startRequests");
		for (int i = 0; i < 2; i++) {
			
			FileInputStream in = null;
			ObjectInputStream ois = null;
			try{
				switch(i){
				case 0:
					in = new FileInputStream(current);
					break;
				case 1:
					in = new FileInputStream(last);
					break;
				}
				ois = new ObjectInputStream(in);
				requestQueue = (BlockingQueue<Request>) ois.readObject();
				break;
			}catch(Exception e){
				//e.printStackTrace();
				if(i == 1){
					throw e;
				}
			}finally{
				if(ois!=null){
					try {
						ois.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if(in!=null){
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return requestQueue;
	}
	
	@Override
	public boolean lastBackupStoped() {
		return lastBackupStoped;
	}
	
	public static void main(String[] args) throws Exception {
		BlockingQueue<StartContext> startRequests = new LinkedBlockingQueue<StartContext>();
		startRequests.add(new StartContext("����URL", "xz", 12));
		startRequests.add(new StartContext("����URL2", "xz", 12));
		startRequests.add(new StartContext("����URL3", "xzx", 92));
		startRequests.add(new StartContext("����URL4", "xaz", 52));
		startRequests.add(new StartContext("����URL5", "xsz", 2));
		startRequests.add(new StartContext("����URL6", "xza", 12));
		/**
		 * ���ݳ�ʼURL
		 */
		List<StartContext> allStartBackups = new ArrayList<StartContext>();
		allStartBackups.add(new StartContext("za����URL1", "1z", 77));
		allStartBackups.add(new StartContext("za����URL2", "1z2", 75));
		allStartBackups.add(new StartContext("za����URL3", "1z3", 79));
		allStartBackups.add(new StartContext("za����URL4", "1z4", 78));
		/**
		 * Ĭ�������ӳ١����ȼ�����
		 */
		BlockingQueue<Request> requestQueue = new RequestPriorityBlockingQueue();
		StartContext context =  new StartContext("http://www.baidu.com", null, 1);
		requestQueue.add(context.createRequest("http://www.baidu.com/1", "1", 1));
		requestQueue.add(context.createRequest("http://www.baidu.com/2", "2", 2));
		requestQueue.add(context.createRequest("http://www.baidu.com/3", "3", 3));
		requestQueue.add(context.createRequest("http://www.baidu.com/4", "4", 4));
		
		RequestQueueBackupImpl impl = new RequestQueueBackupImpl("f:\\zz");
		impl.setBackupPeriod( 60 * 1000);
		impl.commitRequestQueue(requestQueue);
		impl.commitStartBackups(allStartBackups);
		impl.commitStartRequestQueue(startRequests);
		if(impl.lastBackupStoped()){
			System.out.println("�ϴ������˳�");
		}else{
			System.out.println("�ϴη������˳�");
			BlockingQueue<Request> requestQueueB = impl.getLastRequestQueue();
			System.out.println("ȡ��requestQueue");
			System.out.println(requestQueueB==requestQueue);
			while(!requestQueueB.isEmpty()){
				System.out.println(requestQueueB.poll());
			}
			List<StartContext> allStartBackupsB = impl.getLastStartBackups();
			System.out.println("ȡ��allStartBackups");
			System.out.println(allStartBackupsB==allStartBackups);
			int i=0;
			while(i<allStartBackupsB.size()){
				System.out.println(allStartBackupsB.get(i));
				i++;
			}
			BlockingQueue<StartContext> startRequestsB = impl.getLastStartRequestQueue();
			System.out.println("ȡ��startRequests");
			System.out.println(startRequestsB==startRequests);
			while(!startRequestsB.isEmpty()){
				System.out.println(startRequestsB.poll());
			}
		}
		impl.startBackup();
//		while(true){
//			Thread.sleep(1200*60);
//			BlockingQueue<Request> requestQueueB = impl.getLastRequestQueue();
//			System.out.println("ȡ��requestQueue");
//			System.out.println(requestQueueB==requestQueue);
//			while(!requestQueueB.isEmpty()){
//				System.out.println(requestQueueB.poll());
//			}
//			List<StartContext> allStartBackupsB = impl.getLastStartBackups();
//			System.out.println("ȡ��allStartBackups");
//			System.out.println(allStartBackupsB==allStartBackups);
//			int i=0;
//			while(i<allStartBackupsB.size()){
//				System.out.println(allStartBackupsB.get(i));
//				i++;
//			}
//			BlockingQueue<StartContext> startRequestsB = impl.getLastStartRequestQueue();
//			System.out.println("ȡ��startRequests");
//			System.out.println(startRequestsB==startRequests);
//			while(!requestQueueB.isEmpty()){
//				System.out.println(requestQueueB.poll());
//			}
//		}
		
	}
	
	
}
