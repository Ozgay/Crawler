package com.guozhong.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public final class DefaultProxyIpPool extends ProxyIpPool{
	
	/**
	 */
	public static final String IP_RESOURCE = "http://www.kuaidaili.com/api/getproxy/?orderid=992398140135556&num=50&browser=1&protocol=1&method=1&sp1=1&sp2=1&sp3=1&sort=0&sep=1";
	//不限制当天过滤http://www.kuaidaili.com/api/getproxy/?orderid=992398140135556&num=50&browser=1&protocol=1&method=1&sp1=1&sp2=1&sp3=1&sort=0&sep=1
	public DefaultProxyIpPool(int initSize, long pastTime, int max_valid_count) {
		super(initSize, pastTime, max_valid_count);
	}
	@Override
	public List<ProxyIp> initProxyIp(int size) {
		List<ProxyIp>   ip = new ArrayList<ProxyIp>();
		URL url = null;
		BufferedReader br = null;
		StringBuffer buf = new StringBuffer();
		try {
			url = new URL(IP_RESOURCE.replace("num=50", "num="+size));
			InputStream in = url.openStream();
			br = new BufferedReader(new InputStreamReader(in,"utf-8"));
			String temp = null;
			ProxyIp proxy = null;
			while((temp = br.readLine())!=null){
				String str[] = temp.split(":");
				buf.append(temp).append("\n");
				proxy = new ProxyIp(str[0], Integer.parseInt(str[1]));
				ip.add(proxy);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(buf);
			System.out.println(IP_RESOURCE);
		}finally{
			if(br != null){
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return ip;
	}
}
