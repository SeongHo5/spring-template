package ho.seong.cho.parser;

import java.nio.file.Path;
import java.util.Set;

public class Main {

  public static void main(String[] args) throws Exception {
    Path sourceRoot = Path.of(args.length > 1 ? args[1] : "src/main/java");

    // Options
    Set<String> targetAnnotationFullNames =
        Set.of(
            "org.springframework.cache.annotation.Cacheable",
            "org.springframework.cache.annotation.CacheEvict",
            "org.springframework.cache.annotation.CachePut");
    boolean removeFromInterface = true;
    boolean addImportIfPossible = true;

    InterfaceAnnotationMover mover = new InterfaceAnnotationMover();
    mover.run(sourceRoot, targetAnnotationFullNames, removeFromInterface, addImportIfPossible);
  }
}
