package de.theappguys.winzigsql;

import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * Class that generically wraps any SQLiteOpenHelper into a provider.
 *
 * uris are assumed to be of the form:
 * content://[authority]/[table]
 *
 * As this provider allows direct, raw access to the underlying sqlite it is
 * *not* a good idea to make it available to other apps publicly!
 */
public abstract class WinzigDbProvider extends ContentProvider {

	private final String authority;
	//set when onCreate is called
	private SQLiteOpenHelper db;
	private final Uri baseUri;

	private class UriParseResult {
		final String tables;
		final Long   id;

		public UriParseResult(final Uri uri) {
			//uri formats:
			//content://[authority]  for general queries
			//content://[authority]/[tables] for table specific queries
			//content://[authority]/[table]/[id] for entity specific queries

			//check if authority matches
			if (!authority.equals(uri.getEncodedAuthority())) {
				throw new IllegalArgumentException("Expected authority: '" +
			        authority + "' got '" + uri.getEncodedAuthority() + "'.");
			}
			//parse path
			final List<String> pathSegments = uri.getPathSegments();
			//we need at least the database
			/*if (pathSegments == null || pathSegments.isEmpty()) {
				throw new IllegalArgumentException(
						"No db part int uri: '" + uri.toString() + "'.");
			}*/
			//we can only have up to three path arguments ([db_name]/[table]/[id])
			if (pathSegments.size() > 2) {
				throw new IllegalArgumentException("Uri path to long: '" + uri);
			}

			//little bobby tables, we call him.
			tables = !pathSegments.isEmpty() ? pathSegments.get(0) : "";

			//we have an id elements, make sure it is a valid long
			if (pathSegments.size() == 2) {
				try {
					id = Long.parseLong(pathSegments.get(1));
				} catch (final Exception e) {
					throw new IllegalArgumentException(
							"Not a valid id: " + pathSegments.get(1), e);
				}
			} else {
				id = null;
			}
		}
	}

	public static Uri createBaseUri(final String authority) {
	    return UriUtils.contentUri(authority);
	}

	/**
	 * @param authority the first part of the content url of this provider,
	 * should be the namespace of the app
	 */
	public WinzigDbProvider(final String authority) {
		super();
		this.authority = authority;
		this.baseUri = createBaseUri(authority);
	}

	protected abstract SQLiteOpenHelper createDb(final Context context);

	public Uri getBaseUriForDatabase() {
		return baseUri;
	}

	public Uri getBaseUriForTable(final String table) {
		return Uri.withAppendedPath(baseUri, table);
	}

	@Override
	public boolean onCreate() {
		db = createDb(getContext());
		return true;
	}

	@Override
	public Cursor query(final Uri uri, final String[] projection,
			            final String selection, final String[] selectionArgs,
			            final String sortOrder) {
		final UriParseResult parsedUri = new UriParseResult(uri);
		//our resulting cursor
		final Cursor cursor;
		//check if we have a raw query
		if ("".equals(parsedUri.tables)) {
			//if tables aren't specified, we just execute a raw select.
			cursor = db.getReadableDatabase().rawQuery(selection, selectionArgs);
		} else if (parsedUri.id != null) {//query for a single entity
			final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
			queryBuilder.setTables(parsedUri.tables);
			cursor = queryBuilder.query(
					   db.getReadableDatabase(),
			           projection,
			           "_id = " + parsedUri.id,
			           selectionArgs, null, null, sortOrder);
	    } else {
			// Assemble query based on url
			final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
			// Set the table(s)
			queryBuilder.setTables(parsedUri.tables);
			cursor = queryBuilder.query(
					           db.getReadableDatabase(),
					           projection,
					           selection,
					           selectionArgs, null, null, sortOrder);
		}

	    // Make sure that potential listeners are getting notified
	    cursor.setNotificationUri(getContext().getContentResolver(), uri);
	    return cursor;
	}

	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		final UriParseResult parsedUri = new UriParseResult(uri);

		if ("".equals(parsedUri.tables)) {
			throw new IllegalArgumentException(
			    "Cannot insert, no table provided: '" + uri.toString() + "'");
		}

		if (parsedUri.id != null) {
			throw new IllegalArgumentException(
			    "Cannot insert, insert uri contains id: '" + uri.toString() + "'");
		}

		final long id = db.getWritableDatabase().insert(parsedUri.tables, null, values);

		getContext().getContentResolver().notifyChange(uri, null);

		return Uri.withAppendedPath(uri, Long.toString(id));
	}

	@Override
	public int delete(final Uri uri, final String selection,
			          final String[] selectionArgs) {
		final UriParseResult parsedUri = new UriParseResult(uri);

		if ("".equals(parsedUri.tables)) {
			throw new IllegalArgumentException(
			    "Cannot delete, no table provided: '" + uri.toString() + "'");
		}

		final int count;
		if (parsedUri.id != null) {
			count = db.getWritableDatabase().delete(
					parsedUri.tables,
					"_id = ?",
					new String[]{Long.toString(parsedUri.id)});
		} else {
			count = db.getWritableDatabase().delete(
					parsedUri.tables,
					selection,
					selectionArgs);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}

	@Override
	public int update(final Uri uri, final ContentValues values,
			          final String selection, final String[] selectionArgs) {
		final UriParseResult parsedUri = new UriParseResult(uri);

		if ("".equals(parsedUri.tables)) {
			throw new IllegalArgumentException(
			    "Cannot update, no table provided: '" + uri.toString() + "'");
		}

		final int count;
		//we have an update of a single entity
		if (parsedUri.id != null) {
			count = db.getWritableDatabase().update(
					parsedUri.tables,
					values,
					"_id = ?",
					new String[]{Long.toString(parsedUri.id)});
	    } else {//generic update
	    	count = db.getWritableDatabase().update(
					parsedUri.tables,
					values,
					selection,
					selectionArgs);
		}

		getContext().getContentResolver().notifyChange(uri, null);

	    return count;
	}

	@Override
	public String getType(final Uri uri) {
		//null signals that there is no appropriate mime type
		//(which makes sense for generic db query results)
		return null;
	}
}
