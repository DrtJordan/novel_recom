package com.sohu.mrd.util;

/**
 * Created by yonghongli on 2016/8/4.
 */
import java.util.Arrays;
import java.util.Comparator;

public class SimpleTopNTool {

    public static class SortElement{
        private long count = 0;
        private Object val = null;

        public SortElement(long count, Object val) {
            this.count = count;
            this.val = val;
        }

        public long getCount() {
            return count;
        }

        public Object getVal() {
            return val;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    private final int capacity;
    private SortElement[] minHeapArr;
    private int size;

    public SimpleTopNTool(int n) {
        capacity = n;
        minHeapArr = new SortElement[n + 1];//
        clear();
    }

    public void addElement(SortElement element){

        if(size < capacity){
            size++;
            minHeapArr[size] = element;
            siftUp();


        }else if(size>0 &&minHeapArr[1].getCount()<element.getCount()){
            minHeapArr[1] = element;
            siftDown();
        }
    }

    private void siftUp(){

        int cur = size;
        SortElement se = minHeapArr[cur];
        int pIndex = cur>>>1;
        while(pIndex > 0&&(minHeapArr[pIndex].getCount() >se.getCount())){

            minHeapArr[cur]=minHeapArr[pIndex];
            cur=pIndex;
            pIndex=pIndex>>>1;
        }
        minHeapArr[cur]=se;



    }


    private void siftDown(){

        int cur = 1;
        SortElement se = minHeapArr[cur];

        int lIndex = cur << 1;
        int rIndex = lIndex + 1;

        if (rIndex<= size&&minHeapArr[rIndex].getCount()<minHeapArr[lIndex].getCount()){
            lIndex=rIndex;
        }

        while(lIndex <=capacity&&minHeapArr[lIndex].getCount()<se.getCount()){
            minHeapArr[cur]=minHeapArr[lIndex];
            cur = lIndex;

            lIndex = cur << 1;
            rIndex = lIndex + 1;
            if (rIndex<= size&&minHeapArr[rIndex].getCount()<minHeapArr[lIndex].getCount()){
                lIndex=rIndex;
            }
        }
        minHeapArr[cur]=se;

    }


    public void clear(){
        this.size = 0;
        for(int i=0; i < minHeapArr.length; ++i){
            minHeapArr[i] = null;
        }
    }

    public int getSize(){
        return size;
    }

    public SortElement[] getTopN(){
        int len = size < minHeapArr.length ? size : minHeapArr.length - 1;
        SortElement[] res = new SortElement[len];
        for(int i = 0; i<len; ++i){
            res[i] = minHeapArr[i + 1];
        }
        Arrays.sort(res, 0, res.length, new Comparator<SortElement>() {
            @Override
            public int compare(SortElement a, SortElement b) {
                return a.getCount() > b.getCount() ? -1 : 1;
            }
        });

        return res;
    }

    public static SortElement[] sortElement(SortElement[] res){

        Arrays.sort(res, 0, res.length, new Comparator<SortElement>() {
            @Override
            public int compare(SortElement a, SortElement b) {
                return a.getCount() > b.getCount() ? -1 : 1;
            }
        });

        return res;
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        SimpleTopNTool utl = new SimpleTopNTool(100);
        for (int i = 0 ;i<200000;i++){
            utl.addElement(new SortElement(i, "one"));
        }


        System.out.println("getInputSize = " + utl.getSize());

        for(SortElement ele : utl.getTopN()){
            System.out.println(ele.getCount() + " " + ele.getVal().toString());
        }
        long end = System.currentTimeMillis();
        System.out.println((end-start));
    }
}

