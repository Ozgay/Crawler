package com.guozhong.downloader.driverpool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.openqa.selenium.chrome.ChromeDriver;

import com.guozhong.component.listener.ChromeDriverLifeListener;
import com.guozhong.downloader.impl.SimpleHttpClient;




/**
 * Ŀǰֻ�Ỻ���ܲ�ִ��JS��Driver����Ϊ��ִ��JS��Driver�ڶ��ִ�к����ֱ���
 * @author code4crafter@gmail.com <br>
 * Date: 13-7-26 <br>
 * Time: ����1:41 <br>
 */
public final class ChromeWebDriverPool extends DriverPool{
	
	public final static int DEFAULT_TIMEOUT = 30;//Ĭ�ϼ�����ҳ��ʱ8��

	public final static int DEFAULT_MAX_DRIVER = 10;//Ĭ��������Ĺȸ��������
    
    private final static int STAT_RUNNING = 1;

    private final static int STAT_CLODED = 2;
    
    private int pageLoadTimeout ;
    
    private AtomicInteger stat = new AtomicInteger(STAT_RUNNING);

    /**
     * ͳ�������ù���webDriverList��������ͷŵ�
     */
    private List<com.guozhong.downloader.impl.ChromeDriver> webDriverList = Collections.synchronizedList(new ArrayList<com.guozhong.downloader.impl.ChromeDriver>());
    /**
     * store webDrivers available
     */
    private BlockingDeque<com.guozhong.downloader.impl.ChromeDriver> queue = new LinkedBlockingDeque<com.guozhong.downloader.impl.ChromeDriver>();
    
    /**
     * ������
     */
    private ChromeDriverLifeListener chromeDriverLifeListener = null;
    

    public ChromeWebDriverPool(int max_driver ,int pageLoadTimeout) {
        this.pageLoadTimeout = pageLoadTimeout;
        this.max_drivers = max_driver;
    }

    public ChromeWebDriverPool() {
        this(DEFAULT_MAX_DRIVER,DEFAULT_TIMEOUT);
    }

    /**
     * �ӳ���ȡ��һ��WebDriver
     * @return
     * @throws InterruptedException
     */
    public final com.guozhong.downloader.impl.ChromeDriver get() throws InterruptedException {
        checkRunning();
        com.guozhong.downloader.impl.ChromeDriver poll ;
        if(webDriverList.size() < min_drivers){
        	synchronized (webDriverList) {
        		if(webDriverList.size() < min_drivers){
        			createChromeWebDriver();
        		}
        	}
        }
        poll = queue.poll();
        if (poll != null) {
            return poll;
        }
        if (webDriverList.size() < max_drivers) {//���webDriverʹ�õ����������ﵽcapacity���������webDriver
            synchronized (webDriverList) {
                if (webDriverList.size() < max_drivers) {
                	createChromeWebDriver();
                }
            }
        }
        return queue.take();//�˷���������֤��������WebDriver���п��ܵȴ�֮ǰ��WebDriverִ����ص�pool��
    }

	private void createChromeWebDriver() {
		com.guozhong.downloader.impl.ChromeDriver e = new com.guozhong.downloader.impl.ChromeDriver();
		int driverIndex = webDriverList.size();
		e.setIndex(driverIndex);
		e.manage().timeouts().pageLoadTimeout(pageLoadTimeout, TimeUnit.SECONDS);
		if(chromeDriverLifeListener != null){
			chromeDriverLifeListener.onCreated(driverIndex,e);
		}
		queue.add(e);
		webDriverList.add(e);
	}

    public final void returnToPool(com.guozhong.downloader.impl.ChromeDriver webDriver) {//��WebDriver��ӵ�pool��
        checkRunning();
        queue.add(webDriver);
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
            System.out.println("ChromeWebDriverPool Already open!");
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
        for (com.guozhong.downloader.impl.ChromeDriver webDriver : webDriverList) {
        	if(chromeDriverLifeListener != null){
        		chromeDriverLifeListener.onQuit(webDriver.getIndex(),webDriver);
        	}
            webDriver.quit();
        }
        webDriverList.clear();
        queue.clear();
    }
    
    public final void setPageLoadTimeout(int timeout){
    	for (ChromeDriver driver : webDriverList) {
			driver.manage().timeouts().pageLoadTimeout(timeout, TimeUnit.SECONDS);
		}
    	this.pageLoadTimeout = timeout;
    }

	public ChromeDriverLifeListener getChromeDriverLifeListener() {
		return chromeDriverLifeListener;
	}

	public void setChromeDriverLifeListener(
			ChromeDriverLifeListener chromeDriverLifeListener) {
		this.chromeDriverLifeListener = chromeDriverLifeListener;
	}

	@Override
	public Object getDriver(int driverIndex) {
		if(getIndexs.contains(driverIndex)){
			return null;
		}else{
			System.out.println("�ó�"+driverIndex);
			getIndexs.add(driverIndex);
		}
		for (com.guozhong.downloader.impl.ChromeDriver client : webDriverList) {
			if(client.getIndex() == driverIndex){
				queue.remove(client);//�����Ƴ�ʵ������ֹ����δ���֮ǰʹ��
				return client;
			}
		}
		return null;
	}


	@Override
	public void handleComplete(Object driver) {
		com.guozhong.downloader.impl.ChromeDriver chromeDriver = (com.guozhong.downloader.impl.ChromeDriver) driver;
		getIndexs.remove(chromeDriver.getIndex());//�������
    	System.out.println("����"+chromeDriver.getIndex());
    	queue.add(chromeDriver);//�ص�����
	}

}
