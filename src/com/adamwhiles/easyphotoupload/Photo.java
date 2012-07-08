package com.adamwhiles.easyphotoupload;

public class Photo {
	private String photo_location;
	private String caption;

	public Photo() {

	}

	public Photo(String i, String d) {
		this.photo_location = i;
		this.caption = d;
	}

	public String getLocation() {
		return photo_location;
	}

	public void setLocation(String photo_location) {
		this.photo_location = photo_location;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

}