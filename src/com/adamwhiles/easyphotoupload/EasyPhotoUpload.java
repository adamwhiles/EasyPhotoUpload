package com.adamwhiles.easyphotoupload;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.facebook.android.BaseRequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;

public class EasyPhotoUpload extends SherlockActivity {
	public static final String APP_ID = "420243144692776";
	final static int PICK_EXISTING_PHOTO_RESULT_CODE = 1;
	private static final String[] PERMISSIONS = new String[] { "user_photos",
			"publish_stream" };
	public Facebook facebook;
	TextView txtImageLocation;
	File imageFile;
	Context mainContext;
	String imagePath;
	String imageCaption = null;
	String newAlbum = null;
	byte[] data1 = null;
	ListView list1;
	Menu main_menu;
	private Button addPhotoButton;
	private Button uploadPhotosButton;
	public Spinner albumSpinner;
	private Album selectedAlbum;
	private final ArrayList<Photo> m_photos = new ArrayList<Photo>();
	private final ArrayList<String> m_photo_locations = new ArrayList<String>();
	private final ArrayList<String> m_photo_captions = new ArrayList<String>();
	private final ArrayList<Album> m_albums = new ArrayList<Album>();

	private PhotoAdapter m_adapter;
	private SharedPreferences mPrefs;
	private ProgressDialog progressDialog;
	private ProgressDialog progressDialogPicker;
	public ArrayAdapter<Album> spinnerAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// this.requestWindowFeature(Window.FEATURE_NO_TITLE); // Remove default
		// android title bar
		super.onCreate(savedInstanceState);
		if (APP_ID == null) {
			Util.showAlert(this, "Warning", "Facebook Applicaton ID must be "
					+ "specified before running this app");
			Log.e("onCreate", "APP_ID is null");
		}
		setContentView(R.layout.main);
		facebook = new Facebook(APP_ID);
		this.addPhotoButton = (Button) this.findViewById(R.id.btnAdd);
		this.uploadPhotosButton = (Button) this.findViewById(R.id.btnUpload);
		mainContext = this;

		// Setup listview(list1), header and adapter(m_adapter)
		this.list1 = (ListView) findViewById(R.id.lstPhotos);
		m_adapter = new PhotoAdapter(this, R.layout.listview_row, m_photos);
		LayoutInflater infalter = getLayoutInflater();
		// ViewGroup header = (ViewGroup) infalter.inflate(
		// R.layout.listview_header, list1, false);
		// list1.addHeaderView(header);
		list1.setAdapter(m_adapter);

		this.albumSpinner = (Spinner) findViewById(R.id.album_dropdown);
		this.albumSpinner
				.setOnItemSelectedListener(new AlbumSelectedListener());
		spinnerAdapter = new ArrayAdapter<Album>(this,
				android.R.layout.simple_spinner_item, m_albums);

		mPrefs = getPreferences(MODE_PRIVATE);
		String access_token = mPrefs.getString("access_token", null);
		long expires = mPrefs.getLong("access_expires", 0);
		if (access_token != null) {
			Log.v("onCreate", "access_token is not null" + access_token);
			facebook.setAccessToken(access_token);
		}
		if (expires != 0) {
			facebook.setAccessExpires(expires);
		}

		// Check if facebook session exist, if not then get login
		if (!facebook.isSessionValid()) {
			Log.v("onCreate", "Facebook session not valid, starting login");
			facebook.authorize(this, PERMISSIONS, new DialogListener() {
				@Override
				public void onComplete(Bundle values) {
					Log.d("COMPLETE", "AUTH COMPLETE. VALUES: " + values.size());
					Log.d("AUTH TOKEN",
							"== " + values.getString(Facebook.TOKEN));
					SharedPreferences.Editor editor = mPrefs.edit();
					editor.putString("access_token", facebook.getAccessToken());
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
		mPrefs = getPreferences(MODE_PRIVATE);
		String access_token2 = mPrefs.getString("access_token", null);
		long expires2 = mPrefs.getLong("access_expires", 0);
		if (access_token2 != null) {
			facebook.setAccessToken(access_token2);
			Log.e("AUTH TOKEN", "== " + access_token2);
		}
		if (expires2 != 0) {
			facebook.setAccessExpires(expires2);
		}
		getAlbums();
		Log.v("onCreate", "Downloading album list");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		com.actionbarsherlock.view.MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.createAlbum:
			addAlbumAlert();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.v("onResume", "Running onResume");
		mPrefs = getPreferences(MODE_PRIVATE);
		String access_token = mPrefs.getString("access_token", null);
		long expires = mPrefs.getLong("access_expires", 0);
		if (access_token != null) {
			facebook.setAccessToken(access_token);
			Log.v("onResume", "access_token not null, token: " + access_token);
		} else {
			Log.e("onResume", "access_token null");
		}
		if (expires != 0) {
			facebook.setAccessExpires(expires);
		}
		// If facebook session is valid then get list of albums

		getAlbums();
		Log.v("onResume", "Downloading Albums");
		this.albumSpinner.setAdapter(spinnerAdapter);
		this.spinnerAdapter.notifyDataSetChanged();
		Log.v("onResume", "Updating Spinner");
		// Update spinner with albums that were downloaded
		// Define Spinner for album list

		this.uploadPhotosButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.v("uploadButton", "uploadButton Clicked");

				if ((selectedAlbum.getId() == null)
						|| (selectedAlbum.getId() == "")) {
					Toast.makeText(getApplicationContext(),
							"Please Select an Album First", Toast.LENGTH_LONG)
							.show();

				} else {

					if (m_photos.isEmpty()) {
						Toast.makeText(getApplicationContext(),
								"No Pictures Selected", Toast.LENGTH_LONG)
								.show();
					} else {

						// Do action to upload photos from array to facebook
						progressDialog = ProgressDialog
								.show(EasyPhotoUpload.this, "",
										"Uploading Photos...");
						Log.v("uploadButton", "Starting upload thread");
						new Thread(new Runnable() {

							@Override
							public void run() {
								Log.v("uploadButton", "running.....");

								try {

									for (int i = 0; i < m_photos.size();) {
										Bitmap bi = decodeFile(m_photo_locations
												.get(i));
										ByteArrayOutputStream baos = new ByteArrayOutputStream();
										bi.compress(Bitmap.CompressFormat.PNG,
												100, baos);
										data1 = baos.toByteArray();
										uploadImage(data1,
												m_photo_captions.get(i));
										Log.v("uploadButton", "uploaded");
										i++;
									}
									progressDialog.dismiss();
									uploadAlert();
								} catch (Exception e) {

									Log.v("uploadButton", e.getMessage());

								}
							}

						}).start();

					}
				}
			}
		});

		this.addPhotoButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Do action to pick photo
				Intent intent = new Intent(
						Intent.ACTION_PICK,
						android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(intent, PICK_EXISTING_PHOTO_RESULT_CODE);
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {

		// Do this if result came from selection in android photo gallery
		case PICK_EXISTING_PHOTO_RESULT_CODE: {

			if (resultCode == RESULT_OK) {
				// Get the Uri of the photo selected in the gallery
				final Uri photoUri = data.getData();
				imagePath = getPath(photoUri);

				// Call method to get caption and upload the photo to
				// Facebook
				createAlert(imagePath);
			}
			break;
		}
		default: {
			facebook.authorizeCallback(requestCode, resultCode, data);
			break;
		}

		}

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

	private Bitmap decodeFile(String imagePath) {
		try {

			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(imagePath), null, o);

			// The new size we want to scale to
			final int REQUIRED_SIZE = 400;

			// Find the correct scale value. It should be the power of 2.
			int scale = 1;
			while (o.outWidth / scale / 2 >= REQUIRED_SIZE
					&& o.outHeight / scale / 2 >= REQUIRED_SIZE)
				scale *= 2;

			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(imagePath),
					null, o2);
		} catch (FileNotFoundException e) {
		}
		return null;
	}

	// Method to get real path to photo from android gallery and output that
	// path
	public String getPath(Uri contentUri) {

		// can post image
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(contentUri, proj, // Which columns to
														// return
				null, // WHERE clause; which rows to return (all rows)
				null, // WHERE clause selection arguments (none)
				null); // Order-by clause (ascending by name)
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();

		return cursor.getString(column_index);
	}

	public void getAlbums() {
		Log.v("GET ALBUMS", "getAlbums started");
		if (facebook.isSessionValid()) {
			View vg = findViewById(R.id.album_dropdown);
			vg.invalidate();
			Log.v("GET ALBUMS", "Facebook Session is valid");
			m_albums.clear();
			Log.v("GET ALBUMS", "Spinner Array Cleared");
			String response = null;
			try {
				response = facebook.request("me/albums");
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e("GET ALBUMS", "Error: " + e);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e("GET ALBUMS", "Error: " + e);
			}

			JSONObject json = null;
			try {
				json = Util.parseJson(response);
			} catch (FacebookError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e("GET ALBUMS", "Error: " + e);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e("GET ALBUMS", "Error: " + e);
			}
			JSONArray albums = null;
			try {
				albums = json.getJSONArray("data");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e("GET ALBUMS", "Error: " + e);
			}

			if (m_albums.isEmpty()) {
				m_albums.add(new Album("Choose a Album", "", ""));

				for (int i = 0; i < albums.length();) {
					Log.v("GET ALBUMS", "Start process to download albums");
					JSONObject album = null;
					try {
						album = albums.getJSONObject(i);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.e("GET ALBUMS", "Error: " + e);
					}
					try {

						String albumName = album.getString("name");
						String albumID = album.getString("id");
						m_albums.add(new Album(albumName, albumID, ""));
						Log.v("GET ALBUMS", "Added album: " + albumName);

					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.e("GET ALBUMS", "Error: " + e);
					}
					i++;
				}
				View vg1 = null;
				vg1 = findViewById(R.id.album_dropdown);
				vg1.invalidate();
				Log.v("GET ALBUMS", "Updating spinner");
				albumSpinner.setAdapter(spinnerAdapter);
				spinnerAdapter.notifyDataSetChanged();
			}
		}
	}

	// Method for uploading photo to Facebook wall and album.
	private void uploadImage(byte[] byteArray, String caption) {
		Log.v("UPLOAD IMAGE", "uploadImage Started");
		if (facebook.getAccessToken() == null
				|| facebook.getAccessToken() == "") {
			Log.e("UPLOAD IMAGE",
					"Facebook Token null, token: " + facebook.getAccessToken());
		}
		Bundle params = new Bundle();
		params.putString(Facebook.TOKEN, facebook.getAccessToken());
		params.putByteArray("picture", byteArray);
		params.putString("caption", caption);
		try {
			facebook.request(selectedAlbum.getId() + "/photos", params, "POST");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("UPLOAD IMAGE", "Error: " + e);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("UPLOAD IMAGE", "Error: " + e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("UPLOAD IMAGE", "Error: " + e);
		}

	}

	// Create dialog box to get user input for photo caption
	public String createAlert(final String imagePath) {
		Log.v("CREATE ALERT", "Started createAlert");
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Enter Caption for Photo");
		alert.setMessage("Caption :");
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				// Get user entry for photo caption and store in imageCaption
				imageCaption = input.getText().toString();

				m_photos.add(new Photo(imagePath, imageCaption));
				m_photo_locations.add(imagePath);
				m_photo_captions.add(imageCaption);
				m_adapter.notifyDataSetChanged();
				Log.v("CREATE ALERT",
						"Updated Photo Adapter with Caption and Photo");

				return;
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						return;
					}
				});
		AlertDialog captionDialog = alert.create();
		captionDialog.show();
		return imageCaption;

	}

	public void addAlbumAlert() {
		Log.v("ALBUM ALERT", "Started addAlbumAlert");
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Enter New Album Name");
		alert.setMessage("Album Name:");
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Add Album",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						// Get user entry for photo caption and store in
						// imageCaption
						newAlbum = input.getText().toString();
						Bundle params = new Bundle();
						params.putString("name", newAlbum);
						try {
							String response = facebook.request("me/albums",
									params, "POST");
							Log.v("ALBUM ALERT",
									"adding album to facebook in spinner alert");
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							Log.e("ALBUM ALERT", "Error: " + e);
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							Log.e("ALBUM ALERT", "Error: " + e);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							Log.e("ALBUM ALERT", "Error: " + e);
						}
						albumSpinner.setAdapter(spinnerAdapter);
						spinnerAdapter.notifyDataSetChanged();
						Log.v("ALBUM ALERT", "Updated Spinner");
						Toast.makeText(getApplicationContext(),
								"Added New Album!", Toast.LENGTH_SHORT).show();
						getAlbums();
						Log.v("ALBUM ALERT", "Updated album list");

					}

				});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						return;
					}
				});
		AlertDialog createAlbumDialog = alert.create();
		createAlbumDialog.show();

	}

	public void uploadAlert() {
		Log.v("UPLOAD AlERT", "Started uploadAlert");
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Photo Upload Success!");
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {

				EasyPhotoUpload.this.list1.invalidate();
				EasyPhotoUpload.this.m_photos.clear();
				EasyPhotoUpload.this.m_photo_locations.clear();
				EasyPhotoUpload.this.m_photo_captions.clear();
				EasyPhotoUpload.this.list1.setAdapter(m_adapter);
				EasyPhotoUpload.this.m_adapter.notifyDataSetChanged();
			}

		});
		EasyPhotoUpload.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog createAlbumDialog = alert.create();
				createAlbumDialog.show();
			}
		});

	}

	public class PhotoUploadListener extends BaseRequestListener {

		@Override
		public void onComplete(final String response, final Object state) {
			try {
				// process the response here: (executed in background thread)
				Log.d("Facebook-Example", "Response: " + response.toString());
				JSONObject json = Util.parseJson(response);
				@SuppressWarnings("unused")
				final String src = json.getString("src");

				// then post the processed result back to the UI thread
				// if we do not do this, an runtime exception will be generated
				// e.g. "CalledFromWrongThreadException: Only the original
				// thread that created a view hierarchy can touch its views."

			} catch (JSONException e) {
				Log.w("Facebook-Example", "JSON Error in response");
			} catch (FacebookError e) {
				Log.w("Facebook-Example", "Facebook Error: " + e.getMessage());
			}
		}

		@Override
		public void onFacebookError(FacebookError e, Object state) {
		}
	}

}