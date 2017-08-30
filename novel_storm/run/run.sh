#! /bin/bash
 storm jar novel_storm-1.0-SNAPSHOT.jar  com.sohu.mrd.hotnovelrank.HotNovelRankTopology HotNovelRankTopology
 nohup java -cp novel_storm-1.0-SNAPSHOT.jar com.sohu.mrd.KafkaToHDFS.KafkaToHDFS >KafkaToHDFS.log &
