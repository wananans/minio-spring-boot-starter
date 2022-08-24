package cn.wanans.minio.template;

import cn.wanans.minio.properties.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author w
 * @date 2022/8/5
 */
@Slf4j
public class MinioTemplate {

    private MinioProperties minioProperties;

    private MinioClient minioClient;

    public MinioTemplate(MinioProperties minioProperties, MinioClient minioClient) {
        this.minioProperties = minioProperties;
        this.minioClient = minioClient;
    }

    //===========================操作存储桶===========================


    /**
     * 查看存储bucket是否存在
     *
     * @return 是否存在
     */
    public Boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            log.error("[minio bucketExists({}) error]", bucketName, e);
            throw new RuntimeException(e.getMessage());
        }
    }


    /**
     * 创建存储bucket
     */
    public void bucketCreate(String bucketName) {

        if (bucketExists(bucketName)) {
            throw new RuntimeException(String.format("存储桶%s已存在", bucketName));
        }

        try {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());

        } catch (Exception e) {
            log.error("[minio createBucket({}) error:{}]", bucketName, e.getMessage(), e);
        }
    }


    /**
     * 删除存储bucket
     */
    public void bucketDelete(String bucketName) {
        if (!bucketExists(bucketName)) {
            throw new RuntimeException(String.format("存储桶%s不存在", bucketName));
        }
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            log.error("[minio deleteBucket({}) error]", bucketName, e);
        }
    }


    /**
     * 获取全部bucket
     */
    public List<Bucket> bucketList() {
        try {
            return minioClient.listBuckets();
        } catch (Exception e) {
            log.error("[minio getAllBuckets() error]", e);
            return null;
        }
    }


    //===========================操作文件对象===========================

    /**
     * 判断文件是否存在
     *
     * @param fileName 文件名称
     * @return 是否存在
     */
    public Boolean objectExists(String fileName) {

        boolean exists = true;

        StatObjectArgs build = StatObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .object(fileName)
                .build();

        try {
            minioClient.statObject(build);
        } catch (Exception e) {
            exists = false;
        }

        return exists;
    }


    /**
     * 创建文件夹
     *
     * @param path 路径
     * @return path
     */
    public String createDictionary(String path) {

        if (!path.endsWith("/")) {
            path = path + "/";
        }

        try {
            ObjectWriteResponse objectWriteResponse = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(path)
                            .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .build());

            return objectWriteResponse.object();
        } catch (Exception e) {
            log.error("[minio createDictionary() error]", e);
            return null;
        }
    }


    /**
     * 文件上传
     *
     * @param file 文件
     * @return 文件名称
     */
    public String putObject(MultipartFile file) {
        try {
            return putObject(file.getInputStream(), file.getOriginalFilename(), file.getContentType());
        } catch (IOException e) {
            log.error("[minio upload() error]", e);
            return null;
        }
    }


    /**
     * 文件上传
     *
     * @param inputStream      输入流
     * @param originalFilename 存储的文件名
     * @param contentType      contentType
     * @return 存储的文件名
     */
    public String putObject(InputStream inputStream, String originalFilename, String contentType) {

        try {
            PutObjectArgs.Builder stream = PutObjectArgs
                    .builder()
                    .bucket(minioProperties.getBucketName())
                    .stream(inputStream, inputStream.available(), -1)
                    .object(originalFilename);

            if (null != contentType) {
                stream.contentType(contentType);
            }

            //文件名称相同会覆盖
            ObjectWriteResponse objectWriteResponse = minioClient.putObject(stream.build());

            return objectWriteResponse.object();
        } catch (Exception e) {
            log.error("[minio upload() error]", e);
            return null;
        }
    }


    /**
     * 预览图片
     *
     * @param fileName 文件名称
     * @return 文件URL
     */
    public String preview(String fileName) {
        // 查看文件地址
        GetPresignedObjectUrlArgs build = GetPresignedObjectUrlArgs.builder()
                .bucket(minioProperties.getBucketName())
                .object(fileName)
                .method(Method.GET)
                .expiry(24, TimeUnit.HOURS)
                .build();
        try {
            return minioClient.getPresignedObjectUrl(build);
        } catch (Exception e) {
            log.error("[minio preview() error:{}]", e.getMessage(), e);
            return null;
        }
    }


    /**
     * 文件下载
     *
     * @param fileName 文件名称
     * @param res      response
     */
    public void getObject(String fileName, HttpServletResponse res) {

        GetObjectArgs objectArgs = GetObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .object(fileName)
                .build();
        try (GetObjectResponse response = minioClient.getObject(objectArgs)) {
            byte[] buf = new byte[1024];
            int len;
            try (FastByteArrayOutputStream os = new FastByteArrayOutputStream()) {
                while ((len = response.read(buf)) != -1) {
                    os.write(buf, 0, len);
                }
                os.flush();
                byte[] bytes = os.toByteArray();
                res.setCharacterEncoding("utf-8");
                // 设置强制下载不打开
                // res.setContentType("application/force-download");
                res.addHeader("Content-Disposition", "attachment;fileName=" + fileName);
                try (ServletOutputStream stream = res.getOutputStream()) {
                    stream.write(bytes);
                    stream.flush();
                }
            }
        } catch (Exception e) {
            log.error("[minio download() error:{}]", e.getMessage(), e);
        }
    }

    /**
     * 查看文件对象
     *
     * @return 存储bucket内文件对象信息
     */
    public List<Item> objectList() {

        ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder().bucket(minioProperties.getBucketName()).build();
        Iterable<Result<Item>> results = minioClient.listObjects(listObjectsArgs);

        List<Item> items = new ArrayList<>();
        try {
            for (Result<Item> result : results) {
                items.add(result.get());
            }
            return items;
        } catch (Exception e) {
            log.error("[minio listObjects() error]", e);
            return null;
        }
    }

    /**
     * 删除
     *
     * @param fileName 文件名称
     * @return 是否成功
     */
    public boolean objectDelete(String fileName) {

        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .object(fileName)
                .build();

        try {
            minioClient.removeObject(removeObjectArgs);
            return true;
        } catch (Exception e) {
            log.error("[minio delete({},{}) error]", minioProperties.getBucketName(), fileName, e);
            return false;
        }
    }


    public static String generateFileName(String originalFilename) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

        if (StringUtils.hasText(extension)) {
            return uuid + extension;
        }
        return null;
    }

    private String getUrl(String fileName) {
        String endpoint = minioProperties.getEndpoint();

        if (!endpoint.endsWith("/")) {
            endpoint = endpoint + "/";
        }

        String url = null;

        if (StringUtils.hasText(fileName)) {
            if (!fileName.startsWith("/")) {
                fileName = "/" + fileName;
            }
            url = endpoint + minioProperties.getBucketName() + fileName;
            String substring = url.substring(url.lastIndexOf("/") + 1);
            String encode = null;
            try {
                encode = URLEncoder.encode(substring, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            url = url.substring(0, url.lastIndexOf("/") + 1) + encode;
        }

        return url;
    }
}
