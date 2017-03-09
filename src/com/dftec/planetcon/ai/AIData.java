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

import android.util.Log;
import com.dftec.planetcon.data.FleetData;
import com.dftec.planetcon.data.GalaxyData;
import com.dftec.planetcon.data.PlanetData;
import com.dftec.planetcon.data.PlayerData;
import java.util.ArrayList;
import java.util.Random;

// Can not be concurrent due to shared access to ai fields of planets
// (it is not worth to create separate data)
public class AIData {
    private static final boolean DEBUG = false;
    private static final String DEBUG_TAG = "AI";
    private static final int DONE = -1;
    private static final int DEFCON_MAX = 4;

    private final GalaxyData mGalaxy;
    private final Random mRandom;
    private final ArrayList<PlanetData> aOwnPlanets;
    private final ArrayList<PlanetData> aEnemyPlanets;
    private final ArrayList<FleetData> aOwnFleets;
    private final ArrayList<FleetData> aResultFleets;
    private final AttackData mReinforceBest;
    private final AttackData mAttackBest;
    private final PlayerData mPlayerAI;
    private final int mCurrentTurn;
    private final int mCurrentPlayer;
    private final double mShipsTotalNow;
    private double mShipsPlanetUpdated;

    public AIData(GalaxyData galaxyGame, int currentPlayer, int currentTurn) {
        mGalaxy = galaxyGame;
        mRandom = mGalaxy.getRandom();
        aOwnPlanets = new ArrayList<PlanetData>();
        aEnemyPlanets = new ArrayList<PlanetData>();
        aOwnFleets = new ArrayList<FleetData>();
        aResultFleets = new ArrayList<FleetData>();
        mReinforceBest = new AttackData();
        mAttackBest = new AttackData();
        mCurrentTurn = currentTurn;
        mCurrentPlayer = currentPlayer;
        mPlayerAI = mGalaxy.getPlayerData(mCurrentPlayer);
        mShipsTotalNow = mPlayerAI.shipsPlanetNow + mPlayerAI.shipsFleetNow;
        mShipsPlanetUpdated = mPlayerAI.shipsPlanetNow;    //Updated during AI turn when fleets are sent
    }

    /**
     * Summary:
     * 1) Each planet reserves for defense a random amount of ships (average of 1/2, taking into account close threats).
     * 2) Rest of ships are used to perform attacks (sent this turn, or reserved to continue the wave in future turns).
     * All possible targets are evaluated, and each one assigned a value that represents the chance of being the chosen attack.
     * 3) Decide to make more attacks depending on calculated defcon (desired fraction of ships to keep in planet).
     * 4) The ships unused due to defcon (not reserved for defense or attack) are considered to perform reinforcements.
     *
     * TODO: adapt for no fog of war
     */
    public ArrayList<FleetData> processAiData () {
        long startTime = System.currentTimeMillis();
        boolean continueAttacks = false;
        boolean continueReinforces = false;
        int n, attempts, index, defcon;

        if (mPlayerAI.planetsProd == 0) {
            return aResultFleets;
        }

        // Exploration time equal to distance to nearest enemy home planet
        calculateExplorationTime();
        // Defensive level
        defcon = defcon();

        if (DEBUG) {
            mPlayerAI.sName = String.valueOf(defcon);
            Log.d(DEBUG_TAG, "AI: " + mCurrentPlayer + ", defcon " + defcon);
        }

        // Values
        for (FleetData fleet : mGalaxy.getFleets()) {
            // Create different list for own fleets
            if (fleet.player == mCurrentPlayer) {
                aOwnFleets.add(fleet);
            }
        }
        for (PlanetData planet : mGalaxy.getPlanets()) if (planet.index != 0) {
            // Create different list for own and enemy planets
            if (planet.player == mCurrentPlayer) {
                aOwnPlanets.add(planet);
            } else {
                aEnemyPlanets.add(planet);
            }

            calculateValues(planet);

            if (DEBUG) {
                index = planet.sName.indexOf(".");  // do not use "," (savegame separator)
                if (index >= 0) {
                    planet.sName = planet.sName.substring(0, index);
                }
                planet.sName = planet.sName + "." + String.valueOf(planet.aiValue);
            }
        }
//        Log.d( DEBUG_TAG, "Values: " + String.valueOf(System.currentTimeMillis() - startTime));

        // Attacks
        attempts = mPlayerAI.planetsNum + 2;
        for (n = 0; n < attempts; n++) { //min 3 attempts
            // Keep in land a fraction (proportional to defcon) of the ships in land at start of turn
            if (defcon == 0) {
                continueAttacks = mShipsPlanetUpdated > 0;
            } else {
                // At least one attack, even with defcon 4
                continueAttacks = mShipsPlanetUpdated >= (double)(mPlayerAI.shipsPlanetNow * defcon) / DEFCON_MAX;
            }
            if (continueAttacks && calculateAttack()) {
                // Attack added to aResultFleets
                performAttack();
            } else {
                break;
            }
        }
//        Log.d( DEBUG_TAG, "Attacks: " + String.valueOf(System.currentTimeMillis() - startTime) + ", " + n + " / " + attempts + ", " + mShipsPlanetUpdated + " / " + mPlayerAI.shipsPlanetNow);

        // Reinforcements
        attempts = (int) Math.ceil( (double)((mPlayerAI.planetsNum - 1) * defcon) / DEFCON_MAX );
        for (n = 0; n < attempts; n++) {  // attempt only if defcon >= 1 and planetsNum > 1
            // Keep in land a fraction (proportional to defcon) of the total ships at start of turn
            // (reinforces continue after attacks if shipsFleetNow < shipsPlanetNow)
            continueReinforces = mShipsPlanetUpdated > ((mShipsTotalNow / 2) * defcon) / DEFCON_MAX;
            if (continueReinforces && calculateReinforce()) {
                // It does not move reserved ships, only those ships unused due to defcon
                performReinforce();
            } else {
                break;
            }
        }
//        Log.d( DEBUG_TAG, "Reinforces: " + String.valueOf(System.currentTimeMillis() - startTime) + ", " + n + " / " + attempts + ", " + mShipsPlanetUpdated + " / " + mShipsTotalNow);
        return aResultFleets;
    }

    private int defcon() {
        // defcon (average fraction of ships reserved for defense): 0 (0), 1 (1/4), 2 (2/4), 3 (3/4), 4 (4/4)
        // A sudden decrease of defcon encourages a massive attack
        // Avoid sudden increase of defcon because it'd discontinue current attacks
        int defcon;
        int explorationTime = mPlayerAI.aiExploration;

        if (mCurrentTurn < explorationTime) {
            defcon = 0;
        }
        // Semi-random state between 1 and defconMax (without memory)
        else {
            defcon = 2;

            double globalProd = mGalaxy.getGlobalProduction();
            double globalShips = mGalaxy.getGlobalShipsPlanet() + mGalaxy.getGlobalShipsFleet();
            // Player production compared to global production (neutrals included)
            double ratioProd = (double) mPlayerAI.planetsProd / globalProd;
            double ratioAverage = (double) 1 / mGalaxy.getActivePlayers();
            // Ships in planets compared to total ships (planets + fleets)
            double ratioShips = (double) mPlayerAI.shipsPlanetNow / mShipsTotalNow;
            double ratioShipsAverage = (double) mGalaxy.getGlobalShipsPlanet() / globalShips;

            // The more production, the more defensive
            double chance = 1 - ratioProd / ratioAverage;
            if (ratioProd > ratioAverage) {
                defcon++;
//                Log.d( DEBUG_TAG, "Defcon: prod++ " );
            } else if (mRandom.nextDouble() < chance) {
                defcon--;
//                Log.d( DEBUG_TAG, "Defcon: prod-- " + "(" + chance + ")" );
            }
//            Log.d( DEBUG_TAG, "Defcon: prod " + mPlayerAI.planetsProd + " / " + globalProd + " = " + ratioProd + " <> " + ratioAverage );

            // The more ships in land, the more aggressive
            if (ratioShips > ratioShipsAverage) {
                chance = 1 - ratioShipsAverage / ratioShips;
                if (mRandom.nextDouble() < chance) {
                    defcon--;
//                    Log.d( DEBUG_TAG, "Defcon: ships-- " + "(" + chance + ")" );
                }
            } else {
                chance = 1 - ratioShips / ratioShipsAverage;
                if (mRandom.nextDouble() < chance) {
                    defcon++;
//                    Log.d( DEBUG_TAG, "Defcon: ships++ " + "(" + chance + ")" );
                }
            }
//            Log.d( DEBUG_TAG, "Defcon: ships " + mPlayerAI.shipsPlanetNow + " / " + mShipsTotalNow + " = " + ratioShips + " <> " + ratioShipsAverage );

            defcon = Math.max(1, defcon);
            defcon = Math.min(defcon, DEFCON_MAX);
            mPlayerAI.aiDefcon = defcon;
        }
        return defcon;
    }

    private void calculateExplorationTime() {
        int minDistance, homeDistance;
        // Calculate only once (the first time)
        if (mPlayerAI.aiExploration == 0) {
            PlanetData planetHome = mGalaxy.getPlanetData(mCurrentPlayer);
            minDistance = mGalaxy.getMaxDistance();
            for (int p = 1; p <= mGalaxy.getNumPlayers(); p++) if (p != mCurrentPlayer) {
                PlanetData planetEnemy = mGalaxy.getPlanetData(p);
                homeDistance = mGalaxy.getDistance(planetHome, planetEnemy);
                if (homeDistance < minDistance) {
                    minDistance = homeDistance;
                }
            }
            mPlayerAI.aiExploration = minDistance;
        }
    }

    private void calculateValues(PlanetData planet) {
        double value = 0;
        double production, exploration;
        boolean hostile;
        int distance;
        int threatActual = 0;
        int threatProduction = 0;
        int threatRush = 0;
        int border = mGalaxy.getMaxDistance();
        int explorationTime = mPlayerAI.aiExploration;
        PlayerData playerNear;

        for (PlanetData planetNear : mGalaxy.getPlanets()) if (planetNear.index != 0) {
            playerNear = mGalaxy.getPlayerData(planetNear.player);
            production = planetNear.production;
            distance = mGalaxy.getDistance(planetNear, planet);

            // Estimate value taking into account proximity to rest of planets
            if (planetNear.index == planet.index) {
                // It sums the production of the target planet (distance will be 1)
                value += production;
            } else if (planetNear.player == mCurrentPlayer) {
                // Increase value of target planet if close to own planet
                value += production / (1 + distance);
            } else if (planetNear.player == 0) {
                // Increase value of target planet if close to neutral planet
                value += production / ( (1 + distance) * (1 + distance) );
            } else if (planetNear.player != planet.player) {
                hostile = (mGalaxy.getActivePlayers() == 2) || ( (mPlayerAI.aiHostility > 0) && (playerNear.local) );
                // Decrease value of enemy target planet if close to a different enemy
                // (except when the different enemy is human and the AI is hostile)
                if ( (planet.player != mCurrentPlayer) && (hostile == false) ) {
                    exploration = Math.max(1, 1 + explorationTime - mCurrentTurn);
                    value -= (production * exploration * exploration) / ( (1 + distance) * (1 + distance) );
                    // (While expanding, planets close to rest of players are less desirable,
                    // and it encourages to expand to outer planets first, or to rush attack home planets)
                }
                // Increase value of own target planet (reinforcement) if close to an enemy
                else {
                    value += production / ( (1 + distance) * (1 + distance) );
                }
            } else {
                // Do not increase or decrease value when close to planet of same player
            }

            // Estimate threats from enemies of the current AI player
            if ( (planetNear.player != 0) && (planetNear.player != mCurrentPlayer) ) {
                if (distance == 1) {
                    // Ships ready to attack this turn
                    threatActual += planetNear.shipsPublicNow;
                    threatProduction += planetNear.production;
                }
                if (distance < border) {
                    // Distance to closer enemy planet
                    border = distance;
                }
            }
        }
        // Do not allow negative values
        value = Math.max(1, value);
        // Adjust value depending on ai hostility, and winner stats
        value = modifyValue(value, planet);

        planet.aiValue = (int)value + 1;
        planet.aiThreat = (int)Math.ceil(mGalaxy.divideDefense(threatActual, planet));
        planet.aiBorder = border;
        planet.aiSent = 0;

        // Reserved ships depending on estimated threats (average 1/2)
        if ( (planet.player == mCurrentPlayer) && (mPlayerAI.aiDefcon > 0) ) {
            planet.aiReserved = mRandom.nextInt(planet.shipsNow + 1);

            // Reduce the reserved ships when defcon 1, to improve expansion (to 1/4)
            if ( (mPlayerAI.aiDefcon == 1) && (border > 1) ) {
                planet.aiReserved /= 2;
            }
            // Defend home planet from rush attack while expanding
            if ( (mCurrentTurn >= explorationTime) && (mCurrentTurn < 2 * explorationTime - 1) &&
                (mPlayerAI.aiDefcon == 1) && (mPlayerAI.planetsNum > 1) && (planet.index == mPlayerAI.index) ) {
                // it will stop the first turn that defcon grows beyond 1, or when production is lower than that of neighbours
                if ( neighbourRush() ) {
                    threatRush = (1 + mCurrentTurn - explorationTime) * planet.production;
                    // Take into account Defense rule
                    threatRush = (int)Math.ceil(mGalaxy.divideDefense(threatRush, planet));
                    if (planet.shipsNow >= threatRush) {
                        planet.aiReserved = Math.max(planet.aiReserved, threatRush);
                    } else {
                        planet.aiReserved = planet.shipsNow;
                    }
                }
            }
            // Duel against planets that sum less production: try to defend
            // TODO: take into account incoming reinforcements
            if ( (threatActual > 0) && (planet.shipsNow >= threatActual) && (planet.production > threatProduction) ) {
                planet.aiReserved = Math.max(planet.aiReserved, threatActual);
            }
        } else {
            planet.aiReserved = 0;
        }
    }

    private double modifyValue(double value, PlanetData planet) {
        double modProd, modShips;
        PlayerData playerOwner = mGalaxy.getPlayerData(planet.player);
        PlayerData playerWinning = mGalaxy.getActiveWinnerData();

        if (planet.player != 0) {
            // Lower value the higher the production and number of ships of the winning player (help to catch up)
            if (planet.player != playerWinning.index) {
                // the divider will be 1 if all players are even, and lower than 2 either way
                double base = (double) 1 / mGalaxy.getActivePlayers();
                double globalShips = mGalaxy.getGlobalShipsPlanet() + mGalaxy.getGlobalShipsFleet();
                modProd = 1 + Math.max(0, (double) playerWinning.planetsProd / mGalaxy.getGlobalProduction() - base);
                modShips = 1 + Math.max(0, (double) (playerWinning.shipsPlanetNow + playerWinning.shipsFleetNow) / globalShips - base);
                if (modProd > 0) {
                    value = value / (modProd * modShips);
                }
            }
            // Value against humans divided if hostility negative
            if ( (mPlayerAI.aiHostility < 0) && (playerOwner.local) ) {
                value = value / distanceModifier(Math.abs(mPlayerAI.aiHostility));
            }
            // Value against humans multiplied if hostility positive
            else if ( (mPlayerAI.aiHostility > 0) && (playerOwner.local) ) {
                value = value * distanceModifier(mPlayerAI.aiHostility);
            }
        }
        return value;
    }

    private double distanceModifier(double distance) {
        if (mPlayerAI.aiDefcon == 0) {
            return distance;
        } else {
            return Math.sqrt(distance);
        }
    }

    private boolean neighbourRush() {
        PlayerData playerNeighbour;
        int numPlayers = mGalaxy.getNumPlayers();

        // If there is some neighbour with less production, possible incoming rush attack from him
        if (mCurrentPlayer > 1) {
            playerNeighbour = mGalaxy.getPlayerData(mCurrentPlayer - 1);
        } else {
            playerNeighbour = mGalaxy.getPlayerData(numPlayers);
        }
        if (mPlayerAI.planetsProd >= playerNeighbour.planetsProd) {
            return true;
        }
        if (mCurrentPlayer < numPlayers) {
            playerNeighbour = mGalaxy.getPlayerData(mCurrentPlayer + 1);
        } else {
            playerNeighbour = mGalaxy.getPlayerData(1);
        }
        if (mPlayerAI.planetsProd >= playerNeighbour.planetsProd) {
            return true;
        }
        return false;
    }

    private void sumSingleFleet(PlanetData planetTo, int turn, FleetData fleet, int[] shipsSent) {
        PlanetData planetFleet = mGalaxy.getPlanetData(fleet.to);
        if ( (fleet.to == planetTo.index) && (fleet.at <= turn) ) {
            // fleets of "player" to attack this "planet" before "turn"
            shipsSent[0] += fleet.ships;
            // fleets of "player" to attack this "planet" in this "turn"
            if (fleet.at == turn) shipsSent[1] += fleet.ships;
        }
        if ( (planetTo.player == planetFleet.player) && (fleet.at == turn) ) {
            // fleets of "player" to attack the owner of "planet" in this "turn"
            shipsSent[2] += fleet.ships;
        }
    }

    private int[] sumFleets(PlanetData planetTo, int turns) {
        // Fleets from current AI "player" in their way to attack this "planet"
        int[] shipsSent = {0, 0, 0};
        int turn = turns + mCurrentTurn;    //turn of arrival

        for (FleetData fleet : aOwnFleets) {
            sumSingleFleet(planetTo, turn, fleet, shipsSent);
        }
        // Fleets planned to be sent the current turn by the current AI "player"
        for (FleetData fleet : aResultFleets) {
            sumSingleFleet(planetTo, turn, fleet, shipsSent);
        }
        return shipsSent;
    }

    private int sumShips(PlanetData planetTo, int turns) {
        int distance;
        int available, built;
        int shipsLand = 0;

        // Ships from planets of "player" available to attack this "planet" in these "turns"
        for (PlanetData planetFrom : mGalaxy.getPlanets()) if (planetFrom.player == mCurrentPlayer) {
            distance = mGalaxy.getDistance(planetFrom, planetTo);
            if (distance <= turns) {
                built = (turns - distance) * (planetFrom.production - planetFrom.upkeepNext);
                // not all built ships will be available to join the attack (proportional to defcon)
                available = planetFrom.shipsNow - planetFrom.aiSent - planetFrom.aiReserved + (built * (DEFCON_MAX - mPlayerAI.aiDefcon)) / DEFCON_MAX;
                if (available > 0) {
                    shipsLand += available;
                }
            }
        }
        return shipsLand;
    }

    private int estimateShips(PlanetData planetTo, int turns, int shipsSent) {
        int shipsPlanet = 0;
        int shipsBuilt = 0;
        double est1, est2, est3;

        PlayerData playerTo = mGalaxy.getPlayerData(planetTo.player);

        if (planetTo.player == 0) {
            // Max possible ships in neutral planet
            shipsPlanet = planetTo.shipsPublicNow;
        } else {
            if (mPlayerAI.aiDefcon == 0) {
                // Trigger rush attacks: considering that players send most ships out during exploration phase
                if (shipsSent == 0) {
                    shipsPlanet = mRandom.nextInt(2 * planetTo.production + 1);
                } else {
                    // Ships needed to conquer it this last turn (if no reinforcements)
                    shipsPlanet = planetTo.shipsPrev;
                }
            } else if (turns == 1) {
                shipsPlanet = planetTo.shipsPublicNow;
            } else {
                // Take into account Upkeep Rule
                if (mGalaxy.getRuleUpkeep() > 0) {
                    shipsBuilt = planetTo.production - planetTo.upkeepNext;
                } else {
                    shipsBuilt = planetTo.production;
                }
                // Ships estimated within 2 turns
                if (turns == 2) {
                    est1 = planetTo.shipsPublicNow + shipsBuilt;
                } else {
                    est1 = 0;
                }
                // if enemy ships distributed to each planet proportionally to production
                est2 = (double)(planetTo.production * playerTo.shipsPlanetNow) / Math.max(1, playerTo.planetsProd);
                // average number of ships per production in own planets (taking into account production of desired planet)
                est3 = (double)(planetTo.production * mPlayerAI.shipsPlanetNow) / (mPlayerAI.planetsProd + planetTo.production);
                shipsPlanet = (int) Math.max(Math.max(est1, est2), est3);
            }
            // Take into account Defense Rule
            shipsPlanet = (int)mGalaxy.multiplyDefense(shipsPlanet, planetTo);
        }
        // Subtract ships already sent (calculated for same distance)
        return 1 + shipsPlanet - shipsSent;
    }

    private int roll(PlanetData planetTo, int shipsAvailable, int shipsNeeded, int[] shipsSent, int distance, int d25, int d50, int d100) {
        int roll;
        boolean shipsArrived;

        // If target is neutral
        if (planetTo.player == 0) {
            if (shipsAvailable < shipsNeeded - planetTo.production) {
                roll = d50 + 25;
            } else if (shipsAvailable < shipsNeeded) {
                roll = d25 + 50;
            } else {
                roll = d25 + 75;
            }
            // Ships already arrived to this neutral planet, and is closer to current AI than to enemy
            shipsArrived = (planetTo.exploredNeutral()) && (distance < planetTo.aiBorder);
            if ( (shipsSent[0] > 0) || (shipsArrived) ) {
                if (shipsAvailable < shipsNeeded) {
                    roll = 75;
                } else {
                    roll = 100;
                }
            }
        } else {
            // If there are ships arriving same turn (wave)
            if (shipsSent[1] > 0) {
                if (shipsAvailable < shipsNeeded) {
                    roll = 75;
                } else {
                    roll = 100;
                }
            }
            // If there are ships attacking to same player in same turn
            else if (shipsSent[2] > 0) {
                if (shipsAvailable < shipsNeeded) {
                    roll = d50 + 25;
                } else {
                    roll = d50 + 50;
                }
            }
            // Rest of cases
            else {
                if (shipsAvailable < shipsNeeded) {
                    roll = d100 / 2;
                } else {
                    roll = d100;
                }
            }
        }
        return roll;
    }

    private boolean calculateAttack() {
        boolean attacking = false;
        int distance;
        int shipsAvailable, shipsNeeded;
        int[] shipsSent, shipsSentTest;
        int d25, d50, d100;
        double roll, value;

        mAttackBest.resetAttack();
        for (PlanetData planetTo : aEnemyPlanets) {
            // Possible targets
            if (planetTo.aiReserved != DONE) {
                // One random roll per target
                d25 = mRandom.nextInt(25);
                d50 = mRandom.nextInt(50);
                d100 = mRandom.nextInt(100);
                for (PlanetData planetFrom : aOwnPlanets) {
                    // Own planets ready to start an attack
                    if ((planetFrom.player == mCurrentPlayer) && (planetFrom.shipsNow - planetFrom.aiSent - planetFrom.aiReserved > 0)) {
                        distance = mGalaxy.getDistance(planetFrom, planetTo);
                        // Attack will arrive before last turn, and not duplicated (target and distance)
                        if (distance <= (mGalaxy.getMaxTurns() - mCurrentTurn) ) {
                            // TODO: avoid evaluating same distance several times
                            shipsAvailable = sumShips(planetTo, distance);
                            shipsSent = sumFleets(planetTo, distance);
                            shipsNeeded = estimateShips(planetTo, distance, shipsSent[0]);

                            // If ships sent previously are not enough
                            if (shipsNeeded > 0) {
                                roll = roll(planetTo, shipsAvailable, shipsNeeded, shipsSent, distance, d25, d50, d100);
                                value = roll * planetTo.aiValue;
                                // The count of ships is more important when only 2 players left
                                /*if (mGalaxy.getActivePlayers() == 2) {
                                    // this makes the ai more aggressive and reactive to enemy movements
                                    value = value / Math.sqrt(shipsNeeded);
                                }*/
                                // Divided by distance modifier (distance more important during first turns)
                                value = value / distanceModifier(distance);
                                if (value > mAttackBest.value) {
                                    mAttackBest.setAttack(planetFrom.index, planetTo.index, distance, shipsAvailable, shipsNeeded, (int)value);
                                    attacking = true;
                                }
                                if (DEBUG) {
                                    Log.d( DEBUG_TAG, " ATTACK"
                                    + ": target " + planetTo.sName
                                    + ", distance " + distance
                                    + ", shipsAvailable " + shipsAvailable
                                    + ", shipsNeeded " + shipsNeeded
                                    + ", value " + (int)value
                                    + ", roll " + (int)roll
                                    + ", shipsEstimated " + (shipsNeeded + shipsSent[0])
                                    + ", shipsSent " + shipsSent[0] + ", " + shipsSent[1] + ", " + shipsSent[2] );
                                }
                            }
                        }
                    }
                }
            }
        }
        return attacking; // false if a feasible attack was not found
    }

    private void performAttack() {
        int shipsNeeded, shipsAvailable, shipsBuilt, shipsSend;
        int distance, turns;
        PlanetData planetTo;

        planetTo = mGalaxy.getPlanetData(mAttackBest.target);
        turns = mAttackBest.distance;
        shipsNeeded = mAttackBest.shipsNeeded;
        if (DEBUG) {
            Log.d( DEBUG_TAG, "BEST ATTACK"
            + ": target " + planetTo.sName
            + ", distance " + mAttackBest.distance
            + ", shipsAvailable " + mAttackBest.shipsAvailable
            + ", shipsNeeded " + mAttackBest.shipsNeeded
            + ", value " + mAttackBest.value );
        }
        for (PlanetData planetFrom : aOwnPlanets) {
            // Continue until no more ships needed
            // Planets with lower index (outer planets) will send their ships first
            if ( (shipsNeeded > 0) && (planetFrom.player == mCurrentPlayer) ) {
                distance = mGalaxy.getDistance(planetFrom, planetTo);
                if (distance == turns) {
                    shipsAvailable = planetFrom.shipsNow - planetFrom.aiSent - planetFrom.aiReserved;
                    // Planets ready to send ships this turn
                    if (shipsAvailable > 0) {
                        if (shipsAvailable >= shipsNeeded) {
                            shipsSend = shipsNeeded;
                            planetTo.aiReserved = DONE;
                        } else {
                            shipsSend = shipsAvailable;
                        }
                        shipsNeeded -= shipsSend;
                        createFleet(planetFrom, planetTo, shipsSend);
                    }
                }
            }
        }
        for (PlanetData planetFrom : aOwnPlanets) {
            if ( (shipsNeeded > 0) && (planetFrom.player == mCurrentPlayer) ) {
                distance = mGalaxy.getDistance(planetFrom, planetTo);
                if (distance < turns) {
                    // Planets reserving their ships to join the attack in next turns (wave)
                    // Not saved, only needed to avoid sending them in other attacks this turn
                    shipsBuilt = (turns - distance) * (planetFrom.production - planetFrom.upkeepNext);
                    shipsAvailable = planetFrom.shipsNow - planetFrom.aiSent - planetFrom.aiReserved + shipsBuilt;
                    if (shipsAvailable > 0) {
                        if (shipsAvailable >= shipsNeeded) {
                            planetFrom.aiReserved += shipsNeeded;
                            shipsNeeded = 0;
                            planetTo.aiReserved = DONE;
                        } else {
                            planetFrom.aiReserved += shipsAvailable;
                            shipsNeeded -= shipsAvailable;
                        }
                    }
                }
            }
        }
    }

    private boolean calculateReinforce() {
        boolean reinforcing = false;
        int distance, shipsAvailable;
        int shipsDesiredFrom, shipsDesiredTo, shipsNeededFrom, shipsNeededTo;
        int[] shipsSent;
        double roll, value;
        double shipsAverageFrom = mShipsPlanetUpdated / mPlayerAI.planetsProd;
        double shipsAverageTo = mShipsTotalNow / mPlayerAI.planetsProd;

        mReinforceBest.resetAttack();
        for (PlanetData planetTo : aOwnPlanets) {
            shipsSent = sumFleets(planetTo, mGalaxy.getMaxDistance());
            // Max Desired ships proportional to production and total average of ships
            shipsDesiredTo = (int) Math.max(planetTo.aiThreat, shipsAverageTo * planetTo.production);
            shipsNeededTo = shipsDesiredTo - planetTo.shipsNow - planetTo.aiSent - shipsSent[0];
            // Required reinforcement if ships lower than current threat (or than desired ships) 
            if (shipsNeededTo > 0) {
                for (PlanetData planetFrom : aOwnPlanets) {
                    if (planetFrom.index != planetTo.index) {
                        // Min Desired ships proportional to production and average of ships in land
                        shipsDesiredFrom = (int) Math.max(planetFrom.aiThreat, shipsAverageFrom * planetFrom.production);
                        shipsNeededFrom = Math.max(1, shipsDesiredFrom - planetFrom.shipsNow - planetFrom.aiSent);
                        shipsAvailable = planetFrom.shipsNow - planetFrom.aiSent - planetFrom.aiReserved;
                        if (shipsAvailable > 0) {
                            distance = mGalaxy.getDistance(planetFrom, planetTo);
                            // Do not send reinforcements further than the closest enemy planet
                            if ((distance <= (mGalaxy.getMaxTurns() - mCurrentTurn)) && (distance <= planetFrom.aiBorder) ) {
                                roll = mRandom.nextInt(100);
                                value = (roll * planetTo.aiValue * shipsNeededTo) / (planetFrom.aiValue * shipsNeededFrom);
                                value = value / distance;

                                if (value > mReinforceBest.value) {
                                    mReinforceBest.setAttack(planetFrom.index, planetTo.index, distance, shipsAvailable, shipsNeededTo, (int)value);
                                    reinforcing = true;
                                }
                                if (DEBUG) {
                                    Log.d( DEBUG_TAG, " REINFORCEMENT"
                                    + ": source " + planetFrom.sName
                                    + ", target " + planetTo.sName
                                    + ", distance " + distance
                                    + ", shipsAvailable " + shipsAvailable
                                    + ", shipsNeeded " + shipsNeededTo
                                    + ", value " + (int)value
                                    + ", roll " + (int)roll );
                                }
                            }
                        }
                    }
                }
            }
        }
        return reinforcing;
    }

    private void performReinforce() {
        PlanetData bestSource, bestTarget;
        int bestShips;

        bestSource = mGalaxy.getPlanetData(mReinforceBest.source);
        bestTarget = mGalaxy.getPlanetData(mReinforceBest.target);
        bestShips = Math.min(mReinforceBest.shipsAvailable, mReinforceBest.shipsNeeded);
        createFleet(bestSource, bestTarget, bestShips);
        if (DEBUG) {
            Log.d( DEBUG_TAG, "BEST REINFORCEMENT"
            + ": source " + bestSource.sName
            + ", target " + bestTarget.sName
            + ", distance " + mReinforceBest.distance
            + ", shipsAvailable " + mReinforceBest.shipsAvailable
            + ", shipsNeeded " + mReinforceBest.shipsNeeded
            + ", value " + mReinforceBest.value );
        }
    }

    // Reserve ships, and add fleet to local array, that will be returned as result when ai task finishes
    // (to prevent concurrent access to mGalaxy fleets)
    private void createFleet(PlanetData planetFrom, PlanetData planetTo, int sentShips) {
        if ( (sentShips > 0) && (planetFrom.shipsNow - planetFrom.aiSent >= sentShips) && (planetFrom.index != planetTo.index) ) {
            int turnArrival = mCurrentTurn + mGalaxy.getDistance(planetFrom, planetTo);
            FleetData fleet = new FleetData(mCurrentTurn, planetFrom.player, planetFrom.index, planetTo.index, turnArrival, sentShips);
            planetFrom.aiSent += sentShips;
            mShipsPlanetUpdated -= sentShips;
            aResultFleets.add(fleet);
        }
    }
}
