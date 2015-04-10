package com.guozhong.downloader.driverpool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.openqa.selenium.WebDriver;

import com.guozhong.component.listener.HttpClientLifeListener;
import com.guozhong.downloader.impl.HttpClientFactory;
import com.guozhong.downloader.impl.SimpleHttpClient;
import com.guozhong.exception.DriverCreateException;

/**
 * @author code4crafter@gmail.com <br>
 * Date: 13-7-26 <br>
 * Time: ����1:41 <br>
 */
public final class HttpClientPool extends DriverPool{
	

	
	
    private HttpClientLifeListener httpClientLifeListener = null;

    /**
     * ͳ���ù���webDriverList�����ͷ�
     *   
     * */
    private List<SimpleHttpClient> httpClientList = Collections.synchronizedList(new ArrayList<SimpleHttpClient>());
    /**
     * store webDrivers available
     */
    private LinkedBlockingQueue<SimpleHttpClient> queue = new LinkedBlockingQueue<SimpleHttpClient>();
    

    public HttpClientPool() {
    }


    /**
     * �ӳ���ȡ��һ��DefaultHttpClient
     * @return
     * @throws InterruptedException
     */
    public final SimpleHttpClient get() throws InterruptedException {
    	SimpleHttpClient poll = null;
    	if(httpClientList.size() < min_drivers){
    		synchronized (httpClientList) {
    			if(httpClientList.size() < min_drivers){
    				createSimpleHttpClient();
    			}
    		}
    	}
    	poll = queue.poll();
        if (poll != null && !getIndexs.contains(poll.getIndex())) {
            return poll;
        }
        if (httpClientList.size() < max_drivers) {//���webDriverʹ�õ����������ﵽcapacity���������webDriver
            synchronized (httpClientList) {
                if (httpClientList.size() < max_drivers) {
                	createSimpleHttpClient();
                }
            }
        }
        return queue.take();//�˷���������֤��������WebDriver���п��ܵȴ�֮ǰ��WebDriverִ����ص�poo
    }

    /**
     */
	private final void createSimpleHttpClient(){
		SimpleHttpClient poll;
		int driverIndex = httpClientList.size() ;
		poll = new SimpleHttpClient();
		poll.setIndex(driverIndex);
		if(httpClientLifeListener != null){
			httpClientLifeListener.onCreated(driverIndex,poll);
		}
		queue.add(poll);
		httpClientList.add(poll);
	}

    public final void returnToPool(SimpleHttpClient httpClient) {//��HttpClient��ӵ�pool   	
    	if(!getIndexs.contains(httpClient.getIndex())){//��ȡ�ó�ȥ��driver���ܻص�����   		 
    		queue.add(httpClient);
    	}
    }
    
    /**
     * ��
     */
    public final void open(){
    }
    
    /**
     * �ر�DefaultHttpClient
     */
    @SuppressWarnings("deprecation")
	public final void closeAll() {
    	for (SimpleHttpClient client : httpClientList) {
    		if(httpClientLifeListener != null){
        		httpClientLifeListener.onQuit(client.getIndex(),client);
        	}
//    		try {
//				client.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		}
        //httpClientList.clear();
        //queue.clear();
    }
    
    public final void setPageLoadTimeout(int timeout){
    }
    
	public void setHttpClientLifeListener(HttpClientLifeListener httpClientLifeListener) {
		this.httpClientLifeListener = httpClientLifeListener;
	}


	@Override
	public Object getDriver(int driverIndex) {
		if(getIndexs.contains(driverIndex)){
			return null;
		}else{
			getIndexs.add(driverIndex);
		}
		for (SimpleHttpClient client : httpClientList) {
			if(client.getIndex() == driverIndex){
				queue.remove(client);//�����Ƴ�ʵ������ֹ����δ���֮ǰʹ��
				return client;
			}
		}
		return null;
	}


	@Override
	public void handleComplete(Object driver) {
		SimpleHttpClient httpClient = (SimpleHttpClient) driver;
		getIndexs.remove(httpClient.getIndex());//�������
    	queue.add(httpClient);//�ص�����
	}
	
}
