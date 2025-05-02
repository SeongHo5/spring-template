package ho.seong.cho.cache.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

@RequiredArgsConstructor
public class GzipJsonRedisSerializer<T> implements RedisSerializer<T> {

  private static final byte[] GZIP_MAGIC_NUMBER = new byte[] {0x1f, (byte) 0x8b};
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final Class<T> targetType;

  @Override
  public byte[] serialize(T value) throws SerializationException {
    if (value == null) {
      return new byte[0];
    }

    try (var baos = new ByteArrayOutputStream();
        var gzipos = new GZIPOutputStream(baos)) {
      MAPPER.writeValue(gzipos, value);
      gzipos.finish();

      return baos.toByteArray();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public T deserialize(byte[] bytes) throws SerializationException {
    if (bytes == null || bytes.length == 0) {
      return null;
    }

    try (var bais = new ByteArrayInputStream(bytes)) {
      if (isGzip(bytes)) {
        return MAPPER.readValue(bais, this.targetType);
      }

      return MAPPER.readValue(bytes, this.targetType);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static boolean isGzip(byte[] bytes) {
    return bytes.length > 2 && bytes[0] == GZIP_MAGIC_NUMBER[0] && bytes[1] == GZIP_MAGIC_NUMBER[1];
  }
}
