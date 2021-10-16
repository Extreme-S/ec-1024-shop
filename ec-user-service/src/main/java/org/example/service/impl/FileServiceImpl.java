package org.example.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.example.config.OSSConfig;
import org.example.service.FileService;
import org.example.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Autowired
    private OSSConfig ossConfig;

    @Override
    public String uploadUserImg(MultipartFile file) {

        //获取相关配置
        String bucketname = ossConfig.getBucketname();
        String endpoint = ossConfig.getEndpoint();
        String accessKeyId = ossConfig.getAccessKeyId();
        String accessKeySecret = ossConfig.getAccessKeySecret();
        //创建OSS对象
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        //获取原生文件名  xxx.jpg
        String originalFileName = file.getOriginalFilename();

        //JDK8的日期格式
        LocalDateTime ldt = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        //拼装路径,oss上存储的路径  user/2022/12/1/sdfdsafsdfdsf.jpg
        String folder = dtf.format(ldt);
        String fileName = CommonUtil.generateUUID();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));

        // 在OSS上的bucket下创建 user 这个文件夹
        String newFileName = "user/" + folder + "/" + fileName + extension;

        try {
            PutObjectResult putObjectResult = ossClient.putObject(bucketname, newFileName, file.getInputStream());
            //拼装返回路径
            if (putObjectResult != null) {
                String imgUrl = "https://" + bucketname + "." + endpoint + "/" + newFileName;
                return imgUrl;
            }

        } catch (IOException e) {
            log.error("文件上传失败:{}", e);
        } finally {
            //oss关闭服务，不然会造成OOM
            ossClient.shutdown();
        }

        return null;
    }
}
