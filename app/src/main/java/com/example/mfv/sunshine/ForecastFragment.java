package com.example.mfv.sunshine;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mfv on 09.01.15.
 */
public class ForecastFragment extends Fragment
{
	ArrayAdapter<String> adapter;
	DataReciever reciever;

	public ForecastFragment()
	{
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_main, container, false);
		adapter = new ArrayAdapter<>(getActivity(),
				R.layout.list_item_forecast, R.id.list_item_forecast_textview);
		reciever = new DataReciever(adapter);
		reciever.execute("London,UK");
		ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
		assert listView != null;
		listView.setAdapter(adapter);

		setHasOptionsMenu(true);

		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.forecastfragmentmenu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		return super.onOptionsItemSelected(item);
	}
}

class DataReciever extends AsyncTask<String, Void, String[]>
{

	private final String TAG = "network";

	//region REQUEST_PARAM_KEY
	private static final String URI_STRING = "http://api.openweathermap.org/data/2.5/forecast/daily";
	private static final String CITY_PARAM = "q";
	private static final String MODE_PARAM = "mode";
	private static final String UINTS_PARAM = "uints";
	private static final String CNT_PARAM = "cnt";
	//endregion

	//region REQUEST_PARAM_VALUE
	private static final String mode = "json";
	private static final String uints = "metric";
	private static final String forecastDays = "7";
	//endregion

	private ArrayAdapter<String> adapter;
	private Exception exception;

	DataReciever(ArrayAdapter<String> adapter)
	{
		this.adapter = adapter;
	}

	@Override
	protected String[] doInBackground(String... params)
	{
		String data = receiveRowData(params[0]);
		loggingException();
		String[] arr = null;
		try
		{
			arr = JSONParser.getWeatherDataFromJson(data);
		}
		catch(JSONException e)
		{
			Log.e(TAG, "json bad", e);
		}
		return arr;
	}

	@Override
	protected void onPostExecute(String[] strings)
	{
		adapter.clear();
		adapter.addAll(strings);
	}

	private void loggingException()
	{
		if(exception != null)
		{
			StringWriter errors = new StringWriter();
			exception.printStackTrace(new PrintWriter(errors));
			String stack = errors.toString();
			Log.e(TAG, stack, exception);
		}
	}

	private String receiveRowData(String city)
	{
		String rowResult = null;
		HttpURLConnection urlConnection = null;
		BufferedReader bufferedReader = null;

		try
		{
			URL url = new URL(createUri(city).toString());
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.connect();

			InputStream inputStream = urlConnection.getInputStream();

			if(inputStream == null) { return null; }

			bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

			StringBuffer buffer = new StringBuffer();
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
			exception = ex;
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
					exception = ex;
				}
			}
			return rowResult;
		}
	}

	private Uri createUri(String city)
	{
		Uri.Builder builder = Uri.parse(URI_STRING).buildUpon();
		builder.appendQueryParameter(CITY_PARAM, city)
				.appendQueryParameter(MODE_PARAM, mode)
				.appendQueryParameter(UINTS_PARAM, uints)
				.appendQueryParameter(CNT_PARAM, forecastDays);
		return builder.build();
	}

}

class JSONParser
{

	/* The date/time conversion code is going to be moved outside the asynctask later,
	 * so for convenience we're breaking it out into its own method now.
	 */
	private static String getReadableDateString(long time)
	{
		// Because the API returns a unix timestamp (measured in seconds),
		// it must be converted to milliseconds in order to be converted to valid date.
		Date date = new Date(time * 1000);
		SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
		return format.format(date).toString();
	}

	/**
	 * Prepare the weather high/lows for presentation.
	 */
	private static String formatHighLows(double high, double low)
	{
		// For presentation, assume the user doesn't care about tenths of a degree.
		long roundedHigh = Math.round(high);
		long roundedLow = Math.round(low);

		String highLowStr = roundedHigh + "/" + roundedLow;
		return highLowStr;
	}

	/**
	 * Take the String representing the complete forecast in JSON Format and
	 * pull out the data we need to construct the Strings needed for the wireframes.
	 * <p/>
	 * Fortunately parsing is easy:  constructor takes the JSON string and converts it
	 * into an Object hierarchy for us.
	 */
	public static String[] getWeatherDataFromJson(String forecastJsonStr)
			throws JSONException
	{

		// These are the names of the JSON objects that need to be extracted.
		final String OWM_LIST = "list";
		final String OWM_WEATHER = "weather";
		final String OWM_TEMPERATURE = "temp";
		final String OWM_MAX = "max";
		final String OWM_MIN = "min";
		final String OWM_DATETIME = "dt";
		final String OWM_DESCRIPTION = "main";
		final String OWM_DAYS = "cnd";

		JSONObject forecastJson = new JSONObject(forecastJsonStr);
		JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

		String[] resultStrs = new String[weatherArray.length()];
		for(int i = 0; i < weatherArray.length(); i++)
		{
			// For now, using the format "Day, description, hi/low"
			String day;
			String description;
			String highAndLow;

			// Get the JSON object representing the day
			JSONObject dayForecast = weatherArray.getJSONObject(i);

			// The date/time is returned as a long.  We need to convert that
			// into something human-readable, since most people won't read "1400356800" as
			// "this saturday".
			long dateTime = dayForecast.getLong(OWM_DATETIME);
			day = getReadableDateString(dateTime);

			// description is in a child array called "weather", which is 1 element long.
			JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
			description = weatherObject.getString(OWM_DESCRIPTION);

			// Temperatures are in a child object called "temp".  Try not to name variables
			// "temp" when working with temperature.  It confuses everybody.
			JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
			double high = temperatureObject.getDouble(OWM_MAX);
			double low = temperatureObject.getDouble(OWM_MIN);

			highAndLow = formatHighLows(high, low);
			resultStrs[i] = day + " - " + description + " - " + highAndLow;
		}

		return resultStrs;
	}
}
