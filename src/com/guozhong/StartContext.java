package com.guozhong;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.NEW;
import org.apache.commons.collections.SynchronizedPriorityQueue;
import org.apache.commons.collections.list.SynchronizedList;
import org.apache.xalan.xsltc.runtime.Hashtable;
import org.jboss.netty.util.internal.ConcurrentHashMap;

import com.gargoylesoftware.htmlunit.javascript.host.arrays.ArrayBufferViewBase;
import com.guozhong.Request.Method;
import com.guozhong.component.Pipeline;
import com.guozhong.model.Proccessable;
/**
 * ����ʱ�����������Ϣ�Ͳ������
 * @author Administrator
 *
 */
public final class StartContext implements Serializable{
	
	/**
	 * ȫ�����Բ��ᱻ���
	 */
	private final HashMap<String, Object> globalAttribute = new HashMap<String, Object>();
	
	/**
	 * ��ʱ����   ÿ��StartContext��ɺ�ᱻ���
	 */
	private final HashMap<String, Object> tempAttribute = new HashMap<String, Object>();
	
	/**
	 * �������url
	 */
	private List<Request> subrequest = new ArrayList<Request>();
	
	private Request startRequest;
	private Pipeline pipeline ;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public StartContext(String url,String tag,int priority,String charSet) {
		startRequest = createRequest(url, tag, priority,charSet);
		startRequest.setSeed(true);
	}
	 
	public StartContext(String url,String tag,int priority) {
		startRequest = createRequest(url, tag, priority);
		startRequest.setSeed(true);
	}
	
	public StartContext(Request startRequest) {
		this.startRequest = startRequest;
	}
	
	public StartContext(){}
	
	
	
	
	 /**
     * 
     * @param url
     * @param tag ��ʼURL����Ϊnull
     * @return
     */
    public  Request createRequest(String url,String tag){
    	Request req = new Request();
    	req.setUrl(url);
    	req.setMethod(Method.GET);
    	req.setTag(tag);
    	return req;
    }
    
    /**
     * 
     * @param url
     * @param tag
     * @param priority
     * @param charSet  ҳ��ı����ʽ Ĭ��gbk
     * @return
     */
    public  Request createRequest(String url,String tag,int priority,String charSet){
    	if(priority >=0 && priority<=1000){
    		Request req = new Request();
        	req.setUrl(url);
        	req.setMethod(Method.GET);
        	req.setPriority(priority) ;
        	req.setTag(tag);
        	req.setPageCharset(charSet);
        	return req;
    	}else{
    		throw new IllegalArgumentException("priority��ֵ������0-1000֮��");
    	}
    }
    
    /**
     * 
     * @param url
     * @param tag
     * @param priority
     * @return
     */
    public  Request createRequest(String url,String tag,int priority){
    	if(priority >=0 && priority<=1000){
    		Request req = new Request();
    		req.setUrl(url);
    		req.setMethod(Method.GET);
    		req.setPriority(priority) ;
    		req.setTag(tag);
    		return req;
    	}else{
    		throw new IllegalArgumentException("priority��ֵ������0-1000֮��");
    	}
    }
    
    /**
     * ����һ����������������
     * @param url
     * @param savePath  �ļ������·��
     * @return
     */
    public Request createBinary(String url,String savePath){
    	Request req = new Request();
    	req.setUrl(url);
    	req.setBinary(savePath);
    	return req;
    }

	public Request getStartRequest() {
		return startRequest;
	}

	public void setStartRequest(Request startRequest) {
		this.startRequest = startRequest;
	}

	
	
	public void addSubRequest(Request request){
		if(startRequest != null){
			this.subrequest.add(request);
		}else{
			startRequest = request;
			startRequest.setSeed(true);
		}
	}
	
	public List<Request> getSubRequest(){
		return this.subrequest;
	}

	/**
	 * ȡ����Ϣ
	 * @param key
	 * @return
	 */
	public  Object getGlobalAttribute(String attribute){
		Object value;
		synchronized (globalAttribute) {
			value = globalAttribute.get(attribute);
		}
		return value;
	}
	
	
	/**
	 * @param attribute
	 * @param value
	 * @return
	 */
	public Object putGlobalAttribute(String attribute, Object value) {
		synchronized (globalAttribute) {
			globalAttribute.put(attribute, value);
		}
		return value;
	}
	
	public  Object getTempAttribute(String attribute){
		Object value;
		synchronized (tempAttribute) {
			value = tempAttribute.get(attribute);
		}
		return value;
	}
	
	public Object putTempAttribute(String attribute, Object value) {
		synchronized (tempAttribute) {
			tempAttribute.put(attribute, value);
		}
		return value;
	}

	/**
	 * �����ʱȫ����Ϣ
	 */
	public void clearTempAttribute(){
		synchronized (tempAttribute) {
			tempAttribute.clear();
		}
	}

	public Pipeline getPipeline() {
		return pipeline;
	}

	public void setPipeline(Pipeline pipeline) {
		this.pipeline = pipeline;
	}
	

	/**
	 * ֱ�ӷ��͵����ߴ洢��
	 * @param proccessable
	 */
	public final void sendToPipeline(Proccessable proccessable){
		sendToPipeline(Arrays.asList(proccessable));
	}
	
	public final void sendToPipeline(List<Proccessable> pro){
		if(pipeline != null){
			pipeline.proccessData(pro);
		}
	}
	
	public boolean isEmpty(){
		if(startRequest == null || subrequest.isEmpty()){
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "StartContext [globalAttribute=" + globalAttribute
				+ ", tempAttribute=" + tempAttribute + ", subrequest="
				+ subrequest + ", startRequest=" + startRequest + ", pipeline="
				+ pipeline + "]";
	}


}
