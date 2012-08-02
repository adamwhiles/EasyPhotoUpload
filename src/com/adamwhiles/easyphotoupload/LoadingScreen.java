package com.adamwhiles.easyphotoupload;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;

public class LoadingScreen extends Activity {
	public static final String APP_ID = "420243144692776";
	final static int PICK_EXISTING_PHOTO_RESULT_CODE = 1;
	private static final String[] PERMISSIONS = new String[] { "user_photos",
			"publish_stream" };
	public Facebook facebook;

	private Spinner albumSpinner;
	private Album selectedAlbum;
	private final ArrayList<Album> m_albums = new ArrayList<Album>();
	private SharedPreferences mPrefs;
	private ArrayAdapter<Album> spinnerAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE); // Remove default
															// android title bar
		super.onCreate(savedInstanceState);
		if (APP_ID == null) {
			Util.showAlert(this, "Warning", "Facebook Applicaton ID must be "
					+ "specified before running this app");
		}
		setContentView(R.layout.loading);
		facebook = new Facebook(APP_ID);
		Log.e("m_albums2", "facebook id: " + APP_ID);

		this.albumSpinner = (Spinner) findViewById(R.id.album_dropdown2);
		this.albumSpinner
				.setOnItemSelectedListener(new AlbumSelectedListener());
		spinnerAdapter = new ArrayAdapter<Album>(this,
				android.R.layout.simple_spinner_item, m_albums);

		// Check if facebook session exist, if not then get login
		if (!facebook.isSessionValid()) {
			Log.e("m_albums2", "facebook session is not valid...");
			facebook.authorize(this, PERMISSIONS, new DialogListener() {
				@Override
				public void onComplete(Bundle values) {
					Log.d("COMPLETE", "AUTH COMPLETE. VALUES: " + values.size());
					Log.d("AUTH TOKEN",
							"== " + values.getString(Facebook.TOKEN));
					SharedPreferences.Editor editor = mPrefs.edit();
					editor.putString("access_token", facebook.getAccessToken());
					Log.e("m_albums2",
							"created access token: "
									+ facebook.getAccessToken());
					editor.putLong("access_expires",
							facebook.getAccessExpires());
					editor.commit();

				}

				@Override
				public void onFacebookError(FacebookError error) {
					Log.d("FACEBOOK ERROR",
							"FB ERROR. MSG: " + error.getMessage()
									+ ", CAUSE: " + error.getCause());
				}

				@Override
				public void onError(DialogError e) {
					Log.e("ERROR", "AUTH ERROR. MSG: " + e.getMessage()
							+ ", CAUSE: " + e.getCause());
				}

				@Override
				public void onCancel() {
					Log.d("CANCELLED", "AUTH CANCELLED");
				}
			});

		}

		// Get saved facebook session
		albumSpinner.setAdapter(spinnerAdapter);
		spinnerAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// If facebook session is valid then get list of albums
		Log.e("m_albums21234132132",
				"on resume: check if session valid starting thread");

		mPrefs = getPreferences(MODE_PRIVATE);
		String access_token = mPrefs.getString("access_token", null);
		Log.e("m_albums233333", "storing access token: " + access_token);
		long expires = mPrefs.getLong("access_expires", 0);
		if (access_token != null) {
			facebook.setAccessToken(access_token);
		}
		if (expires != 0) {
			facebook.setAccessExpires(expires);
		}

		new Thread() {
			@Override
			public void run() {

				try {

					if (facebook.isSessionValid()) {
						Log.e("m_albums2341324132123",
								"on resume: session is valid");
						String response = null;
						try {
							response = facebook.request("me/albums");
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						JSONObject json = null;
						try {
							json = Util.parseJson(response);
						} catch (FacebookError e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						JSONArray albums = null;
						try {
							albums = json.getJSONArray("data");
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						if (m_albums.isEmpty()) {
							m_albums.add(new Album("Choose a Album", "", ""));

							for (int i = 0; i < albums.length();) {
								JSONObject album = null;
								try {
									album = albums.getJSONObject(i);
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								try {

									String albumName = album.getString("name");
									String albumID = album.getString("id");
									m_albums.add(new Album(albumName, albumID,
											""));

								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								i++;

							}
						}
					}

				} catch (Exception e) {

					Log.e("tag", e.getMessage());

				}
			}
		}.start();

		// Update spinner with albums that were downloaded
		// Define Spinner for album list

		albumSpinner.setAdapter(spinnerAdapter);
		spinnerAdapter.notifyDataSetChanged();

	}

	public class AlbumSelectedListener implements OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			selectedAlbum = (Album) albumSpinner.getItemAtPosition(pos);
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {/* not implemented */
		}
	}

}