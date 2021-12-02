package org.openl.binding.impl;

import java.util.concurrent.atomic.AtomicLong;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;

public class IfNodeBinderWithCSRSupport extends IfNodeBinder {

    private static final AtomicLong SPR_NAME_COUNTER = new AtomicLong();

    private static CustomSpreadsheetResultOpenClass mergeTwoCustomSpreadsheetResultTypes(
            CustomSpreadsheetResultOpenClass type1,
            CustomSpreadsheetResultOpenClass type2,
            IBindingContext bindingContext) {
        String namePart1 = type1.getName().startsWith(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX) ? type1.getName()
            .substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length()) : type1.getName();
        String namePart2 = type2.getName().startsWith(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX) ? type2.getName()
            .substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length()) : type2.getName();

        final String typeName = "IfNode" + namePart1 + "And" + namePart2;

        final CustomSpreadsheetResultOpenClass mergedCustomSpreadsheetResultOpenClass = new CustomSpreadsheetResultOpenClass(
            Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + "$If$" + SPR_NAME_COUNTER.incrementAndGet(),
            type1.getModule(),
            type1.getLogicalTable(),
            false);
        mergedCustomSpreadsheetResultOpenClass.updateWithType(type1);
        mergedCustomSpreadsheetResultOpenClass.updateWithType(type2);
        return mergedCustomSpreadsheetResultOpenClass;
    }

    @Override
    protected IBoundNode buildIfElseNode(ISyntaxNode node,
            IBindingContext bindingContext,
            IBoundNode conditionNode,
            IBoundNode thenNode,
            IOpenClass type,
            IBoundNode elseNode,
            IOpenClass elseType) {
        if (type instanceof CustomSpreadsheetResultOpenClass && elseType instanceof CustomSpreadsheetResultOpenClass) {
            CustomSpreadsheetResultOpenClass type1 = (CustomSpreadsheetResultOpenClass) type;
            CustomSpreadsheetResultOpenClass type2 = (CustomSpreadsheetResultOpenClass) elseType;
            if (!type1.equals(type2) && type1.getModule() == type2.getModule()) {
                return new IfNode(node,
                    conditionNode,
                    thenNode,
                    elseNode,
                    mergeTwoCustomSpreadsheetResultTypes(type1, type2, bindingContext));
            }
        }
        return super.buildIfElseNode(node, bindingContext, conditionNode, thenNode, type, elseNode, elseType);
    }

}
