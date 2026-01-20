package ho.seong.cho.parser;

import static ho.seong.cho.parser.Logger.error;
import static ho.seong.cho.parser.Logger.info;
import static ho.seong.cho.parser.Logger.warn;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

final class InterfaceAnnotationMover {

  // 용어
  // FQN(Fully Qualified Name): 패키지 전체 경로를 포함한 클래스 이름

  void run(
      Path sourceRoot,
      Set<String> targetAnnotationFqns,
      boolean removeFromInterface,
      boolean addImportIfPossible)
      throws Exception {
    info("InterfaceAnnotationMover started");
    info("sourceRoot=" + sourceRoot.toAbsolutePath());
    info("targetAnnotations=" + targetAnnotationFqns);
    info("removeFromInterface=" + removeFromInterface);
    info("addImportIfPossible=" + addImportIfPossible);

    CombinedTypeSolver typeSolver = new CombinedTypeSolver();
    typeSolver.add(new ReflectionTypeSolver());
    typeSolver.add(new JavaParserTypeSolver(sourceRoot));

    ParserConfiguration parserConfig =
        new ParserConfiguration()
            .setCharacterEncoding(StandardCharsets.UTF_8)
            .setSymbolResolver(new JavaSymbolSolver(typeSolver));

    JavaParser parser = new JavaParser(parserConfig);

    // .java 파일을 모두 읽어 파싱
    List<ParsedUnit> units = parseAllJavaFiles(parser, sourceRoot);
    info("Parsed compilation units: " + units.size());

    // 인덱스 Map 생성
    // Key: FQN, Value: 구현 클래스 목록
    Map<String, ClassOrInterfaceDeclaration> typeIndex = new HashMap<>();
    for (ParsedUnit unit : units) {
      unit.compilationUnit()
          .findAll(ClassOrInterfaceDeclaration.class)
          .forEach(
              declaration -> {
                String fqn = resolveTypeFqn(declaration);
                if (fqn != null) {
                  typeIndex.put(fqn, declaration);
                }
              });
    }
    info("Resolved types (FQN) count=" + typeIndex.size());

    // Key: 인터페이스 FQN, Value: 구현 클래스 목록
    Map<String, List<ClassOrInterfaceDeclaration>> implMap = buildImplementationsMap(typeIndex);
    info("Built implementation map for interfaces: " + implMap.keySet());

    for (Map.Entry<String, ClassOrInterfaceDeclaration> entry : typeIndex.entrySet()) {
      ClassOrInterfaceDeclaration declaration = entry.getValue();
      if (!declaration.isInterface()) {
        continue;
      }

      String interfaceFqn = entry.getKey();
      List<ClassOrInterfaceDeclaration> impls = implMap.get(interfaceFqn);
      if (impls == null || impls.isEmpty()) {
        warn("No implementation found for interface: " + interfaceFqn);
        continue;
      }

      info("Processing interface: " + interfaceFqn + " (impl count=" + impls.size() + ")");
      for (MethodDeclaration interfaceMethod : declaration.getMethods()) {
        String interfaceMethodSignature =
            interfaceMethod.getDeclarationAsString(false, false, false);

        List<AnnotationExpr> targetAnnotations =
            findTargetAnnotations(interfaceMethod, targetAnnotationFqns);

        if (targetAnnotations.isEmpty()) {
          continue;
        }

        info(
            "Found target annotations on interface method: "
                + interfaceFqn
                + "#"
                + interfaceMethodSignature);

        for (AnnotationExpr annotation : targetAnnotations) {
          info("\t- annotation=" + resolveAnnotationFqn(annotation, interfaceMethod));
        }

        // 현재 interfaceMethod에 대상 어노테이션이 있음
        // 구현체를 순회하면서, 구현 메서드(동일한 시그니처)를 찾음
        for (ClassOrInterfaceDeclaration impl : impls) {
          String implFqn = resolveTypeFqn(impl);
          Optional<MethodDeclaration> implMethodOpt = findOverridingMethod(impl, interfaceMethod);

          if (implMethodOpt.isEmpty()) {
            warn(
                "No overriding method found in impl: "
                    + implFqn
                    + " for "
                    + interfaceFqn
                    + "#"
                    + interfaceMethodSignature);
            continue;
          }

          // 탐색에 성공했다면, 소스 AST를 가져옴
          MethodDeclaration implMethod = implMethodOpt.get();
          info("\tApplying to implementation: " + implFqn + "#" + interfaceMethodSignature);

          CompilationUnit implCu = implMethod.findCompilationUnit().orElse(null);
          if (implCu == null) {
            error("CompilationUnit missing for " + implFqn + "#" + interfaceMethodSignature);
            continue;
          }

          // 대상 어노테이션을 복사해서 옮겨준다.
          for (AnnotationExpr targetAnnotation : targetAnnotations) {
            String annotationFqn = resolveAnnotationFqn(targetAnnotation, interfaceMethod);

            if (hasSameAnnotation(implMethod, targetAnnotation)) {
              info(
                  "\tAlready present, skip: "
                      + annotationFqn
                      + " on "
                      + implFqn
                      + "#"
                      + interfaceMethodSignature);
              continue;
            }

            AnnotationExpr cloned = targetAnnotation.clone();

            if (addImportIfPossible) {
              maybeAddImportForAnnotation(cloned, interfaceMethod, implCu);
            }

            implMethod.addAnnotation(cloned);
            info(
                "\tAdded annotation: "
                    + annotationFqn
                    + " to "
                    + implFqn
                    + "#"
                    + interfaceMethodSignature);
          }
        }

        if (removeFromInterface) {
          info(
              "Removing target annotations from interface method: "
                  + interfaceFqn
                  + "#"
                  + interfaceMethodSignature);
          removeTargetAnnotations(interfaceMethod, targetAnnotationFqns);
        }
      }
    }

    info("Writing modified source files...");
    for (ParsedUnit u : units) {
      Files.writeString(
          u.path(), LexicalPreservingPrinter.print(u.compilationUnit()), StandardCharsets.UTF_8);
    }
    info("DONE.");
  }

  private static List<ParsedUnit> parseAllJavaFiles(JavaParser parser, Path sourceRoot)
      throws IOException {
    info("Scanning Java files under " + sourceRoot.toAbsolutePath());
    List<Path> javaFiles;
    try (Stream<Path> stream = Files.walk(sourceRoot)) {
      javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
    }

    List<ParsedUnit> units = new ArrayList<>();
    for (Path file : javaFiles) {
      try {
        Optional<CompilationUnit> result = parser.parse(file).getResult();
        if (result.isEmpty()) {
          warn("Parse failed (empty result) | file=" + file.toAbsolutePath());
          continue;
        }

        CompilationUnit cu = result.get();
        LexicalPreservingPrinter.setup(cu);
        units.add(new ParsedUnit(file, cu));

      } catch (Exception e) {
        error("Parse failed | file=" + file.toAbsolutePath(), e);
      }
    }
    return units;
  }

  private static Map<String, List<ClassOrInterfaceDeclaration>> buildImplementationsMap(
      Map<String, ClassOrInterfaceDeclaration> typeIndex) {
    Map<String, List<ClassOrInterfaceDeclaration>> implMap = new HashMap<>();

    for (var entry : typeIndex.entrySet()) {
      ClassOrInterfaceDeclaration declaration = entry.getValue();
      if (declaration.isInterface()) {
        continue;
      }

      // class implements ... / extends ...
      for (ClassOrInterfaceType implemented : declaration.getImplementedTypes()) {
        String ifaceFqn = resolveTypeFqn(implemented);
        if (ifaceFqn == null) {
          continue;
        }

        implMap.computeIfAbsent(ifaceFqn, k -> new ArrayList<>()).add(declaration);
      }

      // 간접 구현(상위 클래스가 implements 하는 경우)은 여기서는 기본적으로 빠짐.
      // 필요하면: 상속 트리 따라가며 implements 전파하는 로직 추가 가능.
    }

    return implMap;
  }

  private static List<AnnotationExpr> findTargetAnnotations(
      MethodDeclaration method, Set<String> targetFqns) {

    List<AnnotationExpr> result = new ArrayList<>();
    for (AnnotationExpr a : method.getAnnotations()) {
      String fqn = resolveAnnotationFqn(a, method);
      if (fqn == null) {
        continue;
      }
      if (targetFqns.contains(fqn)) {
        result.add(a);
      }
    }
    return result;
  }

  private static void removeTargetAnnotations(MethodDeclaration method, Set<String> targetFqns) {
    NodeList<AnnotationExpr> annotationExprs = method.getAnnotations();

    annotationExprs.removeIf(
        a -> {
          String fqn = resolveAnnotationFqn(a, method);
          return fqn != null && targetFqns.contains(fqn);
        });
  }

  private static boolean hasSameAnnotation(MethodDeclaration implMethod, AnnotationExpr anno) {
    String targetSimple = anno.getName().getIdentifier();
    String targetFqn = resolveAnnotationFqn(anno, anno);

    for (AnnotationExpr existing : implMethod.getAnnotations()) {
      if (existing.getName().getIdentifier().equals(targetSimple)) {
        // simple name 같으면 보수적으로 동일로 간주 (충돌 가능성은 낮다고 가정)
        // 필요하면 existing도 FQN resolve 해서 정확히 비교하도록 강화 가능
        if (targetFqn == null) {
          return true;
        }
        String existingFqn = resolveAnnotationFqn(existing, existing);
        if (targetFqn.equals(existingFqn) || existingFqn == null) {
          return true;
        }
      }
    }
    return false;
  }

  private static Optional<MethodDeclaration> findOverridingMethod(
      ClassOrInterfaceDeclaration impl, MethodDeclaration interfaceMethod) {
    String interfaceMethodSignature = interfaceMethod.getDeclarationAsString(false, false, false);

    ResolvedMethodDeclaration resolvedInterfaceMethod;
    try {
      resolvedInterfaceMethod = interfaceMethod.resolve();
    } catch (Exception e) {
      error("Failed to resolve interface method: " + interfaceMethodSignature, e);
      return Optional.empty();
    }

    for (MethodDeclaration candidate : impl.getMethods()) {
      try {
        ResolvedMethodDeclaration resolvedCandidate = candidate.resolve();
        if (sameSignature(resolvedInterfaceMethod, resolvedCandidate)) {
          return Optional.of(candidate);
        }
      } catch (Exception e) {
        error(
            "Failed to resolve candidate method in "
                + resolveTypeFqn(impl)
                + ": "
                + candidate.getDeclarationAsString(false, false, false),
            e);
      }
    }

    return Optional.empty();
  }

  private static boolean sameSignature(ResolvedMethodDeclaration a, ResolvedMethodDeclaration b) {
    if (!a.getName().equals(b.getName())) {
      return false;
    }
    if (a.getNumberOfParams() != b.getNumberOfParams()) {
      return false;
    }

    for (int i = 0; i < a.getNumberOfParams(); i++) {
      String ta = a.getParam(i).getType().describe();
      String tb = b.getParam(i).getType().describe();
      if (!ta.equals(tb)) {
        return false;
      }
    }
    return true;
  }

  private static void maybeAddImportForAnnotation(
      AnnotationExpr annotationExpr,
      MethodDeclaration contextForResolve,
      CompilationUnit targetCu) {
    // @org.springframework.cache.annotation.Cacheable 이렇게 쓰고 있다면, import 문을 넣어줄 필요가 없음
    if (annotationExpr.getName().getQualifier().isPresent()) {
      return;
    }

    String fqn = resolveAnnotationFqn(annotationExpr, contextForResolve);
    if (fqn == null) {
      return;
    }

    String simple = simpleName(fqn);

    // import 충돌 체크: 같은 simple name을 다른 패키지에서 import 했으면 FQN로 바꾸는게 안전
    boolean hasConflict =
        targetCu.getImports().stream().filter(ImportDeclaration::isAsterisk).anyMatch(id -> false);

    for (ImportDeclaration id : targetCu.getImports()) {
      if (id.isAsterisk()) {
        continue;
      }
      String imported = id.getNameAsString();
      if (simpleName(imported).equals(simple) && !imported.equals(fqn)) {
        hasConflict = true;
        break;
      }
    }

    if (hasConflict) {
      // FQN로 바꿔서 안전하게
      annotationExpr.setName(new Name(fqn));
      return;
    }

    // 이미 import 되어있으면 skip
    boolean alreadyImported =
        targetCu.getImports().stream()
            .anyMatch(id -> !id.isAsterisk() && id.getNameAsString().equals(fqn));
    if (alreadyImported) {
      return;
    }

    targetCu.addImport(fqn);
  }

  private static String resolveAnnotationFqn(AnnotationExpr annotationExpr, Node contextNode) {
    // 1) 이미 FQN 형태면 그대로
    if (annotationExpr.getName().getQualifier().isPresent()) {
      return annotationExpr.getNameAsString();
    }

    // 2) SymbolSolver로 resolve 시도
    try {
      // JavaParser에서 AnnotationExpr 직접 resolve가 항상 안정적이진 않아서,
      // 주변 컨텍스트로부터 import/패키지 기반 추론을 섞어준다.
      var cuOpt = contextNode.findCompilationUnit();
      if (cuOpt.isEmpty()) {
        return null;
      }
      CompilationUnit cu = cuOpt.get();

      String simple = annotationExpr.getName().getIdentifier();

      // 명시 import 우선
      for (ImportDeclaration id : cu.getImports()) {
        if (id.isAsterisk()) {
          continue;
        }
        String imported = id.getNameAsString();
        if (simpleName(imported).equals(simple)) {
          return imported;
        }
      }

      return null;
    } catch (Exception e) {
      return null;
    }
  }

  private static String resolveTypeFqn(ClassOrInterfaceDeclaration declaration) {
    try {
      return declaration.resolve().getQualifiedName();
    } catch (Exception e) {
      error("Failed to resolve FQN for type: " + declaration.getNameAsString(), e);
      return null;
    }
  }

  private static String resolveTypeFqn(ClassOrInterfaceType type) {
    try {
      ResolvedReferenceType rrt = type.resolve().asReferenceType();
      return rrt.getQualifiedName();
    } catch (Exception e) {
      error("Failed to resolve FQN for type: " + type.getNameAsString(), e);
      return null;
    }
  }

  // FQN에서 패키지 경로를 떼고 클래스 이름만
  private static String simpleName(String fqn) {
    int idx = fqn.lastIndexOf('.');
    return idx >= 0 ? fqn.substring(idx + 1) : fqn;
  }
}
