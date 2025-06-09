package ho.seong.cho.aws.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
@RequiredArgsConstructor
public class AwsConfig {

  private final AwsProperties awsProperties;

  @Bean
  public S3Client s3Client(AwsBasicCredentials basicCredentials) {
    return S3Client.builder()
        .region(Region.of(this.awsProperties.region()))
        .credentialsProvider(StaticCredentialsProvider.create(basicCredentials))
        .build();
  }

  @Bean
  public S3Presigner s3Presigner(AwsBasicCredentials basicCredentials, S3Client s3Client) {
    return S3Presigner.builder()
        .region(Region.of(this.awsProperties.region()))
        .credentialsProvider(StaticCredentialsProvider.create(basicCredentials))
        .s3Client(s3Client)
        .build();
  }

  @Bean
  public SesClient sesClient(AwsBasicCredentials basicCredentials) {
    return SesClient.builder()
        .region(Region.of(this.awsProperties.region()))
        .credentialsProvider(StaticCredentialsProvider.create(basicCredentials))
        .build();
  }

  @Bean
  public SnsClient snsClient(AwsBasicCredentials basicCredentials) {
    return SnsClient.builder()
        .region(Region.of(this.awsProperties.region()))
        .credentialsProvider(StaticCredentialsProvider.create(basicCredentials))
        .build();
  }

  @Bean
  public DynamoDbClient dynamoDbClient(AwsBasicCredentials basicCredentials) {
    return DynamoDbClient.builder()
        .region(Region.of(this.awsProperties.region()))
        .credentialsProvider(StaticCredentialsProvider.create(basicCredentials))
        .build();
  }

  @Bean
  protected AwsBasicCredentials awsBasicCredentials() {
    return AwsBasicCredentials.create(
        this.awsProperties.accessKey(), this.awsProperties.secretKey());
  }
}
