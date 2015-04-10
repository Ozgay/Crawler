package com.guozhong.proxy;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.guozhong.CrawlTask;
import com.guozhong.exception.ProxyIpPoolException;


/**
 * @author Administrator
 *
 */
public abstract class ProxyIpPool {
	private static Logger logger = Logger.getLogger(CrawlTask.class);
	/**
	 */
	public static final int DEFAULT_CACHE_COUNT = 10;
	
	public static final int DEFAULT_MAX_VALID_COUNT = 5;
	
	private final LinkedBlockingQueue<ProxyIp> cache = new LinkedBlockingQueue<ProxyIp>();
	
	private final Stack<ProxyIp> netWorkQueue = new Stack<ProxyIp>();
	
	/**
	 * �Ƿ�������
	 */
	private boolean enableCache = false;
	
	private int initSize = 5*5;
	
	/**
	 * ÿ��IPĬ�������Ч�����������һ��IP��Ч��������ﵽ��ֵ��Ϊȷ��ip������Ϊ�����������������ӻ������Ƴ�
	 * 
	 */
	private int max_valid_count  = DEFAULT_MAX_VALID_COUNT;
	
	/**
	 * Ip����ʱ��
	 */
	private long pastTime = 1000 * 30;
	
	
	/**
	 * �������߳�����5��
	 * @param initSize
	 */
	public ProxyIpPool(int initSize,long pastTime,int max_valid_count){
		this.initSize = initSize;
		this.pastTime = pastTime;
		this.max_valid_count = max_valid_count;
	}
	
	public int getMaxValidCount() {
		return max_valid_count;
	}

	public void setMaxValidCount(int max_valid_count) {
		this.max_valid_count = max_valid_count;
	}

	public ProxyIp pollProxyIp(){
		//�û���
		ProxyIp ip = null;
		if(enableCache){
			int size = cache.size();
			if(size >= DEFAULT_CACHE_COUNT){
				ip = cache.poll();
				ip.setOwner(this);
				return ip;
			}
		}
		//������
		while(true){
			try{
				ip = netWorkQueue.pop();
			}catch(EmptyStackException e){}
			if(ip!= null){
				if(isPast(ip)){
					logger.info("������ڵ�IP"+netWorkQueue.size()+"��\n");
					netWorkQueue.clear();
				}else{
					ip.setOwner(this);
					return ip;
				}
			}
			synchronized (netWorkQueue) {
				if(netWorkQueue.isEmpty()){
					logger.info("�����µ�IP"+initSize+"��\n");
					List<ProxyIp> ips = null;
					try {
						ips = initProxyIp(initSize);
					} catch (Exception e) {
						e.printStackTrace();
						logger.error(e);
					}
					if(ips == null || ips.size() != initSize){
						logger.warn("", new ProxyIpPoolException("���ش���ipС��Ҫ��"+initSize));
					}
					for (ProxyIp item : ips) {
						netWorkQueue.push(item); 
					}
				}
			}
		}
	}
	
	
	/**
	 * @return
	 */
	protected abstract List<ProxyIp> initProxyIp(int size)throws Exception;
	
	
	
	public void cache(ProxyIp ip){
		if(enableCache){
			cache.add(ip);
		}
	}
	
	public int getCacheSize(){
		return cache.size();
	}

	public boolean isEnableCache() {
		return enableCache;
	}

	public ProxyIpPool setEnableCache(boolean enableCache) {
		if(!enableCache){
			cache.clear();
		}
		this.enableCache = enableCache;
		return this;
	}
	
	/**
	 * ��֤�Ƿ����
	 * @return
	 */
	private final boolean isPast(ProxyIp ip){
		return System.currentTimeMillis() - ip.getFetchTime() > pastTime;
	}
}
