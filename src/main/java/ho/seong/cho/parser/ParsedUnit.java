package ho.seong.cho.parser;

import com.github.javaparser.ast.CompilationUnit;
import java.nio.file.Path;

class ParsedUnit {

  private final Path path;

  private final CompilationUnit compilationUnit;

  ParsedUnit(Path path, CompilationUnit compilationUnit) {
    this.path = path;
    this.compilationUnit = compilationUnit;
  }

  Path path() {
    return path;
  }

  CompilationUnit compilationUnit() {
    return compilationUnit;
  }
}
