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
package com.dftec.planetcon.ai;

public class AttackData implements Comparable<AttackData> {
    public int source;          // AI: Source planet
    public int target;          // AI: Target planet
    public int distance;        // AI: Distance in turns to target
    public int shipsAvailable;  // AI: Ships available to perform the attack
    public int shipsNeeded;     // AI: Estimation of ships needed to conquer the target
    public int value;           // AI: Value assigned to this attack

    public AttackData() {
        this.source = 0;
        this.target = 0;
        this.distance = 0;
        this.shipsAvailable = 0;
        this.shipsNeeded = 0;
        this.value = 0;
    }

    public void resetAttack() {
        this.source = 0;
        this.target = 0;
        this.distance = 0;
        this.shipsAvailable = 0;
        this.shipsNeeded = 0;
        this.value = 0;
    }

    public void setAttack(int source, int target, int distance, int available, int needed, int value) {
        this.source = source;
        this.target = target;
        this.distance = distance;
        this.shipsAvailable = available;
        this.shipsNeeded = needed;
        this.value = value;
    }

    @Override
    //Note: this class has a natural ordering that is inconsistent with equals.
    public int compareTo(AttackData attack) {
        return this.value - attack.value;
    }
}