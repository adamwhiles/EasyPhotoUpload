package com.adamwhiles.easyphotoupload;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.android.*;
import com.facebook.android.Facebook.*;

public class EasyPhotoUpload extends Activity {
	 final static int PICK_EXISTING_PHOTO_RESULT_CODE = 1;
    Facebook facebook = new Facebook("420243144692776");
    private SharedPreferences mPrefs;
    TextView txtImageLocation;
    File imageFile;
    String imageCaption = null;
    
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
  
     		   
  
        
        
        mPrefs = getPreferences(MODE_PRIVATE);
        String access_token = mPrefs.getString("access_token", null);
        long expires = mPrefs.getLong("access_expires", 0);
                
        
        if(access_token != null) {
            facebook.setAccessToken(access_token);
        }
        if(expires != 0) {
            facebook.setAccessExpires(expires);
        }

        if(!facebook.isSessionValid()) {

            facebook.authorize(this, new String[] {"user_photos", "publish_stream"}, new DialogListener() {
                @Override
                public void onComplete(Bundle values) {
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putString("access_token", facebook.getAccessToken());
                    editor.putLong("access_expires", facebook.getAccessExpires());
                    editor.commit();
                }
    
                @Override
                public void onFacebookError(FacebookError error) {}
    
                @Override
                public void onError(DialogError e) {}
    
                @Override
                public void onCancel() {}
            });
        }
        
        txtImageLocation = (TextView)findViewById(R.id.textImageLocation);
        Button buttonSelectImage = (Button)findViewById(R.id.btnSelectImage);
        buttonSelectImage.setOnClickListener(new Button.OnClickListener(){

        	  @Override
        	  public void onClick(View arg0) {
        	   // TODO Auto-generated method stub
        	   Intent intent = new Intent(Intent.ACTION_PICK,
        	     android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        	   startActivityForResult(intent, PICK_EXISTING_PHOTO_RESULT_CODE);
        	   }});
    }
    
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);        
        switch (requestCode) {
        case PICK_EXISTING_PHOTO_RESULT_CODE: {   
          
        if (resultCode == RESULT_OK){
        	  Uri photoUri = data.getData();
        	  String imagePath = getPath(photoUri);
        	  byte[] data1 = null;
        	        	  
                  
                Bitmap bi = BitmapFactory.decodeFile(imagePath);
               	ByteArrayOutputStream baos = new ByteArrayOutputStream();
               	bi.compress(Bitmap.CompressFormat.JPEG, 100, baos);
               	data1 = baos.toByteArray();
               	
                Bundle params = new Bundle();
                params.putString(Facebook.TOKEN, facebook.getAccessToken());
                params.putString("caption", createAlert());
                params.putByteArray("photo", data1);
                
                try {
					facebook.request("me/photos",params,"POST");
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
        	 
        	 
        break;
        }
        default: {
        facebook.authorizeCallback(requestCode, resultCode, data);
        break;
        }
        
        
    }
        
    }
    
    public String getPath(Uri contentUri) {

        // can post image
        String [] proj={MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery( contentUri,
                        proj, // Which columns to return
                        null,       // WHERE clause; which rows to return (all rows)
                        null,       // WHERE clause selection arguments (none)
                        null); // Order-by clause (ascending by name)
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        return cursor.getString(column_index);
}
    
    public String createAlert() {      
        AlertDialog.Builder alert = new AlertDialog.Builder(this);                 
     	  alert.setTitle("Enter Caption for Photo");  
     	  alert.setMessage("Caption :");
     	  final EditText input = new EditText(this); 
     	  alert.setView(input);
     	  
     	  alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {  
     		    public void onClick(DialogInterface dialog, int whichButton) {  
     		        imageCaption = input.getText().toString();
     		        return;                  
     		       }  
     		     });  

     		    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

     		        public void onClick(DialogInterface dialog, int which) {
     		            // TODO Auto-generated method stub
     		            return;   
     		        }
     		    });
     		   AlertDialog helpDialog = alert.create();
     		   helpDialog.show();
     		   return imageCaption;
     		   
  }
    
    public class PhotoUploadListener extends BaseRequestListener {

        public void onComplete(final String response, final Object state) {
            try {
                // process the response here: (executed in background thread)
                Log.d("Facebook-Example", "Response: " + response.toString());
                JSONObject json = Util.parseJson(response);
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

