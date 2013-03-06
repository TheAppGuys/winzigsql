package de.theappguys.winzigsql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Base class for simple domain objects that allow CRUD operations via a
 * Content Provider. Cruddable provides a number of non-static inner classes
 * that allow persisting a number of different basic types.
 *
 * Contains a default <pre><code>equals</code></pre> and
 * <pre><code>hashCode</code></pre> implementation that will work correctly
 * if you only add members that are derived of <pre><code>CrudValue<code><pre>.
 *
 * Note that classes deriving from this class are mutable. This is done on
 * purpose for efficiency reasons (we run on Android, after all). THIS MAKES IT
 * DANGEROUS TO USE THIS CLASS AS KEYS IN MAPS AND VALUES IN SETS. If you do so,
 * make sure you understand the implications of using mutable classes in these
 * instances. If you do not know what I am talking about: Just dont't use
 * subclasses of this class in sets or as keys for maps.
 *
 * To create you own Cruddable instances, do the following:
 *
 * <pre><code>
 * public class MyCruddable extends Cruddable {
 *
 *   public final CrudLong   columnname        = new CrudLong("columnname");
 *   public final CrudString anothercolumnname = new CrudString("anothercolumnname");
 *
 *   public Chart() {
 *       //pass the number of members here for great justice
 *       super("mytablename", 2);
 *   }
 * }
 * </pre><code>
 *
 * There are CrudValue classes for all primitive types in a nullable and
 * non-nullable version.
 *
 */
public abstract class Cruddable implements Serializable {

	/////////////////////// value mapping base classes ///////////////////////
	public abstract class CrudValue<T> implements Serializable {

		protected final int index;

		protected final  String dbFieldName;

		public CrudValue(final String dbFieldName) {
			this.dbFieldName = dbFieldName;
			index = Cruddable.this.values.size();
			Cruddable.this.values.add(this);
		}

		public String getFieldName() {return dbFieldName;}
		public abstract void set(final T t);
		public abstract T    get();
		public abstract boolean isNullable();
		public abstract boolean isNull();
		protected void fromCursor(final Cursor cursor) {
			fromCursor(0, cursor);
		}
		protected abstract void fromCursor(final int offset, Cursor cursor);
		protected abstract void addToContentValues(final ContentValues values);

		@Override
		public int hashCode() {
		    if (isNull()) return 0;
		    else          return get().hashCode();
		}

		@Override
        public boolean equals(final Object other) {
		    if (other == null) return false;
		    if (other == this) return true;
		    if (getClass() != other.getClass()) return false;
		    @SuppressWarnings("unchecked")
            final CrudValue<T> that = (CrudValue<T>) other;

		    if (this.isNull()) {
		        return that.isNull();
		    } else {
		        return this.get().equals(that.get());
		    }
		}

		@Override
		public String toString() {
			return dbFieldName + "=" + get();
		}
	}

	public abstract class CrudNullableValue<T> extends CrudValue<T> {
		protected T t;

		public CrudNullableValue(final String dbFieldName) {
			super(dbFieldName);
		}

		@Override
		public boolean isNullable() {
			return true;
		}

		@Override
		public void set(final T t) {
			this.t = t;
		}

		@Override
		public boolean isNull() {
			return t == null;
		}

		@Override
		public T get() {
			return t;
		}

		public T get(final T def) {
			final T t = get();
			if (t == null) return def;
			else           return t;
		}
	}

	public abstract class CrudNotNullableValue<T> extends CrudValue<T> {
		public CrudNotNullableValue(final String dbFieldName) {super(dbFieldName);}
		@Override
		public boolean isNullable() {
			return false;
		}
		@Override
		public boolean isNull() {
			return false;
		}
	}

	public abstract class CrudNotNullableObjectValue<T> extends CrudNotNullableValue<T> {
		protected T t;

		public CrudNotNullableObjectValue(final String dbFieldName) {super(dbFieldName);}

		@Override
		public T get() {
			return t;
		}
		@Override
		public void set(final T t) {
			if (t == null) throw new NullPointerException("Attempt to set null on " + this.getClass().getSimpleName());
			this.t = t;
		}
	}

	////////////////////////// non-nullable classes ////////////////////////////
	public class CrudBoolean extends CrudNotNullableValue<Boolean> {
		private boolean t = false;
		public CrudBoolean(final String dbFieldName) {super(dbFieldName);}
		@Override
		public Boolean get() {return t;}
		public boolean getBoolean() {return t;}
		@Override
		public void set(final Boolean t) {this.t = t;}
		public void setBoolean(final boolean t) {this.t = t;}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) { t = cursor.getInt(offset + index) != 0;}
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t ? 1 : 0);}
	}
	public class CrudShort extends CrudNotNullableValue<Short> {
		private short t = 0;
		public CrudShort(final String dbFieldName) {super(dbFieldName);}
		@Override
		public Short get() {return t;}
		public short getShort() {return t;}
		@Override
		public void set(final Short t) {this.t = t;}
		public void setShort(final short t) {this.t = t;}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) { t = cursor.getShort(offset + index);}
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t);}
	}
	public class CrudInteger extends CrudNotNullableValue<Integer> {
		private int t = 0;
		public CrudInteger(final String dbFieldName) {super(dbFieldName);}
		@Override
		public Integer get() {return t;}
		public int getInt() {return t;}
		@Override
		public void set(final Integer t) {this.t = t;}
		public void setInt(final int t) {this.t = t;}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) { t = cursor.getInt(offset + index);}
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t);}
	}
	public class CrudLong extends CrudNotNullableValue<Long> {
		private long t = 0l;
		public CrudLong(final String dbFieldName) {super(dbFieldName);}
		@Override
		public Long get() {return t;}
		public long getLong() {return t;}
		@Override
		public void set(final Long t) {this.t = t;}
		public void setLong(final long t) {this.t = t;}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) { t = cursor.getLong(offset + index);}
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t);}
	}
	public class CrudString extends CrudNotNullableObjectValue<String> {
		public CrudString(final String dbFieldName) {super(dbFieldName); t = "";}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) {
			if (cursor.isNull(index + offset)) {
				throw new NullPointerException("Value for " + this.getClass().getSimpleName() + " at cursor index " + index + " is null.");
			}
			t = cursor.getString(index + offset);
		}
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t);}
	}
	public class CrudFloat extends CrudNotNullableValue<Float> {
		private float t = 0.0f;
		public CrudFloat(final String dbFieldName) {super(dbFieldName);}
		@Override
		public Float get() {return t;}
		public float getFloat() {return t;}
		@Override
		public void set(final Float t) {this.t = t;}
		public void setFloat(final float t) {this.t = t;}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) { t = cursor.getFloat(offset + index); }
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t);}
	}
	public class CrudDouble extends CrudNotNullableValue<Double> {
		private double t = 0.0;
		public CrudDouble(final String dbFieldName) {super(dbFieldName);}
		@Override
		public Double get() {return t;}
		public double getDouble() {return t;}
		@Override
		public void set(final Double t) {this.t = t;}
		public void setDouble(final double t) {this.t = t;}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) { t = cursor.getDouble(offset + index); }
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t);}
	}
	public class CrudBlob extends CrudNotNullableObjectValue<byte[]> {
		public CrudBlob(final String dbFieldName) {super(dbFieldName); t = new byte[0];}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) {
			if (cursor.isNull(offset + index)) {
				throw new NullPointerException("Value for " + this.getClass().getSimpleName() + " at cursor index " + (offset + index) + " is null.");
			}
			t = cursor.getBlob(offset + index);
		}
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t);}
	}
	public class CrudDate extends CrudNotNullableObjectValue<Date> {
		public CrudDate(final String dbFieldName) {super(dbFieldName); t = new Date();}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) {
			if (cursor.isNull(offset + index)) {
				throw new NullPointerException("Value for " + this.getClass().getSimpleName() + " at cursor index " + (offset + index) + " is null.");
			}
			t = new Date(cursor.getLong(offset + index));
		}
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t.getTime());}
	}
	public class CrudEnum<T extends Enum<T>> extends CrudNotNullableObjectValue<T> {
		private final Class<T> clazz;
		public CrudEnum(final String dbFieldName, final Class<T> clazz) {
			super(dbFieldName);
			this.clazz = clazz;
			t = clazz.getEnumConstants()[0];
		}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) {
			if (cursor.isNull(offset + index)) {
				throw new NullPointerException("Value for " + this.getClass().getSimpleName() + " at cursor index " + (offset + index) + " is null.");
			}
			t = Enum.valueOf(clazz, cursor.getString(offset + index));
		}
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t.name());}
	}

	/////////////////////////////// nullable classes ///////////////////////////
	public class CrudNullableBoolean extends CrudNullableValue<Boolean> {
		public CrudNullableBoolean(final String dbFieldName) {super(dbFieldName);}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) { t = cursor.isNull(offset + index) ? null : cursor.getInt(offset + index) != 0;}
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t == null ? null : (t ? 1 : 0));}
	}
	public class CrudNullableShort extends CrudNullableValue<Short> {
		public CrudNullableShort(final String dbFieldName) {super(dbFieldName);}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) { t = cursor.isNull(offset + index) ? null : cursor.getShort(offset + index);}
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t);}
	}
	public class CrudNullableInteger extends CrudNullableValue<Integer> {
		public CrudNullableInteger(final String dbFieldName) {super(dbFieldName);}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) { t = cursor.isNull(offset + index) ? null : cursor.getInt(offset + index);}
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t);}
	}
	public class CrudNullableLong extends CrudNullableValue<Long> {
		public CrudNullableLong(final String dbFieldName) {super(dbFieldName);}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) { t = cursor.isNull(offset + index) ? null : cursor.getLong(offset + index);}
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t);}
	}
	public class CrudNullableString extends CrudNullableValue<String> {
		public CrudNullableString(final String dbFieldName) {super(dbFieldName);}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) { t = cursor.isNull(offset + index) ? null : cursor.getString(offset + index); }
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t);}
	}
	public class CrudNullableFloat extends CrudNullableValue<Float> {
		public CrudNullableFloat(final String dbFieldName) {super(dbFieldName);}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) { t = cursor.isNull(offset + index) ? null : cursor.getFloat(offset + index); }
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t);}
	}
	public class CrudNullableDouble extends CrudNullableValue<Double> {
		public CrudNullableDouble(final String dbFieldName) {super(dbFieldName);}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) { t = cursor.isNull(offset + index) ? null : cursor.getDouble(offset + index); }
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t);}
	}
	public class CrudNullableBlob extends CrudNullableValue<byte[]> {
		public CrudNullableBlob(final String dbFieldName) {super(dbFieldName);}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) { t = cursor.isNull(offset + index) ? null : cursor.getBlob(offset + index);}
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t);}
	}
	public class CrudNullableDate extends CrudNullableValue<Date> {
		public CrudNullableDate(final String dbFieldName) {super(dbFieldName);}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) {t = cursor.isNull(offset + index) ? null : new Date(cursor.getLong(offset + index));}
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t == null ? null : t.getTime());}
	}
	public class CrudNullableEnum<T extends Enum<T>> extends CrudNullableValue<T> {
		private final Class<T> clazz;
		public CrudNullableEnum(final String dbFieldName, final Class<T> clazz) {super(dbFieldName); this.clazz = clazz;}
		@Override
		protected void fromCursor(final int offset, final Cursor cursor) {t = cursor.isNull(offset + index) ? null : Enum.valueOf(clazz, cursor.getString(offset + index));}
		@Override
		protected void addToContentValues(final ContentValues values) {values.put(dbFieldName, t == null ? null : t.name());}
	}

	/**
	 * name for the primary id column of all Android sqlite tables
	 */
	public static final String ID = "_id";

	/**
	 * Internal list where all crud values register themselves upon creation.
	 */
	protected final ArrayList<CrudValue<?>> values;

	/**
	 * every crud type must have at least an "_id" field
	 */
	public final CrudNullableLong _id;

	private final String tableName;

	private final String authority;

	/**
	 * @param valueCount the number of values expected for this crud instance
	 * (excluding the _id field) set this to the correct value initially to
	 * avoid resizing of the internal list holding the members
	 */
	public Cruddable(final String tableName, final String authority, final int valueCount) {
		values = new ArrayList<CrudValue<?>>(valueCount + 1);
		//This *needs* to be initialized here, to make sure initialization
		//happens after values is initialized. If we moved initialization of
		//values & id to declaration, we could not set the proper valueCount
		_id = new CrudNullableLong(ID);
		this.tableName = tableName;
		this.authority = authority;
	}

	/**
	 * @return the authority for the content provider url for this cruddable
	 */
	public String getAuthority() {
	    return authority;
	}

	/**
	 * @return name of the table for this Cruddable
	 */
	public String getTableName() {
	    return tableName;
	}

	/**
     * @return the url to the table for this cruddable
     */
    public Uri getBaseUri() {
        return UriUtils.contentUri(authority, tableName);
    }

	/**
	 * simple query by id, sets the values of this instance to the values
	 * of the row with the given id.
	 * @param resolver not null
	 * @param _id the id of the row to use
	 * @throws IllegalArgumentException if the id was not found in the database
	 */
	public void query(final ContentResolver resolver, final long _id) {
		final Cursor cursor = resolver.query(
				UriUtils.appendToUri(getBaseUri(), _id),
				getProjection(), null, null, null);
		try {
			if (cursor.getCount() != 1) {
				throw new IllegalArgumentException(
						"Expected exactly one result for id " + _id + ", got " +
				        cursor.getCount());
			}
			if (cursor.isBeforeFirst()) {
				cursor.moveToNext();
			}
			fromCursor(cursor);

		} finally {
			cursor.close();
		}
	}


	/**
	 * simple query by id, sets the values of this instance to the values
     * of the row with the given id.
	 * @param db database to use, not null
	 * @param _id the id of the row to use
	 */
	public void query(final SQLiteDatabase db, final long _id) {
	    final Cursor cursor = db.query("", getProjection(), "WHERE _id = ?",
	                      new String[]{Long.toString(_id)}, "", "", "");
	    try {
	        cursor.moveToFirst();
	        fromCursor(cursor);
	    } finally {
	        cursor.close();
	    }
	}



	/**
	 * Sets this instances' values with the values found in the cursor at
	 * the cursor's current position
	 * @param cursor not null, must be positioned at a valid row
	 */
	public void fromCursor(final Cursor cursor) {
		for (final CrudValue<?> val : values) {
			val.fromCursor(cursor);
		}
	}

	/**
	 * Sets this instances' values with the values found in the cursor at
     * the cursor's current position.
     * The fields are queried from the given cursor with the given offset.
     * Use this for queries spanning multiple tables.
	 * @param offset the offset into the field indices, must be >= 0
	 * @param cursor not null, must be positioned at a valid row
	 */
	public void fromCursor(final int offset, final Cursor cursor) {
		for (final CrudValue<?> val : values) {
			val.fromCursor(offset, cursor);
		}
	}

	/**
	 * Reads the given cruddables from the given cursor in the order in which they
	 * are listed as arguments. Takes care of the correct offsets for each cruddable.
	 * Use this for queries spanning multiple tables.
	 * @param cursor not null, must be positioned at a valid row
	 * @param cruddables the cruddables to fill from the current row
	 */
	public static void fromCursor(final Cursor cursor, final Cruddable ... cruddables) {
        fromCursor(0, cursor, cruddables);
    }


	public static void fromCursor(final int offset, final Cursor cursor, final Cruddable ... cruddables) {
        int fieldCount = offset;
        for (final Cruddable cruddable : cruddables) {
        	cruddable.fromCursor(fieldCount, cursor);
        	fieldCount += cruddable.getProjectionLength();
        }
	}

	/**
	 * Creates a new db entry with the current values, sets the id to the
	 * id of the new entry.
	 * @param resolver resolver to use for accessing the db, not null
	 * @return the id of the newly created table record
	 * @throws IllegalArgumentException if creation failed
	 */
	public Long create(final ContentResolver resolver) {
		final Uri uri = resolver.insert(getBaseUri(), toContentValues());
		try {
			final List<String> segments = uri.getPathSegments();
			_id.set(Long.parseLong(segments.get(segments.size() - 1)));
		} catch (final Exception e) {
			throw new IllegalArgumentException(
					"Could not read id from uri: " + uri);
		}
		return _id.get();
	}

	/**
     * Creates a new db entry with the current values, sets the id to the
     * id of the new entry.
     * @param db the db to insert the row in, not null
     */
	public Long create(final SQLiteDatabase db) {
	    return db.insert(tableName, null, toContentValues());
    }

	/**
	 * Updates the table row with the same id as this cruddable with the current
	 * values.
	 * @param resolver resolver to use for accessing the db, not null
	 * @throws IllegalArgumentException if the current id is null or if more than
	 * one row was updated
	 */
	public void update(final ContentResolver resolver) {
		if (_id.isNull()) {
			throw new IllegalStateException(
					"Cannot update " + this + ", id is null.");
		}
		final int count = resolver.update(
				UriUtils.appendToUri(getBaseUri(), _id.get()), toContentValues(),
				null, null);
		if (count != 1) {
			throw new IllegalStateException("Update of " + this + "" +
					" failed, expected one update, got: " + count);
		}
	}

	/**
	 * Updates the table row with the same id as this cruddable with the current
     * values.
     * @param db the database to update in, not null
     * @throws IllegalArgumentException if the current id is null or if more than
     * one row was updated
	 */
	public void update(final SQLiteDatabase db) {
	    if (_id.isNull()) {
            throw new IllegalStateException(
                    "Cannot update " + this + ", id is null.");
        }
	    final int count = db.update(tableName,
	                                toContentValues(),
	                                "WHERE " + ID + "=?",
	                                new String[]{_id.get().toString()});
	    if (count != 1) {
            throw new IllegalStateException("Update of " + this + "" +
                    " failed, expected one update, got: " + count);
        }
	}

	/**
	 * Depending on whether the id of this Cruddable is set, either creates
	 * or updates a row through the given content resolver.
	 * @param resolver resolver to use for accessing the db, not null
	 */
	public void createOrUpdate(final ContentResolver resolver) {
		if (_id.isNull()) {
			create(resolver);
		} else {
			update(resolver);
		}
	}

	/**
     * Depending on whether the id of this Cruddable is set, either creates
     * or updates a row.
     * @param db the database to update in, not null
     */
	public void createOrUpdate(final SQLiteDatabase db) {
	    if (_id.isNull()) {
	        create(db);
	    } else {
	        update(db);
	    }
	}

	/**
	 * Deletes the row corresponding to the current id.
	 * @param resolver resolver to use for accessing the db, not null
	 * @throws IllegalStateException if the current id is null
	 */
	public void delete(final ContentResolver resolver) {
		if (_id.isNull()) {
			throw new IllegalStateException(
					"Cannot delete " + this + ", id is null.");
		}
		resolver.delete(
				UriUtils.appendToUri(getBaseUri(), _id.get()),
				null, null);
	}

	/**
     * Deletes the row corresponding to the current id.
     * @param db the database to delete the entry in, not null
     * @throws IllegalStateException if the current id is null
     */
	public void delete(final SQLiteDatabase db) {
	    if (_id.isNull()) {
            throw new IllegalStateException(
                    "Cannot delete " + this + ", id is null.");
        }
	    db.delete(tableName, "WHERE " + ID + "=?",
                             new String[]{_id.get().toString()});
	}

	/**
	 * @return the projection (array of column names) for this Cruddable, has at least length 1
	 */
	public String[] getProjection() {

		final String[] projection = new String[values.size()];
		for (int n = 0; n < projection.length; n++) {
			projection[n] = values.get(n).dbFieldName;
		}
		return projection;
	}

	/**
	 * @param tablePrefix identifier to prefix to the column names. Without
	 * the ".". So if you want the field "_id" to be returned as "foo._id",
	 * pass in "foo"
	 * @return a projection with all fields, all prepended with a table prefix
	 */
	public String[] getProjection(final String tablePrefix) {
		final String[] projection = new String[values.size()];
		for (int n = 0; n < projection.length; n++) {
			projection[n] = tablePrefix + "." + values.get(n).dbFieldName;
		}
		return projection;
	}

	/**
	 * @return number of values of this Cruddable (== size of a full projection)
	 */
	public int getProjectionLength() {
		return values.size();
	}

	/**
	 * Combines the given arrays into one.
	 * Use this to create a projection for a query spanning multiple tables.
	 * @param projections a number of projections
	 * @return an array with the contents of the given projections joined
	 */
	public static String[] combineProjections(final String[] ... projections) {
		int length = 0;
		for (final String[] projection : projections) {
			length += projection.length;
		}
		final String[] result = new String[length];
		int idx = 0;
		for (final String[] projection : projections) {
		    System.arraycopy(projection, 0, result, idx, projection.length);
		    idx += projection.length;
		}
		return result;
	}

	/**
	 * @return the state of this cruddable as ContentValues
	 */
	public ContentValues toContentValues() {
		final ContentValues result = new ContentValues(values.size());

		for (int n = 0; n < values.size(); n++) {
			values.get(n).addToContentValues(result);
		}

		return result;
	}

	/**
	 * Creates a hash code based on the current values.
	 */
	@Override
	public int hashCode() {
	    return values.hashCode();
	}

	/**
	 * Compares this cruddable by the values it contains
	 */
	@Override
	public boolean equals(final Object other) {
	    if (other == null) return false;
	    if (other == this) return true;
	    if (other.getClass() != this.getClass()) return false;
	    final Cruddable that = (Cruddable) other;
	    return this.values.equals(that.values);
	}

	/**
	 * Returns a debug String representation of the current values
	 */
	@Override
	public String toString() {
	    return getClass().getSimpleName() + "{" + TextUtils.join(", ", values) + "}";
	}
}
