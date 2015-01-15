package com.example.mfv.sunshine;

import android.content.Intent;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.mfv.sunshine.Model.JSONParser;
import com.example.mfv.sunshine.Model.WeatherDataReceiver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * Created by mfv on 09.01.15.
 */
public class ForecastFragment extends Fragment
{
	ArrayAdapter<String> adapter;
	DataReceiver receiver;

	public ForecastFragment()
	{
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_main, container, false);
		adapter = new ArrayAdapter<>(getActivity(),
				R.layout.list_item_forecast, R.id.list_item_forecast_textview);
		receiver = new DataReceiver(adapter);
		receiver.execute("London,UK");
		ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
		assert listView != null;
		listView.setAdapter(adapter);
		setHasOptionsMenu(true);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				String forecast = adapter.getItem(position);
				Intent activity = new Intent(getActivity(), DetailActivity.class)
						.putExtra(Intent.EXTRA_TEXT, forecast);
				startActivity(activity);
			}
		});


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

		int id = item.getItemId();
		if(id == R.id.action_refresh)
		{
			Toast.makeText(getActivity(), "Refreshing...", Toast.LENGTH_SHORT).show();
			receiver = new DataReceiver(adapter);
			receiver.execute("Moscow,RU");
		}

		return super.onOptionsItemSelected(item);
	}
}

class DataReceiver extends AsyncTask<String, Void, String[]>
{

	private final String TAG = "network";

	ArrayAdapter<String> adapter;
	WeatherDataReceiver receiver = new WeatherDataReceiver(WeatherDataReceiver.Units.metric, 14);

	DataReceiver(ArrayAdapter<String> adapter)
	{
		this.adapter = adapter;
	}

	@Override
	protected String[] doInBackground(String... params)
	{
		String data = receiver.receiveRowData(params[0]);
		ArrayList<String> arr = new ArrayList<>();
		JSONParser parser = new JSONParser(data);

		for(Map<String, String> m : parser.getDays())
		{
			Date d = new Date(Long.valueOf(m.get(JSONParser.DATE)) * 1000l);
			int max = (int) Double.parseDouble(m.get(JSONParser.MAX));
			int min = (int) Double.parseDouble(m.get(JSONParser.MIN));
			String result = makeOutput(d, m.get(JSONParser.DESCRIPTION), max, min);
			arr.add(result);
		}

		loggingException(receiver.getExeptions());
		ArrayList<Exception> parserExceptions = new ArrayList<>();
		parserExceptions.add(parser.getException());
		loggingException(parserExceptions);

		return arr.toArray(new String[arr.size()]);
	}

	@Override
	protected void onPostExecute(String[] strings)
	{
		adapter.clear();
		adapter.addAll(strings);
	}

	private void loggingException(ArrayList<Exception> exceptions)
	{
		if(exceptions != null && exceptions.size() > 0)
		{
			for(Exception exception : exceptions)
			{
				if(exception != null)
				{
					StringWriter errors = new StringWriter();
					exception.printStackTrace(new PrintWriter(errors));
					String stack = errors.toString();
					Log.e(TAG, stack, exception);
				}
			}
		}
	}

	private String makeOutput(Date date, String weather, int max, int min)
	{
		String prettyData = new SimpleDateFormat("E, MMM d").format(date);
		return String.format("%s - %s - %d/%d", prettyData, weather, max, min);
	}


}

