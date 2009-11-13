package org.openl.rules.ui;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TablePropertyDefinitionUtils;
import org.openl.types.IOpenMethod;

public class TableInstanceSorter extends ATableTreeSorter implements IProjectTypes, ITableNodeTypes {

    static public String[] getTableDisplayValue(TableSyntaxNode tsn) {
        return getTableDisplayValue(tsn, 0);
    }

    static public String[] getTableDisplayValue(TableSyntaxNode tsn, int i) {
        return getTableDisplayValue(tsn, i, null);
    }

    static public String[] getTableDisplayValue(TableSyntaxNode tsn, int i, IOpenMethodGroupsDictionary dictionary) {
        ITableProperties tp = tsn.getTableProperties();
        String display = null;
        String name = null;

        if (tp != null) {

            name = tp.getPropertyValueAsString("name");
            display = tp.getPropertyValueAsString("display");
            if (display == null) {
                display = name;
            }
        }

        if (name == null) {
            name = str2name(tsn.getTable().getGridTable().getCell(0, 0).getStringValue(), tsn.getType());
        }

        if (display == null) {
            display = str2display(tsn.getTable().getGridTable().getCell(0, 0).getStringValue(), tsn.getType());
        }

        String sfx = (i < 2 ? "" : "(" + i + ")");
        String dimensionInfo = StringUtils.EMPTY;

        if (dictionary != null && tp != null && tsn.getMember() instanceof IOpenMethod
                && dictionary.contains((IOpenMethod) tsn.getMember())) {

            String[] dimensionalPropertyNames = TablePropertyDefinitionUtils.getDimensionalTableProperties();

            for (String dimensionalPropertyName : dimensionalPropertyNames) {
                String value = tp.getPropertyValueAsString(dimensionalPropertyName);

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

    static String str2display(String src, String type) {
        // String[] tokens = StringTool.tokenize(src, " \n\r(),");
        //
        // if (XLS_DT.equals(type) && tokens.length >= 3)
        // {
        // return tokens[2];
        // }
        //
        // if (XLS_DATA.equals(type) && tokens.length >= 3)
        // {
        // return tokens[2];
        // }
        //
        // if (XLS_TEST_METHOD.equals(type) && tokens.length >= 3)
        // return tokens[2];
        //
        // if (XLS_METHOD.equals(type) && tokens.length >= 3)
        // return tokens[2];

        return src;
    }

    static String str2name(String src, String type) {
        if (src == null) {
            src = "NO NAME";
        } else if (type.equals(XLS_DT) || type.equals(XLS_SPREADSHEET) || type.equals(XLS_TBASIC)
                || type.equals(XLS_COLUMN_MATCH) || type.equals(XLS_DATA) || type.equals(XLS_DATATYPE)
                || type.equals(XLS_METHOD) || type.equals(XLS_TEST_METHOD) || type.equals(XLS_RUN_METHOD)) {
            String[] tokens = StringUtils.split(src.replaceAll("\\(.*\\)", ""));
            src = tokens[tokens.length - 1].trim();
        }
        return src;
    }

    @Override
    public String[] getDisplayValue(Object sorterObject, int i) {
        TableSyntaxNode tsn = (TableSyntaxNode) sorterObject;
        return getTableDisplayValue(tsn, i, getOpenMethodGroupsDictionary());
    }

    @Override
    public String getName() {
        return "Table Instance";
    }

    @Override
    public Object getProblems(Object sorterObject) {
        TableSyntaxNode tsn = (TableSyntaxNode) sorterObject;

        return tsn.getErrors() != null ? tsn.getErrors() : tsn.getValidationResult();
    }

    @Override
    public String getType(Object sorterObject) {
        TableSyntaxNode tsn = (TableSyntaxNode) sorterObject;
        return PT_TABLE + "." + tsn.getType();
    }

    @Override
    public String getUrl(Object sorterObject) {
        TableSyntaxNode tsn = (TableSyntaxNode) sorterObject;
        return tsn.getUri();
    }

    @Override
    public int getWeight(Object sorterObject) {
        return 0;
    }

    @Override
    protected boolean isUnique() {
        return true;
    }

    @Override
    public Object makeSorterObject(TableSyntaxNode tsn) {
        return tsn;
    }

}
