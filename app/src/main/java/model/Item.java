/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class Item implements Parcelable {

    public static final String TAG = "Item";
    public static final Creator<Item> CREATOR = new Creator<Item>() {
        @Override
        public Item createFromParcel(Parcel in) {
            return new Item(in);
        }

        @Override
        public Item[] newArray(int size) {
            return new Item[size];
        }
    };
    private static final int SCORE = 8;
    private static final int NO_SCORE = 0;
    private final String mName;
    private final String mId;
    private final Theme mTheme;

    public Item(@NonNull String name, @NonNull String id, @NonNull Theme theme) {
        mName = name;
        mId = id;
        mTheme = theme;
    }

    protected Item(Parcel in) {
        mName = in.readString();
        mId = in.readString();
        mTheme = Theme.values()[in.readInt()];
    }

    public String getName() {
        return mName;
    }

    public String getId() {
        return mId;
    }

    public Theme getTheme() {
        return mTheme;
    }

    @Override
    public String toString() {
        return "Category{" +
                "mName='" + mName + '\'' +
                ", mId='" + mId + '\'' +
                ", mTheme=" + mTheme +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mId);
        dest.writeInt(mTheme.ordinal());
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Item item = (Item) o;

        if (!mId.equals(item.mId)) {
            return false;
        }
        if (!mName.equals(item.mName)) {
            return false;
        }
        if (mTheme != item.mTheme) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = mName.hashCode();
        result = 31 * result + mId.hashCode();
        result = 31 * result + mTheme.hashCode();
        return result;
    }
}
