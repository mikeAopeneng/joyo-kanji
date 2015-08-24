/***************************************************************************************
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package website.openeng.anki;

import java.util.HashMap;
import java.util.Set;

public class KanjiDatabaseManager {

    private static HashMap<String, KanjiDb> sKanjiDatabases = new HashMap<String, KanjiDb>();


    /* Prevent class from being instantiated */
    private KanjiDatabaseManager() {
    }


    /**
     * Get a reference over an Anki database, creating the connection if needed.
     * 
     * @param pathDB the path to the database.
     * @return the Anki database.
     */
    public static KanjiDb getDatabase(String pathDB) {

        // If the DB is already opened
        if (sKanjiDatabases.containsKey(pathDB)) {
            return sKanjiDatabases.get(pathDB);
        }

        // If a connection to the desired DB does not exist, we create it
        KanjiDb ankiDB = new KanjiDb(pathDB);

        // Insert the new DB to the map of opened DBs
        sKanjiDatabases.put(pathDB, ankiDB);

        return ankiDB;
    }


    /**
     * Close connection to a given database.
     * 
     * @param pathDB the path to the database to close.
     */
    public static void closeDatabase(String pathDB) {
        KanjiDb ankiDB = sKanjiDatabases.remove(pathDB);
        if (ankiDB != null) {
            ankiDB.closeDatabase();
        }
    }


    /**
     * Close connections to all opened databases. XXX Currently unused.
     */
    public static void closeAllDatabases() {
        Set<String> databases = sKanjiDatabases.keySet();
        for (String pathDB : databases) {
            KanjiDatabaseManager.closeDatabase(pathDB);
        }
    }


    /**
     * Check if there is a valid connection to the given database.
     * 
     * @param pathDB the path to the database we want to check.
     * @return True if the database is already opened, false otherwise.
     */
    public static boolean isDatabaseOpen(String pathDB) {
        return sKanjiDatabases.containsKey(pathDB);
    }
}
