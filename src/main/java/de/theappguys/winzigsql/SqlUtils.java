/**
 *
 */
package de.theappguys.winzigsql;

import java.util.ArrayList;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

/**
 * Util methods for sql execution and parsing.
 */
public class SqlUtils {
	private SqlUtils() {}

	/**
	 * Poor man's sql script parser. Slightly more powerful than the Android
	 * built-in parser as it allows c-style block comments and one line comments.
	 * Assumes all statements are terminated by a semicolon at the end of a
	 * line.
	 * Two hyphens are always regarded as the beginning of a comment, a slash /
	 * and asterisk * are always regarded as the beginning of a block comment, so they
	 * cannot be used in string values (TODO: better parsing of string literals is necessary).
	 * Block comments are removed before line comments, so line comments do not
	 * "hide" the start or end of a block comment.
	 */
	public static List<String> parseSqlScript(final String script) {
		//remove C-style block comments /* ... */
		final StringBuilder withoutBlockComments = new StringBuilder();
		boolean inBlock = false;
		final int length = script.length();
		final int lastN  = length - 1;
		for (int n = 0; n < length; n++) {
			final char current = script.charAt(n);
			final char next    = n < lastN ? script.charAt(n + 1) : 0;

			if (inBlock) {
				if (current == '*' && next == '/') {
					//end  of block
					inBlock = false;
					n++; //skip next char
				} else {
					//ignore char in block
				}
			} else {
				if (current == '/' && next == '*') {
					inBlock = true;
					n++; //skip next char
				} else {
					withoutBlockComments.append(current);
				}
			}
		}

		final ArrayList<String> result = new ArrayList<String>();
		final StringBuilder currentStatement = new StringBuilder();
		for (final String maybeWindowsLine :
		     TextUtils.split(withoutBlockComments.toString(), "\n")) {
		    //remove stupid windows \r
		    final String line;
		    if (maybeWindowsLine.endsWith("\r")) {
		        line = maybeWindowsLine.substring(0, maybeWindowsLine.length() - 1);
		    } else {
		        line = maybeWindowsLine;
		    }

		    //now strip line comments
			final int idx = line.indexOf("--");
			final String lineRest = idx == -1 ?
					//no line comment found, just use the whole line
					line.trim() :
					//found line comment, use only the beginning of the line
				    line.substring(0, idx).trim();
			//append if anything is left
            if (lineRest.length() > 0) {
            	currentStatement.append(lineRest).append('\n');
            	//if we have reached the end of a statement, add it to results
            	if (lineRest.charAt(lineRest.length() -1 ) == ';'){
            		result.add(currentStatement.toString());
            		currentStatement.setLength(0);
            	}
            }
		}
		return result;
	}

	/**
	 * Executes all statements in the given string using
	 * the comment-aware script parser implemented by
	 * parseSqlScript
	 */
	public static void executeStatements(
			final SQLiteDatabase db,
			final String sqlStatements) {
		executeStatements(
        		db,
        		parseSqlScript(sqlStatements));
    }

	/**
	 * Executes all given statements on the given db.
	 * Statements are expected to have no comments in them and each be
	 * placed in a single string.
	 */
	public static void executeStatements(
			final SQLiteDatabase db,
            final Iterable<String> sqlStatements) {
        for (final String statement : sqlStatements) {
            if (TextUtils.isEmpty(statement)) continue;
            db.execSQL(statement);
        }
    }
}
