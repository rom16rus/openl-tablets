package org.openl.rules.ruleservice.publish.cache;

import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.instantiation.SingleModuleInstantiationStrategy;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.OpenClassHelper;
import org.openl.vm.IRuntimeEnv;

/**
 * Lazy method that will compile module declaring it and will get real method to
 * do operations with it.
 * 
 * @author PUdalau
 */
public abstract class LazyMethod extends LazyMember<IOpenMethod> implements IOpenMethod {
    private String methodName;

    private Class<?>[] argTypes;

    public LazyMethod(String methodName, Class<?>[] argTypes, IDependencyManager dependencyManager,
            boolean executionMode, ClassLoader classLoader, IOpenMethod original, Map<String, Object> externalParameters) {
        super(dependencyManager, executionMode, classLoader, original, externalParameters);
        this.methodName = methodName;
        this.argTypes = argTypes;
    }

    public IOpenMethod getMember(IRuntimeEnv env) {
        try {
            SingleModuleInstantiationStrategy instantiationStrategy = getCache().getInstantiationStrategy(getModule(env),
                isExecutionMode(),
                getDependencyManager(),
                getClassLoader());
            instantiationStrategy.setExternalParameters(getExternalParameters());
            CompiledOpenClass compiledOpenClass = instantiationStrategy.compile();
            IOpenClass[] argOpenTypes = OpenClassHelper.getOpenClasses(compiledOpenClass.getOpenClass(), argTypes);
            return compiledOpenClass.getOpenClass().getMatchingMethod(methodName, argOpenTypes);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to load lazy field.", e);
        }
    }

    public IMethodSignature getSignature() {
        return getOriginal().getSignature();
    }

    public IOpenMethod getMethod() {
        return this;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return getMember(env).invoke(target, params, env);
    }

}
