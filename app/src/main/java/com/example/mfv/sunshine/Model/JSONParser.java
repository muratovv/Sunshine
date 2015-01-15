package com.example.mfv.sunshine.Model;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser for row json from openweathermap.org
 */
public class JSONParser
{


	public static final String DATE = "date";
	public static final String PRESSURE = "pressure";
	public static final String HUMIDITY = "humidity";
	public static final String SPEED = "speed";
	public static final String MAX = "max";
	public static final String MIN = "min";
	public static final String DESCRIPTION = "description";

	public JSONParser(String rowData)
	{
		days = new ArrayList<>();
		exception = null;
		city = null;
		country = null;
		if(rowData != null)
		{
			parseRawData(rowData);
		}
	}

	private void parseRawData(String request)
	{
		exception = null;
		try
		{
			JSONObject object = new JSONObject(request);
			city = object.getJSONObject("city").getString("name");
			country = object.getJSONObject("city").getString("country");
			String cnt = object.getString("cnt");
			for(int index = 0; index < Integer.parseInt(cnt); index++)
			{
				JSONObject day = object.getJSONArray("list").getJSONObject(index);
				Map<String, String> mappedDay = new HashMap<>();
				mappedDay.put(DATE, day.getString("dt"));
				mappedDay.put(PRESSURE, day.getString("pressure"));
				mappedDay.put(HUMIDITY, day.getString("humidity"));
				mappedDay.put(SPEED, day.getString("speed"));
				mappedDay.put(MAX, day.getJSONObject("temp").getString("max"));
				mappedDay.put(MIN, day.getJSONObject("temp").getString("min"));
				mappedDay.put(DESCRIPTION,
						day.getJSONArray("weather").getJSONObject(0).getString("main"));
				days.add(mappedDay);
			}
		}
		catch(Exception ex)
		{
			exception = ex;
		}
	}


	public Exception getException()
	{
		return exception;
	}

	public ArrayList<Map<String, String>> getDays()
	{
		return days;
	}

	public String getCountry()
	{

		return country;
	}

	public String getCity()
	{
		return city;
	}

	private String city;
	private String country;
	private ArrayList<Map<String, String>> days;
	private Exception exception;
}