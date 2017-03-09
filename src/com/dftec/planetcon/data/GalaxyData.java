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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import com.dftec.planetcon.R;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.TreeSet;

public class GalaxyData {
    private static final boolean DEBUG = false;
    private static final long RANDOM_SEED = 123456789;
    private static final String VERSION_TAG = "Version";
    private static final String GALAXY_TAG = "Galaxy";
    private static final boolean SYMMETRIC = false;
    // Ascii 65=A, 97=a, ascii 48=0
    private static final int ALPHABET_SIZE = 26;

    private final Random mRandom;

    private int mMaxRadius;
    private int mMaxDiameter;
    private int mMaxTurns;
    private int mCurrentTurn;
    private int mCurrentPlayer;
    private int mRuleFow;
    private int mRuleDefense;
    private int mRuleUpkeep;
    private int mRuleProduction;

    private int mActivePlayers = 0;
    private int mActiveHumans = 0;
    private int mActiveWinner = 0;
    private long mGlobalProduction = 0;
    private long mGlobalShipsPlanet = 0;
    private long mGlobalShipsFleet = 0;

    private int mGrid[][];                  // carries the index of aPlanets, or 0 if no planet
    private ArrayList<PlanetData> aPlanets; // list of planets in the galaxy, from =1 to <=maxPlanets
    private ArrayList<PlayerData> aPlayers; // list of players stats, from =1 (neutral observer) to <=maxPlayers
    private TreeSet<FleetData> aFleets;     // sorted set of fleets
    private ArrayList<FleetData> aArrivals; // list of fleets that arrive this turn

    public GalaxyData(int radius, int players, int neutrals, int startpos, int density, int[] options) throws IllegalArgumentException {
        // Fixed seed for debugging
        if (DEBUG) {
            mRandom = new Random(RANDOM_SEED);
        } else {
            mRandom = new Random();
        }
        // Options: turns, fow, defense, upkeep, order
        mMaxTurns = options[0];
        mRuleFow = options[1];
        mRuleDefense = options[2];
        mRuleUpkeep = options[3];
        mRuleProduction = options[4];
        mCurrentTurn = 1;
        mCurrentPlayer = 1;
        mActivePlayers = 0;
        mActiveHumans = 0;
        mActiveWinner = 0;
        mGlobalProduction = 0;
        mGlobalShipsPlanet = 0;
        mGlobalShipsFleet = 0;

        initGalaxy(radius);
        if (generateGalaxy(players, neutrals, startpos, density) < 0) {
            throw new RuntimeException("Unable to create galaxy with those parameters");
        }
        // Sort planets starting by home planets, then from central to outer planets.
        rearrangePlanets();
    }

    public GalaxyData(PackageInfo pInfo, LineNumberReader reader) throws NumberFormatException, IOException, NameNotFoundException {
        String line;
        String[] args;
        boolean galaxyExists = false;
        PlanetData planet;

        // Fixed seed for debugging
        if (DEBUG) {
            mRandom = new Random(RANDOM_SEED);
        } else {
            mRandom = new Random();
        }
        while ( (line = reader.readLine()) != null ) {
            args = line.split(",");
            if (line.startsWith(GalaxyData.VERSION_TAG)) {
                int n = 0;
                if (pInfo.versionCode != Integer.valueOf(args[++n])) {
                    //throw new RuntimeException("Game version: " + pInfo.versionName + " does not match savegame version: " + args[1]);
                }
            }
            else if (line.startsWith(GalaxyData.GALAXY_TAG)) {
                int n = 0;
                initGalaxy(Integer.valueOf(args[++n]));
                mMaxTurns = Integer.valueOf(args[++n]);
                mCurrentTurn = Integer.valueOf(args[++n]);
                mCurrentPlayer = Integer.valueOf(args[++n]);
                mRuleFow = Integer.valueOf(args[++n]);
                mRuleDefense = Integer.valueOf(args[++n]);
                mRuleUpkeep = Integer.valueOf(args[++n]);
                mRuleProduction = Integer.valueOf(args[++n]);
                mActivePlayers = Integer.valueOf(args[++n]);
                mActiveHumans = Integer.valueOf(args[++n]);
                mActiveWinner = Integer.valueOf(args[++n]);
                mGlobalProduction = Long.valueOf(args[++n]);
                mGlobalShipsPlanet = Long.valueOf(args[++n]);
                mGlobalShipsFleet = Long.valueOf(args[++n]);

                galaxyExists = true;
            } else if (line.startsWith(PlanetData.SAVED_TAG_PLANET) && galaxyExists) {
                planet = new PlanetData(args);
                aPlanets.add(planet);
                mGrid[planet.i][planet.j] = planet.index;
            } else if (line.startsWith(PlayerData.SAVED_TAG_PLAYER) && galaxyExists) {
                aPlayers.add(new PlayerData(args));
            } else if (line.startsWith(FleetData.SAVED_TAG_FLEET) && galaxyExists) {
                aFleets.add(new FleetData(args));
            } else if (line.startsWith(FleetData.SAVED_TAG_ARRIVAL) && galaxyExists) {
                aArrivals.add(new FleetData(args));
            }
        }
        if (!galaxyExists) {
            throw new RuntimeException("Invalid savegame");
        }
    }

    private void initGalaxy(int radius) {
        int i, j;

        mMaxRadius = Math.max(2, radius);
        mMaxDiameter = 2 * mMaxRadius + 1;
        mGrid = new int[mMaxDiameter][mMaxDiameter];
        aPlayers = new ArrayList<PlayerData>();
        aPlanets = new ArrayList<PlanetData>();
        aFleets = new TreeSet<FleetData>();
        aArrivals = new ArrayList<FleetData>();

        aPlayers.add(new PlayerData(0, null));
        // Virtual home planet for player 0 (observer) at center of the galaxy
        aPlanets.add(new PlanetData(mMaxRadius, mMaxRadius, 0));
        for (i = 0; i < mMaxDiameter; i++) {
            for (j = 0; j < mMaxDiameter; j++) {
                mGrid[i][j] = 0;
            }
        }
    }

    private int generateGalaxy(int maxPlayers, int maxNeutrals, int homeRadius, int density) {
        boolean singleOrbit = false;
        int r;              // radius
        int planets = 0;    // counter of planets per orbit
        int homes = 0;      // counter of home planets
        int neutrals = 0;   // counter of neutral planets
        int firstPlayer, lastPlayer;
        int homeTiles = 0;  // counter of tiles in home orbits
        int homesOrbit;     // home planets per orbit
        int planetsOrbit;   // planets per orbit
        double planetsTile; // planets per tile
        double gapHomes;    // tiles between home planets
        double gap;         // tiles between planets
        double arc;         // arc (number of tiles around the orbit)
        Coordinate point;

        if (maxPlayers < 2) maxPlayers = 2;
        if (maxNeutrals < 0) maxNeutrals = 0;
        // Auto calculate home orbits
        if (homeRadius <= 0) {
            //gap = Math.sqrt( (double)(maxDiameter * maxDiameter) / maxPlayers );
            if (maxPlayers <= 16) {
                // Linear formula: home planets in central orbit (further away if many players)
                homeRadius = (int)Math.ceil( ((double)mMaxRadius / 2) * (1 + (double)maxPlayers / 16) );
                // Formula to keep uniform distribution of home planets (solution of 2nd degree equation)
                //homeRadius = (int)Math.ceil( (double)maxRadius * Math.sqrt((double)maxPlayers) / 4 );
                singleOrbit = true;
            } else {
                // Formula to keep uniform distribution of home planets (solution of 2nd degree equation)
                homeRadius = (int)Math.ceil( (double)mMaxRadius / ( Math.sqrt((double)maxPlayers) - 2 ) );
                singleOrbit = false;
            }
        }
        homeRadius = Math.min(homeRadius, mMaxRadius);

        // Sum tiles of all home orbits
        for (r = 1; r <= mMaxRadius; r++) {
            if ((singleOrbit) && (homeRadius == r)
                    || (!singleOrbit) && (r % homeRadius == 0)) {
                homeTiles += 8*r;
            }
        }
        // Expected tiles between home planets
        gapHomes = (double)homeTiles / maxPlayers;
        // Home Planets
        for (r = 1; r <= mMaxRadius; r++) {
            if ((singleOrbit) && (homeRadius == r)
                    || (!singleOrbit) && (r % homeRadius == 0)) {
                // Expected home planets in this orbit
                homesOrbit = (int)Math.ceil( 8*(double)r / gapHomes );

                // If expected one single home planet in this orbit, place all remaining home planets
                if ( (homesOrbit == 1) || (maxPlayers - homes - homesOrbit <= 1) ) {
                    // If last home orbit, update number of remaining home planets
                    homesOrbit = maxPlayers - homes;
                }
                // Update tiles between home planets for this orbit
                gap = 8*(double)r / homesOrbit;
                arc = mRandom.nextInt(8*r);
                firstPlayer = aPlayers.size();
                lastPlayer = firstPlayer + homesOrbit;
                for (planets = firstPlayer; (planets < lastPlayer) && (homes < maxPlayers); planets++) {
                    // Arc rounded to keep uniform distribution around the orbit
                    point = new Coordinate(r, (int)Math.round(arc), mMaxRadius);
                    // Create new home planet and new player
                    if (mGrid[point.i][point.j] == 0) {
                        homes++;
                        mGrid[point.i][point.j] = aPlanets.size();
                        aPlanets.add(new PlanetData(point.i, point.j, mGrid[point.i][point.j]));
                        aPlayers.add(new PlayerData(aPlayers.size(), mRandom));
                    }
                    arc = (arc + gap) % (8*r);
                }
            }
        }
        if (homes != maxPlayers) {
            return -1;
        }

        if (density > 0) {
            maxNeutrals = (int)( (double)density/4 * (double)(mMaxDiameter * mMaxDiameter * maxPlayers)/ homeTiles );
        }
        for (r = mMaxRadius; r > 0; r--) {
            // Uniform distribution of remaining neutral planets (subtracting tiles from inner home orbits)
            planetsTile = (double)(maxNeutrals - neutrals) / ((2*r + 1) * (2*r + 1) - homeTiles);
            if (planetsTile > 0) {
                // Planets per orbit
                planetsOrbit = (int) Math.round(planetsTile * 8*r);

                // If home orbit
                if ((singleOrbit) && (homeRadius == r)
                        || (!singleOrbit) && (r % homeRadius == 0)) {
                    // Subtract tiles so homeTiles does not account the outer home orbits
                    homeTiles -= 8*r;
                }

                // Neutral Planets
                 else {
                     // (double) tiles between planets based on (integer) planets per orbit
                    gap = (8*(double)r / planetsOrbit);
                    // Random initial arc
                    arc = mRandom.nextInt(8*r);
                    for (planets = 1; (planets <= planetsOrbit) && (neutrals < maxNeutrals); planets++) {
                        // Use roundRandom to create pseudo-uniform distribution around the orbit
                        point = new Coordinate(r, roundRandom(SYMMETRIC, arc), mMaxRadius);
                        // Create new neutral planet
                        if (mGrid[point.i][point.j] == 0) {
                            neutrals++;
                            mGrid[point.i][point.j] = aPlanets.size();
                            aPlanets.add(new PlanetData(point.i, point.j, mGrid[point.i][point.j], mRandom));
                        }
                        // When arc reaches 360 degrees (8*r), continue from 0 degrees (0*r)
                        arc = (arc + gap) % (8*r);
                    }
                }
            }
        }
        // Central tile (r=0)
        if (neutrals < maxNeutrals) {
            // Create central neutral planet if needed (depending on random rounding)
            neutrals++;
            mGrid[mMaxRadius][mMaxRadius] = aPlanets.size();
            aPlanets.add(new PlanetData(mMaxRadius, mMaxRadius, mGrid[mMaxRadius][mMaxRadius], mRandom));
        }
        // Final number of planets in this galaxy
        return neutrals + homes;
    }

    private void rearrangePlanets() {   // Set planet.index
        for (PlanetData planet : aPlanets) {
            // Non-home planets
            if ( (planet.index >= aPlayers.size()) && (planet.index < aPlanets.size()) ) {
                // New index starting from home planets, then from central to outer planets.
                planet.index = aPlayers.size() + aPlanets.size() - planet.index - 1;
                mGrid[planet.i][planet.j] = planet.index;
            }
        }
        // use the new index to sort the list
        Collections.sort(aPlanets);
    }

    private String recursiveName(int indexNeutral, String[] alphabet1, String[] alphabet2) {
        int first, second;
        // If there are as much planets as letters, use 1 char as name
        if (indexNeutral < ALPHABET_SIZE) {
            first = (indexNeutral) % ALPHABET_SIZE;
            return alphabet1[first];
        } else {
            indexNeutral -= ALPHABET_SIZE;
        }
        // If there are more planets than letters, use 2 chars as name
        if (indexNeutral < ALPHABET_SIZE * ALPHABET_SIZE) {
            first = (indexNeutral) % ALPHABET_SIZE;
            second = (indexNeutral) / ALPHABET_SIZE;
            return alphabet1[first].concat(alphabet2[second]);
        } else {
            indexNeutral -= ALPHABET_SIZE * ALPHABET_SIZE;
            // If there are even more planets, swap the alphabets and start again
            return recursiveName(indexNeutral, alphabet2, alphabet1);
        }
    }

    public void renamePlanets(Context context) {   // Set planet.names
        for (PlanetData planet : aPlanets) {
            // Non-home planets
            if ( (planet.index >= aPlayers.size()) && (planet.index < aPlanets.size()) ) {
                int indexNeutral = planet.index - aPlayers.size();
                String alphabet1[] = context.getResources().getStringArray(R.array.alphabet_1);
                String alphabet2[] = context.getResources().getStringArray(R.array.alphabet_2);
                planet.sName = recursiveName(indexNeutral, alphabet1, alphabet2);
            }
        }
    }

    public boolean updateDistances(int iFrom, int jFrom) {  // Set planet.distance
        PlanetData planetFrom;
        int distance;

        planetFrom = findPlanetData(iFrom, jFrom);
        if (planetFrom != null) {
            for (PlanetData planetTo : aPlanets) {
                distance = getDistance(planetFrom, planetTo);
                planetTo.distance = distance;
            }
            return true;
        } else {
            return false;
        }
    }

    public int getDistance(PlanetData planetFrom, PlanetData planetTo) {
        return getDistance(planetFrom.i, planetFrom.j, planetTo.i, planetTo.j);
    }

    public int getDistance(double iFrom, double jFrom, double iTo, double jTo) {
        return (int) Math.ceil(Math.sqrt( (iTo-iFrom)*(iTo-iFrom) + (jTo-jFrom)*(jTo-jFrom) )/2);
    }

    public int getMaxDistance() {
        return (int) Math.ceil(Math.sqrt(2.0) * mMaxRadius);
    }

    public int getMaxRadius() {
        return mMaxRadius;
    }

    public int getMaxDiameter() {
        return mMaxDiameter;
    }

    public int getNumPlanets() {
        // Planet 0 is virtual neutral home
        return aPlanets.size() - 1;
    }

    public int getNumPlayers() {
        // Player 0 is virtual neutral observer
        return aPlayers.size() - 1;
    }

    public int getMaxTurns() {
        return mMaxTurns;
    }

    public int getCurrentPlayer() {
        return mCurrentPlayer;
    }

    public int getCurrentTurn() {
        return mCurrentTurn;
    }

    public int getRuleUpkeep() {
        return mRuleUpkeep;
    }

    public int getRuleDefense() {
        return mRuleDefense;
    }

    public int getRuleProduction() {
        return mRuleProduction;
    }

    public int getRuleFow() {
        return mRuleFow;
    }

    public int getActivePlayers() {
        return mActivePlayers;
    }

    public int getActiveHumans() {
        return mActiveHumans;
    }

    public PlayerData getActiveWinnerData() {
        return aPlayers.get(mActiveWinner);
    }

    public long getGlobalProduction() {
        return mGlobalProduction;
    }

    public long getGlobalShipsPlanet() {
        return mGlobalShipsPlanet;
    }

    public long getGlobalShipsFleet() {
        return mGlobalShipsFleet;
    }

    public PlanetData findPlanetData(int ip, int jp) {
        if ( (ip >= 0) && (ip < mMaxDiameter)
                && (jp >= 0) && (jp < mMaxDiameter)) {
            if (mGrid[ip][jp] != 0) {
                // If pointer over existing planet
                return aPlanets.get(mGrid[ip][jp]);
            }
        }
        return null;
    }

    public PlanetData getPlanetData(int planetIndex) {
        if ( (planetIndex >= 0) && (planetIndex < aPlanets.size()) ) {
            return aPlanets.get(planetIndex);
        }
        return null;
    }

    public PlayerData getPlayerData(int playerIndex) {
        if ( (playerIndex >= 0) && (playerIndex < aPlayers.size()) ) {
            return aPlayers.get(playerIndex);
        }
        return null;
    }
/*
    public FleetData getFleetData (int fleetIndex) {
        if ( (fleetIndex >= 0) && (fleetIndex < aFleets.size()) ) {
            return aFleets.get(fleetIndex);
        }
        return null;
    }
*/
    public FleetData getArrivalData(int arrivalIndex) {
        if ( (arrivalIndex >= 0) && (arrivalIndex < aArrivals.size()) ) {
            return aArrivals.get(arrivalIndex);
        }
        return null;
    }

    public PlayerData getCurrentPlayerData() {
        return aPlayers.get(mCurrentPlayer);
    }

    public ArrayList<PlayerData> getPlayers() {
        return aPlayers;
    }

    public ArrayList<PlanetData> getPlanets() {
        return aPlanets;
    }

    public TreeSet<FleetData> getFleets() {
        return aFleets;
    }

    public ArrayList<FleetData> getArrivals() {
        return aArrivals;
    }

    public Random getRandom() {
        return mRandom;
    }

    public void setMaxTurns(int turns) {
        mMaxTurns = turns;
    }

    public void setCurrentTurn(int turn) {
        mCurrentTurn = turn;
    }

    public void setCurrentPlayer(int player) {
        if (player > getNumPlayers()) {
            mCurrentPlayer = getNumPlayers();
        } else {
            mCurrentPlayer = player;
        }
    }

    public void setActivePlayers(int players) {
        mActivePlayers = players;
    }

    public void setActiveHumans(int humans) {
        mActiveHumans = humans;
    }

    public void setActiveWinner(int winner) {
        mActiveWinner = winner;
    }

    public void setGlobalProduction(long production) {
        mGlobalProduction = production;
    }

    public void setGlobalShipsPlanet(long ships) {
        mGlobalShipsPlanet = ships;
    }

    public void setGlobalShipsFleet(long ships) {
        mGlobalShipsFleet = ships;
    }

    public void createFleet(PlanetData planetFrom, PlanetData planetTo, int sentShips) {
        if ( (sentShips > 0) && (planetFrom.shipsNow >= sentShips) && (planetFrom.index != planetTo.index) ) {
            int turnArrival = mCurrentTurn + getDistance(planetFrom, planetTo);
            FleetData fleet = new FleetData(mCurrentTurn, planetFrom.player, planetFrom.index, planetTo.index, turnArrival, sentShips);
            planetFrom.shipsNow -= sentShips;
            aFleets.add(fleet);
        }
    }

    public double multiplyDefense(double ships, PlanetData planet) {
        // Defense value between 10 and 20, real effect between 1.0 and 2.0
        if ( (mRuleDefense > 0) && (planet.player != 0) ) {
            return (ships * planet.defense) / PlanetData.MIN_DEF;
        } else {
            return ships;
        }
    }
    public double divideDefense(double ships, PlanetData planet) {
        if ( (mRuleDefense > 0) && (planet.player != 0) ) {
            return (ships * PlanetData.MIN_DEF) / planet.defense;
        } else {
            return ships;
        }
    }

    public int roundRandom(double value) {
        return roundRandom(true, value);
    }

    public int roundRandom(boolean normal, double value) {
        double prob; //probability to round up

        if (normal) {
            prob = value - (int)value;
        } else {
            prob = 0.5;
        }
        if (mRandom.nextDouble() < prob) {
            return (int)value + 1; //round up
        } else {
            return (int)value;  //round down
        }
    }

    public void save(PackageInfo pInfo, PrintWriter writer) throws NameNotFoundException {
        StringBuilder builder = new StringBuilder();

        //mRandom;
        builder.append(VERSION_TAG);
        builder.append(",").append(pInfo.versionCode);
        writer.println(builder.toString());

        builder.delete(0, builder.length());
        builder.append(GALAXY_TAG);
        builder.append(",").append(mMaxRadius);
        builder.append(",").append(mMaxTurns);
        builder.append(",").append(mCurrentTurn);
        builder.append(",").append(mCurrentPlayer);
        builder.append(",").append(mRuleFow);
        builder.append(",").append(mRuleDefense);
        builder.append(",").append(mRuleUpkeep);
        builder.append(",").append(mRuleProduction);
        builder.append(",").append(mActivePlayers);
        builder.append(",").append(mActiveHumans);
        builder.append(",").append(mActiveWinner);
        builder.append(",").append(mGlobalProduction);
        builder.append(",").append(mGlobalShipsPlanet);
        builder.append(",").append(mGlobalShipsFleet);
        writer.println(builder.toString());

        for (PlanetData planet : aPlanets) if (planet.index != 0) {
            builder.delete(0, builder.length());
            writer.println(planet.save(builder));
        }
        for (PlayerData player : aPlayers) if (player.index != 0) {
            builder.delete(0, builder.length());
            writer.println(player.save(builder));
        }
        for (FleetData fleet : aFleets) {
            builder.delete(0, builder.length());
            writer.println(fleet.save(builder, false));
        }
        for (FleetData arrival : aArrivals) {
            builder.delete(0, builder.length());
            writer.println(arrival.save(builder, true));
        }
    }

}
