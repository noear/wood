package org.noear.wood.generator.entity;

import org.noear.wood.DbContext;
import org.noear.wood.generator.entity.block.TableItem;
import org.noear.wood.generator.entity.block.XmlEntityBlock;
import org.noear.wood.generator.entity.block.XmlSourceBlock;
import org.noear.wood.generator.utils.NamingUtils;
import org.noear.wood.generator.utils.StringUtils;
import org.noear.wood.generator.utils.XmlUtils;
import org.noear.wood.wrap.ColumnWrap;
import org.noear.wood.wrap.SqlTypeUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class XmlEntityGenerator {
    public static void generate(File baseDir, File sourceDir) {
        try {
            String path = (baseDir.getAbsolutePath() + "/src/main/resources/wood-generator.xml");
            File file = new File(path);

            if (file.exists() == false) {
                System.err.println("[Wood] No configuration file: wood-generator.xml");
            }

            generate0(file, sourceDir);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private static void generate0(File file, File sourceDir) throws Exception {
        Document doc = XmlUtils.parseDoc(file);

        Node nm = doc.getDocumentElement();

        NodeList nl = nm.getChildNodes();
        for (int i = 0, len = nl.getLength(); i < len; i++) {
            Node n1 = nl.item(i);

            if (n1.getNodeType() == 1) {
                generate1(n1, sourceDir);
            }
        }
    }

    private static void generate1(Node n1, File sourceDir) throws Exception {
        XmlSourceBlock source = XmlParser.getSource(n1);

        if (source == null) {
            return;
        }

        if (StringUtils.isEmpty(source.driverClassName) == false) {
            Class.forName(source.driverClassName);
        }

        DbContext db = new DbContext(source.schema, source.url, source.username, source.password);

        List<TableItem> tableItems = XmlParser.getTables(source, db);

        for (XmlEntityBlock entityBlock : source.entityBlocks) {
            if (entityBlock.code != null) {
                for (TableItem tableItem : tableItems) {
                    generateJavaFile(source, entityBlock, tableItem, sourceDir);
                }
            }
        }
    }


    private static void generateJavaFile(XmlSourceBlock source, XmlEntityBlock entityBlock, TableItem tableItem, File sourceDir) throws Exception {
        //${entityName}
        //${domainName}
        //${tableName}
        //${fields}
        //${fields_public}
        //

        String tableName = tableItem.tableName;
        String domainName = tableItem.domainName;
        String entityName = entityBlock.entityName.replace(Names.sym_domainName, domainName);

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(entityBlock.targetPackage).append(";").append("\n\n");

        String code = entityBlock.code;
        code = code.replace(Names.sym_entityName, entityName);
        code = code.replace(Names.sym_domainName, domainName);
        code = code.replace(Names.sym_tableName, tableName);

        if (code.contains(Names.sym_fields)) {
            String tmp = buildFields(source, tableItem, false);
            code = code.replace(Names.sym_fields, tmp);
        }

        if (code.contains(Names.sym_fields_public)) {
            String tmp = buildFields(source, tableItem, true);
            code = code.replace(Names.sym_fields_public, tmp);
        }

        if (code.contains(Names.sym_fields_getter)) {
            String tmp = buildFieldsGetter(source, tableItem);
            code = code.replace(Names.sym_fields_getter, tmp);
        }

        if (code.contains(Names.sym_fields_setter)) {
            String tmp = buildFieldsSetter(source, tableItem);
            code = code.replace(Names.sym_fields_setter, tmp);
        }


        if (code.contains(" Date ")) {
            sb.append("import java.util.Date;").append("\n");
        }
        if (code.contains(" BigDecimal ")) {
            sb.append("import java.math.BigDecimal;").append("\n");
        }

        if (code.contains("BaseMapper<")) {
            sb.append("import org.noear.wood.BaseMapper;").append("\n");
        }

        if (code.contains("@Db(") || code.contains("@Table(")) {
            sb.append("import org.noear.wood.annotation.*;").append("\n");
        }

        sb.append(code);

        String dir = (sourceDir.getAbsolutePath() + "/" + entityBlock.targetPackage.replace(".", "/") + "/");

        createFile(dir, entityName, sb.toString());
    }

    private static void createFile(String dir, String fileName, String fileContent) throws Exception {
        File dir2 = new File(dir);
        if (dir2.exists() == false) {
            dir2.mkdirs();
        }

        String fileFullName = dir + fileName + ".java";

        File file = new File(fileFullName);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(fileContent);
        }

        System.out.println("[Wood] Generated : " + file.getAbsolutePath());
    }

    private static String buildFields(XmlSourceBlock source, TableItem table, boolean usePublic) {
        StringBuilder sb = new StringBuilder();
        boolean camel = Names.val_camel.equals(source.namingStyle);

        for (ColumnWrap cw : table.tableWrap.getColumns()) {
            buildColumnRemarks(cw, sb);

            if (table.tableWrap.getPks().contains(cw.getName())) {
                sb.append("@PrimaryKey").append("\n");
            }

            if(usePublic){
                sb.append("  public ");
            }else{
                sb.append("  private ");
            }

            sb.append(SqlTypeUtil.getJavaType(cw, source.typeStyle.startsWith("base"))).append(" ");
            if (camel) {
                sb.append(NamingUtils.toCamelString(cw.getName()));
            } else {
                sb.append(cw.getName());
            }

            sb.append(";").append("\n");
        }

        return sb.toString();
    }

    private static String buildFieldsGetter(XmlSourceBlock source, TableItem table) {
        StringBuilder sb = new StringBuilder();
        boolean camel = Names.val_camel.equals(source.namingStyle);

        for (ColumnWrap cw : table.tableWrap.getColumns()) {
            buildColumnRemarks(cw, sb);

            sb.append("  public ").append(SqlTypeUtil.getJavaType(cw, source.typeStyle.startsWith("base"))).append(" get");
            if (camel) {
                sb.append(NamingUtils.toCamelString(cw.getName(), true));
            } else {
                sb.append(NamingUtils.capitalize(cw.getName()));
            }
            sb.append("(){\n");
            sb.append("  return ");
            if (camel) {
                sb.append(NamingUtils.toCamelString(cw.getName()));
            } else {
                sb.append(cw.getName());
            }
            sb.append(";\n");
            sb.append("  }\n");

            sb.append("\n");
        }

        return sb.toString();
    }

    private static String buildFieldsSetter(XmlSourceBlock source, TableItem table) {
        StringBuilder sb = new StringBuilder();
        boolean camel = Names.val_camel.equals(source.namingStyle);

        for (ColumnWrap cw : table.tableWrap.getColumns()) {
            buildColumnRemarks(cw, sb);

            sb.append("  public void set");
            if (camel) {
                sb.append(NamingUtils.toCamelString(cw.getName(), true));
            } else {
                sb.append(NamingUtils.capitalize(cw.getName()));
            }
            sb.append("(").append(SqlTypeUtil.getJavaType(cw, source.typeStyle.startsWith("base"))).append(" val){\n");
            sb.append("  ");
            if (camel) {
                sb.append(NamingUtils.toCamelString(cw.getName()));
            } else {
                sb.append(cw.getName());
            }
            sb.append(" = val;\n");
            sb.append("  }\n");

            sb.append("\n");
        }

        return sb.toString();
    }

    private static void buildColumnRemarks(ColumnWrap cw, StringBuilder sb) {
        if (StringUtils.isEmpty(cw.getRemarks()) == false) {
            String remarks = cw.getRemarks().replace("\r\n"," ").replace("\n", " ");
            sb.append("  /** ").append(remarks).append(" */\n");
        }
    }
}

