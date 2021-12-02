package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.meta.DoubleValue;
import org.openl.meta.IMetaHolder;
import org.openl.meta.IMetaInfo;
import org.openl.meta.StringValue;
import org.openl.meta.ValueMetaInfo;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.rules.calc.element.SpreadsheetCellRefType;
import org.openl.rules.calc.element.SpreadsheetCellType;
import org.openl.rules.calc.element.SpreadsheetExpressionMarker;
import org.openl.rules.calc.element.SpreadsheetStructureBuilderHolder;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.MessageUtils;
import org.openl.util.StringUtils;
import org.openl.util.text.LocationUtils;

public class SpreadsheetStructureBuilder {

    public static final String DOLLAR_SIGN = "$";

    private final SpreadsheetComponentsBuilder componentsBuilder;

    private IBindingContext spreadsheetBindingContext;

    private final IOpenMethodHeader spreadsheetHeader;

    private final XlsModuleOpenClass xlsModuleOpenClass;

    private final SpreadsheetStructureBuilderHolder spreadsheetStructureBuilderHolder = new SpreadsheetStructureBuilderHolder(
        this);

    public SpreadsheetStructureBuilderHolder getSpreadsheetStructureBuilderHolder() {
        return spreadsheetStructureBuilderHolder;
    }

    public SpreadsheetStructureBuilder(SpreadsheetComponentsBuilder componentsBuilder,
            IOpenMethodHeader spreadsheetHeader,
            XlsModuleOpenClass xlsModuleOpenClass) {
        this.componentsBuilder = componentsBuilder;
        this.spreadsheetHeader = spreadsheetHeader;
        this.xlsModuleOpenClass = xlsModuleOpenClass;
    }

    private final Map<Integer, IBindingContext> rowContexts = new HashMap<>();
    private final Map<Integer, SpreadsheetOpenClass> colComponentOpenClasses = new HashMap<>();
    private final Map<Integer, Map<Integer, SpreadsheetContext>> spreadsheetResultContexts = new HashMap<>();

    private SpreadsheetCell[][] cells;

    private final List<SpreadsheetCell> extractedCellValues = new ArrayList<>();

    public static final ThreadLocal<Stack<Map<SpreadsheetStructureBuilder, List<SpreadsheetCell>>>> preventCellsLoopingOnThis = new ThreadLocal<>();

    private volatile boolean cellsExtracted = false;

    /**
     * Extract cell values from the source spreadsheet table.
     *
     * @return cells of spreadsheet with its values
     */
    public SpreadsheetCell[][] getCells() {
        if (!cellsExtracted) {
            synchronized (this) {
                if (!cellsExtracted) {
                    try {
                        extractCellValues();
                    } finally {
                        cellsExtracted = true;
                    }
                }
            }
        }
        return cells;
    }

    /**
     * Add to {@link SpreadsheetOpenClass} fields that are represented by spreadsheet cells.
     *
     * @param spreadsheetType open class of the spreadsheet
     */
    public void addCellFields(SpreadsheetOpenClass spreadsheetType, boolean autoType) {
        IBindingContext generalBindingContext = componentsBuilder.getBindingContext();

        CellsHeaderExtractor cellsHeadersExtractor = componentsBuilder.getCellsHeadersExtractor();
        int rowsCount = cellsHeadersExtractor.getHeight();
        int columnsCount = cellsHeadersExtractor.getWidth();

        // create cells according to the size of the spreadsheet
        cells = new SpreadsheetCell[rowsCount][columnsCount];

        // create the binding context for the spreadsheet level
        spreadsheetBindingContext = new SpreadsheetContext(generalBindingContext, spreadsheetType, xlsModuleOpenClass);

        for (int rowIndex = 0; rowIndex < rowsCount; rowIndex++) {
            for (int columnIndex = 0; columnIndex < columnsCount; columnIndex++) {
                // build spreadsheet cell
                SpreadsheetCell spreadsheetCell = buildCell(rowIndex, columnIndex, autoType);

                // init cells array with appropriate cell
                cells[rowIndex][columnIndex] = spreadsheetCell;

                // create and add field of the cell to the spreadsheetType
                addSpreadsheetFields(spreadsheetType, rowIndex, columnIndex);
            }
        }
    }

    private void extractCellValues() {
        CellsHeaderExtractor cellsHeadersExtractor = componentsBuilder.getCellsHeadersExtractor();
        int rowsCount = cellsHeadersExtractor.getHeight();
        int columnsCount = cellsHeadersExtractor.getWidth();

        for (int rowIndex = 0; rowIndex < rowsCount; rowIndex++) {
            IBindingContext rowBindingContext = getRowContext(rowIndex);

            for (int columnIndex = 0; columnIndex < columnsCount; columnIndex++) {
                boolean found = false;
                for (SpreadsheetCell cell : extractedCellValues) {
                    int row = cell.getRowIndex();
                    int column = cell.getColumnIndex();
                    if (row == rowIndex && columnIndex == column) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    extractCellValue(rowBindingContext, rowIndex, columnIndex);
                }
            }
        }
    }

    public IOpenClass makeType(SpreadsheetCell cell) {
        if (cell.getType() == null && !cell.isTypeUnknown()) {
            int rowIndex = cell.getRowIndex();
            int columnIndex = cell.getColumnIndex();

            IBindingContext rowContext = getRowContext(rowIndex);
            Stack<Map<SpreadsheetStructureBuilder, List<SpreadsheetCell>>> stack = preventCellsLoopingOnThis.get();
            boolean f = stack == null;
            try {
                if (f) {
                    preventCellsLoopingOnThis.set(stack = new Stack<>());
                }
                Map<SpreadsheetStructureBuilder, List<SpreadsheetCell>> map;
                if (stack.isEmpty()) {
                    map = new HashMap<>();
                    stack.push(map);
                } else {
                    map = stack.peek();
                }
                List<SpreadsheetCell> cellInChain = map.computeIfAbsent(this, (k) -> new ArrayList<>());
                if (cellInChain.contains(cell)) {
                    cell.setTypeUnknown(true);
                    throw new SpreadsheetCellsLoopException("Spreadsheet Expression Loop: " + cellInChain);
                }
                try {
                    cellInChain.add(cell);
                    extractCellValue(rowContext, rowIndex, columnIndex);
                    extractedCellValues.add(cell);
                } finally {
                    cellInChain.remove(cell);
                }
            } finally {
                if (f) {
                    preventCellsLoopingOnThis.remove();
                }
            }
        }
        if (cell.isTypeUnknown()) {
            return NullOpenClass.the;
        } else {
            return cell.getType();
        }
    }

    private void extractCellValue(IBindingContext rowBindingContext, int rowIndex, int columnIndex) {
        Map<Integer, SpreadsheetHeaderDefinition> columnHeaders = componentsBuilder.getColumnHeaders();
        Map<Integer, SpreadsheetHeaderDefinition> rowHeaders = componentsBuilder.getRowHeaders();

        SpreadsheetCell spreadsheetCell = cells[rowIndex][columnIndex];

        if (columnHeaders.get(columnIndex) == null || rowHeaders.get(rowIndex) == null) {
            spreadsheetCell.setValue(null);
            return;
        }

        ILogicalTable cell = LogicalTableHelper.mergeBounds(
            componentsBuilder.getCellsHeadersExtractor().getRowNamesTable().getRow(rowIndex),
            componentsBuilder.getCellsHeadersExtractor().getColumnNamesTable().getColumn(columnIndex));

        IOpenSourceCodeModule source = new GridCellSourceCodeModule(cell.getSource(), spreadsheetBindingContext);
        String code = StringUtils.trimToNull(source.getCode());

        String name = getSpreadsheetCellFieldName(columnHeaders.get(columnIndex).getDefinitionName(),
            rowHeaders.get(rowIndex).getDefinitionName());

        IOpenClass type = spreadsheetCell.getType();

        if (code == null) {
            spreadsheetCell.setValue(type.nullObject());
        } else if (SpreadsheetExpressionMarker.isFormula(code)) {

            int end = 0;
            if (code.startsWith(SpreadsheetExpressionMarker.OPEN_CURLY_BRACKET.getSymbol())) {
                end = -1;
            }

            IOpenSourceCodeModule srcCode = new SubTextSourceCodeModule(source, 1, end);
            IMethodSignature signature = spreadsheetHeader.getSignature();
            IOpenClass declaringClass = spreadsheetHeader.getDeclaringClass();
            IOpenMethodHeader header = new OpenMethodHeader(name, type, signature, declaringClass);
            IBindingContext columnBindingContext = getColumnContext(columnIndex, rowIndex, rowBindingContext);
            OpenL openl = columnBindingContext.getOpenL();
            // columnBindingContext - is never null
            try {
                IOpenMethod method;
                if (header.getType() == null) {
                    method = OpenLManager.makeMethodWithUnknownType(openl,
                        srcCode,
                        name,
                        signature,
                        declaringClass,
                        columnBindingContext);
                    spreadsheetCell.setType(method.getType());
                } else {
                    method = OpenLManager.makeMethod(openl, srcCode, header, columnBindingContext);
                }
                spreadsheetCell.setValue(method);
            } catch (Exception | LinkageError e) {
                spreadsheetCell.setTypeUnknown(true);
                String message = String.format("Cannot parse cell value '%s' to the necessary type.", code);
                spreadsheetBindingContext.addError(SyntaxNodeExceptionUtils
                    .createError(message, e, LocationUtils.createTextInterval(source.getCode()), source));
            }

        } else if (spreadsheetCell.isConstantCell()) {
            try {
                IOpenField openField = rowBindingContext.findVar(ISyntaxConstants.THIS_NAMESPACE, code, true);
                ConstantOpenField constOpenField = (ConstantOpenField) openField;
                spreadsheetCell.setValue(constOpenField.getValue());
            } catch (Exception e) {
                String message = "Cannot parse cell value.";
                spreadsheetBindingContext.addError(SyntaxNodeExceptionUtils.createError(message, e, null, source));
            }
        } else {
            Class<?> instanceClass = type.getInstanceClass();
            if (instanceClass == null) {
                String message = MessageUtils.getTypeDefinedErrorMessage(type.getName());
                spreadsheetBindingContext.addError(SyntaxNodeExceptionUtils.createError(message, source));
            }

            try {
                IBindingContext bindingContext = getColumnContext(columnIndex, rowIndex, rowBindingContext);
                ICell theCellValue = cell.getCell(0, 0);
                Object result = null;
                if (String.class == instanceClass) {
                    result = String2DataConvertorFactory.parse(instanceClass, code, bindingContext);
                } else {
                    if (theCellValue.hasNativeType()) {
                        result = RuleRowHelper.loadNativeValue(theCellValue, type);
                    }
                    if (result == null) {
                        result = String2DataConvertorFactory.parse(instanceClass, code, bindingContext);
                    }
                }

                if (bindingContext.isExecutionMode() && result instanceof IMetaHolder) {
                    IMetaInfo meta = new ValueMetaInfo(name, null, source);
                    ((IMetaHolder) result).setMetaInfo(meta);
                }

                IOpenCast openCast = bindingContext.getCast(JavaOpenClass.getOpenClass(instanceClass), type);
                spreadsheetCell.setValue(openCast.convert(result));
            } catch (Exception t) {
                String message = String.format("Cannot parse cell value '%s' to the necessary type.", code);
                spreadsheetBindingContext.addError(SyntaxNodeExceptionUtils.createError(message, t, null, source));
            }
        }
    }

    /**
     * Creates a field from the spreadsheet cell and add it to the spreadsheetType
     */
    private void addSpreadsheetFields(SpreadsheetOpenClass spreadsheetType,
            int rowIndex,
            int columnIndex) {
        SpreadsheetHeaderDefinition columnHeaders = componentsBuilder.getColumnHeaders().get(columnIndex);
        SpreadsheetHeaderDefinition rowHeaders = componentsBuilder.getRowHeaders().get(rowIndex);

        if (columnHeaders == null || rowHeaders == null) {
            return;
        }

        boolean oneColumnSpreadsheet = componentsBuilder.getColumnHeaders().size() == 1;
        boolean oneRowSpreadsheet = componentsBuilder.getRowHeaders().size() == 1;

        SymbolicTypeDefinition columnDefinition = columnHeaders.getDefinition();
        SymbolicTypeDefinition rowDefinition = rowHeaders.getDefinition();

        // get column name from the column definition
        String columnName = columnDefinition.getName().getIdentifier();

        // get row name from the row definition
        String rowName = rowDefinition.getName().getIdentifier();

        // create name of the field
        String fieldName = getSpreadsheetCellFieldName(columnName, rowName);

        SpreadsheetCell spreadsheetCell = cells[rowIndex][columnIndex];
        // create spreadsheet cell field
        createSpreadsheetCellField(spreadsheetType,
            spreadsheetCell,
            fieldName,
            SpreadsheetCellRefType.ROW_AND_COLUMN);

        if (oneColumnSpreadsheet) {
            // add simplified field name
            String simplifiedFieldName = getSpreadsheetCellSimplifiedFieldName(rowName);
            IOpenField field1 = spreadsheetType.getField(simplifiedFieldName);
            if (field1 == null) {
                createSpreadsheetCellField(spreadsheetType,
                    spreadsheetCell,
                    simplifiedFieldName,
                    SpreadsheetCellRefType.SINGLE_COLUMN);
            }
        } else if (oneRowSpreadsheet) {
            // add simplified field name
            String simplifiedFieldName = getSpreadsheetCellSimplifiedFieldName(columnName);
            IOpenField field1 = spreadsheetType.getField(simplifiedFieldName);
            if (field1 == null || field1 instanceof SpreadsheetCellField && ((SpreadsheetCellField) field1)
                .isLastColumnRef()) {
                createSpreadsheetCellField(spreadsheetType,
                    spreadsheetCell,
                    simplifiedFieldName,
                    SpreadsheetCellRefType.SINGLE_ROW);
            }
        }
    }

    private String getSpreadsheetCellSimplifiedFieldName(String rowName) {
        return (DOLLAR_SIGN + rowName).intern();
    }

    /**
     * Gets the name of the spreadsheet cell field. <br>
     * Is represented as {@link #DOLLAR_SIGN}columnName{@link #DOLLAR_SIGN} rowName, e.g. $Value$Final
     *
     * @param columnName name of cell column
     * @param rowName name of the row column
     * @return {@link #DOLLAR_SIGN}columnName{@link #DOLLAR_SIGN}rowName, e.g. $Value$Final
     */
    public static String getSpreadsheetCellFieldName(String columnName, String rowName) {
        return (DOLLAR_SIGN + columnName + DOLLAR_SIGN + rowName).intern();
    }

    private SpreadsheetCell buildCell(int rowIndex, int columnIndex, boolean autoType) {
        Map<Integer, SpreadsheetHeaderDefinition> columnHeaders = componentsBuilder.getColumnHeaders();
        Map<Integer, SpreadsheetHeaderDefinition> rowHeaders = componentsBuilder.getRowHeaders();

        ILogicalTable cell = LogicalTableHelper.mergeBounds(
            componentsBuilder.getCellsHeadersExtractor().getRowNamesTable().getRow(rowIndex),
            componentsBuilder.getCellsHeadersExtractor().getColumnNamesTable().getColumn(columnIndex));
        ICell sourceCell = cell.getSource().getCell(0, 0);

        String cellCode = sourceCell.getStringValue();

        IOpenField openField = null;

        SpreadsheetCellType spreadsheetCellType;
        if (cellCode == null || cellCode.isEmpty() || columnHeaders.get(columnIndex) == null || rowHeaders
            .get(rowIndex) == null) {
            spreadsheetCellType = SpreadsheetCellType.EMPTY;
        } else if (SpreadsheetExpressionMarker.isFormula(cellCode)) {
            spreadsheetCellType = SpreadsheetCellType.METHOD;
        } else {
            spreadsheetCellType = SpreadsheetCellType.VALUE;
            openField = RuleRowHelper.findConstantField(spreadsheetBindingContext, cellCode);
            if (openField != null) {
                spreadsheetCellType = SpreadsheetCellType.CONSTANT;
            }
        }

        SpreadsheetCell spreadsheetCell;
        ICell sourceCellForExecutionMode = spreadsheetBindingContext.isExecutionMode() ? null : sourceCell;
        spreadsheetCell = new SpreadsheetCell(rowIndex, columnIndex, sourceCellForExecutionMode, spreadsheetCellType);

        SpreadsheetHeaderDefinition columnHeader = columnHeaders.get(columnIndex);
        SpreadsheetHeaderDefinition rowHeader = rowHeaders.get(rowIndex);
        IOpenClass cellType;
        if (openField != null) {
            cellType = openField.getType();
        } else if (columnHeader != null && columnHeader.getType() != null) {
            cellType = columnHeader.getType();
        } else if (rowHeader != null && rowHeader.getType() != null) {
            cellType = rowHeader.getType();
        } else {

            // Try to derive cell type as double.
            //
            try {
                // Try to parse cell value.
                // If parse process will be finished with success then return
                // double type else string type.
                //
                if (autoType) {
                    if (SpreadsheetExpressionMarker.isFormula(cellCode)) {
                        cellType = null;
                    } else if (cellCode != null) {
                        String2DataConvertorFactory.getConvertor(Double.class).parse(cellCode, null);
                        cellType = JavaOpenClass.getOpenClass(Double.class);
                    } else {
                        cellType = NullOpenClass.the;
                    }
                } else {
                    if (!SpreadsheetExpressionMarker.isFormula(cellCode)) {
                        String2DataConvertorFactory.getConvertor(DoubleValue.class).parse(cellCode, null);
                    }
                    cellType = JavaOpenClass.getOpenClass(DoubleValue.class);
                }
            } catch (Exception t) {
                if (autoType) {
                    cellType = JavaOpenClass.getOpenClass(String.class);
                } else {
                    cellType = JavaOpenClass.getOpenClass(StringValue.class);
                }
            }
        }
        spreadsheetCell.setType(cellType);

        return spreadsheetCell;
    }

    private IBindingContext getRowContext(int rowIndex) {
        IBindingContext rowContext = rowContexts.get(rowIndex);

        if (rowContext == null) {
            rowContext = makeRowContext(rowIndex);
            rowContexts.put(rowIndex, rowContext);
        }

        return rowContext;
    }

    private SpreadsheetContext getColumnContext(int columnIndex, int rowIndex, IBindingContext rowBindingContext) {
        Map<Integer, SpreadsheetContext> contexts = spreadsheetResultContexts.computeIfAbsent(columnIndex,
            e -> new HashMap<>());
        return contexts.computeIfAbsent(rowIndex, e -> makeSpreadsheetResultContext(columnIndex, rowBindingContext));
    }

    private SpreadsheetContext makeSpreadsheetResultContext(int columnIndex, IBindingContext rowBindingContext) {
        SpreadsheetOpenClass columnOpenClass = colComponentOpenClasses.computeIfAbsent(columnIndex,
            e -> makeColumnComponentOpenClass(columnIndex));
        return new SpreadsheetContext(rowBindingContext, columnOpenClass, xlsModuleOpenClass);
    }

    private SpreadsheetOpenClass makeColumnComponentOpenClass(int columnIndex) {
        // create name for the column open class
        String columnOpenClassName = String.format("%sColType%d", spreadsheetHeader.getName(), columnIndex);

        IBindingContext generalBindingContext = componentsBuilder.getBindingContext();
        Map<Integer, SpreadsheetHeaderDefinition> headers = componentsBuilder.getRowHeaders();

        SpreadsheetOpenClass columnOpenClass = new SpreadsheetOpenClass(columnOpenClassName,
            generalBindingContext.getOpenL());

        int height = cells.length;

        for (int rowIndex = 0; rowIndex < height; rowIndex++) {

            SpreadsheetHeaderDefinition headerDefinition = headers.get(rowIndex);

            proc(rowIndex, columnOpenClass, columnIndex, headerDefinition);
        }
        return columnOpenClass;
    }

    private IBindingContext makeRowContext(int rowIndex) {

        /* create name for the row open class */
        String rowOpenClassName = String.format("%sRowType%d", spreadsheetHeader.getName(), rowIndex);

        /* create row open class and populate it with fields **/
        IBindingContext generalBindingContext = componentsBuilder.getBindingContext();
        Map<Integer, SpreadsheetHeaderDefinition> headers = componentsBuilder.getColumnHeaders();

        // create row open class for current row
        SpreadsheetOpenClass rowOpenClass = new SpreadsheetOpenClass(rowOpenClassName,
            generalBindingContext.getOpenL());

        // get the width of the whole spreadsheet
        int width = cells[0].length;

        // create for each column in row its field
        for (int columnIndex = 0; columnIndex < width; columnIndex++) {

            SpreadsheetHeaderDefinition columnHeader = headers.get(columnIndex);

            proc(rowIndex, rowOpenClass, columnIndex, columnHeader);
        }

        /* create row binding context */
        return new SpreadsheetContext(spreadsheetBindingContext, rowOpenClass, xlsModuleOpenClass);
    }

    private void proc(int rowIndex,
            ComponentOpenClass rowOpenClass,
            int columnIndex,
            SpreadsheetHeaderDefinition columnHeader) {
        if (columnHeader == null) {
            return;
        }

        SpreadsheetCell cell = cells[rowIndex][columnIndex];

        SymbolicTypeDefinition typeDefinition = columnHeader.getDefinition();
        String fieldName = (DOLLAR_SIGN + typeDefinition.getName().getIdentifier()).intern();
        createSpreadsheetCellField(rowOpenClass, cell, fieldName, SpreadsheetCellRefType.LOCAL);
    }

    private void createSpreadsheetCellField(ComponentOpenClass rowOpenClass,
            SpreadsheetCell cell,
            String fieldName,
            SpreadsheetCellRefType spreadsheetCellRefType) {
        SpreadsheetStructureBuilderHolder structureBuilderContainer = getSpreadsheetStructureBuilderHolder();
        SpreadsheetCellField field;
        if (cell.getSpreadsheetCellType() == SpreadsheetCellType.METHOD) {
            field = new SpreadsheetCellField(structureBuilderContainer,
                    rowOpenClass,
                    fieldName,
                    cell,
                    spreadsheetCellRefType);
        } else {
            field = new SpreadsheetCellField.ConstSpreadsheetCellField(structureBuilderContainer,
                    rowOpenClass,
                    fieldName,
                    cell);
        }
        rowOpenClass.addField(field);
    }
}
