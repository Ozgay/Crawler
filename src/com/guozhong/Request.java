package com.guozhong;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.guozhong.downloader.impl.DefaultFileDownloader;
import com.guozhong.model.Proccessable;


public class Request implements Proccessable,Comparable<Request>{
	

	private String url;

    private Method method;
    
    private String pageCharset = "GBK";
    
    /**
     * �Ƿ�������
     */
    private boolean seed = false;
    /**
     * request���ӵ�����
     */
    private HashMap<String,Object> attributes = null;
    
    /**
     * request�Ĳ���
     */
    private HashMap<String, String> requestParams = null;
    
    /**
     * ����ͷ
     */
    private HashMap<String,String> headers = null;
    
    /**
     * ���һ������
     */
    private String tag;
    
    private int priority = 0;  
    
    
    /**
     * �Ƿ�ʹ��DefaultDownload����
     */
    private boolean isDefaultDownload;
    
    /**
     * �Ƿ��Ƕ���������
     */
    private boolean isBinary = false;
    
    public Request(){
    }
    
    public Request(boolean seed){
    	this.seed = seed;
    }
    
    public final Request setAttribute(String attribute,Object value){
    	if(attributes == null){
    		attributes = new HashMap<String,Object>();
    	}
    	attributes.put(attribute, value);
    	return this;
    }
    
    public final Object getAttribute(String attribute){
    	if(attributes == null){
    		return null;
    	}
    	return attributes.get(attribute);
    }
    
    
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

    

	/**
	 * �Ƿ�������URL
	 * @param seed
	 */
	protected void setSeed(boolean seed) {
		this.seed = seed;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}


	public void putParams(String name,String value){
		iniParmaContainer();
    	if(name != null){
    		if(!requestParams.containsKey(name)&&(value!=null && !value.trim().equals(""))){
    			requestParams.put(name, value);
    		}else if(requestParams.containsKey(name)){
    			throw new IllegalArgumentException("�����Ƿ� name '"+name+"' �Ѿ�����");
    		}else{
    			throw new IllegalArgumentException("�����Ƿ� name = "+name+" value = "+value);
    		}
    	}
    }

	private void iniParmaContainer() {
		if(requestParams == null){
			requestParams = new HashMap<String, String>();
		}
	}
    
    public Set<Entry<String, String>> getParams(){
    	iniParmaContainer();
    	return this.requestParams.entrySet();
    }
    
    public Object getParamsByName(String name){
    	iniParmaContainer();
    	return this.requestParams.get(name);
    }
    
    private void iniHeadersContainer() {
		if(headers == null){
			headers = new HashMap<String, String>();
		}
	}
    
    public void putHeader(String name,String value){
    	iniHeadersContainer();
    	headers.put(name, value);
    }
    
    public Map<String, String> getHedaers(){
    	iniHeadersContainer();
    	return this.headers;
    }
    
    
    public int getPriority() {
		return 1000 - priority;
	}
    
	public void setPriority(int priority) {
		this.priority =1000 - priority;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}
	
	

	public boolean isDefaultDownload() {
		return isDefaultDownload;
	}

	public void setDefaultDownload(boolean defaultDown) {
		this.isDefaultDownload = defaultDown;
	}
	
	public final boolean isStartURL(){
		return seed;
	}



	public boolean isBinary() {
		return isBinary;
	}

	public void setBinary(String savaPath) {
		this.isBinary = true;
		setAttribute(DefaultFileDownloader.SAVE_FILE_NAME, savaPath);
	}

	public String getPageCharset() {
		return pageCharset;
	}

	public void setPageCharset(String pageCharset) {
		this.pageCharset = pageCharset;
	}

	public enum Method{
    	GET,
    	POST;
    }



	/**
	 * request����
	 * getPriorityԽС  ���ȼ�Խ��   ���Ƕ����ϲ�����������  �ײ������ת
	 */
	@Override
	public int compareTo(Request o) {
		if(this.getPriority() < o.getPriority()){
    		return 1;
    	}else if(this.getPriority() == o.getPriority()){
    		return 0;
    	}else{
    		return -1;
    	}
	}

	@Override
	public String toString() {
		return "Request [url=" + url + ", method=" + method + ", seed=" + seed
				+ ", attributes=" + attributes + ", requestParams="
				+ requestParams + ", headers=" + headers + ", tag=" + tag
				+ ", priority=" + priority + ", isDefaultDownload="
				+ isDefaultDownload + ", isBinary=" + isBinary + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Request other = (Request) obj;
		if (method != other.method)
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

}
