package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.types.IOpenField;

/**
 * Representation of the single test unit in the test.
 *
 */
public class TestUnit extends BaseTestUnit {

    private Object expectedResult;
    private String expectedError;

    private Object actualResult;

    TestUnit(TestDescription test, Object res, Throwable error, long executionTime) {
        super(test, res, error, executionTime);
        this.expectedError = test.getExpectedError();
        this.expectedResult = test.getExpectedResult();
        this.actualResult = res;
    }

    /**
     * Gets the expected result.
     *
     * @return the value of expected result.
     */
    @Override
    public Object getExpectedResult() {
        return expectedError == null ? expectedResult : expectedError;
    }

    /**
     * Return the result of running current test case.
     *
     * @return exception that occurred during running, if it was. If no, returns the calculated result.
     */
    @Override
    public Object getActualResult() {
        Throwable actualError = getActualError();
        return actualError == null ? actualResult : actualError;
    }

    @Override
    public ParameterWithValueDeclaration getActualParam() {
        return new ParameterWithValueDeclaration("actual", getActualResult());
    }

    @Override
    public ParameterWithValueDeclaration[] getContextParams(TestUnitsResults objTestResult) {
        return TestUtils.getContextParams(objTestResult.getTestSuite(), getTest());
    }

    @Override
    public List<ComparedResult> getResultParams() {
        List<ComparedResult> params = new ArrayList<ComparedResult>();

        if (expectedError == null && getActualError() == null) {
            List<ComparedResult> results = getComparisonResults();
            for (ComparedResult comparedResult : results) {
                if (!(comparedResult.getActualValue() instanceof ParameterWithValueDeclaration)) {
                    comparedResult.setActualValue(new ParameterWithValueDeclaration(comparedResult.getFieldName(),
                        comparedResult.getActualValue()));
                }
                if (!(comparedResult.getExpectedValue() instanceof ParameterWithValueDeclaration)) {
                    comparedResult.setExpectedValue(new ParameterWithValueDeclaration(comparedResult.getFieldName(),
                        comparedResult.getExpectedValue()));
                }
                params.add(comparedResult);
            }
            return params;
        }

        ComparedResult result = new ComparedResult();
        result.setStatus(getResultStatus());
        result.setActualValue(new ParameterWithValueDeclaration("actualResult", getActualResult()));
        result.setExpectedValue(new ParameterWithValueDeclaration("expectedResult", getExpectedResult()));
        params.add(result);

        return params;
    }

    @Override
    public List<OpenLMessage> getErrors() {
        Throwable actualError = getActualError();
        if (actualError != null) {
            return OpenLMessagesUtils.newErrorMessages(actualError);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected boolean writeFailuresOnly() {
        return false;
    }

    public static class Builder implements ITestResultBuilder {

        private static Builder instance = new Builder();

        private Builder() { /* NON */ }

        public static Builder getInstance() {
            return instance;
        }

        @Override
        public ITestUnit build(TestDescription test, Object res, Throwable error, long executionTime) {
            return new TestUnit(test, res, error, executionTime);
        }
    }
}
