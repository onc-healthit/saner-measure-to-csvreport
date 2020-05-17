package org.hl7.fhir.saner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class SanerCsvFile {
	private List<String> headers;
	private List<String> records;
	
	public SanerCsvFile(List<String> headerList)
	{
		headers = new ArrayList<String>();
		headers.addAll(headerList);
		records = new ArrayList<String>();
	}
	
	public void addRecord(String record) throws Exception
	{
		//Validate record tokens matching with num of headers
		String[] tokens = record.split(",", -1);
 		if(tokens.length != headers.size())
			throw new Exception("Invalid record. Not matched with header count: "+headers.size());
		records.add(record);
	}

	public List<String> getHeaders() {
		return headers;
	}

	public void setHeaders(List<String> headers) {
		this.headers = headers;
	}

	public List<String> getRecords() {
		return records;
	}

	public void setRecords(List<String> records) {
		this.records = records;
	}
	
	public String toString()
	{
		StringBuffer str = new StringBuffer();
		Iterator<String> headerIter = headers.iterator();
		while(headerIter.hasNext())
		{
			str.append(headerIter.next());
			if(headerIter.hasNext())
				str.append(",");
		}
		
		str.append(System.getProperty("line.separator"));
		
		Iterator<String> recordsIter = records.iterator();
		while(recordsIter.hasNext())
		{
			str.append(recordsIter.next());
			if(recordsIter.hasNext())
				str.append(System.getProperty("line.separator"));
		}
		return str.toString();
	}

}
