#! /bin/sh
baseDir=$(cd "$(dirname "$0")"; pwd)
if [ $# = 0 ]; then
    yesterday=`date +%Y%m%d -d "-1days"`
else
    yesterday=$1
fi
sh $baseDir/runSparkJob.sh  com.sohu.mrd.RunALSRecommender ${yesterday}