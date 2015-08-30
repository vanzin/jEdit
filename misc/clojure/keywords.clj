(ns tools.keywords
  "Generate a table showing Clojure symbols, their characteristics, and how
   they are referenced in the Clojure mode file clojure.xml.
   Provides information about current keyword coverage in the mode file and
   supports manual update and normalization of clojure.xml"
  (:gen-class)
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip])
  (:import [java.io FileInputStream]
           [javax.xml.parsers SAXParser SAXParserFactory]))

;;; Point this at your jEdit working directory.
(def ^:private jedit-dir "/home/marc/Work/jedit")

(def ^:private column-headers
  "Column headers for output"
  "namespace,symbol,type,keyword")

(def ^:private namespaces
  "List of namespaces to be checked for symbols." [
  'clojure.core
  'clojure.data
  'clojure.data.xml
  'clojure.data.zip.xml
  'clojure.edn
  'clojure.inspector
  'clojure.instant
  'clojure.java.browse
  'clojure.java.io
  'clojure.java.javadoc
  'clojure.java.shell
  'clojure.main
  'clojure.pprint
  'clojure.reflect
  'clojure.repl
  'clojure.set
  'clojure.stacktrace
  'clojure.string
  'clojure.template
  'clojure.test
  'clojure.walk
  'clojure.xml
  'clojure.zip])

(def ^:private deprecated-keywords
  "Keywords that are marked as deprecated in documentation."
  #{"add-classpath"})

(def ^:private ignored-keywords
  "Keywords that are purposely ignored."
  #{"*allow-unresolved-vars*" "*assert*"
    "*fn-loader*" "*math-context*" "*source-path*"
    "*use-context-classloader*" "*verbose-defrecords*"
    "await1"
    "chunk" "chunk-append" "chunk-buffer" "chunk-cons"
    "chunk-first" "chunk-next" "chunk-rest" "chunked-seq?"
    "-cache-protocol-fn" "-reset-methods"
    "coll?"
    "default-streams-impl"
    "EMPTY-NODE"
    "find-protocol-impl" "find-protocol-method"
    "method-sig"
    "pop-thread-bindings" "push-thread-bindings"
    "print-ctor" "print-method" "print-simple"
    "special-symbol?" "thread-bound?"})

(def ^:private special-forms
  "Special forms are not values of any namespace, hence this separate list."
  ["def" "if" "do" "quote" "var"
   "fn" "loop" "recur" "throw" "try"
   "." "new" "set"])

(defn- predefined-map
  "Return merged map of all deprecated and ignorable keywords."
  []
  (merge
    (into {} (map (fn [keywd] [keywd "(deprecated)"]) deprecated-keywords))
    (into {} (map (fn [keywd] [keywd "(ignored)"])    ignored-keywords))))

(defn- string->keyword
  "If string begins with a colon convert it to a keyword, otherwise return string."
  [string]
  (if (= \: (first string))
    (keyword nil (apply str (rest string)))
    string))

(defn- check-special-form
  [keyword-map sym]
  (printf "%s,%s,%s,%s%n" "" sym "special" (get keyword-map sym))
  (get keyword-map sym))

(defn- check-special-forms
  "Check all special form symbols to see what they are and how they are
   represented in the specified keyword map."
  [keyword-map]
  (map (partial check-special-form keyword-map) special-forms))

(defn- symbol-type
  "Return a string representing the type of value attached to the specified symbol.
   Strings include `macro`, `function`, `dynamic`, `earmuff` (for variables
   named as dynamic symbols but not actually dynamic), and `unknown`."
  [sym-name sym-var]
  (let [sym-meta (meta sym-var)
        sym-val (var-get sym-var)]
    (cond
      (:macro sym-meta) "macro"
      (fn? sym-val) "function"
      (:dynamic sym-meta) "dynamic"
      (re-matches #"\*.+\*" sym-name) "earmuff"
      true "unknown")))

(defn- check-namespace
  "Check all public symbols in the specified namespace to see what they are
   and how they are represented in the specified keyword map."
  [name-space keyword-map]
  (require name-space)
  (doseq [sym (sort (keys (ns-publics name-space)))]
    (let [kstr (str sym)
          type (symbol-type (name sym) (ns-resolve name-space sym))]
      ;; TODO: print symbols beginning with equals (=) in some quoted form
      (printf "%s,%s,%s,%s%n" name-space sym type
        (or (get keyword-map kstr) "[unknown]")))))

(defn- no-dtd-parser
  "Build an XML parser that will ignore DTDs."
  [s ch]
  (let [factory (SAXParserFactory/newInstance)]
    (.setFeature factory "http://apache.org/xml/features/nonvalidating/load-external-dtd" false)
    (let [^SAXParser parser (.newSAXParser factory)]
      (.parse parser s ch))))

(defn- read-xml
  "Parse XMl from the file at the specified path."
  [path]
  (with-open [file-stream (FileInputStream. path)]
    (xml/parse file-stream no-dtd-parser)))

(defn- find-keywords
  "Return list of :KEYWORD entities under the :KEYWORD tag in the XML in
   the file at the specified path."
  [path]
  (loop [loc (zip/xml-zip (read-xml path))]
    (if (zip/end? loc)
      nil
      (let [node (zip/node loc)]
        (if (= :KEYWORDS (:tag node))
          (:content node)
          (recur (zip/next loc)))))))

(defn- xml-keyword-map
  "Return a map from symbol names to keyword types as specified in the jEdit
   mode file `clojure.xml`."
  [path]
  ;; TODO: look for multiple definitions of same keyword in XML
  (let [keywords (find-keywords path)]
    (into (predefined-map)
      (map (fn [keywd] [(-> keywd :content first) (:tag keywd)]) keywords))))

;;; Not building with leiningen to save complexity in non-Clojure project.
;;; Therefore the 'program' starts at the top level of the namespace.
;;; This also supports direct testing when loading the file in a REPL.
(let [{:keys [jedit] :or {jedit jedit-dir}}
        (map string->keyword *command-line-args*)
      keyword-map (xml-keyword-map (str jedit "/modes/clojure.xml"))]
  (println column-headers)
  (check-special-forms keyword-map)
  (doseq [name-space namespaces]
    (try
      (check-namespace name-space keyword-map)
      (catch Exception e
        (printf "%s,***,error,%s%n" name-space "(not-loaded)"))))
  ;; TODO: Should be checking for keywords in map but not special or in namespace!
  ; (doseq [entry keyword-map]
;   (println ">>>" entry))
  )

