package ho.seong.cho.aws.s3;

import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
@RequiredArgsConstructor
public class LocatS3ClientImpl implements LocatS3Client {

  protected static final String DIRECTORY_DELIMETER = "/";

  private static final String ROOT_PATH = "";

  private static final int MAX_OBJECTS_PER_REQUEST = 50;

  private final S3Client s3Client;

  @Value("${service.aws.s3.url}")
  private String s3Url;

  @Value("${service.aws.s3.bucket}")
  private String s3Bucket;

  @Override
  @Cacheable(value = "S3OBJECTS", key = "#directoryPath", unless = "#result.isEmpty()")
  public List<String> getListObjects(String directoryPath) {
    return this.getListObjectsInternal(directoryPath).contents().stream()
        .map(S3Object::key)
        .map(this::buildObjectUrl)
        .toList();
  }

  @Override
  @CacheEvict(value = "S3OBJECTS", key = "#directoryPath", condition = "#result != null")
  public String upload(String directoryPath, MultipartFile file) {
    final String extension = FileUtils.extractExtension(file);
    final String newFileName = System.currentTimeMillis() + extension;
    this.putObjectInternal(newFileName, file);
    return this.buildObjectUrl(newFileName);
  }

  @Override
  public void delete(String key) {
    DeleteObjectRequest request =
        DeleteObjectRequest.builder().bucket(this.s3Bucket).key(key).build();
    this.deleteObjectInternal(request);
  }

  private ListObjectsV2Response getListObjectsInternal(String path) {
    try {
      ListObjectsV2Request request =
          ListObjectsV2Request.builder()
              .bucket(this.s3Bucket)
              .prefix(sanitizePath(path))
              .maxKeys(MAX_OBJECTS_PER_REQUEST)
              .build();
      return this.s3Client.listObjectsV2(request);
    } catch (SdkException e) {
      throw new RuntimeException(e);
    }
  }

  private void putObjectInternal(String fileName, MultipartFile file) {
    try {
      PutObjectRequest request =
          PutObjectRequest.builder().bucket(this.s3Bucket).key(fileName).build();
      this.s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
    } catch (IOException e) {
      throw new RuntimeException(e); // Replace with custom exception
    } catch (SdkException e) {
      throw new RuntimeException(e);
    }
  }

  private void deleteObjectInternal(DeleteObjectRequest request) {
    try {
      this.s3Client.deleteObject(request);
    } catch (SdkException e) {
      throw new RuntimeException(e);
    }
  }

  private String buildObjectUrl(String key) {
    return this.s3Url.concat(key);
  }

  private static String sanitizePath(String path) {
    if (path == null) {
      return ROOT_PATH;
    }
    return path.endsWith(DIRECTORY_DELIMETER) ? path : path.concat(DIRECTORY_DELIMETER);
  }
}
