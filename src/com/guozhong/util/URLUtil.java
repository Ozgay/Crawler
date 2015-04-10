package com.guozhong.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLUtil {

	public static final boolean macthDomain(String url,String domain){
		Matcher m = Pattern.compile("^http://[^/]+").matcher(url);
		if(!m.find()){
			return false;
		}
		return m.group().contains(domain);
	}
	
	/**
	 * ��һ��URL��ַ��ȡ����
	 * @return
	 */
	public static final String extractDomain(String url){
		Matcher m = Pattern.compile("^http://[^/]+").matcher(url);
		if(m.find()){
			return m.group();
		}
		return null;
	}
}
