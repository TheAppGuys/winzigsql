/**
 *
 */
package de.theappguys.winzigsql;

import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Extension of SQLiteOpenHelper that handles initialization & upgrade of
 * the database based on naming conventions for resources in the asset files.
 *
 * Works hand-in-hand with the WinzigDbProvider
 */
public class WinzigDbHelper extends SQLiteOpenHelper {

    public static final String LOG_TAG = "winzigsql";

	private final Context context;

	private final String dbName;

	/**
	 * Let your custom db provider extends this class and call the superconstructor like so:
	 * <pre><code>
	 * private MyDbHelper(final Context context) {
	 *     super (context, "myDbName", 42);
	 * }
	 * </code></pre>
	 *
	 * Then, to obtain instances, implement a get Instance method like this:
	 * <pre><code>
	 * private static MyDbHelper instance = null;
	 *
	 * public static MyDbHelper(final Context context) {
	 *     if (instance == null) {
	 *         return (instance = new MyDbHelper(context))
	 *     } else {
	 *         return instance;
	 *     }
	 * }
	 * </code></pre>
	 *
	 */
	public WinzigDbHelper(final Context context, final String dbName, final int dbVersion) {
		super(context, dbName, null, dbVersion);
		//TODO: use constructor with error handler with last argument
		//final DatabaseErrorHandler errorHandler
		this.dbName = dbName;
		this.context = context;
	}

	/** (non-Javadoc)
	 * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
	 */
	@Override
	public void onCreate(final SQLiteDatabase db) {
		try {
			//load create statements from asset
			final String sqlStatements =
			    ResourceUtils.readRawResourceAsString(
				    context,
				    context.getResources().getIdentifier(
				            "create_db",
				            "raw",
				            context.getApplicationContext().getPackageName()));

			//write the db using the read script
	        SqlUtils.executeStatements(db, sqlStatements);
	        Log.d(LOG_TAG, "db created");
		} catch (final IOException ioe) {
			//TODO: we had a class not found exception on the emulator here.
			//force the exception on various real devices and emulators to see
			//what happened
			throw new SQLiteException("Could not initialize database."
			                          + ioe.getMessage());
		}
	}

	/** (non-Javadoc)
	 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
	 */
	@Override
	public void onUpgrade(final SQLiteDatabase db,
			              final int oldVersion,
			              final int newVersion) {
		for (int n = oldVersion; n < newVersion; n++) {
		    final String updateResourceName =
		            "upgrade_db_" + n ;
			try {
				final int resourceId = context.getResources().getIdentifier(
                        updateResourceName,
                        "raw",
                        context.getApplicationContext().getPackageName());

				//id == 0 means there is no resource with the given name
				//we now hope the user knew what he was doing and no script
				//is necessary for the current db version id...
				if (resourceId == 0) {
				    Log.d(LOG_TAG,
				            "skipping update for version " + n +
				            ", resource not found: " + updateResourceName);
				    continue;
				}

				final String sqlStatements =
		                ResourceUtils.readRawResourceAsString(
		                    context, resourceId);
				SqlUtils.executeStatements(db, sqlStatements);
				Log.d(LOG_TAG, "db updated to version " + n);
			} catch (final IOException ioe) {
				throw new SQLiteException(
				        "Could not update database to " +
						"version " + n + ", problem with resource " +
				        updateResourceName + "? " +
				        ioe.getMessage());
			}
		}
	}

	/**
	 * We override onOpen to enable foreign keys.
	 */
	@Override
	public void onOpen(final SQLiteDatabase db) {
	    super.onOpen(db);
	    final Cursor c = db.rawQuery("PRAGMA foreign_keys;", new String[]{});
	    boolean foreignKeysDisabled = true;
	    try {
	    	//if no row is returned, the db was compiled with foreign key
	    	//support disabled. Otherwise we get 1 for enabled foreign keys,
	    	//and 0 for disabled foreign keys.
		    if (c.isAfterLast()) {
		    	throw new SQLiteException("Db has no foreign key support.");
		    }
		    c.moveToNext();
		    foreignKeysDisabled = c.getInt(0) == 0;
	    } finally {
	    	c.close();
	    }
	    if (foreignKeysDisabled) {
		    if (!db.isReadOnly()) {
		        // Enable foreign key constraints
		        db.execSQL("PRAGMA foreign_keys=ON;");
		    } else {
		    	throw new SQLiteException(
		    	    "Cannot activate foreign key support, db is read-only.");
		    }
	    }
	}

	/**
	 * Only for debugging purposes.
	 * @return true: db was deleted.
	 */
	public boolean dropDb() {
		return context.deleteDatabase(dbName);
	}
}
