package ho.seong.cho.security.crypto;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Column encryption key settings. */
@ConfigurationProperties(prefix = "service.key.encryption")
public record ColumnEncryptionProperties(String currentKeyId, Map<String, String> keys) {

  public ColumnEncryptionProperties {
    keys = keys == null ? Map.of() : Map.copyOf(keys);
  }
}
