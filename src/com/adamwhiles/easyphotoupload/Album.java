package com.adamwhiles.easyphotoupload;

import android.os.Parcel;
import android.os.Parcelable;

public class Album implements Parcelable {
	private String id;
	private String name;
	private String description;

	public Album(String name, String id, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	public Album(Parcel in) {
		name = in.readString();
		id = in.readString();
		description = in.readString();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public static final Parcelable.Creator<Album> CREATOR = new Parcelable.Creator<Album>() {
		@Override
		public Album createFromParcel(Parcel in) {
			return new Album(in);
		}

		@Override
		public Album[] newArray(int size) {
			return new Album[size];
		}
	};

	@Override
	public String toString() {
		// returning name because that is what will be displayed in the Spinner
		// control
		return (this.name);
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel arg0, int arg1) {
		// TODO Auto-generated method stub
		arg0.writeString(name);
		arg0.writeString(id);
		arg0.writeString(description);
	}
}
