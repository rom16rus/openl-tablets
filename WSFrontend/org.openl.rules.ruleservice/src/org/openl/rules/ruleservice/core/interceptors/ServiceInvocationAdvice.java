package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.openl.binding.MethodUtil;
import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.core.RuleServiceOpenLCompilationException;
import org.openl.rules.ruleservice.core.RuleServiceOpenLServiceInstantiationHelper;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.ruleservice.core.RuleServiceWrapperException;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAroundInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallBeforeInterceptor;
import org.openl.rules.testmethod.OpenLUserRuntimeException;
import org.openl.runtime.ASMProxyHandler;
import org.openl.runtime.IEngineWrapper;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.util.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

/**
 * Advice for processing method intercepting. Exception wrapping. And fix memory leaks.
 * <p/>
 * Only for RuleService internal use.
 *
 * @author Marat Kamalov
 */
public final class ServiceInvocationAdvice implements ASMProxyHandler, Ordered {

    private final Logger log = LoggerFactory.getLogger(ServiceInvocationAdvice.class);

    private static final String MSG_SEPARATOR = "; ";

    private final Map<Method, List<ServiceMethodBeforeAdvice>> beforeInterceptors = new HashMap<>();
    private final Map<Method, List<ServiceMethodAfterAdvice<?>>> afterInterceptors = new HashMap<>();
    private final Map<Method, ServiceMethodAroundAdvice<?>> aroundInterceptors = new HashMap<>();
    private final Map<Method, ServiceExtraMethodHandler<?>> serviceExtraMethodAnnotations = new HashMap<>();

    private final Object serviceTarget;
    private final Class<?> serviceClass;
    private final ClassLoader serviceClassLoader;
    private final IOpenClass openClass;
    private final Collection<ServiceInvocationAdviceListener> serviceMethodAdviceListeners;

    public ServiceInvocationAdvice(IOpenClass openClass,
            Object serviceTarget,
            Class<?> serviceClass,
            ClassLoader serviceClassLoader,
            Collection<ServiceInvocationAdviceListener> serviceMethodAdviceListeners) {
        this.serviceTarget = serviceTarget;
        this.serviceClass = serviceClass;
        this.serviceClassLoader = serviceClassLoader;
        this.openClass = openClass;
        this.serviceMethodAdviceListeners = serviceMethodAdviceListeners != null ? new ArrayList<>(
            serviceMethodAdviceListeners) : new ArrayList<>();
        init();
    }

    public Map<Method, List<ServiceMethodAfterAdvice<?>>> getAfterInterceptors() {
        return afterInterceptors;
    }

    public Map<Method, List<ServiceMethodBeforeAdvice>> getBeforeInterceptors() {
        return beforeInterceptors;
    }

    public Map<Method, ServiceMethodAroundAdvice<?>> getAroundInterceptors() {
        return aroundInterceptors;
    }

    public Map<Method, ServiceExtraMethodHandler<?>> getServiceExtraMethodAnnotations() {
        return serviceExtraMethodAnnotations;
    }

    private void init() {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(serviceClassLoader);
            for (Method method : serviceClass.getMethods()) {
                Annotation[] methodAnnotations = method.getAnnotations();
                for (Annotation annotation : methodAnnotations) {
                    checkForBeforeInterceptor(method, annotation);
                    checkForAfterInterceptor(method, annotation);
                    checkForAroundInterceptor(method, annotation);
                    checkForServiceExtraMethodAnnotation(method, annotation);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private void processAware(Object o, Method method) {
        if (o instanceof IOpenClassAware) {
            ((IOpenClassAware) o).setIOpenClass(openClass);
        }
        try {
            AnnotationUtils.inject(o, InjectOpenClass.class, (e) -> openClass);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to inject '{}' class instance.", IOpenClass.class.getTypeName());
        }
        if (o instanceof IOpenMemberAware) {
            IOpenMember openMember = findIOpenMember(method);
            if (openMember != null) {
                ((IOpenMemberAware) o).setIOpenMember(openMember);
            }
        }
        try {
            AnnotationUtils.inject(o, InjectOpenMember.class, (e) -> findIOpenMember(method));
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to inject '{}' class instance.", IOpenMember.class.getTypeName());
        }
    }

    private IOpenMember findIOpenMember(Method method) {
        for (Class<?> interf : serviceTarget.getClass().getInterfaces()) {
            try {
                Method m = interf.getMethod(method.getName(), method.getParameterTypes());
                return RuleServiceOpenLServiceInstantiationHelper.getOpenMember(m, serviceTarget);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private void checkForAroundInterceptor(Method method, Annotation annotation) {
        if (annotation instanceof ServiceCallAroundInterceptor) {
            Class<? extends ServiceMethodAroundAdvice<?>> interceptorClass = ((ServiceCallAroundInterceptor) annotation)
                .value();
            try {
                ServiceMethodAroundAdvice<?> aroundInterceptor = interceptorClass.newInstance();
                processAware(aroundInterceptor, method);
                aroundInterceptors.put(method, aroundInterceptor);
            } catch (Exception e) {
                throw new RuleServiceRuntimeException(String.format(
                    "Failed to instantiate 'around' interceptor for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                    MethodUtil.printQualifiedMethodName(method),
                    interceptorClass.getTypeName()), e);
            }
        }
    }

    private void checkForBeforeInterceptor(Method method, Annotation annotation) {
        if (annotation instanceof ServiceCallBeforeInterceptor) {
            Class<? extends ServiceMethodBeforeAdvice>[] interceptorClasses = ((ServiceCallBeforeInterceptor) annotation)
                .value();
            List<ServiceMethodBeforeAdvice> interceptors = beforeInterceptors.computeIfAbsent(method,
                e -> new ArrayList<>());
            for (Class<? extends ServiceMethodBeforeAdvice> interceptorClass : interceptorClasses) {
                try {
                    ServiceMethodBeforeAdvice preInterceptor = interceptorClass.newInstance();
                    processAware(preInterceptor, method);
                    interceptors.add(preInterceptor);
                } catch (Exception e) {
                    throw new RuleServiceRuntimeException(String.format(
                        "Failed to instantiate 'before' interceptor for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                        MethodUtil.printQualifiedMethodName(method),
                        interceptorClass.getTypeName()), e);
                }
            }
        }
    }

    private void checkForServiceExtraMethodAnnotation(Method method, Annotation annotation) {
        if (annotation instanceof ServiceExtraMethod) {
            Class<? extends ServiceExtraMethodHandler<?>> serviceExtraMethodHandlerClass = ((ServiceExtraMethod) annotation)
                .value();
            try {
                ServiceExtraMethodHandler<?> serviceExtraMethodHandler = serviceExtraMethodHandlerClass.newInstance();
                processAware(serviceExtraMethodHandler, method);
                serviceExtraMethodAnnotations.put(method, serviceExtraMethodHandler);
            } catch (Exception e) {
                throw new RuleServiceRuntimeException(String.format(
                    "Failed to instantiate service method handler for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                    MethodUtil.printQualifiedMethodName(method),
                    serviceExtraMethodHandlerClass.getTypeName()), e);
            }
        }
    }

    private void checkForAfterInterceptor(Method method, Annotation annotation) {
        if (annotation instanceof ServiceCallAfterInterceptor) {
            Class<? extends ServiceMethodAfterAdvice<?>>[] interceptorClasses = ((ServiceCallAfterInterceptor) annotation)
                .value();
            List<ServiceMethodAfterAdvice<?>> interceptors = afterInterceptors.computeIfAbsent(method,
                e -> new ArrayList<>());
            for (Class<? extends ServiceMethodAfterAdvice<?>> interceptorClass : interceptorClasses) {
                try {
                    ServiceMethodAfterAdvice<?> postInterceptor = interceptorClass.newInstance();
                    processAware(postInterceptor, method);
                    interceptors.add(postInterceptor);
                } catch (Exception e) {
                    throw new RuleServiceRuntimeException(String.format(
                        "Failed to instantiate 'afterReturning' interceptor for method '%s'. Please, check that class '%s' is not abstract and has a default constructor.",
                        MethodUtil.printQualifiedMethodName(method),
                        interceptorClass.getTypeName()), e);
                }
            }
        }
    }

    private void beforeInvocation(Method interfaceMethod, Object... args) throws Throwable {
        List<ServiceMethodBeforeAdvice> preInterceptors = beforeInterceptors.get(interfaceMethod);
        if (preInterceptors != null) {
            for (ServiceMethodBeforeAdvice interceptor : preInterceptors) {
                invokeBeforeServiceMethodAdviceOnListeners(interceptor, interfaceMethod, args, null, null);
                interceptor.before(interfaceMethod, serviceTarget, args);
                invokeAfterServiceMethodAdviceOnListeners(interceptor, interfaceMethod, args, null, null);
            }
        }
    }

    private Object serviceExtraMethodInvoke(Method interfaceMethod,
            Object serviceBean,
            Object... args) throws Exception {
        ServiceExtraMethodHandler<?> serviceExtraMethodHandler = serviceExtraMethodAnnotations.get(interfaceMethod);
        if (serviceExtraMethodHandler != null) {
            return serviceExtraMethodHandler.invoke(interfaceMethod, serviceBean, args);
        }
        throw new OpenLRuntimeException("Service method advice is not found.");
    }

    private Object afterInvocation(Method interfaceMethod,
            Object result,
            Exception t,
            Object... args) throws Exception {
        List<ServiceMethodAfterAdvice<?>> postInterceptors = afterInterceptors.get(interfaceMethod);
        if (postInterceptors != null && !postInterceptors.isEmpty()) {
            Object ret = result;
            Exception lastOccurredException = t;
            for (ServiceMethodAfterAdvice<?> interceptor : postInterceptors) {
                invokeBeforeServiceMethodAdviceOnListeners(interceptor,
                    interfaceMethod,
                    args,
                    ret,
                    lastOccurredException);
                try {
                    if (lastOccurredException == null) {
                        ret = interceptor.afterReturning(interfaceMethod, ret, args);
                    } else {
                        ret = interceptor.afterThrowing(interfaceMethod, lastOccurredException, args);
                    }
                    lastOccurredException = null;
                } catch (Exception e) {
                    lastOccurredException = e;
                    ret = null;
                }
                invokeAfterServiceMethodAdviceOnListeners(interceptor,
                    interfaceMethod,
                    args,
                    ret,
                    lastOccurredException);
            }
            if (lastOccurredException != null) {
                throw lastOccurredException;
            } else {
                return ret;
            }
        } else {
            if (t != null) {
                throw t;
            } else {
                return result;
            }
        }
    }

    private void invokeAfterServiceMethodAdviceOnListeners(ServiceMethodAdvice interceptor,
            Method interfaceMethod,
            Object[] args,
            Object ret,
            Exception lastOccurredException) {
        for (ServiceInvocationAdviceListener listener : serviceMethodAdviceListeners) {
            try {
                listener.afterServiceMethodAdvice(interceptor,
                    interfaceMethod,
                    args,
                    ret,
                    lastOccurredException,
                    e -> processAware(e, interfaceMethod));
            } catch (Exception e1) {
                log.error("Exception occurred.", e1);
            }
        }
    }

    private void invokeBeforeServiceMethodAdviceOnListeners(ServiceMethodAdvice interceptor,
            Method interfaceMethod,
            Object[] args,
            Object ret,
            Exception lastOccurredException) {
        for (ServiceInvocationAdviceListener listener : serviceMethodAdviceListeners) {
            try {
                listener.beforeServiceMethodAdvice(interceptor,
                    interfaceMethod,
                    args,
                    ret,
                    lastOccurredException,
                    e -> processAware(e, interfaceMethod));
            } catch (Exception e1) {
                log.error("Exception occurred.", e1);
            }
        }
    }

    @Override
    public Object invoke(Method calledMethod, Object[] args) {
        String methodName = calledMethod.getName();
        Class<?>[] parameterTypes = calledMethod.getParameterTypes();
        Method interfaceMethod = MethodUtil.getMatchingAccessibleMethod(serviceClass, methodName, parameterTypes);
        Object result = null;
        try {
            Method beanMethod = null;
            if (!calledMethod.isAnnotationPresent(ServiceExtraMethod.class)) {
                beanMethod = MethodUtil
                    .getMatchingAccessibleMethod(serviceTarget.getClass(), methodName, parameterTypes);
                if (beanMethod == null) {
                    throw new OpenLRuntimeException(String.format(
                        "Called method is not found in the service bean. Please, check that excel file contains method '%s'.",
                        MethodUtil.printMethod(methodName, parameterTypes)));
                }
            }
            try {
                ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(serviceClassLoader);
                    beforeInvocation(interfaceMethod, args);
                    ServiceMethodAroundAdvice<?> serviceMethodAroundAdvice = aroundInterceptors.get(interfaceMethod);
                    try {
                        invokeBeforeMethodInvocationOnListeners(interfaceMethod, args);
                        if (serviceMethodAroundAdvice != null) {
                            invokeBeforeServiceMethodAdviceOnListeners(serviceMethodAroundAdvice,
                                interfaceMethod,
                                args,
                                null,
                                null);
                            try {
                                result = serviceMethodAroundAdvice
                                    .around(interfaceMethod, beanMethod, serviceTarget, args);
                            } finally {
                                invokeAfterServiceMethodAdviceOnListeners(serviceMethodAroundAdvice,
                                    interfaceMethod,
                                    args,
                                    result,
                                    null);
                            }
                        } else {
                            if (beanMethod != null) {
                                result = beanMethod.invoke(serviceTarget, args);
                            } else {
                                result = serviceExtraMethodInvoke(interfaceMethod, serviceTarget, args);
                            }
                        }
                    } finally {
                        invokeAfterMethodInvocationOnListeners(interfaceMethod, args, result);
                    }
                    result = afterInvocation(interfaceMethod, result, null, args);
                    // repack result if arrays inside it doesn't have the returnType as interfaceMethod
                    if (interfaceMethod.getReturnType().isArray()) {
                        result = ArrayUtils.repackArray(result, interfaceMethod.getReturnType());
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(oldClassLoader);
                }
            } catch (InvocationTargetException e) {
                Throwable t = extractInvocationTargetException(e);
                if (t instanceof Exception) {
                    result = afterInvocation(interfaceMethod, null, (Exception) t, args);
                } else if (t instanceof Error) {
                    throw (Error) t;
                } else {
                    throw new Exception(t);
                }
            } catch (Exception e) {
                result = afterInvocation(interfaceMethod, null, e, args);
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {
                throw new Exception(t);
            }
            return result;
        } catch (Exception t) {
            Pair<ExceptionType, String> p = getExceptionDetailAndType(t);
            throw new RuleServiceWrapperException(p.getRight(),
                p.getLeft(),
                getExceptionMessage(calledMethod, t, args),
                t);
        } finally {
            // Memory leaks fix.
            if (serviceTarget instanceof IEngineWrapper) {
                IEngineWrapper engine = (IEngineWrapper) serviceTarget;
                engine.release();
            } else {
                log.warn(
                    "Service bean does not implement IEngineWrapper interface. Please, don't use deprecated static wrapper classes.");
            }
        }
    }

    private void invokeAfterMethodInvocationOnListeners(Method interfaceMethod, Object[] args, Object result) {
        RuntimeException lastOccurredEx = null;
        for (ServiceInvocationAdviceListener listener : serviceMethodAdviceListeners) {
            try {
                listener
                    .afterMethodInvocation(interfaceMethod, args, result, lastOccurredEx, e -> processAware(e, interfaceMethod));
            } catch (RuntimeException e1) {
                if (lastOccurredEx != null) {
                    e1.addSuppressed(lastOccurredEx);
                }
                lastOccurredEx = e1;
            }
        }
        if (lastOccurredEx != null) {
            throw lastOccurredEx;
        }
    }

    private void invokeBeforeMethodInvocationOnListeners(Method interfaceMethod, Object[] args) {
        RuntimeException lastOccurredEx = null;
        for (ServiceInvocationAdviceListener listener : serviceMethodAdviceListeners) {
            try {
                listener
                    .beforeMethodInvocation(interfaceMethod, args, null, null, e -> processAware(e, interfaceMethod));
            } catch (RuntimeException e1) {
                if (lastOccurredEx != null) {
                    e1.addSuppressed(lastOccurredEx);
                }
                lastOccurredEx = e1;
            }
        }
        if (lastOccurredEx != null) {
            throw lastOccurredEx;
        }
    }

    private Throwable extractInvocationTargetException(Throwable e) {
        Throwable t = e;
        while (t instanceof InvocationTargetException || t instanceof UndeclaredThrowableException) {
            if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException) t).getTargetException();
            }
            if (t instanceof UndeclaredThrowableException) {
                t = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
            }
        }
        return t;
    }

    private Pair<ExceptionType, String> getExceptionDetailAndType(Exception ex) {
        Throwable t = ex;

        ExceptionType type = ExceptionType.SYSTEM;
        String message = ex.getMessage();

        boolean f = true;
        while (f) {
            t = extractInvocationTargetException(t);
            if (t instanceof OpenLUserRuntimeException) {
                type = ExceptionType.USER_ERROR;
                message = t.getMessage();
            } else if (t instanceof OutsideOfValidDomainException) {
                type = ExceptionType.VALIDATION;
                message = ((OutsideOfValidDomainException) t).getOriginalMessage();
            } else if (t instanceof OpenLRuntimeException) {
                type = ExceptionType.RULES_RUNTIME;
                message = ((OpenLRuntimeException) t).getOriginalMessage();
            } else if (t instanceof OpenLCompilationException || t instanceof RuleServiceOpenLCompilationException) {
                type = ExceptionType.COMPILATION;
                message = t.getMessage();
            }
            if (t.getCause() == null) {
                f = false;
            } else {
                t = t.getCause();
            }
        }
        return new ImmutablePair<>(type, message);
    }

    private String getExceptionMessage(Method method, Throwable ex, Object... args) {
        StringBuilder argsTypes = new StringBuilder();
        boolean f = false;
        for (Class<?> clazz : method.getParameterTypes()) {
            if (f) {
                argsTypes.append(", ");
            } else {
                f = true;
            }
            argsTypes.append(clazz.getName());
        }
        StringBuilder argsValues = new StringBuilder();
        f = false;
        for (Object arg : args) {
            if (f) {
                argsValues.append(", ");
            } else {
                f = true;
            }
            if (arg == null) {
                argsValues.append("null");
            } else {
                argsValues.append(arg);

            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("During OpenL rule execution exception was occurred. Method name is '".toUpperCase());
        sb.append(method.getName());
        sb.append("'. Arguments types are: ");
        sb.append(argsTypes);
        sb.append(". Arguments values are: ");
        sb.append(argsValues.toString().replace("\r", "").replace("\n", ""));
        sb.append(". Exception class is: ");
        sb.append(ex.getClass().toString());
        sb.append(".");
        if (ex.getMessage() != null) {
            sb.append(" Exception message is: ");
            sb.append(ex.getMessage());
        }
        sb.append(" OpenL clause messages are: ");
        Throwable t = ex.getCause();
        boolean isNotFirst = false;
        while (t != null && t.getCause() != t) {
            if ((t instanceof OpenLException) && t.getMessage() != null) {
                if (isNotFirst) {
                    sb.append(MSG_SEPARATOR);
                }
                isNotFirst = true;
                if (t instanceof OpenLRuntimeException) {
                    sb.append(((OpenLRuntimeException) t).getOriginalMessage());
                } else {
                    sb.append(t.getMessage());
                }
            }
            t = t.getCause();
        }
        return sb.toString();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
