/**
 *
 */
package de.theappguys.winzigsql;

import android.content.ContentValues;

/**
 * Wraps a fluid interface around content values.
 *
 * On calling build(), a copy of the internal data is returned,
 * so this builder can be reused.
 */
public class ContentValuesBuilder {
	private final ContentValues vals = new ContentValues();

	public ContentValuesBuilder put(final String key, final Boolean value) {
		vals.put(key, value);
		return this;
	}

	public ContentValuesBuilder put(final String key, final Byte value) {
		vals.put(key, value);
		return this;
	}

	public ContentValuesBuilder put(final String key, final byte[] value) {
		vals.put(key, value);
		return this;
	}

	public ContentValuesBuilder put(final String key, final Double value) {
		vals.put(key, value);
		return this;
	}

	public ContentValuesBuilder put(final String key, final Float value) {
		vals.put(key, value);
		return this;
	}

	public ContentValuesBuilder put(final String key, final Long value) {
		vals.put(key, value);
		return this;
	}

	public ContentValuesBuilder put(final String key, final Short value) {
		vals.put(key, value);
		return this;
	}

	public ContentValuesBuilder put(final String key, final String value) {
		vals.put(key, value);
		return this;
	}

	public ContentValuesBuilder putNull(final String key) {
		vals.putNull(key);
		return this;
	}

	public ContentValuesBuilder putNull(final ContentValues other) {
		vals.putAll(other);
		return this;
	}

	public ContentValues build() {
		return new ContentValues(vals);
	}
}
