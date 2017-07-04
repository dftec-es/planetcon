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

import java.util.Random;

public class PlanetData implements Comparable<PlanetData> {
    public static final String SAVED_TAG_PLANET = "Planet";
    public static final int MIN_PROD = 5;
    public static final int MAX_PROD = 15;
    public static final int MIN_DEF = 10;
    public static final int MAX_DEF = 20;

    public int index;           // index 1 for first home planet (index 0 used for virtual home planet)
    public int i;               // galaxy coordinate from left to right
    public int j;               // galaxy coordinate from top to bottom
    public String sName;        // name of the planet
    public int player;          // owner of the planet: player index
    public int playerPrev;      // owner of the planet in previous turn
    public int production;      // ships produced each turn
    public int upkeep;          // ship upkeep paid in this planet
    public int upkeepNext;      // ship upkeep estimated for next turn
    public int defense;         // 1/10 multiplier to defending ships
    public int shipsNow;        // actual value updated during the turn
    public int shipsPrev;       // actual value at the end of previous turn
    public int shipsPublicNow;  // value to show in current turn
    public int shipsPublicPrev; // value shown in previous turn
    // In neutral planets: shipsPrev = initial ships, shipsPublic = max possible ships, defense = -arrived
    public int threat;
    public int attackingPlayer; // owner of the attacking wave
    public int attackingShips;  // largest wave arrived this turn, ready for battle
    public int opposingShips;   // rest of enemy waves arrived this turn
    public int supportingShips; // reinforcements arrived this turn
    public int aiThreat;        // AI: estimated threats
    public int aiBorder;        // AI: distance to closest enemy
    public int aiReserved;      // AI: ships reserved to join other attacks, or to defend
    public int aiSent;          // AI: ships reserved to send this turn
    public int aiValue;         // AI: how much worth to attack it
    public int distance;
    public String sDefense;
    public String sDefensePrev;

    public PlanetData(int i, int j, int index) {
        // Home Planet
        this.index = index;
        this.i = i;
        this.j = j;
        this.sName = String.valueOf(index);
        this.player = index;
        this.playerPrev = index;
        this.production = (MAX_PROD + MIN_PROD)/2;
        this.upkeep = 0;
        this.upkeepNext = 0;
        this.defense = MAX_DEF;
        this.shipsNow = 0;
        this.shipsPrev = this.shipsNow;
        this.shipsPublicNow = 0;
        this.shipsPublicPrev = this.shipsPublicNow;
        this.threat = 0;
        this.attackingPlayer = 0;
        this.attackingShips = 0;
        this.opposingShips = 0;
        this.supportingShips = 0;
        this.aiThreat = 0;
        this.aiBorder = 0;
        this.aiReserved = 0;
        this.aiSent = 0;
        this.aiValue = 0;
        this.distance = 0;
        updateDefense();
    }

    public PlanetData(int i, int j, int index, Random random) {
        // Neutral Planet
        this.index = index;
        this.i = i;
        this.j = j;
        this.sName = String.valueOf(index);
        this.player = 0;
        this.playerPrev = 0;
        // Random between MIN and MAX values
        double rnd = random.nextDouble();
        this.production = (int)(rnd * (1 + MAX_PROD - MIN_PROD) + MIN_PROD);
        this.upkeep = 0;
        this.upkeepNext = 0;
        // Same random for defense and number of ships
        rnd = random.nextDouble();
        this.defense = (int)(rnd * (1 + MAX_DEF - MIN_DEF) + MIN_DEF);
        this.shipsNow = this.production + (int)(rnd * this.production);
        this.shipsPrev = this.shipsNow;
        this.shipsPublicNow = 0;
        this.shipsPublicPrev = this.shipsPublicNow;
        this.threat = 0;
        this.attackingPlayer = 0;
        this.attackingShips = 0;
        this.opposingShips = 0;
        this.supportingShips = 0;
        this.aiThreat = 0;
        this.aiBorder = 0;
        this.aiReserved = 0;
        this.aiSent = 0;
        this.aiValue = 0;
        this.distance = 0;
        updateDefense();
    }

    public PlanetData(String[] args) {
        int n = 0;
        this.index = Integer.valueOf(args[++n]);
        this.i = Integer.valueOf(args[++n]);
        this.j = Integer.valueOf(args[++n]);
        this.sName = String.valueOf(args[++n]);
        this.player = Integer.valueOf(args[++n]);
        this.playerPrev = Integer.valueOf(args[++n]);
        this.production = Integer.valueOf(args[++n]);
        this.upkeep = Integer.valueOf(args[++n]);
        this.upkeepNext = Integer.valueOf(args[++n]);
        this.defense = Integer.valueOf(args[++n]);
        this.shipsNow = Integer.valueOf(args[++n]);
        this.shipsPrev = Integer.valueOf(args[++n]);
        this.shipsPublicNow = Integer.valueOf(args[++n]);
        this.shipsPublicPrev = Integer.valueOf(args[++n]);
        this.threat = Integer.valueOf(args[++n]);
        this.attackingPlayer = 0;
        this.attackingShips = 0;
        this.opposingShips = 0;
        this.supportingShips = 0;
        this.aiThreat = 0;
        this.aiBorder = 0;
        this.aiReserved = 0;
        this.aiSent = 0;
        this.aiValue = 0;
        this.distance = 0;
        updateDefense();
    }

    public String save(StringBuilder builder) {
        builder.append(SAVED_TAG_PLANET);
        builder.append(",").append(this.index);
        builder.append(",").append(this.i);
        builder.append(",").append(this.j);
        builder.append(",").append(this.sName);
        builder.append(",").append(this.player);
        builder.append(",").append(this.playerPrev);
        builder.append(",").append(this.production);
        builder.append(",").append(this.upkeep);
        builder.append(",").append(this.upkeepNext);
        builder.append(",").append(this.defense);
        builder.append(",").append(this.shipsNow);
        builder.append(",").append(this.shipsPrev);
        builder.append(",").append(this.shipsPublicNow);
        builder.append(",").append(this.shipsPublicPrev);
        builder.append(",").append(this.threat);
        //this.attackingPlayer;
        //this.attackingShips;
        //this.opposingShips;
        //this.supportingShips;
        //this.aiThreat;
        //this.aiBorder;
        //this.aiReserved;
        //this.aiSent = 0;
        //this.aiValue;
        //this.distance;
        //this.sDefense;
        //this.sDefensePrev;
        return builder.toString();
    }

    public final void updateDefense() {
        // Store defense value from previous turn
        this.sDefensePrev = this.sDefense;
        this.sDefense = "";
        if (this.player != 0) {
            // Owned planets show defense with one decimal digit
            this.sDefense = String.valueOf((double)this.defense / MIN_DEF);
        } else {
            // Neutral planets do not use defense, show ships arrived instead
            if (this.shipsNow != this.shipsPrev) {
                this.sDefense = String.valueOf(this.shipsNow - this.shipsPrev);
            }
        }
        if (this.sDefensePrev == null) {
            // initialize to same value
            this.sDefensePrev = this.sDefense;
        }
    }

    public boolean exploredNeutral() {
        return (this.player == 0) && (2 * this.production != this.shipsPublicNow);
    }

    @Override
    //Note: this class has a natural ordering that is inconsistent with equals.
    public int compareTo(PlanetData planet) {
        return this.index - planet.index;
    }
}
