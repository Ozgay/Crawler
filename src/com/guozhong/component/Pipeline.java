package com.guozhong.component;

import java.io.Serializable;
import java.util.List;

import com.guozhong.model.Proccessable;

public interface  Pipeline extends Serializable{
	
	/**
	 * ���еĽṹ�����ݽ��������������洢���bean
	 * @param procdata
	 */
	public  void proccessData(List<Proccessable> procdata); 
}
