package org.openl.rules.vm;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.RecursiveAction;

import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.lang.xls.binding.wrapper.IRulesMethodWrapper;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM.SimpleRuntimeEnv;

public class SimpleRulesRuntimeEnv extends SimpleRuntimeEnv {
    private volatile boolean methodArgumentsCacheEnable = false;
    private volatile CacheMode cacheMode = CacheMode.READ_ONLY;
    private volatile boolean ignoreRecalculate = true;
    private volatile boolean originalCalculation = true;
    private ArgumentCachingStorage argumentCachingStorage;
    private IRulesMethodWrapper methodWrapper;

    public SimpleRulesRuntimeEnv() {
        super();
    }

    private SimpleRulesRuntimeEnv(SimpleRulesRuntimeEnv env) {
        super(env);
        this.argumentCachingStorage = env.getArgumentCachingStorage();
        this.methodArgumentsCacheEnable = env.methodArgumentsCacheEnable;
        this.cacheMode = env.cacheMode;
        this.ignoreRecalculate = env.ignoreRecalculate;
        this.originalCalculation = env.originalCalculation;
    }

    @Override
    public IRuntimeEnv clone() {
        return new SimpleRulesRuntimeEnv(this);
    }

    public ArrayDeque<IRuntimeContext> cloneContextStack() {
        return new ArrayDeque<>(contextStack);
    }

    @Override
    protected IRuntimeContext buildDefaultRuntimeContext() {
        return RulesRuntimeContextFactory.buildRulesRuntimeContext();
    }

    public boolean isMethodArgumentsCacheEnable() {
        return methodArgumentsCacheEnable;
    }

    public void changeMethodArgumentsCacheMode(CacheMode mode) {
        this.cacheMode = mode;
    }

    public CacheMode getCacheMode() {
        return cacheMode;
    }

    public void setMethodArgumentsCacheEnable(boolean enable) {
        this.methodArgumentsCacheEnable = enable;
    }

    public boolean isIgnoreRecalculation() {
        return ignoreRecalculate;
    }

    public void setIgnoreRecalculate(boolean ignoreRecalculate) {
        this.ignoreRecalculate = ignoreRecalculate;
    }

    public boolean isOriginalCalculation() {
        return originalCalculation;
    }

    public void setOriginalCalculation(boolean originalCalculation) {
        this.originalCalculation = originalCalculation;
    }

    public ArgumentCachingStorage getArgumentCachingStorage() {
        if (argumentCachingStorage == null) {
            argumentCachingStorage = new ArgumentCachingStorage(this);
        }
        return argumentCachingStorage;
    }

    public IRulesMethodWrapper getMethodWrapper() {
        return methodWrapper;
    }

    public void setMethodWrapper(IRulesMethodWrapper methodWrapper) {
        this.methodWrapper = methodWrapper;
    }

    public IOpenClass getTopClass() {
        return topClass;
    }

    public void setTopClass(IOpenClass topClass) {
        this.topClass = topClass;
    }

    private IOpenClass topClass;

    private Queue<RecursiveAction> actionStack = null;

    public void pushAction(RecursiveAction action) {
        if (actionStack == null) {
            actionStack = new LinkedList<>();
        }
        actionStack.add(action);
    }

    public boolean joinActionIfExists() {
        if (actionStack != null && !actionStack.isEmpty()) {
            RecursiveAction action = actionStack.poll();
            action.join();
            return true;
        }
        return false;
    }

    public boolean cancelActionIfExists() {
        if (actionStack != null && !actionStack.isEmpty()) {
            RecursiveAction action = actionStack.poll();
            action.cancel(true);
            return true;
        }
        return false;
    }
}
