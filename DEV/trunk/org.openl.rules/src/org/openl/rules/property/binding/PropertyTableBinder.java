package org.openl.rules.property.binding;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.data.ITable;
import org.openl.rules.data.binding.DataNodeBinder;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.property.DublicatedPropertiesTableException;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.properties.TableProperties;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.TokenizerParser;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Binder for property table.
 * 
 * @author DLiauchuk
 *
 */
public class PropertyTableBinder extends DataNodeBinder {

    private static final String DEFAULT_TABLE_NAME_PREFIX = "InheritedProperties: ";

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn, OpenL openl,
            IBindingContext cxt, XlsModuleOpenClass module) throws Exception {
        assert cxt instanceof RulesModuleBindingContext;
        
        PropertyTableBoundNode propertyNode = (PropertyTableBoundNode) makeNode(tsn, module);
        
        String tableName = parseHeader(tsn);
        propertyNode.setTableName(tableName);
        if (tableName == null) {
            tableName = DEFAULT_TABLE_NAME_PREFIX + tsn.getUri();
        }                
        
        ITable propertyTable = module.getDataBase().addNewTable(tableName, tsn);
        
        IOpenClass propertiesClass = JavaOpenClass.getOpenClass(TableProperties.class);
        
        ILogicalTable  propTableBody = getTableBody(tsn);     
        
        processTable(module, propertyTable, propTableBody, tableName, propertiesClass, cxt, openl, false);

        TableProperties propertiesInstance = ((TableProperties[])propertyTable.getDataArray())[0];
        
        propertiesInstance.setPropertiesSection(tsn.getTable());
        
        tsn.setTableProperties(propertiesInstance);

        analysePropertiesNode(tsn, propertiesInstance, (RulesModuleBindingContext)cxt, propertyNode);
        
        propertyNode.setPropertiesInstance(propertiesInstance);

        return propertyNode;
    }
    
    /**
     * Parses table header. Consider that second token is the name of the table.
     * <br><b>e.g.: Properties [tableName].</b>
     * 
     * @param tsn <code>{@link TableSyntaxNode}</code>
     * @return table name if exists.
     */
    private String parseHeader(TableSyntaxNode tsn) {
        String tableName = null;
        
        ILogicalTable table = LogicalTable.logicalTable(tsn.getTable());
        
        IOpenSourceCodeModule src = new GridCellSourceCodeModule(table.getGridTable());

        IdentifierNode[] parsedHeader = TokenizerParser.tokenize(src, " \n\r");

        if (parsedHeader.length > 1) {
            tableName = parsedHeader[1].getIdentifier();            
        }
        return tableName;
    }
    
    /**
     * Checks if current property table is a module level property or a category level. Adds it to 
     * <code>{@link RulesModuleBindingContext}</code>. 
     * <br>If module level properties already exists, or there are properties for the category with the same 
     * name throws an <code>{@link DublicatedPropertiesTableException}</code>.
     * 
     * @param tsn <code>{@link TableSyntaxNode}</code>.
     * @param propertiesInstance <code>{@link TableProperties}</code>.
     * @param cxt <code>{@link RulesModuleBindingContext}</code>.
     * @param propertyNode Bound node for current property table.
     * @throws DublicatedPropertiesTableException if module level properties already exists, or there are 
     * properties for the category with the same name.
     */
    private void analysePropertiesNode(TableSyntaxNode tsn, TableProperties propertiesInstance, 
            RulesModuleBindingContext cxt, PropertyTableBoundNode propertyNode) 
            throws DublicatedPropertiesTableException {        
         
        if (isModuleProperties(propertiesInstance)) {
            XlsWorkbookSourceCodeModule  module = ((XlsSheetSourceCodeModule)tsn.getModule()).getWorkbookSource();
            if (!cxt.isExistModuleProperties()) {
                cxt.setModuleProperties(propertiesInstance);
            } else {
                String moduleName = module.getDisplayName();
                throw new DublicatedPropertiesTableException(String.format("Properties for module %s already exists",
                        moduleName), propertyNode);
            }
        } else {
            String category = getCategoryToApplyProperties(tsn, propertiesInstance);
            if (!cxt.isExistCategoryProperties(category)){
                cxt.addCategoryProperties(category, propertiesInstance);
            } else {           
                throw new DublicatedPropertiesTableException(String.format("Properties for category %s already exists", 
                        category), propertyNode);
            }
        }
    }
    
    /**
     * Find out the name of the category to apply properties for.
     * 
     * @param tsn <code>{@link TableSyntaxNode}</code>
     * @param properties <code>{@link TableProperties}</code>
     * @return the name of the category to apply properties for.
     */
    private String getCategoryToApplyProperties(TableSyntaxNode tsn, TableProperties properties) {
        String result = null;
        String category = properties.getCategory();
        if (category != null) {
            result = category; 
        } else {
            result = ((XlsSheetSourceCodeModule)tsn.getModule()).getSheetName();
        }
        return result;
    }
    
    /**
     * Checks if properties are module properties.
     * 
     * @param properties <code>{@link TableProperties}</code>
     * @return <code>TRUE</code> if there is property 'scope' with value 'module'.
     */
    private boolean isModuleProperties(TableProperties properties) {
        boolean result = false;
        String scope = properties.getScope();
        if (scope != null && "module".equals(scope)) {
            result = true;
        }
        return result;
    }

    protected ATableBoundNode makeNode(TableSyntaxNode tsn, XlsModuleOpenClass module) {
        return new PropertyTableBoundNode(tsn, module);
    }   
    
    @Override
    public ILogicalTable getPropertiesTableSection(ILogicalTable table) {
        if (table.getLogicalHeight() < 1) {
            return null;
        }

        ILogicalTable propTable = table.rows(1);
        
        
        return propTable;
    }
}
