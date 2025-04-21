package ho.seong.cho.aws.s3;

import ho.seong.cho.aws.AbstractAwsClient;
import ho.seong.cho.aws.config.AwsProperties;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class MyS3ClientImpl extends AbstractAwsClient implements MyS3Client {

  /** S3 경로 구분자 */
  static final String DIRECTORY_DELIMETER = "/";

  /** 디렉토리 경로 정규식 */
  static final Pattern DIRECTORY_PATH_PATTERN =
      Pattern.compile("^[a-zA-Z0-9_-]+(?:/[a-zA-Z0-9_-]+){0,10}$");

  /** 요청 당 최대 객체 수 */
  private static final int MAX_OBJECTS_PER_REQUEST = 50;

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;

  public MyS3ClientImpl(AwsProperties awsProperties, S3Client s3Client, S3Presigner s3Presigner) {
    super(awsProperties);
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
  }

  @Override
  @Cacheable(value = "S3OBJECTS", key = "#directoryPath", unless = "#result.isEmpty()")
  public List<String> getListObjects(final String directoryPath) {
    S3Utils.validatePath(directoryPath);
    return this.getListObjectsInternal(directoryPath).contents().stream()
        .map(S3Object::key)
        .map(this::buildObjectUrl)
        .toList();
  }

  @Override
  public String generatePresignedUrl(String key, Duration expiration) {
    var getObjectPresignedRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(expiration)
            .getObjectRequest(r -> r.bucket(this.awsProperties.s3().bucket()).key(key))
            .build();

    return this.s3Presigner.presignGetObject(getObjectPresignedRequest).url().toString();
  }

  @Override
  public boolean exists(String key) {
    try {
      var headRequest =
          HeadObjectRequest.builder().bucket(this.awsProperties.s3().bucket()).key(key).build();
      this.s3Client.headObject(headRequest);
      return true;
    } catch (NoSuchKeyException ex) {
      log.debug("Cannot find object with key: {}/ Reason: {}", key, ex.getMessage());
      return false;
    }
  }

  @Override
  @CacheEvict(value = "S3OBJECTS", key = "#directoryPath", condition = "#result != null")
  public String upload(String directoryPath, String fileName, MultipartFile file) {
    S3Utils.validatePath(directoryPath);
    final String extension = S3Utils.extractExtension(file);
    final String key = directoryPath.concat(DIRECTORY_DELIMETER).concat(fileName).concat(extension);
    this.putObjectInternal(key, file);
    return this.buildObjectUrl(key);
  }

  @Override
  @CacheEvict(value = "S3OBJECTS", key = "#directoryPath", condition = "#result != null")
  public String upload(final String directoryPath, final MultipartFile file) {
    return this.upload(directoryPath, String.valueOf(System.currentTimeMillis()), file);
  }

  @Override
  public void deleteByKey(final String key) {
    var request =
        DeleteObjectRequest.builder().bucket(this.awsProperties.s3().bucket()).key(key).build();
    this.deleteObjectInternal(request);
  }

  @Override
  public void deleteByUrl(String url) {
    this.deleteByKey(S3Utils.extractObjectKey(url));
  }

  private ListObjectsV2Response getListObjectsInternal(final String path) {
    try {
      var request =
          ListObjectsV2Request.builder()
              .bucket(this.awsProperties.s3().bucket())
              .prefix(path)
              .maxKeys(MAX_OBJECTS_PER_REQUEST)
              .build();
      return this.s3Client.listObjectsV2(request);
    } catch (SdkException e) {
      throw new RuntimeException(e);
    }
  }

  private void putObjectInternal(final String key, final MultipartFile file) {
    try {
      var request =
          PutObjectRequest.builder().bucket(this.awsProperties.s3().bucket()).key(key).build();
      this.s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
    } catch (SdkException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void deleteObjectInternal(final DeleteObjectRequest request) {
    try {
      this.s3Client.deleteObject(request);
    } catch (SdkException e) {
      throw new RuntimeException(e);
    }
  }

  private String buildObjectUrl(final String key) {
    return this.awsProperties.s3().url().concat(key);
  }
}
