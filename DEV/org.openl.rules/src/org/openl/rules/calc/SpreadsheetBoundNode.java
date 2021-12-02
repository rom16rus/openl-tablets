package org.openl.rules.calc;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.engine.OpenLSystemProperties;
import org.openl.message.OpenLMessagesUtils;
import org.openl.meta.TableMetaInfo;
import org.openl.rules.binding.RulesModuleBindingContextHelper;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.SpreadsheetMetaInfoReader;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ILogicalTable;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;

// TODO: refactor
// Extract all the binding and build code to the SpreadsheetBinder
public class SpreadsheetBoundNode extends AMethodBasedNode {

    private SpreadsheetStructureBuilder structureBuilder;
    private SpreadsheetComponentsBuilder componentsBuilder;
    private SpreadsheetOpenClass spreadsheetOpenClass;
    private SpreadsheetCell[][] cells;

    private IBindingContext bindingContext;

    public SpreadsheetBoundNode(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            IOpenMethodHeader header,
            XlsModuleOpenClass module) {

        super(tableSyntaxNode, openl, header, module);
    }

    @Override
    public XlsModuleOpenClass getModule() {
        return (XlsModuleOpenClass) super.getModule();
    }

    private CustomSpreadsheetResultOpenClass buildCustomSpreadsheetResultType(Spreadsheet spreadsheet) {
        if (!spreadsheet.isTypeCustomSpreadsheetResult()) {
            throw new IllegalArgumentException("Only custom spreadsheets are supported.");
        }
        Collection<IOpenField> spreadsheetOpenClassFields = spreadsheet.getSpreadsheetType()
            .getFields()
            .stream()
            .filter(e -> !"this".equals(e.getName()))
            .collect(Collectors.toList());

        String typeName = Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + spreadsheet.getName();

        CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = new CustomSpreadsheetResultOpenClass(
            typeName,
            spreadsheet.getRowNames(),
            spreadsheet.getColumnNames(),
            spreadsheet.getRowNamesForResultModel(),
            spreadsheet.getColumnNamesForResultModel(),
            spreadsheet.getRowTitles(),
            spreadsheet.getColumnTitles(),
            getModule(),
            spreadsheet.isTableStructureDetails(),
            true);

        customSpreadsheetResultOpenClass
            .setMetaInfo(new TableMetaInfo("Spreadsheet", spreadsheet.getName(), spreadsheet.getSourceUrl()));

        for (IOpenField field : spreadsheetOpenClassFields) {
            CustomSpreadsheetResultField customSpreadsheetResultField = new CustomSpreadsheetResultField(
                customSpreadsheetResultOpenClass,
                field);
            customSpreadsheetResultOpenClass.addField(customSpreadsheetResultField);
        }
        return customSpreadsheetResultOpenClass;
    }

    @Override
    protected ExecutableRulesMethod createMethodShell() {
        Spreadsheet spreadsheet;
        if (componentsBuilder.isExistsReturnHeader()) {
            spreadsheet = new Spreadsheet(getHeader(), this, false);
        } else {
            /*
             * We need to generate a customSpreadsheet class only if return type of the spreadsheet is SpreadsheetResult
             * and the customspreadsheet property is true
             */
            boolean isTypeCustomSpreadsheetResult = SpreadsheetResult.class == getType()
                .getInstanceClass() && !(getType() instanceof CustomSpreadsheetResultOpenClass) && OpenLSystemProperties
                    .isCustomSpreadsheetTypesSupported(bindingContext.getExternalParams());

            spreadsheet = new Spreadsheet(getHeader(), this, isTypeCustomSpreadsheetResult);
        }
        spreadsheet.setSpreadsheetType(spreadsheetOpenClass);
        // As custom spreadsheet result is being generated at runtime,
        // call this method to ensure that CSR will be generated during the
        // compilation.
        // Add generated type to be accessible through binding context.
        //
        spreadsheet.setRowNames(componentsBuilder.getRowNames());
        spreadsheet.setColumnNames(componentsBuilder.getColumnNames());

        spreadsheet.setRowNamesForResultModel(componentsBuilder.getRowNamesForResultModel());
        spreadsheet.setColumnNamesForResultModel(componentsBuilder.getColumnNamesForResultModel());

        spreadsheet.setRowTitles(componentsBuilder.getCellsHeadersExtractor().getRowNames());
        spreadsheet.setColumnTitles(componentsBuilder.getCellsHeadersExtractor().getColumnNames());

        spreadsheet.getTableStructureDetails(
            Boolean.TRUE.equals(getTableSyntaxNode().getTableProperties().getTableStructureDetails()));

        if (spreadsheet.isTypeCustomSpreadsheetResult()) {
            CustomSpreadsheetResultOpenClass type;
            try {
                type = buildCustomSpreadsheetResultType(spreadsheet); // Can throw RuntimeException
                IOpenClass bindingContextType = bindingContext.addType(ISyntaxConstants.THIS_NAMESPACE, type);
                spreadsheet.setCustomSpreadsheetResultType((CustomSpreadsheetResultOpenClass) bindingContextType);
            } catch (Exception | LinkageError e) {
                String message = String.format("Cannot define type '%s'.",
                    Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + spreadsheet.getName());
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, e, getTableSyntaxNode());
                bindingContext.addError(error);
                spreadsheet.setCustomSpreadsheetResultType(
                    (CustomSpreadsheetResultOpenClass) bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE,
                        Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + spreadsheet.getName()));
            }
        }
        return spreadsheet;
    }

    public void validateRowsColumnsForResultModel(Spreadsheet spreadsheet) {
        long columnsForResultModelCount = Arrays.stream(spreadsheet.getColumnNamesForResultModel())
            .filter(Objects::nonNull)
            .count();
        long rowsForResultModelCount = Arrays.stream(spreadsheet.getRowNamesForResultModel())
            .filter(Objects::nonNull)
            .count();

        Map<String, String> fNames = new HashMap<>();
        int warnCnt = 0;
        for (int i = 0; i < spreadsheet.getRowNamesForResultModel().length; i++) {
            for (int j = 0; j < spreadsheet.getColumnNamesForResultModel().length; j++) {
                if (spreadsheet.getColumnNamesForResultModel()[j] != null && spreadsheet
                    .getRowNamesForResultModel()[i] != null && warnCnt < 10) { // Don't show more than 10 conflict
                    // messages
                    String fieldName = SpreadsheetStructureBuilder
                        .getSpreadsheetCellFieldName(spreadsheet.getColumnNames()[j], spreadsheet.getRowNames()[i]);

                    IOpenField field = spreadsheet.getSpreadsheetType().getField(fieldName);
                    IOpenClass t = field.getType();
                    while (t.isArray()) {
                        t = t.getComponentClass();
                    }
                    boolean f = !JavaOpenClass.VOID.equals(t) && !JavaOpenClass.CLS_VOID.equals(t) && !NullOpenClass.the
                        .equals(t);
                    // IGNORE VOID TYPES

                    if (f) {
                        String refName;
                        if (columnsForResultModelCount == 1) {
                            refName = SpreadsheetStructureBuilder.DOLLAR_SIGN + spreadsheet.getRowNames()[i];
                        } else if (rowsForResultModelCount == 1) {
                            refName = SpreadsheetStructureBuilder.DOLLAR_SIGN + spreadsheet.getColumnNames()[j];
                        } else {
                            refName = fieldName;
                        }

                        StringBuilder sb = new StringBuilder();
                        if (columnsForResultModelCount == 1) {
                            sb.append(ClassUtils.decapitalize(spreadsheet.getRowNamesForResultModel()[i]));
                        } else if (rowsForResultModelCount == 1) {
                            sb.append(ClassUtils.decapitalize(spreadsheet.getColumnNamesForResultModel()[j]));
                        } else {
                            sb.append(ClassUtils.decapitalize(spreadsheet.getColumnNamesForResultModel()[j]));
                            sb.append(ClassUtils.capitalize(spreadsheet.getRowNamesForResultModel()[i]));
                        }
                        String fName = sb.toString();
                        if (StringUtils.isBlank(fName)) {
                            fName = "_";
                        }
                        String key = fName.length() > 1 ? (Character.toLowerCase(fName.charAt(0)) + fName.substring(1))
                                                        : fName.toLowerCase();
                        String v = fNames.put(key, refName);
                        if (v != null) {
                            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(String.format(
                                "Cells '%s' and '%s' conflict with each other in the spreadsheet output model.",
                                v,
                                refName), getTableSyntaxNode()));
                            warnCnt++;
                        }
                    }
                }
            }
        }
    }

    public void preBind(IBindingContext bindingContext) throws SyntaxNodeException {
        if (!bindingContext.isExecutionMode()) {
            getTableSyntaxNode().setMetaInfoReader(new SpreadsheetMetaInfoReader(this));
        }
        this.bindingContext = Objects.requireNonNull(bindingContext, "bindingContext cannot be null");
        TableSyntaxNode tableSyntaxNode = getTableSyntaxNode();
        validateTableBody(tableSyntaxNode, bindingContext);
        IOpenMethodHeader header = getHeader();
        RulesModuleBindingContextHelper.compileAllTypesInSignature(header.getSignature(), bindingContext);
        componentsBuilder = new SpreadsheetComponentsBuilder(tableSyntaxNode, bindingContext);
        componentsBuilder.buildHeaders(header.getType());
        structureBuilder = new SpreadsheetStructureBuilder(componentsBuilder, header, getModule());
        String headerType = header.getName() + "Type";
        OpenL openL = bindingContext.getOpenL();
        spreadsheetOpenClass = new SpreadsheetOpenClass(headerType, openL);

        Boolean autoType = tableSyntaxNode.getTableProperties().getAutoType();
        structureBuilder.addCellFields(spreadsheetOpenClass, autoType);
    }

    @Override
    public void finalizeBind(IBindingContext bindingContext) throws Exception {
        super.finalizeBind(bindingContext);

        ILogicalTable tableBody = getTableSyntaxNode().getTableBody();

        getTableSyntaxNode().getSubTables().put(IXlsTableNames.VIEW_BUSINESS, tableBody);

        cells = structureBuilder.getCells();
        Spreadsheet spreadsheet = (Spreadsheet) getMethod();
        if (spreadsheet != null) {
            validateRowsColumnsForResultModel(spreadsheet);

            spreadsheet.setCells(cells);

            spreadsheet.setResultBuilder(componentsBuilder.buildResultBuilder(spreadsheet, bindingContext));
        }
    }

    public SpreadsheetCell[][] getCells() {
        return cells;
    }

    private void validateTableBody(TableSyntaxNode tableSyntaxNode,
            IBindingContext bindingContext) throws SyntaxNodeException {
        ILogicalTable tableBody = tableSyntaxNode.getTableBody();
        if (tableBody == null) {
            throw SyntaxNodeExceptionUtils.createError(
                "Table has no body. Try to merge header cell horizontally to identify table.",
                getTableSyntaxNode());
        }

        int height = tableBody.getHeight();
        int width = tableBody.getWidth();

        if (height < 2 || width < 2) {
            String message = "Spreadsheet has empty body. Spreadsheet table should has at least 2x3 cells.";
            BindHelper.processWarn(message, tableSyntaxNode, bindingContext);
        }
    }

    public Spreadsheet getSpreadsheet() {
        return (Spreadsheet) getMethod();
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        if (cells != null) {
            for (SpreadsheetCell[] cellArray : cells) {
                if (cellArray != null) {
                    for (SpreadsheetCell cell : cellArray) {
                        if (cell != null) {
                            CompositeMethod method = (CompositeMethod) cell.getMethod();
                            if (method != null) {
                                method.updateDependency(dependencies);
                            }
                        }
                    }
                }
            }
        }
    }

    public SpreadsheetComponentsBuilder getComponentsBuilder() {
        return componentsBuilder;
    }

    @Override
    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        if (cxt.isExecutionMode()) {
            super.removeDebugInformation(cxt);
            // clean the builder, that was used for creating spreadsheet
            //
            this.structureBuilder.getSpreadsheetStructureBuilderHolder().clear();
            this.bindingContext = null;
        }
    }
}
