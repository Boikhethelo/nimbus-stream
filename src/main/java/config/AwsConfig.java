package config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Builds the AWS S3 client as a Spring-managed singleton.
 * <p>
 * Deliberately holds NO business logic - only client construction. This keeps
 * S3StorageService free to focus purely on "how do we talk to S3 for our use case"
 * <p>
 * No access keys are configured here. On EC2 / ECS this resolves credentials
 * automatically from the instance/task IAM role via the SDK's default credential
 * chain - nothing to store, rotate, or leak. Locally, the AWS CLI's configured
 * profile (`aws configure`) is picked up the same way.
 */

@Configuration //tells Spring this class produces beans, not a component to be used directly.
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3") //class only activates if the storage provider is s3
public class AwsConfig {

    @Value("${aws.region}") // Injects the AWS region from config
    private String region;

    @Bean //builds a low level client used for object operations
    public S3Client s3Client(){
        return S3Client.builder().region(Region.of(region)).build();
    }

    @Bean //used to generate presigned URLs for offloading video streaming bandwidth to S3 directly instead of piping it through Spring Boot.
    public S3Presigner s3Presigner(){
        return S3Presigner.builder().region(Region.of(region)).build();
    }

}
