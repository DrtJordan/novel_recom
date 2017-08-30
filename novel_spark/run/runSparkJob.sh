#! /bin/sh
class=$1
if [ $2 = 0 ]; then
    yesterday=`date +%Y%m%d -d "-1days"`
else
    yesterday=$2
fi
SPARK_HOME=/opt/spark/spark
JAR=/home/mrd/novel_recom/novel_spark-1.0-SNAPSHOT.jar
echo ${SPARK_HOME}/bin/spark-submit --class $class --master yarn-client ${JAR}
${SPARK_HOME}/bin/spark-submit --class $class --master yarn-client --executor-memory 4g --driver-memory 4g ${JAR} ${yesterday}