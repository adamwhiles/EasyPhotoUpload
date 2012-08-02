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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.BaseRequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;

public class EasyPhotoUpload extends Activity {
	public static final String APP_ID = "420243144692776";
	final static int PICK_EXISTING_PHOTO_RESULT_CODE = 1;
	private static final String[] PERMISSIONS = new String[] { "user_photos",
			"publish_stream" };
	public Facebook facebook;
	TextView txtImageLocation;
	File imageFile;
	String imagePath;
	String imageCaption = null;
	byte[] data1 = null;
	ListView list1;
	private Button addPhotoButton;
	private Button uploadPhotosButton;
	private Spinner albumSpinner;
	private Album selectedAlbum;
	private final ArrayList<Photo> m_photos = new ArrayList<Photo>();
	private final ArrayList<String> m_photo_locations = new ArrayList<String>();
	private final ArrayList<String> m_photo_captions = new ArrayList<String>();
	private final ArrayList<Album> m_albums = new ArrayList<Album>();

	private PhotoAdapter m_adapter;
	private SharedPreferences mPrefs;
	private ProgressDialog progressDialog;
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
		setContentView(R.layout.main);
		facebook = new Facebook(APP_ID);

		this.addPhotoButton = (Button) this.findViewById(R.id.btnAdd);
		this.uploadPhotosButton = (Button) this.findViewById(R.id.btnUpload);

		// Setup listview(list1), header and adapter(m_adapter)
		this.list1 = (ListView) findViewById(R.id.lstPhotos);
		m_adapter = new PhotoAdapter(this, R.layout.listview_row, m_photos);
		LayoutInflater infalter = getLayoutInflater();
		ViewGroup header = (ViewGroup) infalter.inflate(
				R.layout.listview_header, list1, false);
		list1.addHeaderView(header);
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
			facebook.setAccessToken(access_token);
		}
		if (expires != 0) {
			facebook.setAccessExpires(expires);
		}

		// Check if facebook session exist, if not then get login
		if (!facebook.isSessionValid()) {

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
		if (access_token != null) {
			facebook.setAccessToken(access_token2);
		}
		if (expires != 0) {
			facebook.setAccessExpires(expires2);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		// If facebook session is valid then get list of albums
		if (facebook.isSessionValid()) {
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
						m_albums.add(new Album(albumName, albumID, ""));

					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					i++;
				}
			}
		}
		// Update spinner with albums that were downloaded
		// Define Spinner for album list

		albumSpinner.setAdapter(spinnerAdapter);
		spinnerAdapter.notifyDataSetChanged();

		this.uploadPhotosButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

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

						new Thread() {

							@Override
							public void run() {

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
										i++;
									}
								} catch (Exception e) {

									Log.e("tag", e.getMessage());

								}
								progressDialog.dismiss();
							}

						}.start();

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
				Uri photoUri = data.getData();
				// Get the actual path to the image
				imagePath = getPath(photoUri);

				// Process the image taken from the gallery
				Bitmap bi = decodeFile(imagePath);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				bi.compress(Bitmap.CompressFormat.JPEG, 100, baos);
				data1 = baos.toByteArray();

				// Call method to get caption and upload the photo to Facebook
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

	// Method for uploading photo to Facebook wall and album.
	private void uploadImage(byte[] byteArray, String caption) {
		Bundle params = new Bundle();
		params.putString(Facebook.TOKEN, facebook.getAccessToken());
		params.putByteArray("picture", byteArray);
		params.putString("caption", caption);
		try {
			facebook.request(selectedAlbum.getId() + "/photos", params, "POST");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// Create dialog box to get user input for photo caption
	public String createAlert(final String imagePath) {
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

	public class PhotoUploadListener extends BaseRequestListener {

		@Override
		public void onComplete(final String response, final Object state) {
			try {
				Toast.makeText(getApplicationContext(), "Upload Success",
						Toast.LENGTH_LONG).show();
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