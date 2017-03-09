/*
 * Copyright 2014 David Fernandez <dftec.es@gmail.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dftec.planetcon.data;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

public class PlayerPref implements Parcelable {
    public String sColor;
    public String sId;
    public String sName;
    public String sAI;
    public String sHostility;

    public PlayerPref() {
        sColor = "0";
        sId = "0";
        sName = "";
        sAI = "0";
        sHostility = "0";
    }

    private PlayerPref(Parcel in) {
        sColor = in.readString();
        sId = in.readString();
        sName = in.readString();
        sAI = in.readString();
        sHostility = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return sColor + ":" + sName + ":" + sId + ":" + sAI + ":" + sHostility;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(sColor);
        out.writeString(sId);
        out.writeString(sName);
        out.writeString(sAI);
        out.writeString(sHostility);
    }

    public static final Parcelable.Creator<PlayerPref> CREATOR = new Parcelable.Creator<PlayerPref>() {
        public PlayerPref createFromParcel(Parcel in) {
            return new PlayerPref(in);
        }

        public PlayerPref[] newArray(int size) {
            return new PlayerPref[size];
        }
    };

    public void setPlayerColor(int color) {
        this.sColor = PlayerData.colorToHex(color);
    }

    public int getPlayerColor() {
        return Color.parseColor(this.sColor);
    }

}