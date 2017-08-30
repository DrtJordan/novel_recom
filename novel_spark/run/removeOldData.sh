#! /bin/bash
delateday=`date  +%Y%m%d -d "-2days"`
hadoop fs -rmr  hdfs://heracles/user/mrd/novel_user_behivor/${delateday}