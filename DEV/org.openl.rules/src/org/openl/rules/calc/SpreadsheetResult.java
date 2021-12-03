package org.openl.rules.calc;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.openl.binding.impl.AllowOnlyStrictFieldMatchType;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;
import org.openl.types.IOpenField;
import org.openl.types.java.CustomJavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.CollectionUtils;
import org.slf4j.LoggerFactory;

/**
 * Serializable bean that handles result of spreadsheet calculation.
 */
@XmlRootElement
@CustomJavaOpenClass(type = SpreadsheetResultOpenClass.class, variableInContextFinder = SpreadsheetResultRootDictionaryContext.class, normalize = true)
@AllowOnlyStrictFieldMatchType
public class SpreadsheetResult implements Serializable {

    private static final int MAX_WIDTH = 4;
    private static final int MAX_HEIGHT = 10;
    private static final int MAX_DEPTH = 2;
    private static final int MAX_VALUE_LENGTH = 10 * 1024;

    Object[][] results;
    String[] columnNames;
    String[] rowNames;
    transient String[] rowNamesForResultModel;
    transient String[] columnNamesForResultModel;
    transient Map<String, Point> fieldsCoordinates;

    transient boolean tableStructureDetails;

    /**
     * logical representation of calculated spreadsheet table it is needed for web studio to display results
     */
    private transient ILogicalTable logicalTable;

    /**
     * Spreadsheet open class. This field is used for output bean generation.
     */
    private transient CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass;

    public SpreadsheetResult() {
    }

    public SpreadsheetResult(Object[][] results, String[] rowNames, String[] columnNames) {
        this(results, rowNames, columnNames, new String[rowNames.length], new String[columnNames.length], null);
        initFieldsCoordinates();
    }

    public SpreadsheetResult(Object[][] results,
            String[] rowNames,
            String[] columnNames,
            String[] rowNamesForResultModel,
            String[] columnNamesForResultModel,
            Map<String, Point> fieldsCoordinates) {
        this.rowNames = Objects.requireNonNull(rowNames);
        this.columnNames = Objects.requireNonNull(columnNames);
        this.rowNamesForResultModel = Objects.requireNonNull(rowNamesForResultModel);
        this.columnNamesForResultModel = Objects.requireNonNull(columnNamesForResultModel);
        if (rowNames.length != rowNamesForResultModel.length) {
            throw new IllegalArgumentException(
                "The length of rowNames is not equal to the length of rowNamesForResultModel.");
        }
        if (columnNames.length != columnNamesForResultModel.length) {
            throw new IllegalArgumentException(
                "The length of columnNames is not equal to the length of columnNamesForResultModel.");
        }
        this.results = results;

        this.fieldsCoordinates = fieldsCoordinates;
    }

    public SpreadsheetResult(SpreadsheetResult spr) {
        this(spr.results,
            spr.rowNames,
            spr.columnNames,
            spr.rowNamesForResultModel,
            spr.columnNamesForResultModel,
            spr.fieldsCoordinates);
        this.logicalTable = spr.logicalTable;
        this.customSpreadsheetResultOpenClass = spr.customSpreadsheetResultOpenClass;
        this.tableStructureDetails = spr.tableStructureDetails;
    }

    public boolean isFieldUsedInModel(String fieldName) {
        Point point = fieldsCoordinates.get(fieldName);
        if (point != null) {
            return columnNamesForResultModel[point.getColumn()] != null && rowNamesForResultModel[point
                .getRow()] != null;
        }
        return false;
    }

    static Map<String, Point> buildFieldsCoordinates(String[] columnNames, String[] rowNames) {
        Map<String, Point> fieldsCoordinates = new HashMap<>();
        if (columnNames != null && rowNames != null) {
            long nonNullsColumnsCount = Arrays.stream(columnNames).filter(Objects::nonNull).count();
            long nonNullsRowsCount = Arrays.stream(rowNames).filter(Objects::nonNull).count();
            boolean isSingleColumn = nonNullsColumnsCount == 1;
            boolean isSingleRow = nonNullsRowsCount == 1;
            for (int i = 0; i < rowNames.length; i++) {
                for (int j = 0; j < columnNames.length; j++) {
                    if (columnNames[j] != null && rowNames[i] != null) {
                        fieldsCoordinates.put(
                            SpreadsheetStructureBuilder.getSpreadsheetCellFieldName(columnNames[j], rowNames[i]),
                            Point.get(j, i));
                        if (isSingleColumn) {
                            fieldsCoordinates.put(SpreadsheetStructureBuilder.DOLLAR_SIGN + rowNames[i],
                                Point.get(j, i));
                        } else {
                            if (isSingleRow) {
                                fieldsCoordinates.put(SpreadsheetStructureBuilder.DOLLAR_SIGN + columnNames[j],
                                    Point.get(j, i));
                            }
                        }
                    }
                }
            }
        }
        return fieldsCoordinates;
    }

    private void initFieldsCoordinates() {
        this.fieldsCoordinates = buildFieldsCoordinates(columnNames, rowNames);
    }

    @XmlTransient
    public int getHeight() {
        return rowNames.length;
    }

    public Object[][] getResults() {
        return results;
    }

    public void setResults(Object[][] results) {
        this.results = results.clone();
    }

    @XmlTransient
    public int getWidth() {
        return columnNames.length;
    }

    public String[] getColumnNames() {
        return columnNames != null ? columnNames.clone() : null;
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public String[] getRowNames() {
        return rowNames != null ? rowNames.clone() : null;
    }

    public void setRowNames(String[] rowNames) {
        this.rowNames = rowNames;
    }

    @XmlTransient
    public boolean isTableStructureDetails() {
        return tableStructureDetails;
    }

    public void setTableStructureDetails(boolean tableStructureDetails) {
        this.tableStructureDetails = tableStructureDetails;
    }

    public Object getValue(int row, int column) {
        return results[row][column];
    }

    public void setFieldValue(String name, Object value) {
        if (fieldsCoordinates == null) { // Required if default constructor is
            // used with setter methods.
            initFieldsCoordinates();
        }
        Point fieldCoordinates = fieldsCoordinates.get(name);

        if (fieldCoordinates != null) {
            setValue(fieldCoordinates.getRow(), fieldCoordinates.getColumn(), value);
        }
    }

    protected void setValue(int row, int column, Object value) {
        results[row][column] = value;
    }

    public String getColumnName(int column) {
        return columnNames[column];
    }

    public String getRowName(int row) {
        return rowNames[row];
    }

    /**
     *
     * @return logical representation of calculated spreadsheet table it is needed for web studio to display results
     */
    @XmlTransient
    public ILogicalTable getLogicalTable() {
        return logicalTable;
    }

    public void setLogicalTable(ILogicalTable logicalTable) {
        this.logicalTable = logicalTable;
    }

    public Object getFieldValue(String name) {
        if (fieldsCoordinates == null) { // Required if default constructor is
            // used with setter methods.
            initFieldsCoordinates();
        }
        Point fieldCoordinates = fieldsCoordinates.get(name);

        if (fieldCoordinates != null) {
            return getValue(fieldCoordinates.getRow(), fieldCoordinates.getColumn());
        }
        return null;
    }

    public boolean hasField(String name) {
        if (fieldsCoordinates == null) { // Required if default constructor is
            // used with setter methods.
            initFieldsCoordinates();
        }
        return fieldsCoordinates.get(name) != null;
    }

    @Override
    public String toString() {
        try {
            if (CollectionUtils.isEmpty(rowNames) || CollectionUtils.isEmpty(columnNames)) {
                return "[EMPTY]";
            } else {
                return printTable();
            }
        } catch (Exception e) {
            // If it's impossible to print the table, fallback to default
            // toString() implementation
            LoggerFactory.getLogger(getClass()).debug(e.getMessage(), e);
            return super.toString();
        }
    }

    private static final ThreadLocal<Integer> DEPTH_LOCAL_THREAD = new ThreadLocal<>();

    private String truncateStringValue(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() > MAX_VALUE_LENGTH) {
            return value.substring(0, MAX_VALUE_LENGTH) + " ... TRUNCATED ...";
        } else {
            return value;
        }
    }

    private String printTable() {
        StringBuilder sb = new StringBuilder();
        Integer d = DEPTH_LOCAL_THREAD.get();
        d = d != null ? d : 0;
        try {
            DEPTH_LOCAL_THREAD.set(d + 1);
            int maxWidth = Math.min(MAX_WIDTH, getWidth());
            int maxHeight = Math.min(MAX_HEIGHT, getHeight());

            int[] width = new int[maxWidth + 1];

            for (int i1 = 0; i1 <= maxHeight; i1++) {
                for (int j1 = 0; j1 <= maxWidth; j1++) {
                    width[j1] = Math.max(width[j1],
                        i1 > 0 && j1 > 0 && getValue(i1 - 1,
                            j1 - 1) instanceof SpreadsheetResult && d > MAX_DEPTH ? "... TRUNCATED TABLE ..."
                                .length() : truncateStringValue(getStringValue(j1, i1)).length());
                }
            }

            for (int i = 0; i <= maxHeight; i++) {
                for (int j = 0; j <= maxWidth; j++) {
                    if (j != 0) {
                        sb.append(" | ");
                    }
                    String cell;
                    if (i > 0 && j > 0 && getValue(i - 1, j - 1) instanceof SpreadsheetResult && d > MAX_DEPTH) {
                        cell = "... TRUNCATED TABLE ...";
                    } else {
                        cell = truncateStringValue(getStringValue(j, i));
                    }

                    sb.append(cell);
                    for (int k = 0; k < width[j] - cell.length(); k++) {
                        sb.append(' ');
                    }
                }
                sb.append('\n');
            }
            if (getWidth() > MAX_WIDTH || getHeight() > MAX_HEIGHT) {
                sb.append("... TRUNCATED TABLE ...");
            }
        } finally {
            if (d == 0) {
                DEPTH_LOCAL_THREAD.remove();
            } else {
                DEPTH_LOCAL_THREAD.set(d);
            }
        }

        return sb.toString();
    }

    private String getStringValue(int col, int row) {
        if (col == 0 && row == 0) {
            return "-X-";
        }
        if (col == 0) {
            return getRowName(row - 1);
        }
        if (row == 0) {
            return getColumnName(col - 1);
        }

        Object value = getValue(row - 1, col - 1);

        if (value == null) {
            return "";
        } else {
            String s = Arrays.deepToString(new Object[] { value });
            return s.substring(1, s.length() - 1);
        }
    }

    @XmlTransient
    public CustomSpreadsheetResultOpenClass getCustomSpreadsheetResultOpenClass() {
        return customSpreadsheetResultOpenClass;
    }

    public void setCustomSpreadsheetResultOpenClass(CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
        this.customSpreadsheetResultOpenClass = customSpreadsheetResultOpenClass;
    }

    public Object toPlain() {
        if (getCustomSpreadsheetResultOpenClass() != null) {
            return getCustomSpreadsheetResultOpenClass().createBean(this);
        } else {
            return toMap();
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new HashMap<>();
        if (columnNames != null && rowNames != null) {
            long nonNullsColumnsCount = Arrays.stream(columnNamesForResultModel).filter(Objects::nonNull).count();
            long nonNullsRowsCount = Arrays.stream(rowNamesForResultModel).filter(Objects::nonNull).count();
            final boolean isSingleRow = nonNullsRowsCount == 1;
            final boolean isSingleColumn = nonNullsColumnsCount == 1;
            final boolean isTableStructureDetailsPresented = tableStructureDetails;
            String[][] TableDetails = isTableStructureDetailsPresented ? new String[rowNames.length][columnNames.length]
                                                                       : null;
            if (customSpreadsheetResultOpenClass != null) {
                Map<String, String> xmlNamesMap = customSpreadsheetResultOpenClass.getXmlNamesMap();
                for (Map.Entry<String, List<IOpenField>> e : customSpreadsheetResultOpenClass.getBeanFieldsMap()
                    .entrySet()) {
                    List<IOpenField> openFields = e.getValue();
                    for (IOpenField openField : openFields) {
                        Point p = fieldsCoordinates.get(openField.getName());
                        if (p != null && columnNamesForResultModel[p.getColumn()] != null && rowNamesForResultModel[p
                            .getRow()] != null) {
                            values.put(xmlNamesMap.get(e.getKey()),
                                convertSpreadsheetResult(getValue(p.getRow(), p.getColumn()), true));
                            if (isTableStructureDetailsPresented) {
                                TableDetails[p.getRow()][p.getColumn()] = xmlNamesMap.get(e.getKey());
                            }
                        }
                    }
                }
            } else {
                for (int i = 0; i < rowNamesForResultModel.length; i++) {
                    for (int j = 0; j < columnNamesForResultModel.length; j++) {
                        if (columnNamesForResultModel[j] != null && rowNamesForResultModel[i] != null) {
                            String fName;
                            if (isSingleColumn) {
                                fName = rowNamesForResultModel[i];
                            } else {
                                if (isSingleRow) {
                                    fName = columnNamesForResultModel[j];
                                } else {
                                    fName = columnNamesForResultModel[j] + "_" + rowNamesForResultModel[i];
                                }
                            }
                            String fNewName = fName;
                            int k = 1;
                            while (values.containsKey(fNewName)) {
                                fNewName = fName + k;
                                k++;
                            }
                            values.put(fNewName, convertSpreadsheetResult(getValue(i, j), true));
                            if (isTableStructureDetailsPresented) {
                                TableDetails[i][j] = fNewName;
                            }
                        }
                    }
                }
            }
            if (tableStructureDetails) {
                values.put(CustomSpreadsheetResultOpenClass.findNonConflictFieldName(values.keySet(), "TableDetails"),
                    TableDetails);
                values.put(CustomSpreadsheetResultOpenClass.findNonConflictFieldName(values.keySet(), "RowNames"),
                    rowNames);
                values.put(CustomSpreadsheetResultOpenClass.findNonConflictFieldName(values.keySet(), "ColumnNames"),
                    columnNames);
            }
        }
        return values;
    }

    private static Object convertSpreadsheetResult(Object v, boolean toMap) {
        if (v instanceof SpreadsheetResult) {
            SpreadsheetResult spreadsheetResult = (SpreadsheetResult) v;
            if (spreadsheetResult.getCustomSpreadsheetResultOpenClass() == null) {
                return convertSpreadsheetResult(v,
                    spreadsheetResult.getCustomSpreadsheetResultOpenClass()
                        .getModule()
                        .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                        .toCustomSpreadsheetResultOpenClass()
                        .getBeanClass(),
                    toMap);
            } else {
                return convertSpreadsheetResult(v, null, toMap);
            }
        }
        return convertSpreadsheetResult(v, null, toMap);
    }

    public static Object convertSpreadsheetResult(Object v) {
        return convertSpreadsheetResult(v, null, false);
    }

    public static Object convertSpreadsheetResult(Object v, Class<?> toType) {
        return convertSpreadsheetResult(v, toType, false);
    }

    @SuppressWarnings("unchecked")
    private static Object convertSpreadsheetResult(Object v, Class<?> toType, boolean spreadsheetResultsToMap) {
        if (v == null) {
            return null;
        }
        if (v instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) v;
            Collection<Object> newCollection;
            try {
                newCollection = (Collection<Object>) v.getClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                return v;
            }
            for (Object o : collection) {
                newCollection.add(convertSpreadsheetResult(o, spreadsheetResultsToMap));
            }
            return newCollection;
        }
        if (v instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) v;
            Map<Object, Object> newMap;
            try {
                newMap = (Map<Object, Object>) v.getClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                return v;
            }
            for (Entry<Object, Object> e : map.entrySet()) {
                newMap.put(convertSpreadsheetResult(e.getKey(), spreadsheetResultsToMap),
                    convertSpreadsheetResult(e.getValue(), spreadsheetResultsToMap));
            }
            return newMap;
        }
        if (v.getClass().isArray()) {
            Class<?> componentType = v.getClass().getComponentType();
            Class<?> t = v.getClass();
            while (t.isArray()) {
                t = t.getComponentType();
            }
            int len = Array.getLength(v);
            if (ClassUtils.isAssignable(t, SpreadsheetResult.class)) {
                Object tmpArray = Array
                    .newInstance(toType != null && toType.isArray() ? toType.getComponentType() : Object.class, len);
                for (int i = 0; i < len; i++) {
                    Array.set(tmpArray,
                        i,
                        convertSpreadsheetResult(Array.get(v, i),
                            toType != null && toType.isArray() ? toType.getComponentType() : null,
                            spreadsheetResultsToMap));
                }
                if (toType != null && toType.isArray() && Object.class != toType.getComponentType()) {
                    return tmpArray;
                }
                Class<?> c = null;
                boolean f = true;
                for (int i = 0; i < len; i++) {
                    Object v1 = Array.get(tmpArray, i);
                    if (v1 != null) {
                        if (c == null) {
                            c = v1.getClass();
                        } else {
                            if (!c.equals(v1.getClass())) {
                                f = false;
                            }
                        }
                    }
                }
                if (f && c != null) {
                    Object newArray = Array.newInstance(c, len);
                    for (int i = 0; i < len; i++) {
                        Array.set(newArray, i, Array.get(tmpArray, i));
                    }
                    return newArray;
                }
                return tmpArray;
            } else if (ClassUtils.isAssignable(SpreadsheetResult.class, t) || ClassUtils.isAssignable(t,
                Map.class) || ClassUtils.isAssignable(t, Collection.class)) {
                Object newArray = Array.newInstance(componentType, len);
                for (int i = 0; i < len; i++) {
                    Array.set(newArray,
                        i,
                        convertSpreadsheetResult(Array.get(v, i), componentType, spreadsheetResultsToMap));
                }
                return newArray;
            } else {
                return v;
            }
        }
        if (v instanceof SpreadsheetResult) {
            SpreadsheetResult spreadsheetResult = (SpreadsheetResult) v;
            if (Map.class == toType || spreadsheetResultsToMap) {
                return spreadsheetResult.toMap();
            } else if (toType == spreadsheetResult.getCustomSpreadsheetResultOpenClass()
                .getModule()
                .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                .toCustomSpreadsheetResultOpenClass()
                .getBeanClass()) {
                return spreadsheetResult.getCustomSpreadsheetResultOpenClass()
                    .getModule()
                    .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                    .toCustomSpreadsheetResultOpenClass()
                    .createBean(spreadsheetResult);
            } else {
                return spreadsheetResult.toPlain();
            }
        }
        return v;
    }
}
