## About WinzigSQL ##

WinzigSQL is a very tiny utility lib that is supposed to aid in the task of accessing a database under Android.

It consists of three major classes and some utilities, which each can be used separately from each other. 
WinzigSQL is *not* an OR-Mapper, nor will it ever be one..
Using WinzigSQL makes only sense if you really know SQL.. 
It is not Framework that is to handle every aspect of database interaction. 
Except for the simplest CRUD operations, 
it will not generate SQL for you. 
It is supposed to make 80-90% of typical database tasks in Android apps easier and tries to be very small and
efficient in doing so.

WinzigSQL helps you with three areas of database handling under Android:

* it has a primitive DSL to build Row-Mapping / DAO classes
* it has an extended version of `SQLiteOpenHelper` that handles db creation and updates from SQL Files packed with the app following a naming convention.
* it has a wrapper class that allows to create a `ContentProvider` from a db in a few lines, thus making databases easy to use with `Loader<Cursor>` which in turn makes it very easy to fill Activities / Fragments asynchronously with data from a database

The idea of WinzigSQL is to get to grips with it in 30 minutes. It is a typical 80:20 solution and aimed at people who prefer to do most of their 
database tasks by hand. 

##License##
WinzigSQL is licensed under the 3-clause BSD license. For details see the `LICENSE` file that comes with the source.

##Maven##
Note: not uploaded to Maven Central yet, so this does not yet work:

    <dependency>
      <groupId>de.theappguys</groupId>
      <artifactId>winzigsql</artifactId>
      <version>0.1-SNAPSHOT</version>      
    </dependency>

## Basic Usage ##
This is a quick tutorial that shows all of WinzigSQL's major features. you can use each of them separately, but they are designed to work hand-in-hand.

WinzigSQL does not create any table statements etc. for you, so the first step is to provide the SQL files necessary to set up your database.
The table creation statements are always executed from the file: `res/raw/create_db.sql`. Lets fill it with some dummy tables: 

    --Table for foos
    CREATE TABLE foo (
    _id INTEGER NOT NULL PRIMARY KEY,
    somevalue TEXT NOT NULL,
    somenumber INTEGER --nullable
    );

    /* Charts contain a number of measurements
    */
    CREATE TABLE bar (
    _id INTEGER NOT NULL PRIMARY KEY,
    foo_id INTEGER NOT NULL REFERENCES foo(_id) ON DELETE CASCADE,
    cooldata TEXT NOT NULL
    );

    --init the tables with some dummy data
    INSERT INTO foo (_id, somevalue, somenumber) values (0, "hello world", null);
    INSERT INTO foo (_id, somevalue, somenumber) values (1, "answer", 42);
    
    INSERT INTO bar (foo_id, cooldata) values (0, "data 1");
    INSERT INTO bar (foo_id, cooldata) values (0, "data 2");
    INSERT INTO bar (foo_id, cooldata) values (0, "data 3");
    
    INSERT INTO bar (foo_id, cooldata) values (1, "data 4");
    
Now you can create your db like so:

    new WinzigDbHelper(context, "your_db_name", your_db_version);
    
To access your table, you can create quick mapping classes like so:

    public class Foo extends Cruddable {

    public final CrudString somevalue = new CrudString("somevalue");
    public final CrudNullableInteger somenumber = new CrudNullableInteger("somenumber");

    public Foo() {
        super("foo", 2);
    }
    }
    

Note that there is no builtin mechanism to handle foreign keys, 1-n, m-n mappings etc. This is totally up to you. 
This is intended, as in all frameworks we encountered so far we found the mechanisms for working automatically with 
these mappings were more complicated than the just doing a manual select & mapping.

## Usage with content providers ##
The main reason we created WinzigSQL was to make using loaders easier. If your db gets a bit more complex and you have more than a few 
rows in your tables, queries may take some time. It is a big no-no to do your database handling on the UI thread. So the best solution 
is to fill your views with data from the db using a `Loader<Cursor>`. Regrettably, there is no quick way to do this in Android. In order
to use `Loader`s, you need  `ContentProvider`s. Creating your own `ContentProvider` with basic CRUD support requires an annoying amount of 
boilerplate. With WinzigSQL, you do it like this: 

    package com.example.yourapp.sql;

    public class YourDbProvider extends WinzigDbProvider {

        public YourDbProvider() {
            super("your.namespace");
        }

        @Override
        protected SQLiteOpenHelper createDb(final Context context) {
            return new WinzigDbHelper(context, "your_db_name", YOUR_DB_VERSION);
        }
    }

That's it. Note that you can also use an ordinary `SQLiteOpenHelper` here, there is no need to use a `WinzigDbHelper`. So if you want 
your own db creation & update code, you can just implement your `SQLiteOpenHelper` the old fashioned way by hand.

Now you need to register your db provider in the manifest. *NOTE THAT THIS PROVIDER GIVES FULL ACCESS TO YOUR DB, SO YOU ALMOST CERTAINLY 
DO NOT WANT TO MAKE IT AVAILABLE FOR OTHER APPS!* To register your provider, add the following to the manifest: 

    <manifest .... >
    ...
    <application ...>
    
    <provider
        android:name=".sql.YourDbProvider"
        android:authorities="com.example.your_db_name.sql"
        android:exported="false" ← THIS IS IMPORTANT!
        >
    </provider>
    
    </application>
    </manifest>

## The WinzigDbHelper ##
As mentioned above, the `WinzigDbHelper` expects a setup script for the complete database in the resources. 
WinzigSQL has an SQL "Parser" (a hack, really) that allows for parsing of SQL files that follow the following rules:

* all statements are terminated by a semicolon `;`
* line comments begin with `--`: 
    CREATE TABLE foo (--this is a line comment
* Block comments are C-style `/*  ... */`    
    CREATE TABLE foo /* this is the start of a block comment
    still in the block comment
    comment done */

String literals can at the moment not yet contain `--` or `/*`, this will trigger the comment handling.
This will hopefully get fixed in the future. As long as your script follows these rules, you can just drop 
it into the assets instead of putting it into string constants or string resourcen (like often found) in tutorials.

This makes handling, creation & testing of the statements *a lot* easier.

The `WinzigDbHelper` also follows conventions when it comes to updating your database. For each version `n` it expects
a file `assets/sql/upgrade_db_[n].sql`.

So if the db on the device has version 2 and an update of your app uses version 4, the `WinzigDbHelper` will look for and 
execute the following scripts: 

    res/raw/upgrade_db_3.sql
    res/raw/upgrade_db_4.sql
    
If one of the scripts is missing, it is silently skipped.    

## The WinzigDbProvider ##
The `WinzigDbProvider` is a base class that let's you implement your own Providers with a minimal amount of code.
Note that -as mentioned above- these subclasses will allow full access to your database, so they should not be made
publicly available. 

    public class MyDbProvider extends WinzigDbProvider {

    public MyDbProvider() {
        super("com.example.your.app.namespace");
    }

    @Override
    protected SQLiteOpenHelper createDb(final Context context) {
        return new WinzigDbHelper(context, "your_db_name", dbVersion);
    }
    }

Now you need to add the provider to your app's manifest (see tutorial above).

After doing that, your db is available wrapped by a provider to your UI code and you 
can quickly populate your UI asynchronously with a Loader.

    
## The Cruddable ##
The `Cruddable` class is a base class for constructing simple row mappers / DAOs.

To use it, extend `Cruddable` and add a member that is a subclass of `CrudValue` (see example above) for each row
you want to map.

The Cruddable has convenience methods to perform CRUD operations on a ContentProvider or Database and can set all its
members' state from a cursor. It only accesses the cursor by index, it never looks up column names, which helps squeeze the
last bit of performance when reading data from cursors. 

   
##Version History##

###0.1###    
Initial Version