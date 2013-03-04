package de.theappguys.winzigsql;

import android.net.Uri;
import android.net.Uri.Builder;

/**
 * static utility methods to build URIs
 */
public class UriUtils {

	private UriUtils() {}

	/**
	 * Builds a content Uri for a content provider
	 */
	public static Uri contentUri(
			final String authority, final Object ... segments) {
		final Builder builder = new Uri.Builder()
		    .scheme("content").authority(authority);
		for (final Object segment : segments) {
			builder.appendPath(segment.toString());
		}
	    return builder.build();
	}

	/**
	 * appends to an existing uri
	 */
	public static Uri appendToUri(final Uri uri, final Object ... segments) {
		final Builder builder = uri.buildUpon();
		for (final Object segment : segments) {
			builder.appendPath(segment.toString());
		}
		return builder.build();
	}
}
