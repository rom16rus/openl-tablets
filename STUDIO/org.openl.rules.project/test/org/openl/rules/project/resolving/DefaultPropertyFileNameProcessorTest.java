package org.openl.rules.project.resolving;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.enumeration.CurrenciesEnum;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.table.properties.ITableProperties;

public class DefaultPropertyFileNameProcessorTest {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");

    @Test
    public void unknownPropertyTest() {
        try {
            new DefaultPropertiesFileNameProcessor("%unknownProperty%");
        } catch (InvalidFileNamePatternException e) {
            assertEquals("Found unsupported property 'unknownProperty' in file name pattern.", e.getMessage());
            return;
        }
        Assert.fail();
    }

    @Test
    public void lobTest() throws Exception {
        ITableProperties props = new DefaultPropertiesFileNameProcessor(
            "%lob%-%nature%-%state%-%effectiveDate:yyyy-MM-dd%-%startRequestDate:yyyy-MM-dd%")
                .process("AL-BL-CL-GL-NY-2018-07-01-2018-05-03");
        assertArrayEquals(props.getLob(), new String[] { "AL" });
        assertArrayEquals(props.getState(), new UsStatesEnum[] { UsStatesEnum.NY });
        assertEquals(props.getNature(), "BL-CL-GL");
        assertEquals(props.getEffectiveDate(), new Date(118, 06, 01, 0, 0, 0));
        assertEquals(props.getStartRequestDate(), new Date(118, 04, 03, 0, 0, 0));

        props = new DefaultPropertiesFileNameProcessor(
            "%lob%-%nature%-%state%-%effectiveDate:yyyyMMdd%-%startRequestDate:yyyyMMdd%")
                .process("AL,BL-CL,GL-DE,OH-20180701-20170621");
        assertArrayEquals(props.getLob(), new String[] { "AL", "BL" });
        assertArrayEquals(props.getState(), new UsStatesEnum[] { UsStatesEnum.DE, UsStatesEnum.OH });
        assertEquals(props.getNature(), "CL,GL");
        assertEquals(props.getEffectiveDate(), new Date(118, 06, 01, 0, 0, 0));
        assertEquals(props.getStartRequestDate(), new Date(117, 05, 21, 0, 0, 0));

        props = new DefaultPropertiesFileNameProcessor(
            "%lob%-%state%-%effectiveDate:ddMMyyyy%-%startRequestDate:ddMMyyyy%")
                .process("AL,BL-CL,GL-CA-20072019-21062020");
        assertArrayEquals(props.getLob(), new String[] { "AL", "BL-CL", "GL" });
        assertArrayEquals(props.getState(), new UsStatesEnum[] { UsStatesEnum.CA });
        assertNull(props.getNature());
        assertEquals(props.getEffectiveDate(), new Date(119, 06, 20, 0, 0, 0));
        assertEquals(props.getStartRequestDate(), new Date(120, 05, 21, 0, 0, 0));
    }

    @Test
    public void testPatternProperty_with_lob_array() throws NoMatchFileNameException,
                                                     InvalidFileNamePatternException,
                                                     ParseException {

        ITableProperties properties = new DefaultPropertiesFileNameProcessor(
            ".*-%lob%-%effectiveDate:ddMMyyyy%-%startRequestDate:ddMMyyyy%")
                .process("rules/Project-PMT,CMT-01012017-01012018.ext");

        assertArrayEquals(new String[] { "CMT", "PMT" }, properties.getLob());

        assertEquals(dateFormat.parse("01012017"), properties.getEffectiveDate());
        assertEquals(dateFormat.parse("01012018"), properties.getStartRequestDate());
    }

    @Test
    public void testPatternProperty_with_currencies_array() throws NoMatchFileNameException,
                                                            InvalidFileNamePatternException,
                                                            ParseException {

        ITableProperties properties = new DefaultPropertiesFileNameProcessor(
            ".*-%lob%-%effectiveDate:ddMMyyyy%-%startRequestDate:ddMMyyyy%-%currency%")
                .process("Project-PMT,CMT-01012017-01012018-EUR,UAH.xlsx");

        assertArrayEquals(new String[] { "CMT", "PMT" }, properties.getLob());
        assertArrayEquals(new CurrenciesEnum[] { CurrenciesEnum.EUR, CurrenciesEnum.UAH }, properties.getCurrency());

        assertEquals(dateFormat.parse("01012017"), properties.getEffectiveDate());
        assertEquals(dateFormat.parse("01012018"), properties.getStartRequestDate());
    }

    @Test(expected = NoMatchFileNameException.class)
    public void testPatternProperty_with_unknownEnumValue_array() throws NoMatchFileNameException,
                                                                  InvalidFileNamePatternException {

        new DefaultPropertiesFileNameProcessor(
            ".*-%lob%-%effectiveDate:ddMMyyyy%-%startRequestDate:ddMMyyyy%-%currency%")
                .process("Project-PMT,CMT-01012017-01012018-EUR,DEFAULT,UAH.xlsx");
    }

    @Test
    public void testPatternProperty_date_separator() throws NoMatchFileNameException,
                                                     InvalidFileNamePatternException,
                                                     ParseException {

        ITableProperties properties = new DefaultPropertiesFileNameProcessor(
            "%lob%-%state%-%startRequestDate:yyyy-MM-dd%").process("path/to.rules/AUTO-FL-2016-01-01");

        assertArrayEquals(new String[] { "AUTO" }, properties.getLob());
        assertArrayEquals(new UsStatesEnum[] { UsStatesEnum.FL }, properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    @Test
    public void testMultiPatterns0() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        PropertiesFileNameProcessor processor = PropertiesFileNameProcessorBuilder
            .buildDefault("%lob%-%state%-%startRequestDate%", "AUTO-%lob%-%startRequestDate%");
        ITableProperties properties = processor.process("AUTO-CW-20160101.xlsx");

        assertArrayEquals(new String[] { "AUTO" }, properties.getLob());
        assertArrayEquals(UsStatesEnum.values(), properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    @Test
    public void testMultiPatterns() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        PropertiesFileNameProcessor processor = PropertiesFileNameProcessorBuilder
            .buildDefault("%lob%-%state%-%startRequestDate%", "AUTO-%lob%-%startRequestDate%");
        ITableProperties properties = processor.process("AUTO-Any-20160101");

        assertArrayEquals(new String[] { "AUTO" }, properties.getLob());
        assertArrayEquals(UsStatesEnum.values(), properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    @Test
    public void testMultiPatterns1() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        PropertiesFileNameProcessor processor = PropertiesFileNameProcessorBuilder
            .buildDefault("%lob%-%state%-%startRequestDate%", "AUTO-%lob%-%startRequestDate%");
        ITableProperties properties = processor.process("AUTO-FL,ME-20160101.xlsx");

        assertArrayEquals(new String[] { "AUTO" }, properties.getLob());
        assertArrayEquals(new UsStatesEnum[] { UsStatesEnum.FL, UsStatesEnum.ME }, properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    @Test
    public void testMultiPatterns2() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        PropertiesFileNameProcessor processor = PropertiesFileNameProcessorBuilder
            .buildDefault("%lob%-%state%-%startRequestDate%", "AUTO-%lob%-%startRequestDate%");
        ITableProperties properties = processor.process("path.to/rules/AUTO-PMT-20160101.xlsx");

        assertArrayEquals(new String[] { "PMT" }, properties.getLob());
        assertArrayEquals(null, properties.getState());

        assertEquals(dateFormat.parse("01012016"), properties.getStartRequestDate());
    }

    @Test
    public void testMultiPatterns3() throws InvalidFileNamePatternException {
        try {
            PropertiesFileNameProcessor processor = PropertiesFileNameProcessorBuilder
                .buildDefault("%lob%-%state%-%startRequestDate%", "AUTO-%lob%-%startRequestDate%");
            processor.process("path.to/rules/Tests.xlsx");
            fail("Ooops...");
        } catch (NoMatchFileNameException e) {
            assertEquals("Module 'Tests' does not match file name pattern 'AUTO-%lob%-%startRequestDate%'.",
                e.getMessage());
        }
    }

    @Test
    public void testPropertyGroupsWrongDatePattern() throws InvalidFileNamePatternException {
        try {
            new DefaultPropertiesFileNameProcessor("%lob%-%state%-%effectiveDate,startRequestDate:ddMMyyyy%")
                .process("AUTO-FL,ME-20160101.ext");
            fail("Ooops...");
        } catch (NoMatchFileNameException e) {
            assertEquals(
                "Module 'AUTO-FL,ME-20160101' does not match file name pattern '%lob%-%state%-%effectiveDate,startRequestDate:ddMMyyyy%'.\r\n Invalid property: effectiveDate.\r\n Message: Failed to parse a date '20160101'..",
                e.getMessage());
        }
    }

    @Test
    public void testPropertyGroupsNegative() {
        try {
            new DefaultPropertiesFileNameProcessor("%lob%-%state%-%effectiveDate,lob%");
            fail("Ooops...");
        } catch (InvalidFileNamePatternException e) {
            assertEquals("Incompatible properties in the group: [effectiveDate, lob].", e.getMessage());
        }

        try {
            new DefaultPropertiesFileNameProcessor("%lob,nature%-%state%-%effectiveDate%");
            fail("Ooops...");
        } catch (InvalidFileNamePatternException e) {
            assertEquals("Incompatible properties in the group: [lob, nature].", e.getMessage());
        }

        try {
            new DefaultPropertiesFileNameProcessor("%lob%-%state,lang%-%effectiveDate%");
            fail("Ooops...");
        } catch (InvalidFileNamePatternException e) {
            assertEquals("Incompatible properties in the group: [state, lang].", e.getMessage());
        }

        try {
            new DefaultPropertiesFileNameProcessor("%lob%-%state,foo%-%effectiveDate%");
            fail("Ooops...");
        } catch (InvalidFileNamePatternException e) {
            assertEquals("Found unsupported property 'foo' in file name pattern.", e.getMessage());
        }
    }

    @Test
    public void testPropertyGroups() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        ITableProperties properties = new DefaultPropertiesFileNameProcessor(
            "%lob%-%state%-%effectiveDate,startRequestDate%").process("AUTO-FL,ME-20160101.xlsx");
        assertArrayEquals(new String[] { "AUTO" }, properties.getLob());
        assertArrayEquals(new UsStatesEnum[] { UsStatesEnum.FL, UsStatesEnum.ME }, properties.getState());
        Date date = dateFormat.parse("01012016");
        assertEquals(date, properties.getStartRequestDate());
        assertEquals(date, properties.getEffectiveDate());
    }

    @Test
    public void testPropertyGroups1() throws NoMatchFileNameException, InvalidFileNamePatternException, ParseException {
        ITableProperties properties = new DefaultPropertiesFileNameProcessor(
            "%lob%-%state%-%effectiveDate,startRequestDate:ddMMyyyy%").process("AUTO-FL,ME-01012016");
        assertArrayEquals(new String[] { "AUTO" }, properties.getLob());
        assertArrayEquals(new UsStatesEnum[] { UsStatesEnum.FL, UsStatesEnum.ME }, properties.getState());
        Date date = dateFormat.parse("01012016");
        assertEquals(date, properties.getStartRequestDate());
        assertEquals(date, properties.getEffectiveDate());
    }

}
