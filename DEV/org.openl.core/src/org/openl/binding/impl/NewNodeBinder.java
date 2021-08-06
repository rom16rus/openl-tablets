package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.util.MessageUtils;

/**
 * @author snshor
 */
public class NewNodeBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv,
     * org.openl.binding.IBindingContext)
     */
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        int childrenCount = node.getNumberOfChildren();

        if (childrenCount < 1) {
            return makeErrorNode("New node must have at least one subnode", node, bindingContext);
        }

        ISyntaxNode typeNode = node.getChild(0);
        String typeName = ((IdentifierNode) typeNode).getIdentifier();
        IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);

        if (type == null) {
            return makeErrorNode(MessageUtils.getTypeNotFoundMessage(typeName), typeNode, bindingContext);
        }
        if (type.getInstanceClass() == null) {
            return makeErrorNode(MessageUtils.getTypeDefinedErrorMessage(typeName), typeNode, bindingContext);
        }

        IBoundNode[] children = bindChildren(node, bindingContext, 1, childrenCount);
        if (hasErrorBoundNode(children)) {
            return new ErrorBoundNode(node);
        }
        IOpenClass[] types = getTypes(children);

        IMethodCaller methodCaller = MethodSearch.findConstructor(types, bindingContext, type);
        BindHelper.checkOnDeprecation(node, bindingContext, methodCaller);

        if (methodCaller == null) {
            String constructor = MethodUtil.printMethod(type.getName(), types);
            return makeErrorNode(MessageUtils.getConstructorNotFoundMessage(constructor), typeNode, bindingContext);
        }

        return new ConstructorParamsNode(new MethodBoundNode(node, methodCaller, children), true);
    }

}
