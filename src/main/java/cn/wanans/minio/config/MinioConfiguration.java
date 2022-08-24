package cn.wanans.minio.config;

import cn.wanans.minio.properties.MinioProperties;
import cn.wanans.minio.template.MinioTemplate;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author w
 * @date 2022/8/5
 */
@Slf4j
@EnableConfigurationProperties(MinioProperties.class)
@Configuration
public class MinioConfiguration {

    @Autowired
    private MinioProperties minioProperties;


    @ConditionalOnProperty(prefix = "minio", value = {"endpoint", "bucket-name", "access-key", "secret-key"})
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }


    @ConditionalOnProperty(prefix = "minio", value = {"endpoint", "bucket-name", "access-key", "secret-key"})
    @Bean
    public MinioTemplate minioTemplate() {
        MinioTemplate minioTemplate = new MinioTemplate(minioProperties, minioClient());
        String bucketName = minioProperties.getBucketName();

        if (minioProperties.getAutoCreate()) {
            if (minioTemplate.bucketExists(bucketName)) {
                log.info("[默认存储桶已存在:{}]", bucketName);
            } else {
                log.info("[创建默认存储桶:{}]", bucketName);
                minioTemplate.bucketCreate(bucketName);
            }
        }
        return minioTemplate;
    }

}
