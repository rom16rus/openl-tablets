package org.openl.rules.ui;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.datatype.binding.DatatypeNodeBinder;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.validation.properties.dimentional.DispatcherTableBuilder;
import org.openl.types.IOpenMethod;

public class TableSyntaxNodeUtils {

    private static final String DISPLAY_TABLE_PROPERTY_NAME = "display";    

    public static String[] getTableDisplayValue(TableSyntaxNode tableSyntaxNode) {
        
        return getTableDisplayValue(tableSyntaxNode, 0);
    }

    public static String[] getTableDisplayValue(TableSyntaxNode tableSyntaxNode, int i) {
        
        return getTableDisplayValue(tableSyntaxNode, i, null);
    }

    public static String[] getTableDisplayValue(TableSyntaxNode tableSyntaxNode, int i, OverloadedMethodsDictionary dictionary) {
        
        ITableProperties tableProperties = tableSyntaxNode.getTableProperties();
        
        String display = null;
        String name = null;

        if (tableProperties != null) {

            name = tableProperties.getName();
            // FIXME: What a property name 'display'??? there is no such property.
            display = tableProperties.getPropertyValueAsString(DISPLAY_TABLE_PROPERTY_NAME);
        
            if (display == null) {
                display = name;
            }
        }

        if (name == null) {
            name = str2name(tableSyntaxNode.getGridTable().getCell(0, 0).getStringValue(), tableSyntaxNode.getType());
        }

        if (display == null) {
            display = str2display(tableSyntaxNode.getGridTable().getCell(0, 0).getStringValue(), tableSyntaxNode.getType());
        }

        String sfx = (i < 2 ? "" : " (" + i + ")");
        String dimensionInfo = StringUtils.EMPTY;

        if (dictionary != null && tableProperties != null && tableSyntaxNode.getMember() instanceof IOpenMethod
                && dictionary.contains((IOpenMethod) tableSyntaxNode.getMember())) {

            String[] dimensionalPropertyNames = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();

            for (String dimensionalPropertyName : dimensionalPropertyNames) {
                String value = tableProperties.getPropertyValueAsString(dimensionalPropertyName);

                if (!StringUtils.isEmpty(value)) {
                    String propertyInfo = StringUtils.join(new Object[] { dimensionalPropertyName, "=", value });
                    dimensionInfo = StringUtils.join(new Object[] { dimensionInfo,
                            StringUtils.isEmpty(dimensionInfo) ? StringUtils.EMPTY : ", ", propertyInfo });
                }

            }
        }

        if (!StringUtils.isEmpty(dimensionInfo)) {
            sfx = StringUtils.join(new Object[] { sfx, StringUtils.isEmpty(sfx) ? StringUtils.EMPTY : " ", "[",
                    dimensionInfo, "]" });
        }

        return new String[] { name + sfx, display + sfx, display + sfx };
    }

    private static String str2display(String src, String type) {

        return src;
    }

    public static String str2name(String src, String type) {
        if (src == null) {
            src = "NO NAME";
        } else if (type.equals(XlsNodeTypes.XLS_DATATYPE.toString())) {
            String[] tokens = StringUtils.split(src.replaceAll("\\(.*\\)", ""));
            src = tokens[DatatypeNodeBinder.TYPE_INDEX].trim();
        } else if (type.equals(XlsNodeTypes.XLS_DT.toString()) || type.equals(XlsNodeTypes.XLS_SPREADSHEET.toString())
                || type.equals(XlsNodeTypes.XLS_TBASIC.toString()) || type.equals(XlsNodeTypes.XLS_COLUMN_MATCH.toString())
                || type.equals(XlsNodeTypes.XLS_DATA.toString()) || type.equals(XlsNodeTypes.XLS_DATATYPE.toString())
                || type.equals(XlsNodeTypes.XLS_METHOD.toString()) || type.equals(XlsNodeTypes.XLS_TEST_METHOD.toString())
                || type.equals(XlsNodeTypes.XLS_RUN_METHOD.toString())) {

            String[] tokens = StringUtils.split(src.replaceAll("\\(.*\\)", ""));
            src = tokens[tokens.length - 1].trim();
        }
        return src;
    }

    /**
     * @param tsn TableSyntaxNode to check.
     * @return <code>true</code> if this table is internal service table, that was auto generated. 
     */
    public static boolean isAutoGeneratedTable(TableSyntaxNode tsn) {
        if (tsn.getMember() instanceof IOpenMethod) {
            if (((IOpenMethod) tsn.getMember()).getName().startsWith(DispatcherTableBuilder.DEFAULT_DISPATCHER_TABLE_NAME)) {
                return true;// This is dimension properties gap/overlap
                            // validation table
            }
        }
        return false;
    }
}
