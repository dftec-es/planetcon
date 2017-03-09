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

public class FleetData implements Comparable<FleetData> {
    public static final String SAVED_TAG_FLEET = "Fleet";
    public static final String SAVED_TAG_ARRIVAL = "Arrival";

    public int turn;
    public int player;                  // owner of the fleet: player index
    public int from;                    // source planet index
    public int to;                      // target planet index
    public int at;                      // arrival at this turn
    public int ships;                   // amount of ships
    public int threat;                  // sum of ships sent from same planet in same turn
    public int wave;                    // sum of ships from same player arriving to same planet at same turn

    public FleetData(int turn, int player, int from, int to, int at, int ships) {
        this.turn = turn;
        this.player = player;
        this.from = from;
        this.to = to;
        this.at = at;
        this.ships = ships;
        this.threat = 0;
        this.wave = 0;
    }

    public FleetData(String[] args) {
        int n = 0;
        this.turn = Integer.valueOf(args[++n]);
        this.player = Integer.valueOf(args[++n]);
        this.from = Integer.valueOf(args[++n]);
        this.to = Integer.valueOf(args[++n]);
        this.at = Integer.valueOf(args[++n]);
        this.ships = Integer.valueOf(args[++n]);
        this.threat = Integer.valueOf(args[++n]);
        this.wave = Integer.valueOf(args[++n]);
    }

    public String save(StringBuilder builder, boolean arrival) {
        if (arrival) {
            builder.append(SAVED_TAG_ARRIVAL);
        } else {
            builder.append(SAVED_TAG_FLEET);
        }
        builder.append(",").append(this.turn);
        builder.append(",").append(this.player);
        builder.append(",").append(this.from);
        builder.append(",").append(this.to);
        builder.append(",").append(this.at);
        builder.append(",").append(this.ships);
        builder.append(",").append(this.threat);
        builder.append(",").append(this.wave);
        return builder.toString();
    }

    @Override
    //Note: this class has a natural ordering that is inconsistent with equals.
    public int compareTo(FleetData fleet) {
        // first lower turn (arrival), then target planet, then lower number of ships
        if (this.at != fleet.at)  {
            return this.at - fleet.at;
        } else if (this.to != fleet.to)  {
            return this.to - fleet.to;
        } else if (this.ships != fleet.ships)  {
            return this.ships - fleet.ships;
        } else if (this.turn != fleet.turn)  {
            return this.turn - fleet.turn;
        } else if (this.from != fleet.from)  {
            return this.from - fleet.from;
        } else {
            return this.player - fleet.player;
        }
    }
}