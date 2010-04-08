package org.openl.rules.validator.dt;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openl.domain.DateRangeDomain;
import org.openl.domain.EnumDomain;
import org.openl.domain.IntRangeDomain;
import org.openl.domain.StringDomain;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.type.DateRangeDomainAdaptor;
import org.openl.rules.dt.type.EnumDomainAdaptor;
import org.openl.rules.dt.type.IDomainAdaptor;
import org.openl.rules.dt.type.IntRangeDomainAdaptor;
import org.openl.rules.dt.validator.DesionTableValidationResult;
import org.openl.rules.dt.validator.DecisionTableValidator;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;


public class ValidatorTest extends BaseOpenlBuilderHelper{
    
    private static String __src = "test/rules/Test_Validator_DT.xls";
    
    public ValidatorTest() {
        super(__src);        
    }
    
    @Test
    public void testOk() {
        String tableName = "Rules String validationOK(TestValidationEnum1 value1, TestValidationEnum2 value2)";
        Map<String, IDomainAdaptor> domains = new HashMap<String, IDomainAdaptor>();
        
//        EnumDomain<TestValidationEnum1> enumDomain1 = new EnumDomain<TestValidationEnum1>(new TestValidationEnum1[]{TestValidationEnum1.V1, TestValidationEnum1.V2});        
//        EnumDomainAdaptor enumDomainAdaptor1 = new EnumDomainAdaptor(enumDomain1);
//        domains.put("value1", enumDomainAdaptor1);
//        
//        EnumDomain<TestValidationEnum2> enumDomain2 = new EnumDomain<TestValidationEnum2>(new TestValidationEnum2[]{TestValidationEnum2.V1, TestValidationEnum2.V2});        
//        EnumDomainAdaptor enumDomainAdaptor2 = new EnumDomainAdaptor(enumDomain2);
//        domains.put("value2", enumDomainAdaptor2);
        
        DesionTableValidationResult dtValidResult = testTable(tableName, domains);
        assertFalse(dtValidResult.hasProblems());
    }    
    
    @Test
    public void testGap() {
        String tableName = "Rules String validationGap(TestValidationEnum1 value1, TestValidationEnum2 value2)";        
        DesionTableValidationResult dtValidResult = testTable(tableName, null);
        assertEquals(1, dtValidResult.getUncovered().length);
    }
    
    @Test
    public void testOverlap() {
        String tableName = "Rules String validationOverlap(TestValidationEnum1 value1, TestValidationEnum2 value2)";        
        DesionTableValidationResult dtValidResult = testTable(tableName, null);
        assertEquals(1, dtValidResult.getOverlappings().length);
    }
    
    @Test
    public void testIntRule() {
        String tableName = "Rules void hello1(int hour)";
        IntRangeDomain intRangeDomain = new IntRangeDomain(0,24);
        Map<String, IDomainAdaptor> domains = new HashMap<String, IDomainAdaptor>();
        IntRangeDomainAdaptor intRangeDomainAdaptor = new IntRangeDomainAdaptor(intRangeDomain);
        domains.put("hour", intRangeDomainAdaptor);
        
        DesionTableValidationResult dtValidResult = testTable(tableName, domains);
        assertEquals(1, dtValidResult.getUncovered().length);
        assertEquals("Param value missing", "hour=24", dtValidResult.getUncovered()[0].getValues().toString());
    }
        
    private DesionTableValidationResult testTable(String tableName, Map<String, IDomainAdaptor> domains) {
        DesionTableValidationResult result = null;
        TableSyntaxNode[] tsns = getTableSyntaxNodes();
        TableSyntaxNode resultTsn = findTable(tableName, tsns);
        if (resultTsn != null) {
            ITableProperties tableProperties  = resultTsn.getTableProperties();
            assertNotNull(tableProperties);
            assertTrue(getJavaWrapper().getCompiledClass().getBindingErrors().length == 0);
            assertTrue(getJavaWrapper().getCompiledClass().getParsingErrors().length == 0);
            
            DecisionTable dt = (DecisionTable) resultTsn.getMember();
            try {
                //System.out.println("Validating <" + tableName+ ">");
                result = DecisionTableValidator.validateTable(dt, domains, getJavaWrapper().getOpenClass());
                
                if (result.hasProblems()) {
                    resultTsn.setValidationResult(result);
                    //System.out.println("There are problems in table!!\n");
                } else {
                    //System.out.println("NO PROBLEMS IN TABLE!!!!\n");
                }
            } catch (Exception t) {
                //System.out.println("Exception " + t.getMessage());
            }
        } else {
            fail();
        }
        return result;
    }
    
    @Test
    public void testOk2() {
        String tableName = "Rules void hello2(int currentValue)";        
        IntRangeDomain intRangeDomain = new IntRangeDomain(0,50);
        Map<String, IDomainAdaptor> domains = new HashMap<String, IDomainAdaptor>();
        IntRangeDomainAdaptor intRangeDomainAdaptor = new IntRangeDomainAdaptor(intRangeDomain);
        domains.put("currentValue", intRangeDomainAdaptor);
        
        DesionTableValidationResult dtValidResult = testTable(tableName, domains);
        assertFalse(dtValidResult.hasProblems());
    } 
    
    @Test
    public void testString() {
        String tableName = "Rules void helloString(String stringValue)";        
        Map<String, IDomainAdaptor> domains = new HashMap<String, IDomainAdaptor>();
        StringDomain stringDomain = new StringDomain(new String[]{"value1", "value2", "value3"});
        EnumDomainAdaptor enumDomainStrAdaptor = new EnumDomainAdaptor(stringDomain);
        
        domains.put("stringValue", enumDomainStrAdaptor);
        domains.put("localValue", enumDomainStrAdaptor);
        
        DesionTableValidationResult dtValidResult = testTable(tableName, domains);
        assertTrue(dtValidResult.hasProblems());
    }

    @Test
    public void testDate() {
        String tableName = "Rules void testDate(Date currentDate)";
        Map<String, IDomainAdaptor> domains = new HashMap<String, IDomainAdaptor>();
        DateRangeDomain dateRangeDomain = new DateRangeDomain(new Date(0, 0, 1), new Date(150, 0, 1));//[01.01.1900 .. 01.01.2050]
        DateRangeDomainAdaptor adaptor = new DateRangeDomainAdaptor(dateRangeDomain);

        domains.put("currentDate", adaptor);
        domains.put("min", adaptor);
        domains.put("max", adaptor);

        DesionTableValidationResult dtValidResult = testTable(tableName, domains);
        assertTrue(!dtValidResult.hasProblems());
        
        dateRangeDomain.setMax(new Date(250, 0, 1));//01.01.2150
        dtValidResult = testTable(tableName, domains);
        assertTrue(dtValidResult.getUncovered().length == 1);
    }

}
