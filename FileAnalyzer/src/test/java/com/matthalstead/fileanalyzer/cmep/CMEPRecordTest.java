package com.matthalstead.fileanalyzer.cmep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.Ignore;
import org.junit.Test;

import com.matthalstead.fileanalyzer.cmep.CMEPRecord.IntervalTriplet;

/**
 * 
 * @author Matt
 *
 */
public class CMEPRecordTest {
	
	private void assertTimestampsEqual(String message, String expected, Date foundDate) throws Exception {
		if (expected == null) {
			assertNull(message + ": should have been null", foundDate);
		} else {
			assertNotNull(message + ": should have been non-null", foundDate);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			Date expectedDate = sdf.parse(expected);
			assertEquals(message, expectedDate, foundDate);
		}
	}
	
	private void assertTripletsEqual(String message, String expectedTimestamp, String expectedDQLetter, long expectedDQFlags, String expectedValue, IntervalTriplet found) throws Exception {
		assertTimestampsEqual(message + ": timestamp was wrong", expectedTimestamp, found.getTimestamp());
		assertEquals(message + ": data quality letter was wrong", expectedDQLetter, found.getDataQualityLetter());
		assertEquals(message + ": data quality flags was wrong", expectedDQFlags, found.getDataQualityFlags());
		assertEquals(message + ": value was wrong", expectedValue, found.getValueString());
		
	}
	
	@Test
	public void testSimple() throws Exception {

		String str = "MEPMD01,20080501,VENDOR,UTIL:153000,1234567,9876543,201907091845,9988776,OK,G,CCFREG,1.0,00000100,3,201907071800,R0,12345,201907080500,R0,12346,201907090500,R0,12347";
		CMEPRecord record = CMEPRecord.parse(str);
		assertEquals("RecordType was wrong", "MEPMD01", record.getRecordType());
		assertEquals("ModuleId was wrong", "1234567", record.getModuleId());
		assertEquals("MeterID was wrong", "9876543", record.getMeterId());
		assertTimestampsEqual("FileTimestamp was wrong", "201907091845", record.getFileTimestamp());
		assertEquals("MeterLocation was wrong", "9988776", record.getMeterLocation());
		assertEquals("Units was wrong", "CCFREG", record.getUnits());
		assertEquals("IntervalLengthCode was wrong", "00000100", record.getIntervalLengthCode());
		assertEquals("ListedIntervalCount was wrong", 3, record.getListedIntervalCount());
		
		List<IntervalTriplet> foundTriplets = record.getTriplets();
		assertNotNull("Found triplets were null", foundTriplets);
		assertEquals("Wrong size of triplets list", 3, foundTriplets.size());
		assertTripletsEqual("interval[0] was wrong", "201907071800", "R", 0L, "12345", foundTriplets.get(0));
		assertTripletsEqual("interval[1] was wrong", "201907080500", "R", 0L, "12346", foundTriplets.get(1));
		assertTripletsEqual("interval[2] was wrong", "201907090500", "R", 0L, "12347", foundTriplets.get(2));

	}
	
	@Test
	@Ignore
	public void testMeterIdWithLeadingComma() throws Exception {

		String str = "MEPMD01,20080501,VENDOR,UTIL:153000,1234567,9876543,201907091845,,9988776,OK,G,CCFREG,1.0,00000100,2,201907080500,R0,2073,201907090500,R0,2074";
		CMEPRecord record = CMEPRecord.parse(str);
		assertEquals("RecordType was wrong", "MEPMD01", record.getRecordType());
		assertEquals("ModuleId was wrong", "1234567", record.getModuleId());
		assertEquals("MeterID was wrong", "9876543", record.getMeterId());
		assertTimestampsEqual("FileTimestamp was wrong", "201907091845", record.getFileTimestamp());
		assertEquals("MeterLocation was wrong", ",9988776", record.getMeterLocation());
		assertEquals("Units was wrong", "CCFREG", record.getUnits());
		assertEquals("IntervalLengthCode was wrong", "00000100", record.getIntervalLengthCode());
		assertEquals("ListedIntervalCount was wrong", 2, record.getListedIntervalCount());
		
		List<IntervalTriplet> foundTriplets = record.getTriplets();
		assertNotNull("Found triplets were null", foundTriplets);
		assertEquals("Wrong size of triplets list", 2, foundTriplets.size());
		assertTripletsEqual("interval[0] was wrong", "201907080500", "R", 0L, "2073", foundTriplets.get(0));
		assertTripletsEqual("interval[1] was wrong", "201907090500", "R", 0L, "2074", foundTriplets.get(1));

	}

}
