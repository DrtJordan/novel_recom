package com.sohu.mrd.service;


import com.alibaba.fastjson.JSONArray;

/**
 * Created by yonghongli on 2017/3/10.
 */
public interface NovelRecomService {

    JSONArray getNovelRecom(String cid, String pid) ;

}
