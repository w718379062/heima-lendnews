package com.heima.minio.test;


import com.heima.file.service.FileStorageService;
import com.sun.scenario.effect.impl.sw.java.JSWBlend_SRC_OUTPeer;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@SpringBootTest

public class MinioTest {

//    public static void main(String[] args) {
//        FileInputStream fileInputStream =null;
//        try {
//            fileInputStream=  new FileInputStream("D://aaa.jpg");
//            //创建minio客户端连接
//            MinioClient minioClient = MinioClient.builder().credentials("minio", "minio123").endpoint("http://192.168.200.130:9000").build();
//            //上传文件
//            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
//                    .object("list.html")//文件名
//                    .contentType("image/jpg")//文件类型
//                    .bucket("leadnews").stream(fileInputStream, fileInputStream.available(), -1).build(); //文件流//桶名词  与minio创建的名词一致
//            minioClient.putObject(putObjectArgs);
//
//        } catch (Exception e) {
//
//            e.printStackTrace();
//
//        }
//
//    }


    //注入miniostarter
    @Autowired
    private FileStorageService service;
    @Test
    public  void testUpLode() throws Exception {


        FileInputStream fileInputStream = new FileInputStream("C:\\Users\\86159\\Pictures\\Saved Pictures\\循环使用场景.png");
        String htmlFile = service.uploadImgFile("", "aaa.jpg", fileInputStream);
        System.out.println(htmlFile);
    }



}
