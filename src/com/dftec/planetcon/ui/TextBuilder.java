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
package com.dftec.planetcon.ui;

import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.text.Spanned;
import com.dftec.planetcon.R;
import com.dftec.planetcon.activities.SettingsActivity;
import com.dftec.planetcon.data.FleetData;
import com.dftec.planetcon.data.GalaxyData;
import com.dftec.planetcon.data.PlanetData;
import com.dftec.planetcon.data.PlayerData;
import java.util.ArrayList;

public class TextBuilder {
    private final Resources mRes;
    private final StringBuilder mBuilderItem;
    private final ArrayList<CharSequence> aInfoThreats;
    private final ArrayList<CharSequence> aInfoStats;
    private final ArrayList<CharSequence> aInfoFleets;
    private final ArrayList<CharSequence> aInfoArrivals;
    private GalaxyData mGalaxy;

    public TextBuilder(Context context) {
        mRes = context.getResources();
        mBuilderItem = new StringBuilder();
        aInfoThreats = new ArrayList<CharSequence>();
        aInfoStats = new ArrayList<CharSequence>();
        aInfoFleets = new ArrayList<CharSequence>();
        aInfoArrivals = new ArrayList<CharSequence>();
   }

    public void setGalaxy(GalaxyData galaxyGame) {
        mGalaxy = galaxyGame;
    }

    public ArrayList<CharSequence> getInfoThreats() {
        return aInfoThreats;
    }

    public ArrayList<CharSequence> getInfoStats() {
        return aInfoStats;
    }

    public ArrayList<CharSequence> getInfoFleets() {
        return aInfoFleets;
    }

    public ArrayList<CharSequence> getInfoArrivals() {
        return aInfoArrivals;
    }

    public void updateInfoGame(boolean threats, boolean stats, boolean fleets, boolean arrivals) {
        if (threats) {
            aInfoThreats.clear();
            for (PlanetData planet : mGalaxy.getPlanets()) if (planet.index != 0) {
                aInfoThreats.add(getItemThreats(planet));
            }
        }
        if (stats) {
            aInfoStats.clear();
            for (PlayerData playerStats : mGalaxy.getPlayers()) if (playerStats.index != 0) {
                aInfoStats.add(getItemStats(playerStats));
            }
        }
        if (fleets) {
            aInfoFleets.clear();
            for (FleetData fleet : mGalaxy.getFleets()) {
                if (fleet.player == mGalaxy.getCurrentPlayer()) {
                    aInfoFleets.add(getItemFleets(fleet));
                }
            }
        }
        if (arrivals) {
            aInfoArrivals.clear();
            for (FleetData arrival : mGalaxy.getArrivals()) if (arrival.wave != 0) {
                aInfoArrivals.add(getItemArrivals(arrival));
            }
        }
    }

    public void appendColoredString(StringBuilder html, int htmlColor, Object text) {
        html.append( "<font color=\"" );
        //html.append( String.format("#%06X", (0xFFFFFF & htmlColor)) );
        html.append( htmlColor );
        html.append( "\">" );
        html.append( text );
        html.append( "</font>" );
    }
    
    public void appendUnbreakablePadding(StringBuilder html, int maxPad, String value) {
        //padding of unbreakable spaces
        int pad = Math.max(0, maxPad - value.length());
        for (int p=1; p<=pad; p++) {
            html.append( "&nbsp;" );
        }
    }

    public CharSequence getInfoShips(PlanetData planetFrom, PlanetData planetTo, int turn) {
        // Same than mBuilderItem.delete(0, mBuilderItem.length());
        mBuilderItem.setLength(0);

        mBuilderItem.append( mRes.getString(R.string.text_from) ).append( "&nbsp;" );
        int htmlColor = (mGalaxy.getPlayerData(planetFrom.player).colorText);
        appendColoredString(mBuilderItem, htmlColor, planetFrom.sName);

        mBuilderItem.append( "&nbsp;" ).append( mRes.getString(R.string.text_to).toLowerCase() ).append( "&nbsp;" );
        htmlColor = (mGalaxy.getPlayerData(planetTo.player).colorText);
        appendColoredString(mBuilderItem, htmlColor, planetTo.sName);

        mBuilderItem.append( "&nbsp;" ).append( mRes.getString(R.string.text_at).toLowerCase() );
        mBuilderItem.append( "&nbsp;" ).append( mRes.getString(R.string.text_turn).toLowerCase() );
        mBuilderItem.append( "&nbsp;" ).append( turn );

        return fromHtml(mBuilderItem.toString());
    }

    public CharSequence getInfoTurn() {
        String value;
        mBuilderItem.setLength(0);
        mBuilderItem.append( mRes.getString(R.string.info_turn).toUpperCase() );

        mBuilderItem.append( "&nbsp;" ).append( mGalaxy.getCurrentTurn() );
        mBuilderItem.append( "&nbsp;" ).append( mRes.getString(R.string.text_of).toLowerCase() );
        mBuilderItem.append( "&nbsp;" ).append( mGalaxy.getMaxTurns() ).append( ", " );
        value = mGalaxy.getCurrentPlayerData().sName;
        mBuilderItem.append( value );
        appendUnbreakablePadding(mBuilderItem, 3, value);

        return fromHtml(mBuilderItem.toString());
    }

    public CharSequence getInfoPlanet(PlanetData focusedPlanet) {
        mBuilderItem.setLength(0);
        mBuilderItem.append( mRes.getString(R.string.info_planet).toUpperCase() );
        int effectiveShips;

        // Info selected planet
        if (focusedPlanet != null) {
            PlayerData owner = mGalaxy.getPlayerData(focusedPlanet.player);
            int htmlColor = (owner.colorText);

            mBuilderItem.append( "&nbsp;" );
            appendColoredString(mBuilderItem, htmlColor, focusedPlanet.sName);
            if (owner.index != 0) {
                String openQuote, closeQuote;
                switch (owner.getAILevel()) {
                // Number of quotes equal to ai level (hostility)
                case 1:
                    openQuote = "&lsaquo;";
                    closeQuote = "&rsaquo;";
                    break;
                case 2:
                    openQuote = "&laquo;";
                    closeQuote = "&raquo;";
                    break;
                case 3:
                    openQuote = "&lsaquo;&laquo;";
                    closeQuote = "&raquo;&rsaquo;";
                    break;
                case 4:
                    openQuote = "&laquo;&laquo;";
                    closeQuote = "&raquo;&raquo;";
                    break;
                case 5:
                    openQuote = "&lsaquo;&laquo;&laquo;";
                    closeQuote = "&raquo;&raquo;&rsaquo;";
                    break;
                case 6:
                    openQuote = "&laquo;&laquo;&laquo;";
                    closeQuote = "&raquo;&raquo;&raquo;";
                    break;
                default:
                    // Human player
                    openQuote = "(";
                    closeQuote = ")";
                    break;
                }

                mBuilderItem.append( " " ).append( openQuote );
                appendColoredString(mBuilderItem, htmlColor, owner.sName);
                mBuilderItem.append( closeQuote );
                mBuilderItem.append( "&nbsp;" );
            }
            mBuilderItem.append( "<br>" );
            mBuilderItem.append( mRes.getString(R.string.info_production) );
            mBuilderItem.append( "&nbsp;" );
            appendColoredString(mBuilderItem, htmlColor, focusedPlanet.production);
            if ( (owner.index != 0) && (mGalaxy.getRuleUpkeep() > 0) ) {
                mBuilderItem.append( "&nbsp;" ).append( "-" ).append( "&nbsp;" );
                appendColoredString(mBuilderItem, htmlColor, focusedPlanet.upkeepNext);
            }

            mBuilderItem.append( "<br>" );
            mBuilderItem.append( mRes.getString(R.string.info_ships) );
            mBuilderItem.append( "&nbsp;" );

            if (owner.index == 0) {
                int value = Math.max(0, focusedPlanet.shipsPublicNow - focusedPlanet.production);
                appendColoredString(mBuilderItem, htmlColor, value);
                mBuilderItem.append( "&nbsp;" ).append( mRes.getString(R.string.text_to).toLowerCase() ).append( "&nbsp;" );
                appendColoredString(mBuilderItem, htmlColor, focusedPlanet.shipsPublicNow);
                mBuilderItem.append( "&nbsp;" ).append( "?" );
            } else {
                if (mGalaxy.getCurrentPlayer() == focusedPlanet.player) {
                    appendColoredString(mBuilderItem, htmlColor, focusedPlanet.shipsNow);
                } else {
                    appendColoredString(mBuilderItem, htmlColor, focusedPlanet.shipsPublicNow);
                }
                if (mGalaxy.getRuleDefense() != SettingsActivity.DEFENSE_NONE) {
                    mBuilderItem.append( "&nbsp;" ).append( "&times;" ).append( "&nbsp;" );
                    appendColoredString(mBuilderItem, htmlColor, focusedPlanet.sDefense);

                    mBuilderItem.append( " " ).append( "(" );
                    if (mGalaxy.getCurrentPlayer() == focusedPlanet.player) {
                        effectiveShips = ( (focusedPlanet.defense * focusedPlanet.shipsNow) / PlanetData.MIN_DEF);
                        appendColoredString(mBuilderItem, htmlColor, effectiveShips);
                    } else {
                        effectiveShips = ( (focusedPlanet.defense * focusedPlanet.shipsPublicNow) / PlanetData.MIN_DEF);
                        appendColoredString(mBuilderItem, htmlColor, effectiveShips);
                    }
                    mBuilderItem.append( ")" );
                }

            }

        } else {
            mBuilderItem.append( "<br>" ).append( mRes.getString(R.string.info_production) );
            mBuilderItem.append( "<br>" ).append( mRes.getString(R.string.info_ships) );
        }
        return fromHtml(mBuilderItem.toString());
    }

    public CharSequence getItemStatsHeader() {
        String value;

        mBuilderItem.setLength(0);
        value = mRes.getString(R.string.stats_name);
        mBuilderItem.append( value ).append( "<br>" );
        value = mRes.getString(R.string.stats_production);
        mBuilderItem.append( value ).append( "<br>" );
        value = mRes.getString(R.string.stats_ships);
        mBuilderItem.append( value ).append( "<br>" );

        value = mRes.getString(R.string.stats_inplanet);
        mBuilderItem.append( value ).append( "<br>" );
        value = mRes.getString(R.string.stats_planets);
        mBuilderItem.append( value ).append( "<br>" );

        value = mRes.getString(R.string.stats_infleet);
        mBuilderItem.append( value ).append( "<br>" );
        value = mRes.getString(R.string.stats_fleets);
        mBuilderItem.append( value ).append( "<br>" );

        value = mRes.getString(R.string.stats_produced);
        mBuilderItem.append( value ).append( "<br>" );
        value = mRes.getString(R.string.stats_lost);
        mBuilderItem.append( value ).append( "<br>" );
        value = mRes.getString(R.string.stats_arrived);
        mBuilderItem.append( value ).append( "<br>" ).append( "<br>" );

        return fromHtml(mBuilderItem.toString());
    }

    private CharSequence getItemStats(PlayerData playerStats) {
        String value;
        int htmlColor = (playerStats.colorText);

        mBuilderItem.setLength(0);
        mBuilderItem.append( "<font color=\"" );
        mBuilderItem.append( htmlColor );
        mBuilderItem.append( "\">" );

        value = playerStats.sName;
        mBuilderItem.append( value );
        mBuilderItem.append( "<br>" );
        value = String.valueOf(playerStats.planetsProd);
        mBuilderItem.append( value ).append( "<br>" );
        value = String.valueOf(playerStats.shipsPlanetNow + playerStats.shipsFleetNow);
        mBuilderItem.append( value ).append( "<br>" );

        value = String.valueOf(playerStats.shipsPlanetNow);
        mBuilderItem.append( value ).append( "<br>" );
        value = String.valueOf(playerStats.planetsNum);
        mBuilderItem.append( value ).append( "<br>" );

        value = String.valueOf(playerStats.shipsFleetNow);
        mBuilderItem.append( value ).append( "<br>" );
        value = String.valueOf(playerStats.fleetsNow);
        mBuilderItem.append( value ).append( "<br>" );

        value = String.valueOf(playerStats.shipsProduced);
        mBuilderItem.append( value ).append( "<br>" );
        value = String.valueOf(playerStats.shipsProduced - playerStats.shipsPlanetNow - playerStats.shipsFleetNow);
        mBuilderItem.append( value ).append( "<br>" );
        value = String.valueOf(playerStats.shipsArrived);
        mBuilderItem.append( value ).append( "<br>" );

        // min 4 chars wide
        mBuilderItem.append( "&nbsp;" ).append( "&nbsp;" ).append( "&nbsp;" ).append( "&nbsp;" ).append( "<br>" );

        mBuilderItem.append( "</font>" );
        return fromHtml(mBuilderItem.toString());
    }

    private CharSequence getItemThreats(PlanetData planet) {
        int htmlColor;
        String value;

        //Info ships per planet
        mBuilderItem.setLength(0);
        PlayerData owner = mGalaxy.getPlayerData(planet.player);
        PlayerData ownerPrev = mGalaxy.getPlayerData(planet.playerPrev);

        //htmlColor = (owner.colorText);
        mBuilderItem.append( "(" );
        value = planet.sName;
        appendUnbreakablePadding(mBuilderItem, 2, value);
        //appendColoredString(mBuilderItem, htmlColor, value);
        mBuilderItem.append( value ).append( ")" ).append( "&nbsp;" );

        if (ownerPrev.index != 0) {
            value = String.valueOf(planet.shipsPrev);
        } else {
            value = String.valueOf(2 * planet.production);
        }
        htmlColor = (ownerPrev.colorText);
        appendUnbreakablePadding(mBuilderItem, 3, value);
        appendColoredString(mBuilderItem, htmlColor, value);

        mBuilderItem.append( "&nbsp;" ).append( "&rarr;" ).append( "&nbsp;" );  //> (&rsaquo;)

        if (owner.index != 0) {
            value = String.valueOf(planet.shipsPublicNow);
        } else if (planet.exploredNeutral()) {
            value = String.valueOf(planet.shipsPublicNow);
        } else {
            value = "";
        }
        htmlColor = (owner.colorText);
        appendColoredString(mBuilderItem, htmlColor, value);
        appendUnbreakablePadding(mBuilderItem, 3, value);

        if (mGalaxy.getRuleDefense() != SettingsActivity.DEFENSE_NONE) {
            if (owner.index != 0) {
                mBuilderItem.append( "&nbsp;" ).append( "&times;" );    //x
                value = planet.sDefense;
            } else {
                mBuilderItem.append( "&nbsp;" ).append( "?" );    //?
                value = planet.sDefense;
            }
            appendColoredString(mBuilderItem, htmlColor, value);
            appendUnbreakablePadding(mBuilderItem, 3, value);
        }
        mBuilderItem.append( "&nbsp;" );
//        mBuilderItem.append( "<br>" );
        return fromHtml(mBuilderItem.toString());
    }

    private CharSequence getItemArrivals(FleetData fleet) {
        int htmlColor;
        PlanetData planetTo = mGalaxy.getPlanetData(fleet.to);
        PlayerData playerFleet = mGalaxy.getPlayerData(fleet.player);
        PlayerData ownerNow = mGalaxy.getPlayerData(planetTo.player);
        PlayerData ownerPrev = mGalaxy.getPlayerData(planetTo.playerPrev);
        String ships, mark, action;

        // Previous Planet owner same than incoming ships (reinforcement)
        if (fleet.player == planetTo.playerPrev) {
            action = mRes.getString(R.string.text_reinforce);
        }
        // Planet keeps same owner (no conquest: defeat)
        else if (planetTo.player == planetTo.playerPrev) {
            action = mRes.getString(R.string.text_attack);
        }
        // Current Planet owner same than incoming ships (conquest)
        else if (fleet.player == planetTo.player) {
            action = mRes.getString(R.string.text_conquer);
        }
        // (conquest by other: defeat)
        else {
            action = mRes.getString(R.string.text_combat);
        }

        //Values: (fleet.ships) action (planetTo.sName). Result (planetTo.shipsNow - planetTo.upkeep)
        //Colors: (fleet.player) action (planetTo.playerPrev). Result (planetTo.player)
        mBuilderItem.setLength(0);

        mBuilderItem.append( "(" );
        appendUnbreakablePadding(mBuilderItem, 2, planetTo.sName);
        //appendColoredString(mBuilderItem, htmlColor, planetTo.sName);
        mBuilderItem.append( planetTo.sName ).append( ")" ).append( "&nbsp;" );

        htmlColor = (playerFleet.colorText);
        appendUnbreakablePadding(mBuilderItem, 3, String.valueOf(fleet.wave));
        appendColoredString(mBuilderItem, htmlColor, fleet.wave);
        mBuilderItem.append( "&nbsp;" ).append( mRes.getString(R.string.text_vs).toLowerCase() ).append( "&nbsp;" );

        htmlColor = (ownerPrev.colorText);
        if (ownerPrev.index != 0) {
            ships = String.valueOf(planetTo.shipsPrev);
            mark = "";
        } else {
            ships = String.valueOf(planetTo.shipsPublicPrev);
            mark = "?";
        }
        appendUnbreakablePadding(mBuilderItem, 3, ships + mark);
        appendColoredString(mBuilderItem, htmlColor, ships);
        mBuilderItem.append( mark );
        mBuilderItem.append( "&nbsp;" ).append( "&rarr;" ).append( "&nbsp;" );  //->

        htmlColor = (ownerNow.colorText);
        if (ownerNow.index != 0) {
            ships = String.valueOf(planetTo.shipsPublicNow - (planetTo.production - planetTo.upkeep));
            mark = "";
        } else {
            ships = String.valueOf(planetTo.shipsPublicNow);
            mark = "?";
        }
        appendColoredString(mBuilderItem, htmlColor, ships);
        mBuilderItem.append( mark );
        appendUnbreakablePadding(mBuilderItem, 3, ships + mark);

        mBuilderItem.append( "&nbsp;" ).append( action );
        appendUnbreakablePadding(mBuilderItem, 10, String.valueOf(action));
//        mBuilderItem.append( "<br>" );

        return fromHtml(mBuilderItem.toString());
    }

    private CharSequence getItemFleets(FleetData fleet) {
        int htmlColor;
        PlanetData planetFrom = mGalaxy.getPlanetData(fleet.from);
        PlanetData planetTo = mGalaxy.getPlanetData(fleet.to);

        mBuilderItem.setLength(0);

        mBuilderItem.append( mRes.getString(R.string.text_turn) ).append( "&nbsp;" );
        mBuilderItem.append( fleet.at ).append( ":" ).append( "&nbsp;" );
        htmlColor = (mGalaxy.getPlayerData(fleet.player).colorText);
        appendColoredString(mBuilderItem, htmlColor, fleet.ships);

        mBuilderItem.append( "&nbsp;" ).append( mRes.getString(R.string.text_to_2).toLowerCase() ).append( "&nbsp;" );
        htmlColor = (mGalaxy.getPlayerData(planetTo.player).colorText);
        appendColoredString(mBuilderItem, htmlColor, planetTo.sName);

        mBuilderItem.append( "&nbsp;" ).append( mRes.getString(R.string.text_from_2).toLowerCase() ).append( "&nbsp;" );
        htmlColor = (mGalaxy.getPlayerData(planetFrom.player).colorText);
        appendColoredString(mBuilderItem, htmlColor, planetFrom.sName);

        // Padding at the end to keep same column length when measured
        appendUnbreakablePadding(mBuilderItem, 2, String.valueOf(fleet.at));
        appendUnbreakablePadding(mBuilderItem, 3, String.valueOf(fleet.ships));
        appendUnbreakablePadding(mBuilderItem, 2, String.valueOf(planetFrom.sName));
        appendUnbreakablePadding(mBuilderItem, 2, String.valueOf(planetTo.sName));
//        mBuilderItem.append( "<br>" );

        return fromHtml(mBuilderItem.toString());
    }

//    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html){
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= 24) {
           result = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
           result = Html.fromHtml(html);
        }
        return result;
    }

}
