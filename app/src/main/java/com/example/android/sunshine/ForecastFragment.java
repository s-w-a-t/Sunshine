package com.example.android.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.format.Time;
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

import com.example.android.sunshine.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */

public class ForecastFragment extends Fragment {
    private ForecastAdapter mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String locationSetting = Utility.getPreferredLocation(getActivity());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                     locationSetting, System.currentTimeMillis());

        Cursor cur = getActivity().getContentResolver().query(weatherForLocationUri,
                null, null, null, sortOrder);

        // The CursorAdapter will take data from our cursor and populate the ListView
        // However, we cannot use FLAG_AUTO_REQUERY since it is deprecated, so we will end
        // up with an empty list the first time we run.
        mForecastAdapter = new ForecastAdapter(getActivity(), cur, 0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        return rootView;
    }

    private void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity());
        String location = Utility.getPreferredLocation(getActivity());
        weatherTask.execute(location);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    //region old FetchWeatherTask

//    //Adding AsyncTask to pull data from server
//    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
//        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
//
//        //converting data
//        private String getReadableDateString(long time) {
//            SimpleDateFormat shortenedDataFormat = new SimpleDateFormat("EEE MMM dd");
//            return shortenedDataFormat.format(time);
//        }
//
//        //method max/min temperature for presentation and returns the result of rounding the argument to an integer.
//        private String formatHighLows(double high, double low) {
//            long roundedHigh = Math.round(high);
//            long roundedLow = Math.round(low);
//
//            String highLowStr = roundedHigh + "/" + roundedLow;
//            return highLowStr;
//        }
//
//        //pull out data from json code
//        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays) throws JSONException {
//
//            //JSON objects that need to be extracted.
//            final String OWM_LIST = "list";
//            final String OWM_WEATHER = "weather";
//            final String OWM_TEMPERATURE = "temp";
//            final String OWM_MAX = "max";
//            final String OWM_MIN = "min";
//            final String OWM_DESCRIPTION = "main";
//
//            JSONObject forecastJson = new JSONObject(forecastJsonStr);
//            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
//
//            // normalized UTC date for all of our weather
//            Time dayTime = new Time();
//            dayTime.setToNow();
//            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
//
//            //I don't understand
//            dayTime = new Time();
//
//            String[] resultStrs = new String[numDays];
//            for (int i = 0; i < weatherArray.length(); i++) {
//
//                //format "Day, description, hi/low(highAndLow)"
//                String day;
//                String description;
//                String highAndLow;
//
//                JSONObject dayForecast = weatherArray.getJSONObject(i);
//
//                //conver time to UTC...
//                long dateTime;
//                dateTime = dayTime.setJulianDay(julianStartDay + i);
//                day = getReadableDateString(dateTime);
//
//                //description weather...
//                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
//                description = weatherObject.getString(OWM_DESCRIPTION);
//
//                //high/low temperature
//                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
//                double high = temperatureObject.getDouble(OWM_MAX);
//                double low = temperatureObject.getDouble(OWM_MIN);
//
//                //convert to Fahrenheit
//                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
//                String typeTemperature = sharedPreferences.getString(
//                        getString(R.string.pref_unit_key),
//                        getString(R.string.pref_unit_cels));
//                if (typeTemperature.equals(getString(R.string.pref_unit_fahr))) {
//                    high = (high * 1.8) + 32;
//                    low = (low * 1.8) + 32;
//                }
//
//                highAndLow = formatHighLows(high, low);
//                resultStrs[i] = day + " - " + description + " - " + highAndLow;
//            }
//            for (String s : resultStrs) {
//
//            }
//            return resultStrs;
//        }
//
//
//        @Override
//        protected String[] doInBackground(String... params) {
//            if (params.length == 0) {
//                return null;
//            }
//            HttpURLConnection urlConnection = null;
//            BufferedReader reader = null;
//            String forecastJsonStr = null;
//
//            String format = "json";
//            String units = "metric";
//            int numDays = 7;
//
//            try {
//                final String FORECAST_BASE_URL =
//                        "http://api.openweathermap.org/data/2.5/forecast/daily?";
//                final String QUERY_PARAM = "q";
//                final String FORMAT_PARAM = "mode";
//                final String UNITS_PARAM = "units";
//                final String DAYS_PARAM = "cnt";
//
//                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
//                        .appendQueryParameter(QUERY_PARAM, params[0])
//                        .appendQueryParameter(FORMAT_PARAM, format)
//                        .appendQueryParameter(UNITS_PARAM, units)
//                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
//                        .build();
//                URL url = new URL(builtUri.toString());
//
//                // Create the request to OpenWeatherMap, and open the connection
//                urlConnection = (HttpURLConnection) url.openConnection();
//                urlConnection.setRequestMethod("GET");
//                urlConnection.connect();
//
//                // Read the input stream into a String
//                InputStream inputStream = urlConnection.getInputStream();
//                StringBuffer buffer = new StringBuffer();
//                if (inputStream == null) {
//                    // Nothing to do.
//                    return null;
//                }
//                reader = new BufferedReader(new InputStreamReader(inputStream));
//
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
//                    // But it does make debugging a *lot* easier if you print out the completed
//                    // buffer for debugging.
//                    buffer.append(line + "\n");
//                }
//
//                if (buffer.length() == 0) {
//                    // Stream was empty.  No point in parsing.
//                    return null;
//                }
//                forecastJsonStr = buffer.toString();
//            } catch (IOException e) {
//                Log.e(LOG_TAG, "Error ", e);
//                return null;
//            } finally {
//                if (urlConnection != null) {
//                    urlConnection.disconnect();
//                }
//                if (reader != null) {
//                    try {
//                        reader.close();
//                    } catch (final IOException e) {
//                        Log.e(LOG_TAG, "Error closing stream", e);
//                    }
//                }
//            }
//            try {
//                return getWeatherDataFromJson(forecastJsonStr, numDays);
//            } catch (JSONException e) {
//                Log.e(LOG_TAG, e.getMessage(), e);
//                e.printStackTrace();
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(String[] result) {
//            if (result != null) {
//                mForecastAdapter.clear();
//                for (String dayForecastStr : result) {
//                    mForecastAdapter.add(dayForecastStr);
//                }
//            }
//        }
//    }
    //endregion
}
