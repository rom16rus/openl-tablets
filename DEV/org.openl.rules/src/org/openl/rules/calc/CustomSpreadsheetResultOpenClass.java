package org.openl.rules.calc;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Type;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.binding.impl.module.ModuleSpecificType;
import org.openl.gen.FieldDescription;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomSpreadsheetResultOpenClass extends ADynamicClass implements ModuleSpecificType {
    private final Logger log = LoggerFactory.getLogger(CustomSpreadsheetResultOpenClass.class);
    private static final String[] EMPTY_STRING_ARRAY = new String[] {};
    private static final Comparator<String> FIELD_COMPARATOR = (o1, o2) -> {
        // We do not expect empty fields names, so the length of strings always be greater than zero.
        char c1 = Character.toUpperCase(o1.charAt(0));
        char c2 = Character.toUpperCase(o2.charAt(0));
        if (c1 != c2) {
            return c1 - c2;
        }

        int len1 = o1.length();
        int len2 = o2.length();
        int lim = Math.min(len1, len2);
        int k = 1;
        while (k < lim) {
            c1 = o1.charAt(k);
            c2 = o2.charAt(k);
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    };

    private String[] rowNames;
    private String[] columnNames;
    private String[] rowNamesForResultModel;
    private String[] columnNamesForResultModel;
    private final List<Pair<String[], String[]>> rowAndColumnNamesForResultModelHistory;
    private String[] rowTitles;
    private String[] columnTitles;
    private Map<String, Point> fieldsCoordinates;
    private final XlsModuleOpenClass module;
    private volatile Class<?> beanClass;
    private volatile SpreadsheetResultSetter[] spreadsheetResultSetters;
    private boolean simpleRefByRow;
    private boolean simpleRefByColumn;
    private long columnsForResultModelCount;
    private long rowsForResultModelCount;
    private boolean tableStructureDetails;
    private boolean ignoreCompilation;

    private ILogicalTable logicalTable;

    private volatile byte[] beanClassByteCode;
    private volatile String beanClassName;
    volatile Map<String, List<IOpenField>> beanFieldsMap;
    volatile Map<String, String> xmlNamesMap;
    private String[] sprStructureFieldNames;
    private volatile boolean initializing;

    private final boolean generateBeanClass;

    public CustomSpreadsheetResultOpenClass(String name,
            String[] rowNames,
            String[] columnNames,
            String[] rowNamesForResultModel,
            String[] columnNamesForResultModel,
            String[] rowTitles,
            String[] columnTitles,
            XlsModuleOpenClass module,
            boolean tableStructureDetails,
            boolean generateBeanClass) {
        super(name, SpreadsheetResult.class);
        this.rowNames = Objects.requireNonNull(rowNames);
        this.columnNames = Objects.requireNonNull(columnNames);
        this.rowNamesForResultModel = Objects.requireNonNull(rowNamesForResultModel);
        this.columnNamesForResultModel = Objects.requireNonNull(columnNamesForResultModel);

        this.columnsForResultModelCount = Arrays.stream(columnNamesForResultModel).filter(Objects::nonNull).count();
        this.rowsForResultModelCount = Arrays.stream(rowNamesForResultModel).filter(Objects::nonNull).count();

        this.simpleRefByRow = columnsForResultModelCount == 1;
        this.simpleRefByColumn = rowsForResultModelCount == 1;

        this.rowAndColumnNamesForResultModelHistory = new ArrayList<>();
        this.rowAndColumnNamesForResultModelHistory
            .add(Pair.of(this.columnNamesForResultModel, this.rowNamesForResultModel));

        this.rowTitles = Objects.requireNonNull(rowTitles);
        this.columnTitles = Objects.requireNonNull(columnTitles);

        this.fieldsCoordinates = SpreadsheetResult.buildFieldsCoordinates(this.columnNames, this.rowNames);
        this.module = module;
        this.tableStructureDetails = tableStructureDetails;
        this.generateBeanClass = generateBeanClass;
    }

    public CustomSpreadsheetResultOpenClass(String name,
            XlsModuleOpenClass module,
            ILogicalTable logicalTable,
            boolean generateBeanClass) {
        this(name,
            EMPTY_STRING_ARRAY,
            EMPTY_STRING_ARRAY,
            EMPTY_STRING_ARRAY,
            EMPTY_STRING_ARRAY,
            EMPTY_STRING_ARRAY,
            EMPTY_STRING_ARRAY,
            module,
            false,
            generateBeanClass);
        this.simpleRefByRow = true;
        this.simpleRefByColumn = true;
        this.logicalTable = logicalTable;
    }

    @Override
    public void addField(IOpenField field) throws DuplicatedFieldException {
        if (!(field instanceof CustomSpreadsheetResultField)) {
            throw new IllegalStateException(String.format("Expected type '%s', but found type '%s'.",
                CustomSpreadsheetResultField.class.getTypeName(),
                field.getClass().getTypeName()));
        }
        super.addField(field);
    }

    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        if (ioc instanceof CustomSpreadsheetResultOpenClass && !(ioc instanceof CombinedSpreadsheetResultOpenClass)) {
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) ioc;
            return !getModule().isExternalModule(customSpreadsheetResultOpenClass.getModule(),
                new IdentityHashMap<>()) && this.getName().equals(customSpreadsheetResultOpenClass.getName());
        }
        return false;
    }

    @Override
    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    public byte[] getBeanClassByteCode() {
        return beanClassByteCode.clone();
    }

    @Override
    public Collection<IOpenClass> superClasses() {
        return Collections.singleton(getModule().getSpreadsheetResultOpenClassWithResolvedFieldTypes());
    }

    protected IOpenField searchFieldFromSuperClass(String fname, boolean strictMatch) throws AmbiguousFieldException {
        return null;
    }

    public XlsModuleOpenClass getModule() {
        return module;
    }

    private void extendSpreadsheetResult(String[] rowNames,
            String[] columnNames,
            String[] rowNamesForResultModel,
            String[] columnNamesForResultModel,
            String[] rowTitles,
            String[] columnTitles,
            Collection<IOpenField> fields,
            boolean simpleRefByRow,
            boolean simpleRefByColumn,
            boolean tableStructureDetails) {
        if (beanClass != null) {
            throw new IllegalStateException(
                "Bean class for custom spreadsheet result is already generated. This spreadsheet result type cannot be extended.");
        }

        List<String> nRowNames = Arrays.stream(this.rowNames).collect(toList());
        List<String> nRowNamesForResultModel = Arrays.stream(this.rowNamesForResultModel).collect(toList());
        Set<String> existedRowNamesSet = Arrays.stream(this.rowNames).collect(toSet());

        List<String> nColumnNames = Arrays.stream(this.columnNames).collect(toList());
        List<String> nColumnNamesForResultModel = Arrays.stream(this.columnNamesForResultModel).collect(toList());
        Set<String> existedColumnNamesSet = Arrays.stream(this.columnNames).collect(toSet());

        List<String> nRowTitles = Arrays.stream(this.rowTitles).collect(toList());
        List<String> nColumnTitles = Arrays.stream(this.columnTitles).collect(toList());

        boolean fieldCoordinatesNeedUpdate = false;
        boolean rowColumnsForResultModelNeedUpdate = false;

        for (int i = 0; i < rowNames.length; i++) {
            if (!existedRowNamesSet.contains(rowNames[i])) {
                nRowNames.add(rowNames[i]);
                nRowNamesForResultModel.add(rowNamesForResultModel[i]);
                nRowTitles.add(rowTitles[i]);
                fieldCoordinatesNeedUpdate = true;
                rowColumnsForResultModelNeedUpdate = true;
            } else if (rowNamesForResultModel[i] != null) {
                int k = nRowNames.indexOf(rowNames[i]);
                nRowNamesForResultModel.set(k, rowNamesForResultModel[i]);
                rowColumnsForResultModelNeedUpdate = true;
            }
        }

        for (int i = 0; i < columnNames.length; i++) {
            if (!existedColumnNamesSet.contains(columnNames[i])) {
                nColumnNames.add(columnNames[i]);
                nColumnNamesForResultModel.add(columnNamesForResultModel[i]);
                nColumnTitles.add(columnTitles[i]);
                fieldCoordinatesNeedUpdate = true;
                rowColumnsForResultModelNeedUpdate = true;
            } else if (columnNamesForResultModel[i] != null) {
                int k = nColumnNames.indexOf(columnNames[i]);
                nColumnNamesForResultModel.set(k, columnNamesForResultModel[i]);
                rowColumnsForResultModelNeedUpdate = true;
            }
        }

        if (fieldCoordinatesNeedUpdate) {
            this.rowNames = nRowNames.toArray(EMPTY_STRING_ARRAY);
            this.rowTitles = nRowTitles.toArray(EMPTY_STRING_ARRAY);

            this.columnNames = nColumnNames.toArray(EMPTY_STRING_ARRAY);
            this.columnTitles = nColumnTitles.toArray(EMPTY_STRING_ARRAY);

            this.fieldsCoordinates = Collections
                .unmodifiableMap(SpreadsheetResult.buildFieldsCoordinates(this.columnNames, this.rowNames));
        }

        if (rowColumnsForResultModelNeedUpdate) {
            this.simpleRefByRow = simpleRefByRow && this.simpleRefByRow;
            this.simpleRefByColumn = simpleRefByColumn && this.simpleRefByColumn;

            this.rowAndColumnNamesForResultModelHistory.add(Pair.of(columnNamesForResultModel, rowNamesForResultModel));

            this.rowNamesForResultModel = nRowNamesForResultModel.toArray(EMPTY_STRING_ARRAY);
            this.columnNamesForResultModel = nColumnNamesForResultModel.toArray(EMPTY_STRING_ARRAY);
            this.columnsForResultModelCount = Arrays.stream(this.columnNamesForResultModel)
                .filter(Objects::nonNull)
                .count();
            this.rowsForResultModelCount = Arrays.stream(this.rowNamesForResultModel).filter(Objects::nonNull).count();
        }

        for (IOpenField field : fields) {
            IOpenField thisField = getField(field.getName());
            if (thisField == null) {
                addField(new CustomSpreadsheetResultField(this, field));
            } else {
                fieldMap().put(field.getName(),
                    new CastingCustomSpreadsheetResultField(this, field.getName(), thisField, field));
            }
        }

        this.tableStructureDetails = this.tableStructureDetails || tableStructureDetails;

    }

    public String[] getRowNames() {
        return rowNames.clone();
    }

    public String[] getColumnNames() {
        return columnNames.clone();
    }

    public String[] getRowTitles() {
        return rowTitles.clone();
    }

    public String[] getColumnTitles() {
        return columnTitles.clone();
    }

    public Map<String, Point> getFieldsCoordinates() {
        return fieldsCoordinates;
    }

    @Override
    public void updateWithType(IOpenClass openClass) {
        if (beanClassByteCode != null) {
            throw new IllegalStateException(
                "Java bean class for custom spreadsheet result is loaded to classloader. " + "Custom spreadsheet result cannot be extended.");
        }
        if (openClass instanceof SpreadsheetResultOpenClass) {
            this.updateWithType(((SpreadsheetResultOpenClass) openClass).toCustomSpreadsheetResultOpenClass());
            return;
        }
        CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) openClass;
        if (customSpreadsheetResultOpenClass.getModule() != getModule()) {
            customSpreadsheetResultOpenClass = customSpreadsheetResultOpenClass.convertToModuleType(getModule(), false);
        }
        this.extendSpreadsheetResult(customSpreadsheetResultOpenClass.rowNames,
            customSpreadsheetResultOpenClass.columnNames,
            customSpreadsheetResultOpenClass.rowNamesForResultModel,
            customSpreadsheetResultOpenClass.columnNamesForResultModel,
            customSpreadsheetResultOpenClass.rowTitles,
            customSpreadsheetResultOpenClass.columnTitles,
            customSpreadsheetResultOpenClass.getFields(),
            customSpreadsheetResultOpenClass.simpleRefByRow,
            customSpreadsheetResultOpenClass.simpleRefByColumn,
            customSpreadsheetResultOpenClass.tableStructureDetails);
    }

    @Override
    public Collection<IOpenField> getFields() {
        return new ArrayList<>(fieldMap().values());
    }

    private IOpenField fixModuleFieldType(IOpenField openField) {
        IOpenClass type = openField.getType();
        int dim = 0;
        while (type.isArray()) {
            type = type.getComponentClass();
            dim++;
        }
        IOpenClass t = toModuleType(type);
        if (t != type) {
            if (dim > 0) {
                t = t.getArrayType(dim);
            }
            return new CustomSpreadsheetResultField(this, openField.getName(), t);
        }
        return openField;
    }

    public String[] getRowNamesForResultModel() {
        return rowNamesForResultModel.clone();
    }

    public String[] getColumnNamesForResultModel() {
        return columnNamesForResultModel.clone();
    }

    @Override
    public CustomSpreadsheetResultOpenClass convertToModuleTypeAndRegister(ModuleOpenClass module) {
        return convertToModuleType(module, true);
    }

    private CustomSpreadsheetResultOpenClass convertToModuleType(ModuleOpenClass module, boolean register) {
        if (getModule() != module) {
            if (register && module.findType(getName()) != null) {
                throw new IllegalStateException("Type has already exists in the module.");
            }
            CustomSpreadsheetResultOpenClass type = new CustomSpreadsheetResultOpenClass(getName(),
                rowNames,
                columnNames,
                rowNamesForResultModel,
                columnNamesForResultModel,
                rowTitles,
                columnTitles,
                (XlsModuleOpenClass) module,
                tableStructureDetails,
                generateBeanClass);
            if (register) {
                module.addType(type);
            }
            for (IOpenField field : getFields()) {
                if (field instanceof CustomSpreadsheetResultField) {
                    type.addField(type.fixModuleFieldType(field));
                }
            }
            type.setMetaInfo(getMetaInfo());
            type.logicalTable = this.logicalTable;
            return type;
        }
        return this;
    }

    public ILogicalTable getLogicalTable() {
        return logicalTable;
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        SpreadsheetResult spr = new SpreadsheetResult(new Object[rowNames.length][columnNames.length],
            rowNames,
            columnNames,
            rowNamesForResultModel,
            columnNamesForResultModel,
            fieldsCoordinates);
        spr.setLogicalTable(logicalTable);
        return spr;
    }

    public Object createBean(SpreadsheetResult spreadsheetResult) {
        Class<?> clazz = getBeanClass();
        Object target;
        try {
            target = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.debug("Ignored error: ", e);
            return null;
        }

        for (SpreadsheetResultSetter spreadsheetResultSetter : spreadsheetResultSetters) {
            spreadsheetResultSetter.set(spreadsheetResult, target);
        }
        return target;
    }

    public boolean isBeanClassInitialized() {
        return beanClass != null;
    }

    public Class<?> getBeanClass() {
        if (beanClass == null) {
            synchronized (this) {
                if (beanClass == null) {
                    try {
                        generateBeanClass();
                        Class<?> beanClass = getModule().getClassGenerationClassLoader().loadClass(getBeanClassName());
                        List<SpreadsheetResultSetter> sprSetters = new ArrayList<>();
                        for (Field field : beanClass.getDeclaredFields()) {
                            if (!field.isSynthetic()) {// SONAR adds synthetic fields
                                List<IOpenField> openFields = beanFieldsMap.get(field.getName());
                                if (openFields != null) {
                                    List<SpreadsheetResultFieldValueSetter> sprSettersForField = new ArrayList<>();
                                    for (IOpenField openField : openFields) {
                                        SpreadsheetResultFieldValueSetter spreadsheetResultValueSetter = new SpreadsheetResultFieldValueSetter(
                                            field,
                                            openField);
                                        sprSettersForField.add(spreadsheetResultValueSetter);
                                    }
                                    sprSetters.add(new SpreadsheetResultValueSetter(
                                        sprSettersForField.toArray(SpreadsheetResultFieldValueSetter.EMPTY_ARRAY)));
                                } else if (field.getName().equals(sprStructureFieldNames[0])) {
                                    sprSetters.add(new SpreadsheetResultRowNamesSetter(field));
                                } else if (field.getName().equals(sprStructureFieldNames[1])) {
                                    sprSetters.add(new SpreadsheetResultColumnNamesSetter(field));
                                } else if (field.getName().equals(sprStructureFieldNames[2])) {
                                    sprSetters
                                        .add(new SpreadsheetResultFieldNamesSetter(field, beanFieldsMap, xmlNamesMap));
                                }
                            }
                        }
                        spreadsheetResultSetters = sprSetters.toArray(SpreadsheetResultSetter.EMPTY_ARRAY);
                        this.beanClass = beanClass;
                    } catch (Exception | LinkageError e) {
                        throw new IllegalStateException(
                            String.format("Failed to create bean class for '%s' spreadsheet result.", getName()),
                            e);
                    }
                }
            }
        }
        return beanClass;
    }

    public void generateBeanClass() {
        if (!generateBeanClass) {
            throw new IllegalStateException("This custom spreadsheet result cannot be converted to a bean.");
        }
        if (beanClassByteCode == null) {
            synchronized (this) {
                if (beanClassByteCode == null && !initializing) {
                    try {
                        initializing = true;
                        final String beanClassName = getBeanClassName();
                        JavaBeanClassBuilder beanClassBuilder = new JavaBeanClassBuilder(beanClassName)
                            .withAdditionalConstructor(false)
                            .withEqualsHashCodeToStringMethods(false);
                        TreeMap<String, String> xmlNames = new TreeMap<>(FIELD_COMPARATOR);
                        @SuppressWarnings("unchecked")
                        List<IOpenField>[][] used = new List[rowNames.length][columnNames.length];
                        Map<String, List<IOpenField>> fieldsMap = new HashMap<>();
                        List<Pair<Point, IOpenField>> fields = getListOfFields();
                        IdentityHashMap<XlsModuleOpenClass, IdentityHashMap<XlsModuleOpenClass, Boolean>> cache = new IdentityHashMap<>();
                        addFieldsToJavaClassBuilder(beanClassBuilder, fields, used, xmlNames, true, fieldsMap, cache);
                        addFieldsToJavaClassBuilder(beanClassBuilder, fields, used, xmlNames, false, fieldsMap, cache);
                        sprStructureFieldNames = addSprStructureFields(beanClassBuilder,
                            fieldsMap.keySet(),
                            xmlNames.values());
                        byte[] bc = beanClassBuilder.byteCode();
                        getModule().getClassGenerationClassLoader().addGeneratedClass(beanClassName, bc);
                        beanFieldsMap = Collections.unmodifiableMap(fieldsMap);
                        xmlNamesMap = Collections.unmodifiableMap(xmlNames);
                        beanClassByteCode = bc;
                    } finally {
                        initializing = false;
                    }
                }
            }
        }
    }

    private List<Pair<Point, IOpenField>> getListOfFields() {
        return getFields().stream()
            .map(e -> Pair.of(fieldsCoordinates.get(e.getName()), e))
            .sorted(COMP)
            .collect(toList());
    }

    public Map<String, List<IOpenField>> getBeanFieldsMap() {
        if (beanFieldsMap == null) {
            generateBeanClass();
        }
        return beanFieldsMap;
    }

    public Map<String, String> getXmlNamesMap() {
        if (xmlNamesMap == null) {
            generateBeanClass();
        }
        return xmlNamesMap;
    }

    public static String findNonConflictFieldName(Collection<String> beanFieldNames, String fName) {
        String fNewName = fName;
        int i = 1;
        while (beanFieldNames.contains(fNewName)) {
            fNewName = fName + i;
            i++;
        }
        return fNewName;
    }

    public boolean isGenerateBeanClass() {
        return generateBeanClass;
    }

    private String[] addSprStructureFields(JavaBeanClassBuilder beanClassBuilder,
            Set<String> beanFieldNames,
            Collection<String> xmlNames) {
        if (tableStructureDetails) {
            String[] sprStructureFieldNames = new String[3];
            sprStructureFieldNames[0] = findNonConflictFieldName(beanFieldNames, "rowNames");
            sprStructureFieldNames[1] = findNonConflictFieldName(beanFieldNames, "columnNames");
            sprStructureFieldNames[2] = findNonConflictFieldName(beanFieldNames, "tableDetails");
            beanClassBuilder.addField(sprStructureFieldNames[0],
                new FieldDescription(String[].class
                    .getName(), null, null, null, findNonConflictFieldName(xmlNames, "RowNames"), false));
            beanClassBuilder.addField(sprStructureFieldNames[1],
                new FieldDescription(String[].class
                    .getName(), null, null, null, findNonConflictFieldName(xmlNames, "ColumnNames"), false));
            beanClassBuilder.addField(sprStructureFieldNames[2],
                new FieldDescription(String[][].class
                    .getName(), null, null, null, findNonConflictFieldName(xmlNames, "TableDetails"), false));
            return sprStructureFieldNames;
        }
        return new String[3];
    }

    private static final Comparator<Pair<Point, IOpenField>> COMP = Comparator.comparing(Pair::getLeft,
        Comparator.nullsLast(Comparator.comparingInt(Point::getRow).thenComparingInt(Point::getColumn)));

    public boolean isExternalCustomSpreadsheetResultOpenClass(
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass,
            IdentityHashMap<XlsModuleOpenClass, IdentityHashMap<XlsModuleOpenClass, Boolean>> cache) {
        return getModule().isExternalModule(customSpreadsheetResultOpenClass.getModule(), cache);
    }

    public boolean isExternalSpreadsheetResultOpenClass(SpreadsheetResultOpenClass spreadsheetResultOpenClass,
            IdentityHashMap<XlsModuleOpenClass, IdentityHashMap<XlsModuleOpenClass, Boolean>> cache) {
        return getModule().isExternalModule(spreadsheetResultOpenClass.getModule(), cache);
    }

    public IOpenClass toModuleType(IOpenClass type) {
        if (type instanceof SpreadsheetResultOpenClass) {
            SpreadsheetResultOpenClass spreadsheetResultOpenClass = (SpreadsheetResultOpenClass) type;
            if (!isExternalSpreadsheetResultOpenClass(spreadsheetResultOpenClass, new IdentityHashMap<>())) {
                return getModule().getSpreadsheetResultOpenClassWithResolvedFieldTypes();
            }
        } else if (type instanceof ModuleSpecificType) {
            if (!getModule().isExternalModule((XlsModuleOpenClass) ((ModuleSpecificType) type).getModule(),
                new IdentityHashMap<>())) {
                if (type instanceof CustomSpreadsheetResultOpenClass) {
                    if (!((CustomSpreadsheetResultOpenClass) type).isGenerateBeanClass()) {
                        return getModule().getSpreadsheetResultOpenClassWithResolvedFieldTypes();
                    }
                }
                IOpenClass p = getModule().findType(type.getName());
                if (p == null) {
                    return ((ModuleSpecificType) type).convertToModuleTypeAndRegister(getModule());
                }
                return p;
            }
        }
        return type;
    }

    private void addFieldsToJavaClassBuilder(JavaBeanClassBuilder beanClassBuilder,
            List<Pair<Point, IOpenField>> fields,
            List<IOpenField>[][] used,
            Map<String, String> usedXmlNames,
            boolean addFieldNameWithCollisions,
            Map<String, List<IOpenField>> beanFieldsMap,
            IdentityHashMap<XlsModuleOpenClass, IdentityHashMap<XlsModuleOpenClass, Boolean>> cache) {
        for (Pair<Point, IOpenField> pair : fields) {
            Point point = pair.getLeft();
            if (point == null) {
                continue;
            }
            int row = point.getRow();
            int column = point.getColumn();
            String rowName = rowNamesForResultModel[row];
            String columnName = columnNamesForResultModel[column];
            if (rowName != null && columnName != null) {
                IOpenField field = null;
                if (used[row][column] == null) {
                    String fieldName;
                    String xmlName;
                    if (simpleRefByRow) {
                        fieldName = rowName;
                        xmlName = rowName;
                        field = getField(SpreadsheetStructureBuilder.DOLLAR_SIGN + rowName);
                    } else if (simpleRefByColumn) {
                        fieldName = columnName;
                        xmlName = columnName;
                        field = getField(SpreadsheetStructureBuilder.DOLLAR_SIGN + columnName);
                    } else if (absentInHistory(rowName, columnName)) {
                        continue;
                    } else if (StringUtils.isBlank(columnName)) { // * in the column
                        fieldName = rowName;
                        xmlName = rowName;
                    } else if (StringUtils.isBlank(rowName)) { // * in the row
                        fieldName = columnName;
                        xmlName = columnName;
                    } else {
                        fieldName = columnName + StringUtils.capitalize(rowName);
                        xmlName = columnName + "_" + rowName;
                    }
                    if (field == null) {
                        field = pair.getRight();
                    }
                    if (!field.getName().startsWith(SpreadsheetStructureBuilder.DOLLAR_SIGN)) {
                        continue;
                    }
                    if (StringUtils.isBlank(fieldName)) {
                        fieldName = "_";
                        xmlName = "_";
                    }
                    String typeName;
                    IOpenClass t = field.getType();
                    int dim = 0;
                    while (t.isArray()) {
                        dim++;
                        t = t.getComponentClass();
                    }
                    if (t instanceof CustomSpreadsheetResultOpenClass || t instanceof SpreadsheetResultOpenClass || t instanceof AnySpreadsheetResultOpenClass) {
                        String fieldClsName;
                        XlsModuleOpenClass additionalClassGenerationClassloaderModule = null;
                        if (t instanceof CustomSpreadsheetResultOpenClass) {
                            CustomSpreadsheetResultOpenClass csroc = (CustomSpreadsheetResultOpenClass) t;
                            boolean externalCustomSpreadsheetResultOpenClass = isExternalCustomSpreadsheetResultOpenClass(
                                csroc,
                                cache);
                            if (externalCustomSpreadsheetResultOpenClass) {
                                additionalClassGenerationClassloaderModule = csroc.getModule();
                            }
                            if (csroc.isGenerateBeanClass()) {
                                fieldClsName = csroc.getBeanClassName();
                                csroc.generateBeanClass();
                            } else {
                                XlsModuleOpenClass m = externalCustomSpreadsheetResultOpenClass ? csroc.getModule()
                                                                                                : getModule();
                                fieldClsName = m.getGlobalTableProperties()
                                    .getSpreadsheetResultPackage() + ".AnySpreadsheetResult";
                                m.getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                                    .toCustomSpreadsheetResultOpenClass()
                                    .generateBeanClass();
                            }
                        } else if (t instanceof SpreadsheetResultOpenClass) {
                            SpreadsheetResultOpenClass spreadsheetResultOpenClass = (SpreadsheetResultOpenClass) t;
                            final boolean externalSpreadsheetResultOpenClass = isExternalSpreadsheetResultOpenClass(
                                spreadsheetResultOpenClass,
                                cache);
                            XlsModuleOpenClass m = externalSpreadsheetResultOpenClass ? spreadsheetResultOpenClass
                                .getModule() : getModule();
                            if (externalSpreadsheetResultOpenClass) {
                                additionalClassGenerationClassloaderModule = spreadsheetResultOpenClass.getModule();
                            }
                            fieldClsName = m.getGlobalTableProperties()
                                .getSpreadsheetResultPackage() + ".AnySpreadsheetResult";
                            m.getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                                .toCustomSpreadsheetResultOpenClass()
                                .generateBeanClass();
                        } else {
                            fieldClsName = Map.class.getName();
                        }
                        if (additionalClassGenerationClassloaderModule != null) {
                            getModule().getClassGenerationClassLoader()
                                .addClassLoader(
                                    additionalClassGenerationClassloaderModule.getClassGenerationClassLoader());
                        }
                        typeName = dim > 0 ? (IntStream.range(0, dim)
                            .mapToObj(e -> "[")
                            .collect(joining()) + "L" + fieldClsName + ";") : fieldClsName;
                    } else if (JavaOpenClass.VOID.equals(t) || JavaOpenClass.CLS_VOID.equals(t) || NullOpenClass.the
                        .equals(t)) {
                        continue; // IGNORE VOID FIELDS
                    } else {
                        Class<?> instanceClass = field.getType().getInstanceClass();
                        if (instanceClass.isPrimitive()) {
                            typeName = ClassUtils.primitiveToWrapper(instanceClass).getName();
                        } else {
                            typeName = instanceClass.getName();
                        }
                    }
                    Collection<Consumer<FieldVisitor>> fieldVisitorWriters = Collections.singleton((fieldVisitor) -> {
                        AnnotationVisitor annotationVisitor = fieldVisitor
                            .visitAnnotation(Type.getDescriptor(SpreadsheetCell.class), true);
                        annotationVisitor.visit("column", columnName);
                        annotationVisitor.visit("row", rowName);
                        if (simpleRefByRow) {
                            annotationVisitor.visit("simpleRefByRow", true);
                        }
                        if (simpleRefByColumn) {
                            annotationVisitor.visit("simpleRefByColumn", true);
                        }
                        annotationVisitor.visitEnd();
                    });

                    fieldName = ClassUtils.decapitalize(fieldName); // FIXME: WSDL decapitalize field name without this
                    if (!usedXmlNames.containsKey(fieldName) && !usedXmlNames.containsValue(xmlName)) {
                        FieldDescription fieldDescription = new FieldDescription(typeName,
                            null,
                            null,
                            null,
                            xmlName,
                            false,
                            fieldVisitorWriters,
                            null);
                        beanClassBuilder.addField(fieldName, fieldDescription);
                        beanFieldsMap.put(fieldName, fillUsed(used, point, field));
                        usedXmlNames.put(fieldName, xmlName);
                    } else if (addFieldNameWithCollisions) {
                        String newFieldName = fieldName;
                        int i = 1;
                        while (usedXmlNames.containsKey(newFieldName)) {
                            newFieldName = fieldName + i;
                            i++;
                        }
                        String newXmlName = xmlName;
                        i = 1;
                        while (usedXmlNames.containsValue(newXmlName)) {
                            newXmlName = xmlName + i;
                            i++;
                        }
                        FieldDescription fieldDescription = new FieldDescription(typeName,
                            null,
                            null,
                            null,
                            newXmlName,
                            false,
                            fieldVisitorWriters,
                            null);
                        beanClassBuilder.addField(newFieldName, fieldDescription);
                        beanFieldsMap.put(newFieldName, fillUsed(used, point, field));
                        usedXmlNames.put(newFieldName, newXmlName);
                    }
                } else {
                    boolean f = false;
                    for (IOpenField openField : used[row][column]) { // Do not add the same twice
                        if (openField.getName().equals(pair.getRight().getName())) {
                            f = true;
                            break;
                        }
                    }
                    if (!f) {
                        used[row][column].add(pair.getRight());
                    }
                }
            }
        }
    }

    private boolean absentInHistory(String rowName, String columnName) {
        for (Pair<String[], String[]> p : rowAndColumnNamesForResultModelHistory) {
            for (String col : p.getLeft()) {
                if (Objects.equals(columnName, col)) {
                    for (String row : p.getRight()) {
                        if (Objects.equals(rowName, row)) {
                            return false; // column and row exist in the given Spreadsheet
                        }
                    }
                    break; // Skip checking of rest columns, because of the rowName does not exist
                }
            }
        }
        return true;
    }

    private List<IOpenField> fillUsed(List<IOpenField>[][] used, Point point, IOpenField field) {
        List<IOpenField> fields = new ArrayList<>();
        fields.add(field);
        if (simpleRefByRow) {
            Arrays.fill(used[point.getRow()], fields);
        } else if (simpleRefByColumn) {
            for (int w = 0; w < used.length; w++) {
                used[w][point.getColumn()] = fields;
            }
        } else {
            used[point.getRow()][point.getColumn()] = fields;
        }
        return fields;
    }

    public String getBeanClassName() {
        if (beanClassName == null) {
            synchronized (this) {
                if (beanClassName == null) {
                    String name = getName();
                    if (name.startsWith(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX)) {
                        if (name.length() > Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length()) {
                            name = name.substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length());
                        }
                        String firstLetterUppercaseName = StringUtils.capitalize(name);
                        if (getModule()
                            .findType(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + firstLetterUppercaseName) == null) {
                            name = firstLetterUppercaseName;
                        }
                    }
                    beanClassName = getModule().getGlobalTableProperties().getSpreadsheetResultPackage() + "." + name;
                }
            }
        }
        return beanClassName;
    }

    private interface SpreadsheetResultSetter {
        SpreadsheetResultSetter[] EMPTY_ARRAY = new SpreadsheetResultSetter[0];

        void set(SpreadsheetResult spreadsheetResult, Object target);
    }

    private static final class SpreadsheetResultValueSetter implements SpreadsheetResultSetter {
        private final SpreadsheetResultFieldValueSetter[] spreadsheetResultFieldValueSetters;

        private SpreadsheetResultValueSetter(SpreadsheetResultFieldValueSetter[] spreadsheetResultFieldValueSetters) {
            this.spreadsheetResultFieldValueSetters = Objects.requireNonNull(spreadsheetResultFieldValueSetters);
        }

        @Override
        public void set(SpreadsheetResult spreadsheetResult, Object target) {
            for (SpreadsheetResultFieldValueSetter valueSetter : spreadsheetResultFieldValueSetters) {
                if (valueSetter.set(spreadsheetResult, target)) {
                    return;
                }
            }
        }
    }

    private static final class SpreadsheetResultFieldValueSetter {
        static final SpreadsheetResultFieldValueSetter[] EMPTY_ARRAY = new SpreadsheetResultFieldValueSetter[0];
        private final Field field;
        private final IOpenField openField;

        private SpreadsheetResultFieldValueSetter(Field field, IOpenField openField) {
            this.field = Objects.requireNonNull(field);
            this.openField = Objects.requireNonNull(openField);
            this.field.setAccessible(true);
        }

        public boolean set(SpreadsheetResult spreadsheetResult, Object target) {
            if (!spreadsheetResult.isFieldUsedInModel(openField.getName())) {
                return false;
            }
            Object v = openField.get(spreadsheetResult, null);
            try {
                if (v != null) {
                    Object cv = SpreadsheetResult.convertSpreadsheetResult(v, field.getType());
                    field.set(target, cv);
                    return true;
                }
            } catch (IllegalAccessException e) {
                LoggerFactory.getLogger(SpreadsheetResultFieldValueSetter.class).debug("Ignored error: ", e);
            }
            return false;
        }
    }

    private static class SpreadsheetResultColumnNamesSetter implements SpreadsheetResultSetter {
        private final Field field;

        public SpreadsheetResultColumnNamesSetter(Field field) {
            this.field = Objects.requireNonNull(field);
            this.field.setAccessible(true);
        }

        @Override
        public void set(SpreadsheetResult spreadsheetResult, Object target) {
            try {
                if (spreadsheetResult.isTableStructureDetails()) {
                    field.set(target, spreadsheetResult.columnNames);
                }
            } catch (IllegalAccessException e) {
                LoggerFactory.getLogger(SpreadsheetResultColumnNamesSetter.class).debug("Ignored error: ", e);
            }
        }
    }

    private static class SpreadsheetResultRowNamesSetter implements SpreadsheetResultSetter {
        private final Field field;

        public SpreadsheetResultRowNamesSetter(Field field) {
            this.field = Objects.requireNonNull(field);
            this.field.setAccessible(true);
        }

        @Override
        public void set(SpreadsheetResult spreadsheetResult, Object target) {
            try {
                if (spreadsheetResult.isTableStructureDetails()) {
                    field.set(target, spreadsheetResult.rowNames);
                }
            } catch (IllegalAccessException e) {
                LoggerFactory.getLogger(SpreadsheetResultRowNamesSetter.class).debug("Ignored error: ", e);
            }
        }
    }

    private static class SpreadsheetResultFieldNamesSetter implements SpreadsheetResultSetter {
        private final Field field;
        private final Map<String, List<IOpenField>> beanFieldsMap;
        private final Map<String, String> xmlNamesMap;

        public SpreadsheetResultFieldNamesSetter(Field field,
                Map<String, List<IOpenField>> beanFieldsMap,
                Map<String, String> xmlNamesMap) {
            this.field = Objects.requireNonNull(field);
            this.beanFieldsMap = Objects.requireNonNull(beanFieldsMap);
            this.field.setAccessible(true);
            this.xmlNamesMap = Objects.requireNonNull(xmlNamesMap);
        }

        @Override
        public void set(SpreadsheetResult spreadsheetResult, Object target) {
            if (spreadsheetResult.isTableStructureDetails()) {
                String[][] tableStructureDetails = new String[spreadsheetResult.getRowNames().length][spreadsheetResult
                    .getColumnNames().length];
                for (Map.Entry<String, List<IOpenField>> e : beanFieldsMap.entrySet()) {
                    List<IOpenField> openFields = e.getValue();
                    for (IOpenField openField : openFields) {
                        Point p = spreadsheetResult.fieldsCoordinates.get(openField.getName());
                        if (p != null && spreadsheetResult.rowNamesForResultModel[p
                            .getRow()] != null && spreadsheetResult.columnNamesForResultModel[p.getColumn()] != null) {
                            tableStructureDetails[p.getRow()][p.getColumn()] = xmlNamesMap.get(e.getKey());
                        }
                    }
                }
                try {
                    field.set(target, tableStructureDetails);
                } catch (IllegalAccessException e) {
                    LoggerFactory.getLogger(SpreadsheetResultFieldNamesSetter.class).debug("Ignored error: ", e);
                }
            }
        }
    }

    public boolean isIgnoreCompilation() {
        return ignoreCompilation;
    }

    public void setIgnoreCompilation(boolean ignoreCompilation) {
        this.ignoreCompilation = ignoreCompilation;
    }

    @Override
    protected Map<MethodKey, IOpenMethod> initConstructorMap() {
        Map<MethodKey, IOpenMethod> constructorMap = super.initConstructorMap();
        Map<MethodKey, IOpenMethod> spreadsheetResultConstructorMap = new HashMap<>();
        for (Map.Entry<MethodKey, IOpenMethod> entry : constructorMap.entrySet()) {
            IOpenMethod constructor = new CustomSpreadsheetResultConstructor(entry.getValue(), this);
            spreadsheetResultConstructorMap.put(new MethodKey(constructor), constructor);
        }
        return spreadsheetResultConstructorMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        CustomSpreadsheetResultOpenClass that = (CustomSpreadsheetResultOpenClass) o;

        return Objects.equals(module, that.module);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (module != null ? module.hashCode() : 0);
        return result;
    }
}
