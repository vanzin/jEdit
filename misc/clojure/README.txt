The misc/clojure directory is concerned with verifying modes/clojure.xml.

This is something only of interest to Clojure users who are also jEdit
maintainers. If you come looking because the clojure.xml file seems out
of date you might want to check out the jEdit code and use this tool
to examine the situation and update it as required by some new Clojure release.

When a Clojure version is released, run the tool in misc/clojure and examine
the resulting tabular results. Make changes to the current modes/clojure.xml.
Re-run the tool to check your work. Check in the new modes/clojure.xml file
(and misc/clojure/keywords.csv file as documentation?) as a patch.

misc/clojure/keywords.clj

The Clojure script misc/clojure/keywords.clj checks through all of
main Clojure namespaces, comparing the symbols therein to the current
modes/clojure.xml file, then generates a CSV file representing the results.

misc/clojure/run

On a linux system the run shell script will execute the script when called as:

    $ cd misc/clojure
    $ ./run

This should generate a new misc/clojure/keywords.csv file suitable for loading
into a spreadsheet program or post-processing or whatever.

The rightmost column in the table is the most important. Symbols that are not
covered at all are in the rightmost column as '[unknown]'. Some symbols are
'(ignored)' or '(deprecated)'. Most are marked as one of the jEdit mode
symbol types (e.g. :KEYWORD1, :LITERAL3, or :OPERATOR). Other columns list
the containing namespace, symbol name, and the type of the symbol.

Within the misc/clojure/run script is a symbol representing the location
of the Clojure jar file for your system. Check that and edit as necessary.

runnin in a repl

Use load-file within a repl to execute the misc/clojure/keywords.clj script.
The script will execute and spew the csv output into your repl.

When running the misc/clojure/keywords.clj file in a repl there is a symbol
at the top of misc/clojure/keywords.clj representing the location of the
working jEdit directory. Normally the misc/clojure/run script provides this.
To load the script directly into a repl the default value in the Clojure script
must be properly set.

notes

Spewing out a new modes/clojure.xml file from misc/clojure/keywords.csv
initially seemed like a good idea but later seemed like too much fiddly work.
Manually editing the modes/clojure.xml file takes some time but should be
a rare occurence, corresponding to major releases of Clojure.

A full leiningen installation seemed redundant for a tool that only a Clojure
afficionado will ever use. The result is these hacky instructions. In attempting
an elegant laziness I perhaps doom future maintainers to editing two files.
Mea culpa. Feel free to patch in a project.clj file or whatever.

Regarding instructions for Windows, Mac, whatever. Please feel free to add some.

