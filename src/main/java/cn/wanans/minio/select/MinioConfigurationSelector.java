package cn.wanans.minio.select;

import cn.wanans.minio.config.MinioConfiguration;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author w
 * @date 2022/8/24
 */
public class MinioConfigurationSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{MinioConfigurationSelector.class.getName(), MinioConfiguration.class.getName()};
    }
}
