package com.adamwhiles.easyphotoupload;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PhotoAdapter extends ArrayAdapter<Photo> {
	// declaring our ArrayList of items
	private final ArrayList<Photo> objects;

	/*
	 * here we must override the constructor for ArrayAdapter the only variable
	 * we care about now is ArrayList<Item> objects, because it is the list of
	 * objects we want to display.
	 */
	public PhotoAdapter(Context context, int textViewResourceId,
			ArrayList<Photo> objects) {
		super(context, textViewResourceId, objects);
		this.objects = objects;
	}

	/*
	 * we are overriding the getView method here - this is what defines how each
	 * list item will look.
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		// assign the view we are converting to a local variable
		View v = convertView;

		// first check to see if the view is null. if so, we have to inflate it.
		// to inflate it basically means to render, or show, the view.
		if (v == null) {
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.listview_row, null);
		}

		/*
		 * Recall that the variable position is sent in as an argument to this
		 * method. The variable simply refers to the position of the current
		 * object in the list. (The ArrayAdapter iterates through the list we
		 * sent it)
		 * 
		 * Therefore, i refers to the current Item object.
		 */
		Photo i = objects.get(position);

		if (i != null) {

			// This is how you obtain a reference to the TextViews.
			// These TextViews are created in the XML files we defined.

			ImageView thumb = (ImageView) v
					.findViewById(R.id.txtPhotoLocationActual);
			TextView mt = (TextView) v.findViewById(R.id.txtPhotoCaption);
			TextView mtd = (TextView) v
					.findViewById(R.id.txtPhotoCaptionActual);

			// check to see if each individual textview is null.
			// if not, assign some text!

			if (thumb != null) {

				String imagePath = i.getLocation();
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 50;
				Bitmap bm = BitmapFactory.decodeFile(imagePath, options);
				thumb.setImageBitmap(bm);
			}
			if (mt != null) {
				mt.setText("Photo Caption: ");
			}
			if (mtd != null) {
				mtd.setText(i.getCaption());
			}
		}

		// the view must be returned to our activity
		return v;

	}

}