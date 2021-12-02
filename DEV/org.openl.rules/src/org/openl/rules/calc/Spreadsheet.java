package org.openl.rules.calc;

import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.rules.annotations.Executable;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.result.IResultBuilder;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.Point;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

@Executable
public class Spreadsheet extends ExecutableRulesMethod {

    public static final String SPREADSHEETRESULT_TYPE_PREFIX = "SpreadsheetResult";

    private IResultBuilder resultBuilder;

    private SpreadsheetCell[][] cells;

    /**
     * Top left cell of the whole Spreadsheet is not included. So the row names starts from [1, 0] in the Spreadsheet
     * table body
     */
    private String[] rowNames;

    /**
     * Top left cell is not included. So the column names starts from [0, 1] in the Spreadsheet table body
     */
    private String[] columnNames;

    private String[] rowNamesForResultModel;

    private String[] columnNamesForResultModel;

    private String[] rowTitles;

    private String[] columnTitles;

    private boolean tableStructureDetails;

    /**
     * Type of the Spreadsheet with all its fields Is some type of internal. Is used on calculating the results of the
     * cells.
     */
    private SpreadsheetOpenClass spreadsheetType;

    /**
     * Invoker for current method.
     */
    private volatile Invokable invoker;

    /**
     * Custom return type of the spreadsheet method. Is a public type of the spreadsheet
     */
    private CustomSpreadsheetResultOpenClass customSpreadsheetResultType;

    /**
     * Whether <code>spreadsheetCustomType</code> should be generated or not.
     */
    private boolean typeCustomSpreadsheetResult;

    public Spreadsheet() {
        super(null, null);
    }

    public Spreadsheet(IOpenMethodHeader header, SpreadsheetBoundNode boundNode, boolean typeCustomSpreadsheetResult) {
        super(header, boundNode);
        initProperties(getSyntaxNode().getTableProperties());
        this.typeCustomSpreadsheetResult = typeCustomSpreadsheetResult;
    }

    @Override
    public IOpenClass getType() {
        if (isTypeCustomSpreadsheetResult()) {
            return customSpreadsheetResultType;
        } else {
            return super.getType();
        }
    }

    public void setCustomSpreadsheetResultType(CustomSpreadsheetResultOpenClass spreadsheetCustomResultType) {
        this.customSpreadsheetResultType = spreadsheetCustomResultType;
    }

    public boolean isTypeCustomSpreadsheetResult() {
        return typeCustomSpreadsheetResult;
    }

    @Override
    public SpreadsheetBoundNode getBoundNode() {
        return (SpreadsheetBoundNode) super.getBoundNode();
    }

    public SpreadsheetCell[][] getCells() {
        return cells;
    }

    @Override
    public BindingDependencies getDependencies() {
        BindingDependencies bindingDependencies = new RulesBindingDependencies();
        getBoundNode().updateDependency(bindingDependencies);

        return bindingDependencies;
    }

    public IResultBuilder getResultBuilder() {
        return resultBuilder;
    }

    @Override
    public String getSourceUrl() {
        TableSyntaxNode syntaxNode = getSyntaxNode();
        return syntaxNode == null ? null : syntaxNode.getUri();
    }

    public SpreadsheetOpenClass getSpreadsheetType() {
        return spreadsheetType;
    }

    public int getHeight() {
        return cells.length;
    }

    public void setCells(SpreadsheetCell[][] cells) {
        this.cells = cells;
    }

    public void setColumnNames(String[] colNames) {
        this.columnNames = colNames;
    }

    public void setResultBuilder(IResultBuilder resultBuilder) {
        this.resultBuilder = resultBuilder;
    }

    public void setRowNames(String[] rowNames) {
        this.rowNames = rowNames;
    }

    public void setRowTitles(String[] rowTitles) {
        this.rowTitles = rowTitles;
    }

    public String[] getRowNamesForResultModel() {
        return rowNamesForResultModel;
    }

    public void setRowNamesForResultModel(String[] rowNamesForResultModel) {
        this.rowNamesForResultModel = rowNamesForResultModel;
    }

    public String[] getColumnNamesForResultModel() {
        return columnNamesForResultModel;
    }

    public void setColumnNamesForResultModel(String[] columnNamesForResultModel) {
        this.columnNamesForResultModel = columnNamesForResultModel;
    }

    public String[] getRowTitles() {
        return rowTitles;
    }

    public void setColumnTitles(String[] columnTitles) {
        this.columnTitles = columnTitles;
    }

    public String[] getColumnTitles() {
        return columnTitles;
    }

    public void setSpreadsheetType(SpreadsheetOpenClass spreadsheetType) {
        this.spreadsheetType = spreadsheetType;
    }

    public int getWidth() {
        return cells.length == 0 ? 0 : cells[0].length;
    }

    public String[] getRowNames() {
        return rowNames;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public boolean isTableStructureDetails() {
        return tableStructureDetails;
    }

    public void getTableStructureDetails(boolean tableStructureDetails) {
        this.tableStructureDetails = tableStructureDetails;
    }

    @Override
    protected Object innerInvoke(Object target, Object[] params, IRuntimeEnv env) {
        return getInvoker().invoke(target, params, env);
    }

    protected Invokable createInvoker() {
        return new SpreadsheetInvoker(this);
    }

    protected Invokable getInvoker() {
        if (invoker == null) {
            synchronized (this) {
                if (invoker == null) {
                    invoker = createInvoker();
                }
            }
        }
        return invoker;

    }

    public void setInvoker(SpreadsheetInvoker invoker) {
        this.invoker = invoker;
    }

    volatile Map<String, Point> fieldsCoordinates;

    public Map<String, Point> getFieldsCoordinates() {
        if (fieldsCoordinates == null) {
            synchronized (this) {
                if (fieldsCoordinates == null) {
                    fieldsCoordinates = SpreadsheetResult.buildFieldsCoordinates(columnNames, rowNames);
                }
            }
        }
        return fieldsCoordinates;
    }

}
