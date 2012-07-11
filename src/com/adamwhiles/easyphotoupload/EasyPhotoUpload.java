package com.adamwhiles.easyphotoupload;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.BaseRequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;

public class EasyPhotoUpload extends Activity {
	final static int PICK_EXISTING_PHOTO_RESULT_CODE = 1;
	Facebook facebook = new Facebook("");
	private SharedPreferences mPrefs;
	TextView txtImageLocation;
	File imageFile;
	String imagePath;
	String imageCaption = null;
	byte[] data1 = null;
	ListView list1;
	private Button addPhotoButton;
	private Button uploadPhotosButton;
	private final ArrayList<Photo> m_photos = new ArrayList<Photo>();
	private final ArrayList<String> m_photo_locations = new ArrayList<String>();
	private final ArrayList<String> m_photo_captions = new ArrayList<String>();
	private PhotoAdapter m_adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE); // Remove default
															// android title bar
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

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

		// Setup access token and store the token
		mPrefs = getPreferences(MODE_PRIVATE);
		String access_token = mPrefs.getString("access_token", null);
		long expires = mPrefs.getLong("access_expires", 0);

		// Check if access token for Facebook has already need generated and
		// pickup the session so the user doesnt
		// have to login again.
		if (access_token != null) {
			facebook.setAccessToken(access_token);
		}
		if (expires != 0) {
			facebook.setAccessExpires(expires);
		}

		if (!facebook.isSessionValid()) {

			facebook.authorize(this, new String[] { "user_photos",
					"publish_stream" }, new DialogListener() {
				@Override
				public void onComplete(Bundle values) {
					SharedPreferences.Editor editor = mPrefs.edit();
					editor.putString("access_token", facebook.getAccessToken());
					editor.putLong("access_expires",
							facebook.getAccessExpires());
					editor.commit();
				}

				@Override
				public void onFacebookError(FacebookError error) {
				}

				@Override
				public void onError(DialogError e) {
				}

				@Override
				public void onCancel() {
				}
			});
		}

		this.uploadPhotosButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Do action to upload photos from array to facebook

				for (int i = 0; i < m_photos.size();) {
					Bitmap bi = decodeFile(m_photo_locations.get(i));
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					bi.compress(Bitmap.CompressFormat.JPEG, 100, baos);
					data1 = baos.toByteArray();
					uploadImage(data1, m_photo_captions.get(i));
					i++;
				}
				Toast.makeText(getApplicationContext(),
						"Photo Upload Success!", Toast.LENGTH_LONG).show();
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

	private Bitmap decodeFile(String imagePath) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(imagePath), null, o);

			// The new size we want to scale to
			final int REQUIRED_SIZE = 100;

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
			facebook.request("me/photos", params, "POST");
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

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// MenuInflater inflater = getMenuInflater();
	// inflater.inflate(R.menu.options_menu, menu);
	// return true;
	// }

	/*
	 * 
	 * public boolean onOptionsItemSelected(MenuItem item) { // Handle item
	 * selection switch (item.getItemId()) { case R.id.add_photo:
	 * 
	 * // Display Gallery Picker Intent intent = new Intent( Intent.ACTION_PICK,
	 * android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
	 * startActivityForResult(intent, PICK_EXISTING_PHOTO_RESULT_CODE);
	 * 
	 * return true; case R.id.upload_photos:
	 * 
	 * // Call uploadImage to upload image and caption to facebook // album and
	 * wall // uploadImage(data1, imageCaption); for (int i = 0; i <
	 * m_photos.size();) {
	 * 
	 * Bitmap bi = decodeFile(m_photo_locations.get(i)); ByteArrayOutputStream
	 * baos = new ByteArrayOutputStream();
	 * bi.compress(Bitmap.CompressFormat.JPEG, 100, baos); data1 =
	 * baos.toByteArray(); uploadImage(data1, m_photo_captions.get(i)); i++; }
	 * return true; default: return super.onOptionsItemSelected(item); } }
	 */

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
