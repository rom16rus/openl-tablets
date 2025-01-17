package org.openl.rules.webstudio.web.test;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.util.formatters.IFormatter;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;
import org.springframework.stereotype.Service;

/**
 * A helper class which contains utility methods.
 */
@Service
public final class Helper {

    public Helper() {
        // THIS CONSTRUCTOR MUST BE EMPTY!!!
    }

    public TreeNode getRoot(ParameterDeclarationTreeNode parameter) {
        if (parameter == null) {
            return null;
        }
        TreeNodeImpl root = new TreeNodeImpl();
        root.addChild(parameter.getName(), parameter);
        return root;
    }

    public String format(Object value) {
        return FormattersManager.format(value);
    }

    public String formatText(Object value, boolean showRealNumbers) {
        if (value instanceof Number) {
            IFormatter formatter = FormattersManager.getFormatter(value.getClass(), showRealNumbers ? FormattersManager.DEFAULT_NUMBER_FORMAT : null);
            return formatter.format(value);
        } else {
            return format(value);
        }
    }

    public boolean isSpreadsheetResult(Object value) {
        return value instanceof SpreadsheetResult;
    }
}
