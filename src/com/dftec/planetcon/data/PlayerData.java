/*
 * Copyright 2014-17 David Fernandez <dftec.es@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.dftec.planetcon.data;

import android.graphics.Color;
import com.dftec.planetcon.activities.SettingsActivity;
import java.util.Random;

public class PlayerData implements Comparable<PlayerData> {
    public static final String SAVED_TAG_PLAYER = "Player";
    public static final float COLOR_VALUE = 0.8f;
    public static final float COLOR_SAT = 0.2f;
    public static final int NEUTRAL_HUE = 180;

    public int index;           // index 1 for first player (index 0 used for virtual observer)
    public String sName;        // name of the player
    public int color;           // ARGB color (x00000000 to xFFFFFFFF) of the player
    public int colorText;       // less saturation, more bright, for texts
    public float[] hsv;         // temp Hsv color (0 to 360, 0 to 1, 0 to 1) of the player
    public boolean ended;       // turn ended (ships sent)
    public boolean eliminated;  // eliminated player: no planets nor ships
    public boolean local;       // local player (in this machine)
    public boolean ai;          // computer controlled player
    public int aiDefcon;        // defensive status: from 0 to 5 (0 = exploration status)
    public int aiExploration;   // distance to closer enemy home planet
    public int aiHostility;     // hostility towards humans: from -4 to +4 (0 = normal)

    public int planetsNum;      // number of planets
    public int planetsProd;     // production of ships summing all planets
    public int shipsPlanetPrev; // ships in planets (previous turn)
    public int shipsPlanetNow;  // ships in planets (actual)
    public int shipsFleetPrev;  // ships in fleets (previous turn)
    public int shipsFleetNow;   // ships in fleets (actual)
    public int shipsProduced;   // ships produced (historical stats)
    public int shipsArrived;    // number of ships arrived (historical stats)
    public int fleetsPrev;      // number of fleets (previous turn)
    public int fleetsNow;       // number of fleets (actual)

    public int defenseTotal;    // temp value used to calculate defense
    public int defenseCount;    // temp value used to calculate defense
    public int upkeepTotal;     // temp value used to calculate upkeep
    public int upkeepCount;     // temp value used to calculate upkeep
    public int upkeepNextTotal; // temp value used to estimate next upkeep
    public int upkeepNextCount; // temp value used to estimate next upkeep


    public PlayerData(int index, Random random) {
        this.index = index;
        this.sName = "   ";

        this.hsv = new float[3];
        // Player color
        if (index > 0) {
            setPlayerHue(defaultHue(index - 1));
        }
        // Neutral color
        else {
            setNeutralHue();
        }

        this.ended = false;
        this.eliminated = false;
        this.local = false;
        this.ai = false;
        this.aiDefcon = 0;
        this.aiExploration = 0;
        this.setAIHostility(SettingsActivity.HOSTILITY_MAX, random);
        this.planetsNum = 1;
        this.planetsProd = 0;
        this.shipsPlanetNow = 0;
        this.shipsPlanetPrev = 0;
        this.shipsFleetNow = 0;
        this.shipsFleetPrev = 0;
        this.shipsProduced = 0;
        this.shipsArrived = 0;
        this.fleetsNow = 0;
        this.fleetsPrev = 0;
        this.defenseTotal = 0;
        this.defenseCount = 0;
        this.upkeepTotal = 0;
        this.upkeepCount = 0;
        this.upkeepNextTotal = 0;
        this.upkeepNextCount = 0;
    }

    public PlayerData(String[] args) {
        int n = 0;
        this.index = Integer.valueOf(args[++n]);
        this.sName = String.valueOf(args[++n]);
        this.color = Integer.valueOf(args[++n]);
        this.colorText = Integer.valueOf(args[++n]);
        this.hsv = new float[3];
        Color.colorToHSV(this.color, this.hsv);
        this.ended = Boolean.valueOf(args[++n]);
        this.eliminated = Boolean.valueOf(args[++n]);
        this.local = Boolean.valueOf(args[++n]);
        this.ai = Boolean.valueOf(args[++n]);
        this.aiDefcon = Integer.valueOf(args[++n]);
        this.aiExploration = Integer.valueOf(args[++n]);
        this.aiHostility = Integer.valueOf(args[++n]);
        this.planetsNum = Integer.valueOf(args[++n]);
        this.planetsProd = Integer.valueOf(args[++n]);
        this.shipsPlanetPrev = Integer.valueOf(args[++n]);
        this.shipsPlanetNow = Integer.valueOf(args[++n]);
        this.shipsFleetPrev = Integer.valueOf(args[++n]);
        this.shipsFleetNow = Integer.valueOf(args[++n]);
        this.shipsProduced = Integer.valueOf(args[++n]);
        this.shipsArrived = Integer.valueOf(args[++n]);
        this.fleetsPrev = Integer.valueOf(args[++n]);
        this.fleetsNow = Integer.valueOf(args[++n]);
        this.defenseTotal = 0;
        this.defenseCount = 0;
        this.upkeepTotal = 0;
        this.upkeepCount = 0;
        this.upkeepNextTotal = 0;
        this.upkeepNextCount = 0;
    }

    public String save(StringBuilder builder) {
        builder.append(SAVED_TAG_PLAYER);
        builder.append(",").append(this.index);
        builder.append(",").append(this.sName);
        builder.append(",").append(this.color);
        builder.append(",").append(this.colorText);
        //hsv;
        builder.append(",").append(this.ended);
        builder.append(",").append(this.eliminated);
        builder.append(",").append(this.local);
        builder.append(",").append(this.ai);
        builder.append(",").append(this.aiDefcon);
        builder.append(",").append(this.aiExploration);
        builder.append(",").append(this.aiHostility);
        builder.append(",").append(this.planetsNum);
        builder.append(",").append(this.planetsProd);
        builder.append(",").append(this.shipsPlanetPrev);
        builder.append(",").append(this.shipsPlanetNow);
        builder.append(",").append(this.shipsFleetPrev);
        builder.append(",").append(this.shipsFleetNow);
        builder.append(",").append(this.shipsProduced);
        builder.append(",").append(this.shipsArrived);
        builder.append(",").append(this.fleetsPrev);
        builder.append(",").append(this.fleetsNow);
        //defenseTotal;
        //defenseCount;
        //upkeepTotal;
        //upkeepCount;
        //upkeepNextTotal;
        //upkeepNextCount;
        return builder.toString();
    }

    public final void setAIHostility(int hostility, Random random) {
        if (random != null) {
            this.aiHostility = 2 * random.nextInt(hostility + 1) - hostility;
        } else {
            this.aiHostility = hostility;
        }
        if (this.aiHostility > SettingsActivity.HOSTILITY_MAX) {
            this.aiHostility = SettingsActivity.HOSTILITY_MAX;
        }
        if (this.aiHostility < -SettingsActivity.HOSTILITY_MAX) {
            this.aiHostility = -SettingsActivity.HOSTILITY_MAX;
        }
    }

    public static final String colorToHex(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    public static final String defaultHexColor(int index) {
        // Returns the default color in hex format (to use as preference)
        float[] hsvColor = new float[3];
        hsvColor[0] = defaultHue(index - 1);
        hsvColor[1] = 1;
        hsvColor[2] = COLOR_VALUE;
        return colorToHex(Color.HSVToColor(hsvColor));
    }

    public static final float defaultHue(int player) {
        // Initial color for player with index = player + 1
        if (player < 0) {
            return NEUTRAL_HUE;
        } else {
            switch (player) {
            case 0: return 0;       //Red
            case 1: return 120;     //Green
            case 2: return 210;     //Azure(Sky)
            case 3: return 60;      //Yellow
            case 4: return 180;     //Cyan/Aqua
            case 5: return 300;     //Magenta/Fuchsia
            case 6: return 30;      //Orange/Amber
            case 7: return 150;     //Spring
            case 8: return 270;     //Violet
            case 9: return 90;      //Chartreuse(Lime)
            case 10: return 240;    //Blue(Navy)
            case 11: return 330;    //Rose(Pink)
            default: return recursiveHue (player);
            }
        }
    }

    /**
     * Assign colors to players like a fractal, with hues as separate as possible in each cycle.
     * 1st cycle: 0, 120, 240
     * 2nd cycle (+60): 60, 180, 300
     * 3th cycle (+30): 30, 150, 270, 90, 210, 330
     * 4th cycle (+15): 15, 135, 255, 75, 195, 315, 45, 165, 285, 105, 225, 345
     */
    public static final float recursiveHue(int player) {
        // if 3, alternates red, green, blue variations, same than default colors.
        float firstCycle = 3;

        // First cycle
        if (player < firstCycle) {
            return player * 360f / firstCycle;
        }
        // Each cycle has as much values as all previous cycles summed (powers of 2)
        else {
            // floor of log2 (player / firstCycle)
            int numCycles = (int)Math.floor(Math.log(player / firstCycle) / Math.log(2));
            // divDown stores the largest power of 2 that is still lower than player
            int divDown = (int)(firstCycle * Math.pow(2, numCycles));
            // Same hues than previous cycle, but summing an offset (half than previous cycle)
            return recursiveHue(player % divDown) + 180f / divDown;
        }
    }

    public final void setNeutralHue() {
        // Neutral player used for texts of neutral planets
        this.hsv[0] = NEUTRAL_HUE;
        // color text
        this.hsv[1] = 0;
        this.hsv[2] = COLOR_VALUE;
        this.colorText = Color.HSVToColor(this.hsv);
        // final color
        this.hsv[1] = COLOR_SAT;
        this.hsv[2] = 1;
        this.color = Color.HSVToColor(this.hsv);
    }

    public final void setPlayerHue(float hue) {
        // Same than setPlayerHSV(hue, 1, COLOR_VALUE);
        this.hsv[0] = hue;
        // color text
        this.hsv[1] = 1 - COLOR_SAT;
        this.hsv[2] = COLOR_VALUE;
        this.colorText = Color.HSVToColor(this.hsv);
        // final color
        this.hsv[1] = 1;
        this.hsv[2] = COLOR_VALUE;
        this.color = Color.HSVToColor(this.hsv);        
    }

    /**
     * Lightness L = (value / 2) * (2 - sat)
     * => sat = 2 - 2 * L / value
     * satText = 2 - 2 * (L + dL) / valText
     * satText = 2 - (value * (2 - sat) + dL/2) / COLOR_VALUE
     */
    public void setPlayerHSV(float hue, float sat, float value) {
        Color.colorToHSV(color, this.hsv);
        //If negative, keep old value
        if (hue < 0) hue = this.hsv[0];
        if (sat < 0) sat = this.hsv[1];
        if (value < 0) value = this.hsv[2];
        // color text brighter (dL = COLOR_SAT / 2)
        float satText = 2 - (value * (2 - sat) + COLOR_SAT) / COLOR_VALUE;
        this.hsv[0] = hue;
        this.hsv[1] = Math.max(COLOR_SAT, satText);
        this.hsv[2] = COLOR_VALUE;
        this.colorText = Color.HSVToColor(this.hsv);
        // final color darker
        this.hsv[0] = hue;
        this.hsv[1] = sat;
        this.hsv[2] = value;
        this.color = Color.HSVToColor(this.hsv);
    }

    public void setPlayerColor(String color) {
        // setPlayerColor(getPlayerColor()) must keep the final color unchanged
        Color.colorToHSV(Color.parseColor(color), this.hsv);
        setPlayerHSV(this.hsv[0], this.hsv[1], this.hsv[2]);
    }

    public void setPlayerAI(int ai) {
        if (ai == SettingsActivity.AI_HUMAN ) {
            this.ai = false;
            this.local = true;
        } else if (ai == SettingsActivity.AI_HIDE ) {
            this.ai = true;
            this.local = false;
        } else if (ai == SettingsActivity.AI_SHOW ) {
            this.ai = true;
            this.local = true;
        } else { //SettingsActivity.AI_IDLE
            this.ai = false;
            this.local = false;
        }
    }

    public String getPlayerColor() {
        // Returns the player color in hex format (to use as preference)
        return colorToHex(this.color);
    }

    public String getPlayerAIColor() {
        // Returns ai color with fixed value, in hex format (to use as preference)
        float[] hsvColor = new float[3];
        Color.colorToHSV(color, this.hsv);
        hsvColor[0] = this.hsv[0];
        hsvColor[1] = this.hsv[1];
        hsvColor[2] = COLOR_VALUE;
        return colorToHex(Color.HSVToColor(hsvColor));
    }

    public int getPlayerAI() {
        if (this.ai) {
            if (this.local) {
                return SettingsActivity.AI_SHOW;
            } else {
                return SettingsActivity.AI_HIDE;
            }
        } else {
            if (this.local) {
                return SettingsActivity.AI_HUMAN;
            } else {
                return SettingsActivity.AI_IDLE;
            }
        }
    }

    public int getAIHostility() {
        return this.aiHostility;
    }

    public int getAILevel() {
        // Returns the ai hostility as positive number to show in screen
        if (this.ai) {
            if (this.aiHostility <= -SettingsActivity.HOSTILITY_MAX) {
                return 1;
            } else if (this.aiHostility == 0) {
                return 3;
            } else if (this.aiHostility >= SettingsActivity.HOSTILITY_MAX) {
                return 5;
            } else if (this.aiHostility < 0) {
                return 2;
            } else if (this.aiHostility > 0) {
                return 4;
            }
        }
        return 0;
    }

    @Override
    //Note: this class has a natural ordering that is inconsistent with equals.
    public int compareTo(PlayerData player) {
        // first higher Production, then number of Ships, then number of Planets
        if (this.planetsProd != player.planetsProd)  {
            return this.planetsProd - player.planetsProd;
        } else if ( (this.shipsPlanetNow + this.shipsFleetNow) != (player.shipsPlanetNow + player.shipsFleetNow) )  {
            return (this.shipsPlanetNow + this.shipsFleetNow) - (player.shipsPlanetNow + player.shipsFleetNow);
        } else if (this.planetsNum != player.planetsNum)  {
            return this.planetsNum - player.planetsNum;
        } else if (this.shipsArrived != player.shipsArrived)  {
            return this.shipsArrived - player.shipsArrived;
        }else if (this.shipsProduced != player.shipsProduced)  {
            return this.shipsProduced - player.shipsProduced;
        }else {
            return this.index - player.index;
        }
    }
}