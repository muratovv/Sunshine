package com.example.mfv.sunshine.Model;

import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by mfv on 14.01.15
 */

public class WeatherDataReceiver
{
	public static enum Units
	{
		metric, imperial
	}

	public WeatherDataReceiver(Units unit, int weatherDays)
	{
		this.unit = unit;
		forecastDays = String.valueOf(weatherDays);
		exceptions = new ArrayList<>();
	}

	public String receiveRowData(String city)
	{
		String rowResult = null;
		HttpURLConnection urlConnection = null;
		BufferedReader bufferedReader = null;

		clearExceptionList();

		try
		{
			URL url = new URL(createUri(city).toString());
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setConnectTimeout(5000);
			urlConnection.connect();

			InputStream inputStream = urlConnection.getInputStream();
			if(inputStream == null) { return null; }

			bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

			StringBuilder buffer = new StringBuilder();
			String line;

			while((line = bufferedReader.readLine()) != null)
			{
				buffer.append(line)
						.append("\n");
			}

			if(buffer.length() == 0) { return null; }

			rowResult = buffer.toString();


		}
		catch(Exception ex)
		{
			exceptions.add(ex);
		}
		finally
		{
			if(urlConnection != null) { urlConnection.disconnect(); }
			if(bufferedReader != null)
			{
				try
				{
					bufferedReader.close();
				}
				catch(IOException ex)
				{
					exceptions.add(ex);
				}
			}
		}
		return rowResult;
	}

	public ArrayList<Exception> getExeptions()
	{
		return exceptions;
	}

	private void clearExceptionList()
	{
		exceptions.clear();
	}

	private Uri createUri(String city)
	{
		Uri.Builder builder = Uri.parse(URI_STRING).buildUpon();
		builder.appendQueryParameter(CITY_PARAM, city)
				.appendQueryParameter(MODE_PARAM, mode)
				.appendQueryParameter(UINTS_PARAM, unit.name())
				.appendQueryParameter(CNT_PARAM, forecastDays);
		return builder.build();
	}

	//region REQUEST_PARAM_KEY
	private static final String URI_STRING = "http://api.openweathermap.org/data/2.5/forecast/daily";
	private static final String CITY_PARAM = "q";
	private static final String MODE_PARAM = "mode";
	private static final String UINTS_PARAM = "units";
	private static final String CNT_PARAM = "cnt";
	//endregion

	//region REQUEST_PARAM_VALUE
	private static final String mode = "json";
	//endregion

	private ArrayList<Exception> exceptions;
	private Units unit;
	private String forecastDays;
}
