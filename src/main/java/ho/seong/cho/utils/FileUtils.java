package ho.seong.cho.utils;

import ho.seong.cho.exception.custom.InternalProcessingException;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.Tika;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

public final class FileUtils {

  private FileUtils() {}

  public static String resolveMimeType(@NotNull MultipartFile file) {
    try (InputStream input = file.getInputStream()) {
      Tika tika = new Tika();
      final var mimeType = tika.detect(input);
      return mimeType != null ? mimeType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    } catch (IOException e) {
      throw new InternalProcessingException(e);
    }
  }
}
