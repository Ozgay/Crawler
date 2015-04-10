package com.guozhong.downloader.driverpool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.openqa.selenium.WebDriver;

import com.guozhong.downloader.impl.ExtendWebDriver;
import com.guozhong.downloader.impl.SimpleHttpClient;





/**
 * Ŀǰֻ�Ỻ���ܲ�ִ��JS��Driver����Ϊ��ִ��JS��Driver�ڶ��ִ�к����ֱ���
 * @author code4crafter@gmail.com <br>
 * Date: 13-7-26 <br>
 * Time: ����1:41 <br>
 */
public final class WebDriverPool extends DriverPool{
	
	public final static int DEFAULT_TIMEOUT = 15;//Ĭ�ϼ�����ҳ��ʱ8��

    private final static int DEFAULT_NOTJSDRIVER = 10;

    
    /**
     * ����ִ��JS������
     */
    private int notjsdriver ;
    
    private final static int STAT_RUNNING = 1;

    private final static int STAT_CLODED = 2;
    
    private int pageLoadTimeout ;

    private AtomicInteger stat = new AtomicInteger(STAT_RUNNING);

    /**
     * ͳ�������ù���webDriverList��������ͷŵ�
     */
    private List<ExtendWebDriver> webDriverList = Collections.synchronizedList(new ArrayList<ExtendWebDriver>());
    /**
     * store webDrivers available
     */
    private LinkedBlockingQueue<ExtendWebDriver> queue = new LinkedBlockingQueue<ExtendWebDriver>();
    

    public WebDriverPool(int notjsdriver,int pageLoadTimeout) {
        this.notjsdriver = notjsdriver;
        this.pageLoadTimeout = pageLoadTimeout;
    }

    public WebDriverPool() {
        this(DEFAULT_NOTJSDRIVER,DEFAULT_TIMEOUT);
    }

    /**
     * �ӳ���ȡ��һ��WebDriver
     * @return
     * @throws InterruptedException
     */
    public final ExtendWebDriver get(boolean isExeJs) throws InterruptedException {
        checkRunning();
        ExtendWebDriver poll ;
        if(isExeJs){
        	poll = new ExtendWebDriver(true);
        }else{
        	if(webDriverList.size() < min_drivers){
        		synchronized (webDriverList) {
        			if(webDriverList.size() < min_drivers){
        				createExtendWebDriver();
        			}
        		}
        	}
        	poll = queue.poll();
        }
        
        if (poll != null) {
            return poll;
        }
        
        if (webDriverList.size() < max_drivers) {//���webDriverʹ�õ�����û�дﵽcapacity���������webDriver
            synchronized (webDriverList) {
                if (webDriverList.size() < max_drivers) {
                	createExtendWebDriver();
                }
            }
        }
        return queue.take();//�˷���������֤��������WebDriver���п��ܵȴ�֮ǰ��WebDriverִ����ص�pool��
    }

	private void createExtendWebDriver() {
		ExtendWebDriver e = new ExtendWebDriver(false);
		int driverIndex = webDriverList.size();
		e.setIndex(driverIndex);
		e.manage().timeouts().pageLoadTimeout(pageLoadTimeout, TimeUnit.SECONDS);
		queue.add(e);
		webDriverList.add(e);
	}

    public final void returnToPool(ExtendWebDriver webDriver) {//��WebDriver��ӵ�pool��
        checkRunning();
        if(webDriver.isJavascriptEnabled()){
        	webDriver.quit();
        }else{
        	webDriver.clearHeaders();
        	queue.add(webDriver);
        }
    }
    
    /**
     * ����Ƿ�������
     */
    protected final void checkRunning() {
        if (!stat.compareAndSet(STAT_RUNNING, STAT_RUNNING)) {
            throw new IllegalStateException("Already closed! please open");
        }
    }
    
    /**
     * ��
     */
    public final void open(){
    	if (!stat.compareAndSet(STAT_CLODED, STAT_RUNNING)) {
            //throw new IllegalStateException("Already open!");
            System.out.println("WebDriverPool Already open!");
        }
    }

    /**
     * �ر����е�WebDriver
     */
    public final void closeAll() {
        boolean b = stat.compareAndSet(STAT_RUNNING, STAT_CLODED);
        if (!b) {
            throw new IllegalStateException("Already closed!");
        }
        for (WebDriver webDriver : webDriverList) {
            webDriver.quit();
        }
        webDriverList.clear();
        queue.clear();
    }
    
    public final void setPageLoadTimeout(int timeout){
    	for (WebDriver driver : webDriverList) {
			driver.manage().timeouts().pageLoadTimeout(timeout, TimeUnit.SECONDS);
		}
    	this.pageLoadTimeout = timeout;
    }

	@Override
	public Object getDriver(int driverIndex) {
		if(getIndexs.contains(driverIndex)){
			return null;
		}else{
			System.out.println("�ó�"+driverIndex);
			getIndexs.add(driverIndex);
		}
		for (ExtendWebDriver client : this.webDriverList) {
			if(client.getIndex() == driverIndex){
				queue.remove(client);//�����Ƴ�ʵ������ֹ����δ���֮ǰʹ��
				return client;
			}
		}
		return null;
	}

	@Override
	public void handleComplete(Object driver) {
		ExtendWebDriver extendWebDriver = (ExtendWebDriver) driver;
		getIndexs.remove(extendWebDriver.getIndex());//�������
    	System.out.println("����"+extendWebDriver.getIndex());
    	queue.add(extendWebDriver);//�ص�����
	}

}
