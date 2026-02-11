package ho.seong.cho.parser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public final class MybatisCrudCodegenerator {

  public static void main(String[] args) throws Exception {
    Settings s = Settings.create();

    try (Connection conn =
        DriverManager.getConnection(
            "jdbc:mariadb://127.0.0.1:3306/schema", "root", "yourpassword")) {
      conn.setReadOnly(true);
      String catalog =
          (s.schema() == null || s.schema().isBlank()) ? conn.getCatalog() : s.schema();
      if (catalog == null || catalog.isBlank()) {
        throw new IllegalStateException("schema(database)를 알 수 없음.");
      }

      DatabaseMetaData md = conn.getMetaData();

      List<TableMeta> tables = loadTables(md, catalog);
      for (TableMeta t : tables) {
        generateForTable(s, t);
      }

      System.out.println("[codegen] done. tables=" + tables.size());
    }
  }

  // ----------------------------
  // Settings
  // ----------------------------
  private record Settings(
      boolean enabled,
      boolean overwrite,
      String schema,
      String domainPackage,
      String mapperPackage,
      String mapperBasePackage,
      Path domainDir,
      Path mapperDir,
      Path mapperXmlDir) {

    static Settings create() {
      return new Settings(
          true,
          false,
          "pntbiz_beacon",
          "io.indoorplus.core.domain",
          "io.indoorplus.core.infrastructure.persistence.mybatis.mapper",
          "io.indoorplus.core.infrastructure.persistence.mybatis",
          Path.of("base/core/src/main/java"),
          Path.of("base/core/src/main/java"),
          Path.of("base/core/src/main/resources/mybatis/mapper"));
    }
  }

  // ----------------------------
  // Metadata Model
  // ----------------------------
  private record ColumnMeta(
      String name,
      int jdbcTypeCode,
      String typeName,
      int size,
      int scale,
      boolean nullable,
      boolean autoIncrement,
      int ordinalPosition,
      boolean primaryKey,
      int pkSeq,
      String comment) {}

  private record TableMeta(
      String tableName, String domainName, List<ColumnMeta> columns, List<ColumnMeta> pkColumns) {
    boolean hasSinglePk() {
      return pkColumns.size() == 1;
    }

    boolean hasCompositePkOrNoPk() {
      return pkColumns.size() != 1;
    }
  }

  private static List<TableMeta> loadTables(DatabaseMetaData md, String catalog) throws Exception {
    List<String> tableNames = new ArrayList<>();

    try (ResultSet rs = md.getTables(catalog, null, "%", new String[] {"TABLE"})) {
      while (rs.next()) {
        String name = rs.getString("TABLE_NAME");
        if (name == null || name.isBlank()) {
          continue;
        }
        tableNames.add(name);
      }
    }

    tableNames.sort(String::compareToIgnoreCase);

    List<TableMeta> tables = new ArrayList<>();
    for (String tableName : tableNames) {
      tables.add(loadTableMeta(md, catalog, tableName));
    }
    return tables;
  }

  private static TableMeta loadTableMeta(DatabaseMetaData md, String catalog, String tableName)
      throws Exception {
    Map<String, Integer> pkSeqMap = new HashMap<>();
    try (ResultSet rs = md.getPrimaryKeys(catalog, null, tableName)) {
      while (rs.next()) {
        String col = rs.getString("COLUMN_NAME");
        int seq = rs.getInt("KEY_SEQ");
        if (col != null) pkSeqMap.put(col, seq);
      }
    }

    List<ColumnMeta> cols = new ArrayList<>();
    try (ResultSet rs = md.getColumns(catalog, null, tableName, "%")) {
      while (rs.next()) {
        String colName = rs.getString("COLUMN_NAME");
        int dataType = rs.getInt("DATA_TYPE");
        String typeName = rs.getString("TYPE_NAME");
        int size = rs.getInt("COLUMN_SIZE");
        int scale = rs.getInt("DECIMAL_DIGITS");
        String isNullable = rs.getString("IS_NULLABLE"); // YES/NO
        String isAuto = rs.getString("IS_AUTOINCREMENT"); // YES/NO/"" (driver dependent)
        int ordinal = rs.getInt("ORDINAL_POSITION");
        String comment = rs.getString("REMARKS");

        boolean nullable = "YES".equalsIgnoreCase(isNullable);
        boolean autoInc = "YES".equalsIgnoreCase(isAuto);

        int pkSeq = pkSeqMap.getOrDefault(colName, 0);
        boolean pk = pkSeq > 0;

        cols.add(
            new ColumnMeta(
                colName, dataType, typeName, size, scale, nullable, autoInc, ordinal, pk, pkSeq,
                comment));
      }
    }

    cols.sort(Comparator.comparingInt(ColumnMeta::ordinalPosition));

    List<ColumnMeta> pkCols =
        cols.stream()
            .filter(ColumnMeta::primaryKey)
            .sorted(Comparator.comparingInt(ColumnMeta::pkSeq))
            .toList();

    String domainName = toDomainName(tableName);

    return new TableMeta(tableName, domainName, cols, pkCols);
  }

  private static String toDomainName(String tableName) {
    String raw = tableName;
    if (raw.toUpperCase(Locale.ROOT).startsWith("TB_")) {
      raw = raw.substring(3);
    }
    String[] parts = raw.split("_");
    StringBuilder sb = new StringBuilder();
    for (String p : parts) {
      if (p.isBlank()) continue;
      String lower = p.toLowerCase(Locale.ROOT);
      sb.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
    }
    return sb.toString();
  }

  // ----------------------------
  // Generate per table
  // ----------------------------
  private static void generateForTable(Settings s, TableMeta t) throws Exception {
    // 1) Domain
    Path domainFile = toJavaFilePath(s.domainDir(), s.domainPackage(), t.domainName());
    writeIfAllowed(domainFile, s.overwrite(), () -> generateDomainJava(s, t));
    // 2) Mapper Interface
    String mapperName = t.domainName() + "Mapper";
    Path mapperFile = toJavaFilePath(s.mapperDir(), s.mapperPackage(), mapperName);
    writeIfAllowed(mapperFile, s.overwrite(), () -> generateMapperInterfaceJava(s, t, mapperName));
    // 3) Mapper XML
    Path mapperXmlFile = s.mapperXmlDir().resolve(mapperName + ".xml");
    writeIfAllowed(mapperXmlFile, s.overwrite(), () -> generateMapperXml(s, t, mapperName));

    System.out.println("[codegen] " + t.tableName() + " -> " + t.domainName());
  }

  private static Path toJavaFilePath(Path baseJavaDir, String pkg, String className) {
    String rel = pkg.replace('.', '/');
    return baseJavaDir.resolve(rel).resolve(className + ".java");
  }

  private interface WriterJob {
    void write() throws Exception;
  }

  private static void writeIfAllowed(Path file, boolean overwrite, WriterJob job) throws Exception {
    Files.createDirectories(Objects.requireNonNull(file.getParent()));
    if (Files.exists(file) && !overwrite) {
      return;
    }
    job.write();
  }

  // ----------------------------
  // Domain Java (POJO)
  // ----------------------------
  private static void generateDomainJava(Settings s, TableMeta t) throws Exception {
    CompilationUnit cu = new CompilationUnit();
    cu.setPackageDeclaration(s.domainPackage());

    // imports
    cu.addImport("lombok.AccessLevel");
    cu.addImport("lombok.Getter");
    cu.addImport("lombok.NoArgsConstructor");
    cu.addImport("lombok.AllArgsConstructor");

    ClassOrInterfaceDeclaration clazz = cu.addClass(t.domainName(), Modifier.Keyword.PUBLIC);

    clazz.setJavadocComment(t.tableName());

    // Lombok annotations
    clazz.addAnnotation("Getter");

    // MyBatis/프레임워크용 기본 생성자
    clazz.addAnnotation(
        new NormalAnnotationExpr(
            new Name("NoArgsConstructor"),
            NodeList.nodeList(
                new MemberValuePair(
                    "access", new FieldAccessExpr(new NameExpr("AccessLevel"), "PROTECTED")))));

    // constructor 기반 매핑(Setter 없이)을 위해 전체 생성자 생성
    clazz.addAnnotation("AllArgsConstructor");

    // fields
    for (ColumnMeta c : t.columns()) {
      var field = clazz.addField(toJavaType(c), c.name(), Modifier.Keyword.PRIVATE);

      if (c.comment() != null && !c.comment().isBlank()) {
        field.setJavadocComment(c.comment());
      }
    }

    Path file = toJavaFilePath(s.domainDir(), s.domainPackage(), t.domainName());
    try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
      w.write(cu.toString());
    }
  }

  // ----------------------------
  // Mapper Interface Java
  // ----------------------------
  private static void generateMapperInterfaceJava(Settings s, TableMeta t, String mapperName)
      throws Exception {
    CompilationUnit cu = new CompilationUnit();
    cu.setPackageDeclaration(s.mapperPackage());

    cu.addImport("org.apache.ibatis.annotations.Mapper");
    cu.addImport("org.apache.ibatis.annotations.Param");
    cu.addImport("java.util.List");
    cu.addImport("java.util.Optional");
    cu.addImport(s.domainPackage() + "." + t.domainName());

    // base interfaces
    cu.addImport(s.mapperBasePackage() + ".EntityMapper");
    cu.addImport(s.mapperBasePackage() + ".IdEntityMapper");

    ClassOrInterfaceDeclaration itf = cu.addInterface(mapperName, Modifier.Keyword.PUBLIC);
    itf.addAnnotation("Mapper");

    String domain = t.domainName();
    String idType = t.hasSinglePk() ? toJavaType(t.pkColumns().get(0)) : null;

    // PK 상황별 extends 전략
    if (t.hasSinglePk()) {
      // extends CrudMapper<Domain, ID>
      itf.addExtendedType("IdEntityMapper<" + domain + ", " + idType + ">");
    } else {
      // extends BaseMapper<Domain>
      itf.addExtendedType("EntityMapper<" + domain + ">");

      // 복합 PK면 update는 제공
      if (!t.pkColumns().isEmpty()) {
        MethodDeclaration m = itf.addMethod("update");
        m.setType("int");
        m.setBody(null);
        m.setModifiers();
        m.addParameter(domain, "entity");
      }
      // PK 없으면 update 생성 X
    }

    Path file = toJavaFilePath(s.mapperDir(), s.mapperPackage(), mapperName);
    try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
      w.write(cu.toString());
    }
  }

  // ----------------------------
  // Mapper XML
  // ----------------------------
  private static void generateMapperXml(Settings s, TableMeta t, String mapperName)
      throws Exception {
    String namespace = s.mapperPackage() + "." + mapperName;
    String domainFqcn = s.domainPackage() + "." + t.domainName();
    String resultMapId = t.domainName() + "ResultMap";

    Document doc =
        DocumentHelper.createDocument()
            .addDocType(
                "mapper",
                "-//mybatis.org//DTD Mapper 3.0//EN",
                "http://mybatis.org/dtd/mybatis-3-mapper.dtd");

    Element mapper = doc.addElement("mapper");
    mapper.addAttribute("namespace", namespace);

    // ResultMap
    Element rm = mapper.addElement("resultMap");
    rm.addAttribute("id", resultMapId);
    rm.addAttribute("type", domainFqcn);

    Element ctor = rm.addElement("constructor");

    for (ColumnMeta c : t.pkColumns()) {
      Element e = ctor.addElement("idArg");
      e.addAttribute("column", c.name());
      e.addAttribute("javaType", toMybatisJavaTypeFqcn(c));
      e.addAttribute("jdbcType", jdbcTypeName(c.jdbcTypeCode(), c.typeName()));
    }

    for (ColumnMeta c : t.columns()) {
      if (c.primaryKey()) {
        continue;
      }

      Element e = ctor.addElement("arg");
      e.addAttribute("column", c.name());
      e.addAttribute("javaType", toMybatisJavaTypeFqcn(c));
      e.addAttribute("jdbcType", jdbcTypeName(c.jdbcTypeCode(), c.typeName()));
    }

    // ----------------------------
    // columns
    // ----------------------------
    List<ColumnMeta> insertCols =
        t.columns().stream()
            .filter(c -> !(t.hasSinglePk() && c.primaryKey() && c.autoIncrement()))
            .toList();

    List<ColumnMeta> setCols = t.columns().stream().filter(c -> !c.primaryKey()).toList();

    // ----------------------------
    // save (insert)
    // ----------------------------
    Element insert = mapper.addElement("insert");
    insert.addAttribute("id", "save");
    insert.addAttribute("parameterType", domainFqcn);

    if (t.hasSinglePk() && t.pkColumns().get(0).autoIncrement()) {
      ColumnMeta pk = t.pkColumns().get(0);
      insert.addAttribute("useGeneratedKeys", "true");
      insert.addAttribute("keyProperty", pk.name());
      insert.addAttribute("keyColumn", pk.name());
    }

    insert.setText("\n" + indentBlock(formatInsertSql(t.tableName(), insertCols, null), 2) + "\n");

    // ----------------------------
    // saveAll (batch insert)
    // ----------------------------
    Element insertAll = mapper.addElement("insert");
    insertAll.addAttribute("id", "saveAll");
    insertAll.addAttribute("parameterType", "map");

    insertAll.addText(
        "\n" + indentBlock(formatInsertHeaderSql(t.tableName(), insertCols), 2) + "\n");

    Element foreach = insertAll.addElement("foreach");
    foreach.addAttribute("collection", "entities");
    foreach.addAttribute("item", "e");
    foreach.addAttribute("separator", ",");

    foreach.addText("\n" + indentBlock(formatValuesTuple(insertCols, "e"), 2) + "\n");

    // ----------------------------
    // update (PK 있을 때만)
    // ----------------------------
    if (!t.pkColumns().isEmpty() && !setCols.isEmpty()) {
      Element upd = mapper.addElement("update");
      upd.addAttribute("id", "update");
      upd.addAttribute("parameterType", domainFqcn);

      upd.addText("\n" + indentBlock(formatUpdateHeaderSql(t.tableName()), 2) + "\n");
      upd.addText("\n" + indentBlock(formatUpdateSetSql(setCols, null), 2) + "\n");
      upd.addText("\n" + indentBlock(formatWhereByPkSql(t.pkColumns(), null), 2) + "\n");
    }

    // ----------------------------
    // findById / existsById / deleteById / deleteAllById : 단일 PK만
    // ----------------------------
    if (t.hasSinglePk()) {
      ColumnMeta pk = t.pkColumns().get(0);
      String idParamType = toMybatisParamType(pk);

      // findById
      Element sel = mapper.addElement("select");
      sel.addAttribute("id", "findById");
      sel.addAttribute("parameterType", idParamType);
      sel.addAttribute("resultMap", resultMapId);
      sel.setText("\n" + indentBlock(formatSelectByIdSql(t.tableName(), pk.name()), 2) + "\n");

      // existsById
      Element ex = mapper.addElement("select");
      ex.addAttribute("id", "existsById");
      ex.addAttribute("parameterType", idParamType);
      ex.addAttribute("resultType", "boolean");
      ex.setText("\n" + indentBlock(formatExistsByIdSql(t.tableName(), pk.name()), 2) + "\n");

      // deleteById
      Element del = mapper.addElement("delete");
      del.addAttribute("id", "deleteById");
      del.addAttribute("parameterType", idParamType);
      del.setText("\n" + indentBlock(formatDeleteByIdSql(t.tableName(), pk.name()), 2) + "\n");

      // deleteAllById
      Element delAll = mapper.addElement("delete");
      delAll.addAttribute("id", "deleteAllById");
      delAll.addAttribute("parameterType", "map");

      delAll.addText(
          "\n" + indentBlock(formatDeleteAllHeaderSql(t.tableName(), pk.name()), 2) + "\n");

      Element inForeach = delAll.addElement("foreach");
      inForeach.addAttribute("collection", "ids");
      inForeach.addAttribute("item", "id");
      inForeach.addAttribute("open", "(");
      inForeach.addAttribute("separator", ",");
      inForeach.addAttribute("close", ")");

      // 값 목록은 한 줄씩 두면 보기 좋고, XML pretty가 개행 유지해줌
      inForeach.addText("\n" + indent(6) + "#{id}\n");
    }

    // ----------------------------
    // findAll
    // ----------------------------
    Element findAll = mapper.addElement("select");
    findAll.addAttribute("id", "findAll");
    findAll.addAttribute("resultMap", resultMapId);
    findAll.setText("\n" + indentBlock(formatSelectAllSql(t.tableName()), 2) + "\n");

    // ----------------------------
    // count
    // ----------------------------
    Element count = mapper.addElement("select");
    count.addAttribute("id", "count");
    count.addAttribute("resultType", "long");
    count.setText("\n" + indentBlock(formatCountSql(t.tableName()), 2) + "\n");

    // ----------------------------
    // write
    // ----------------------------
    Files.createDirectories(s.mapperXmlDir());
    Path file = s.mapperXmlDir().resolve(mapperName + ".xml");

    OutputFormat format = OutputFormat.createPrettyPrint();
    format.setEncoding("UTF-8");
    format.setIndent(true);
    format.setIndent("  ");
    format.setNewlines(true);
    format.setLineSeparator("\n");
    format.setNewLineAfterDeclaration(false);
    format.setSuppressDeclaration(false);
    format.setExpandEmptyElements(false);

    format.setTrimText(false);
    format.setPadText(false);

    try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
      XMLWriter xw = new XMLWriter(bw, format);
      xw.write(doc);
    }
  }

  private static String indentBlock(String sql, int level) {
    String pad = indent(level);
    return sql.lines()
        .map(line -> line.isBlank() ? line : pad + line)
        .reduce((a, b) -> a + "\n" + b)
        .orElse("");
  }

  private static String joinWhereByPk(List<ColumnMeta> pkCols, String itemPrefix) {
    // `pk1` = #{pk1} AND `pk2` = #{pk2}
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < pkCols.size(); i++) {
      if (i > 0) sb.append(" AND ");
      ColumnMeta c = pkCols.get(i);
      String prop = (itemPrefix == null) ? c.name() : (itemPrefix + "." + c.name());
      sb.append(q(c.name())).append(" = #{").append(prop).append("}");
    }
    return sb.toString();
  }

  private static String q(String identifier) {
    return "`" + identifier + "`";
  }

  // ----------------------------
  // SQL formatting helpers (NO CDATA)
  // ----------------------------
  private static String indent(int n) {
    return "  ".repeat(Math.max(0, n));
  }

  private static String formatInsertSql(
      String tableName, List<ColumnMeta> cols, String itemPrefix) {
    // INSERT INTO `t`
    //             (
    //                       `c1`,
    //                       ...
    //             )
    //             VALUES
    //             (
    //                       #{c1},
    //                       ...
    //             )
    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO ").append(q(tableName)).append("\n");
    sb.append(indent(4)).append("(\n");
    sb.append(formatColumnLines(cols, 8));
    sb.append(indent(4)).append(")\n");
    sb.append(indent(4)).append("VALUES\n");
    sb.append(indent(4)).append("(\n");
    sb.append(formatValueLines(cols, itemPrefix, 8));
    sb.append(indent(4)).append(")");
    return sb.toString();
  }

  private static String formatInsertHeaderSql(String tableName, List<ColumnMeta> cols) {
    // batch insert header (VALUES 뒤는 foreach가 이어짐)
    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO ").append(q(tableName)).append("\n");
    sb.append(indent(4)).append("(\n");
    sb.append(formatColumnLines(cols, 8));
    sb.append(indent(4)).append(")\n");
    sb.append(indent(4)).append("VALUES");
    return sb.toString();
  }

  private static String formatValuesTuple(List<ColumnMeta> cols, String itemPrefix) {
    // foreach 내부 한 tuple
    StringBuilder sb = new StringBuilder();
    sb.append(indent(6)).append("(\n");
    sb.append(formatValueLines(cols, itemPrefix, 8));
    sb.append(indent(6)).append(")");
    return sb.toString();
  }

  private static String formatUpdateHeaderSql(String tableName) {
    return "UPDATE " + q(tableName);
  }

  private static String formatUpdateSetSql(List<ColumnMeta> setCols, String itemPrefix) {
    // SET
    //             `col` = #{col},
    //             ...
    StringBuilder sb = new StringBuilder();
    sb.append("SET\n");

    for (int i = 0; i < setCols.size(); i++) {
      ColumnMeta c = setCols.get(i);
      String prop = (itemPrefix == null) ? c.name() : (itemPrefix + "." + c.name());

      sb.append(indent(2)).append(q(c.name())).append(" = #{").append(prop).append("}");

      if (i < setCols.size() - 1) sb.append(",");
      sb.append("\n");
    }

    // 마지막에 불필요한 개행 하나 줄이고 싶으면 아래 라인 주석 해제
    // return sb.toString().stripTrailing();
    return sb.toString().trim(); // 혹은 stripTrailing()
  }

  private static String formatWhereByPkSql(List<ColumnMeta> pkCols, String itemPrefix) {
    return "WHERE " + joinWhereByPk(pkCols, itemPrefix);
  }

  private static String formatSelectByIdSql(String tableName, String pkColumn) {
    return "" + "SELECT *\n" + "FROM " + q(tableName) + "\n" + "WHERE " + q(pkColumn) + " = #{id}";
  }

  private static String formatExistsByIdSql(String tableName, String pkColumn) {
    // 보기 좋게 EXISTS 블록 줄바꿈 고정
    return ""
        + "SELECT EXISTS(\n"
        + indent(2)
        + "SELECT 1\n"
        + indent(2)
        + "FROM "
        + q(tableName)
        + "\n"
        + indent(2)
        + "WHERE "
        + q(pkColumn)
        + " = #{id}\n"
        + ")";
  }

  private static String formatDeleteByIdSql(String tableName, String pkColumn) {
    return "" + "DELETE FROM " + q(tableName) + "\n" + "WHERE " + q(pkColumn) + " = #{id}";
  }

  private static String formatDeleteAllHeaderSql(String tableName, String pkColumn) {
    return "" + "DELETE FROM " + q(tableName) + "\n" + "WHERE " + q(pkColumn) + " IN";
  }

  private static String formatSelectAllSql(String tableName) {
    return "" + "SELECT *\n" + "FROM " + q(tableName);
  }

  private static String formatCountSql(String tableName) {
    return "SELECT COUNT(1)\nFROM " + q(tableName);
  }

  private static String formatColumnLines(List<ColumnMeta> cols, int indentLevel) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < cols.size(); i++) {
      sb.append(indent(indentLevel)).append(q(cols.get(i).name()));
      if (i < cols.size() - 1) sb.append(",");
      sb.append("\n");
    }
    return sb.toString();
  }

  private static String formatValueLines(
      List<ColumnMeta> cols, String itemPrefix, int indentLevel) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < cols.size(); i++) {
      ColumnMeta c = cols.get(i);
      String prop = (itemPrefix == null) ? c.name() : (itemPrefix + "." + c.name());
      sb.append(indent(indentLevel)).append("#{").append(prop).append("}");
      if (i < cols.size() - 1) sb.append(",");
      sb.append("\n");
    }
    return sb.toString();
  }

  // ----------------------------
  // Type mapping
  // ----------------------------
  private static String toMybatisJavaTypeFqcn(ColumnMeta c) {
    String jt = toJavaType(c);
    return switch (jt) {
      case "String" -> "java.lang.String";
      case "Integer" -> "java.lang.Integer";
      case "Long" -> "java.lang.Long";
      case "Boolean" -> "java.lang.Boolean";
      case "Float" -> "java.lang.Float";
      case "Double" -> "java.lang.Double";
      case "BigDecimal" -> "java.math.BigDecimal";
      case "LocalDate" -> "java.time.LocalDate";
      case "LocalTime" -> "java.time.LocalTime";
      case "LocalDateTime" -> "java.time.LocalDateTime";
      default -> jt; // byte[] 등
    };
  }

  private static String toJavaType(ColumnMeta c) {
    // "컬럼명은 camelCase" 가정. 타입은 전부 wrapper로 (MyBatis 매핑 안정성)
    int t = c.jdbcTypeCode();
    String typeName = c.typeName() == null ? "" : c.typeName().toUpperCase(Locale.ROOT);

    // tinyint(1) -> Boolean (MySQL/MariaDB 관행)
    if (t == Types.TINYINT && c.size() == 1) return "Boolean";
    if (typeName.contains("BOOL") || typeName.contains("BOOLEAN")) return "Boolean";

    return switch (t) {
      case Types.BIT, Types.BOOLEAN -> "Boolean";
      case Types.TINYINT, Types.SMALLINT, Types.INTEGER -> "Integer";
      case Types.BIGINT -> "Long";
      case Types.FLOAT, Types.REAL -> "Float";
      case Types.DOUBLE -> "Double";
      case Types.NUMERIC, Types.DECIMAL -> "BigDecimal";
      case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.CLOB -> "String";
      case Types.DATE -> "LocalDate";
      case Types.TIME -> "LocalTime";
      case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> "LocalDateTime";
      case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> "byte[]";
      default -> "String"; // JSON, ENUM, 기타는 String으로 보수적 처리
    };
  }

  private static String toMybatisParamType(ColumnMeta c) {
    String jt = toJavaType(c);
    return switch (jt) {
      case "String" -> "java.lang.String";
      case "Integer" -> "java.lang.Integer";
      case "Long" -> "java.lang.Long";
      case "Boolean" -> "java.lang.Boolean";
      case "Float" -> "java.lang.Float";
      case "Double" -> "java.lang.Double";
      case "BigDecimal" -> "java.math.BigDecimal";
      case "LocalDate" -> "java.time.LocalDate";
      case "LocalTime" -> "java.time.LocalTime";
      case "LocalDateTime" -> "java.time.LocalDateTime";
      default -> jt; // byte[] 등
    };
  }

  private static String jdbcTypeName(int jdbcTypeCode, String typeName) {
    try {
      return JDBCType.valueOf(jdbcTypeCode).getName();
    } catch (Exception ignored) {
      // driver/metadata 예외 대응: TYPE_NAME fallback
      if (typeName == null || typeName.isBlank()) return "VARCHAR";
      String up = typeName.toUpperCase(Locale.ROOT);
      // MyBatis JDBCType enum에 맞춰 대충 정규화
      if (up.contains("DATETIME")) return "TIMESTAMP";
      if (up.contains("TEXT")) return "LONGVARCHAR";
      if (up.contains("JSON")) return "VARCHAR";
      return up.replace(' ', '_');
    }
  }

  private static String capitalize(String s) {
    if (s == null || s.isEmpty()) return s;
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}
