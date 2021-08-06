package org.openl.binding;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.binding.exception.*;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessage;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * @author snshor
 *
 */
public interface IBindingContext extends ICastFactory {

    Collection<OpenLMessage> getMessages();

    void addMessage(OpenLMessage message);

    void addMessages(Collection<OpenLMessage> messages);

    void addError(SyntaxNodeException error);

    /**
     * Adds new type to binding context.
     *
     * @param namespace type namespace
     * @param type type
     * @throws Exception if an error has occurred
     */
    IOpenClass addType(String namespace, IOpenClass type) throws DuplicatedTypeException;

    ILocalVar addVar(String namespace, String name, IOpenClass type) throws DuplicatedVarException;

    INodeBinder findBinder(ISyntaxNode node);

    IMethodCaller findMethodCaller(String namespace,
            String name,
            IOpenClass[] parTypes) throws AmbiguousMethodException;

    IOpenClass findType(String namespace, String typeName) throws AmbiguousTypeException;

    /**
     * @see {@link IOpenClass#getField(String, boolean)}
     */
    IOpenField findVar(String namespace, String vname, boolean strictMatch) throws AmbiguousFieldException;

    /**
     * @return reference to the variable holding a range object. The specifics of the range object is that it is defined
     *         by a pair of the variables called start and end. There is no common range interface, the details must be
     *         contained in the implementation of a particular range type
     */

    IOpenField findRange(String namespace, String rangeStartName, String rangeEndName) throws OpenLCompilationException;

    @Override
    IOpenCast getCast(IOpenClass from, IOpenClass to);

    SyntaxNodeException[] getErrors();

    int getLocalVarFrameSize();

    OpenL getOpenL();

    IOpenClass getReturnType();

    List<SyntaxNodeException> popErrors();

    Collection<OpenLMessage> popMessages();

    void popLocalVarContext();

    /**
     * Used for doing temporary processing within current context
     */
    void pushErrors();

    void pushMessages();

    void pushLocalVarContext();

    void setReturnType(IOpenClass type);

    void setExecutionMode(boolean executionMode);

    /**
     * @return <code>true</code> if it is execution mode binding.
     */
    boolean isExecutionMode();

    void setExternalParams(Map<String, Object> params);

    Map<String, Object> getExternalParams();

    /**
     * Analyzes the binding context and returns the name for internal/temporary/service variable with the name:
     * varNamePrefix + '$' + available_index.
     */
    default String getTemporaryVarName() {
        int index = 0;
        String tmpVarName = "tmp$";
        while (findVar(ISyntaxConstants.THIS_NAMESPACE, tmpVarName, true) != null) {
            tmpVarName = "tmp$" + index;
            index++;
        }
        return tmpVarName;
    }
}
