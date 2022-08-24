package cn.wanans.minio.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author w
 * @date 2022/8/5
 */
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioProperties {

    /**
     * Minio服务地址
     */
    private String endpoint;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 访问key
     */
    private String accessKey;

    /**
     * 访问秘钥
     */
    private String secretKey;

    /**
     * 是否自动创建存储桶
     */
    private Boolean autoCreate = false;
}
