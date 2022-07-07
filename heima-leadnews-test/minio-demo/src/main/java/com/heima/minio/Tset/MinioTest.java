package com.heima.minio.Tset;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import java.io.FileInputStream;

public class MinioTest {
    public static void main(String[] args) {

        FileInputStream fileInputStream = null;
        try {

            fileInputStream =  new FileInputStream("D:\\Javacode\\RueJi\\RuJi_Dian_Can\\src\\main\\resources\\backend\\page\\member\\list.html");;

            //1.创建minio链接客户端
            MinioClient minioClient = MinioClient.builder().credentials("minio", "minio123").endpoint("http://192.168.200.130:9000").build();
            //2.上传
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .object("aa.html")//文件名
                    .contentType("text/html")//文件类型
                    .bucket("leadnews")//桶名词  与minio创建的名词一致
                    .stream(fileInputStream, fileInputStream.available(), -1) //文件流
                    .build();
            minioClient.putObject(putObjectArgs);

            //System.out.println("http://192.168.200.130:9000/leadnews/ak47.jpg");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
