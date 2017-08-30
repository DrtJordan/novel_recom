package org.apache.storm.hdfs.common.rotation;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;

public class MoveFileAction implements RotationAction {
    private static final Logger LOG = LoggerFactory.getLogger(MoveFileAction.class);

    private String destination;

    public MoveFileAction toDestination(String destDir){
        destination = destDir;
        return this;
    }

    @Override
    public void execute(FileSystem fileSystem, Path filePath) throws IOException {
       // Path destPath = new Path(destination, filePath.getName());
        long time = System.currentTimeMillis();
        String dateString = new SimpleDateFormat("yyyyMMdd").format(time);
        String hourString = new SimpleDateFormat("yyyyMMddHHmm").format(time);
        //Path destPath = new Path(destination, dateString+"/"+hourString);
        Path destPath = new Path(destination, dateString);
        fileSystem.mkdirs(destPath);
        destPath = new Path(destPath, hourString+"-"+filePath.getName());
        boolean success = fileSystem.rename(filePath, destPath);
        LOG.info("Moving file {} to {},return {}", filePath, destPath,success);
        return;
    }
}
