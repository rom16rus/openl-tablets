package org.openl.rules.testmethod;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.openl.rules.table.OpenLArgumentsCloner;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

@RunWith(MockitoJUnitRunner.class)
public class TestDescriptionTest {
    @Mock
    private IRuntimeEnv env;
    @Mock
    private Object target;

    private SomeArgument[] arguments;
    private final TestRunner testRunner = new TestRunner(TestUnit.Builder.getInstance());
    private final OpenLArgumentsCloner cloner = new OpenLArgumentsCloner();

    @Before
    public void setUp() {
        arguments = new SomeArgument[] { new SomeArgument("test") };
    }

    @Test
    public void testNotModifyInputParameters() {
        TestDescription description = new TestDescription(createTestMethodMock(), null, arguments, null);

        testRunner.runTest(description, target, env, cloner, 1);
        assertEquals("test", arguments[0].value);

        testRunner.runTest(description, target, env, cloner, 5);
        assertEquals("test", arguments[0].value);
    }

    private IOpenMethod createTestMethodMock() {
        IMethodSignature signature = createMethodSignatureMock();
        IOpenMethod method = mock(IOpenMethod.class);

        when(method.getSignature()).thenReturn(signature);

        when(method.invoke(any(), any(Object[].class), any(IRuntimeEnv.class))).thenAnswer((Answer<Void>) invocation -> {
            Object[] params = (Object[]) invocation.getArguments()[1];

            // Modify method input parameters
            SomeArgument someArgument = (SomeArgument) params[0];
            someArgument.value = "modified" + someArgument.value;
            return null;
        });

        return method;
    }

    private IMethodSignature createMethodSignatureMock() {
        IMethodSignature signature = mock(IMethodSignature.class);
        when(signature.getNumberOfParameters()).thenReturn(arguments.length);

        when(signature.getParameterName(anyInt())).thenAnswer((Answer<String>) invocation -> "param" + invocation.getArguments()[0]);

        return signature;
    }

    private static class SomeArgument {
        public String value;

        public SomeArgument(String value) {
            this.value = value;
        }
    }
}
