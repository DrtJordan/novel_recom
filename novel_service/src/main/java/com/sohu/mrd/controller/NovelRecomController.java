package com.sohu.mrd.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sohu.mrd.constant.HttpConstant;
import com.sohu.mrd.service.NovelRecomService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;



/**
 * Created by yonghongli on 2017/3/10.
 */
@RestController
@RequestMapping(value = "/novels")
public class NovelRecomController {
    private static final Logger logger = LogManager.getLogger(NovelRecomController.class);


    private NovelRecomService tr ;

    @Autowired
    public NovelRecomController(NovelRecomService tr) {
        this.tr = tr;
    }

    @RequestMapping(value ="/recom")
    @ResponseBody
    public Object novelRecom(@RequestParam String cid, @RequestParam String pid){
        long start = System.currentTimeMillis() ;
        JSONObject result = new JSONObject();
        if((cid == null&&pid == null)){
            result.put(HttpConstant.HTTP_RES_STATUS, HttpConstant.HTTP_RES_STATUS_FAIL_PARAMETER) ;
        }else{
            JSONArray res = tr.getNovelRecom(cid,pid) ;
            result.put("data",res) ;
            result.put("size",res.size()) ;
        }
        long end = System.currentTimeMillis() ;
        result.put("cost", (end-start)+" ms") ;
        result.put("cid",cid) ;
        result.put(HttpConstant.HTTP_RES_STATUS, HttpConstant.HTTP_RES_STATUS_SUCCESS) ;
        logger.info("[novelRecom] cid:{}, result:{}", cid, result.toString());
        return result ;
    }
}