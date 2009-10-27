/**
 * Created Apr 19, 2007
 */
package org.openl.rules.search;

import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.AStringBoolOperator;

/**
 * Handles checking of the table, during advanced search if it satisfy to the search condition for property 
 * selections.
 * @author <i>snshor</i><br>
 * commented by <i>DLiauchuk<i>
 */

public class PropertySelector extends ATableSyntaxNodeSelector {
    
    private AStringBoolOperator propertyNameSelector;

    private AStringBoolOperator propertyValueSelector;

    /**
     * Constructs the object, according to the info that was set to the search condition.
     * 
     * @param se Search condition. 
     */
    public PropertySelector(SearchConditionElement se) {
        propertyNameSelector = se.isAny(se.getElementValueName()) ? null : AStringBoolOperator.makeOperator("matches", 
                se.getElementValueName());
        propertyValueSelector = se.isAny(se.getElementValue()) ? null : AStringBoolOperator.makeOperator(se.getOpType2(),
                se.getElementValue());
    }
    
    /**
     * Checks if the property from the table matches to the property defined in search condition. 
     * 
     * @param property Property from table.
     * @param tableProperties All properties from table. This parameter is used, because of the method 
     * <code>{@link TableProperties#getPropertyValueAsString(String)}</code> to get the value of the property as string.
     * 
     * @return <code>True</code> if property name from search condition is <code>null</code> or matches to the property 
     * name from the table and at the same time if the property value from search condition is <code>null</code> or 
     * matches to the the property value from the table. In other way <code>false</code>.
     */
    private boolean doesPropertyMatch(TableProperties.Property property, TableProperties tableProperties) {
        return (propertyNameSelector == null || propertyNameSelector.isMatching(property.getKey().getValue()))
                && (propertyValueSelector == null || propertyValueSelector.isMatching(tableProperties
                        .getPropertyValueAsString(property.getKey().getValue())));
    }

    @Override
    public boolean doesTableMatch(TableSyntaxNode tsn) {
        TableProperties tableProperties = tsn.getTableProperties();
        if (tableProperties == null) {
            return propertyNameSelector == null && propertyValueSelector == null;
        }

        TableProperties.Property[] properties = tableProperties.getProperties();
        for (int i = 0; i < properties.length; i++) {
            if (doesPropertyMatch(properties[i], tableProperties)) {
                return true;
            }
        }

        return false;
    }

}
