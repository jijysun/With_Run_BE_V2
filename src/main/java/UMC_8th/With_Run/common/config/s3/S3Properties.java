package UMC_8th.With_Run.common.config.s3;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cloud.aws.credentials")
@Getter
@Setter
public class S3Properties {
    private String accessKey;
    private String secretKey;
}

