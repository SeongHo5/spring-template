package ho.seong.cho.parser;

import com.github.javaparser.ast.CompilationUnit;
import java.nio.file.Path;

record ParsedUnit(Path path, CompilationUnit compilationUnit) {}
