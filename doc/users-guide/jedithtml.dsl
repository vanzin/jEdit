<!DOCTYPE style-sheet PUBLIC "-//James Clark//DTD DSSSL Style Sheet//EN" [
<!ENTITY dbstyle PUBLIC "-//Norman Walsh//DOCUMENT DocBook HTML Stylesheet//EN"
CDATA DSSSL> ]>

<style-sheet>
<style-specification use="html">
<style-specification-body>

(define %html-ext% ".html")
(define %shade-verbatim% #t)
(define %root-filename% "index")
(define %use-id-as-filename% #t)
(define %body-attr%
	(list
		(list "BGCOLOR" "#FFFFFF")))

(define %admon-graphics% #f)
(define %spacing-paras% #f)
(define %html-manifest% #t)

;; make these pretty
(element guibutton ($bold-seq$))
(element guimenu ($bold-seq$))
(element guimenuitem ($bold-seq$))
(element guisubmenu ($bold-seq$))
(element application ($mono-seq$))
(element glossterm ($bold-seq$))
(element (funcdef function) ($bold-seq$))
(element funcsynopsis (process-children))

(element void (process-children))

(define %funcsynopsis-style% 'ansi)

;; abstract is used to tag data, not to render output
(element (listitem abstract) (process-children))

;; workaround for stupid Swing HTML limitation - it can't display
;; DocBook's quotes properly

(element quote
	(make sequence
		(literal "\"")
		(process-children)
		(literal "\"")))

;; Swing HTML doesn't support tables properly

(define %gentext-nav-use-tables% #f)

;; another Swing HTML fix: "WIDTH" hard-coded here at 100%

(element (listitem informalexample)
  (let ((id (element-id))
  	(rule-before? %informalexample-rules%)
	(rule-after? %informalexample-rules%))
    (make element gi: "DIV"
	  attributes: (list
			(list "BORDER" "0")
			(list "BGCOLOR" "#E0E0E0")
			(list "WIDTH" "100%"))
	  (if id
	      (make element gi: "A"
		    attributes: (list (list "NAME" id))
		    (empty-sosofo))
	      (empty-sosofo))

	  (if %spacing-paras%
	      (make element gi: "P" (empty-sosofo))
	      (empty-sosofo))

	  (if rule-before?
	      (make empty-element gi: "HR")
	      (empty-sosofo))

	  (process-children)

	  (if rule-after?
	      (make empty-element gi: "HR")
	      (empty-sosofo))

	  (if %spacing-paras%
	      (make element gi: "P" (empty-sosofo))
	      (empty-sosofo)))))

(root
	(make sequence
		(process-children)
		(make-xml-toc)))

(define (make-xml-toc)
	(make entity
		system-id: "toc.xml"
		(literal "<?xml version='1.0'?>")
		(make element gi: "TOC"
			(with-mode xmltoc
			(process-children)))))

(define (xml-toc-entry nd gilist)
	(make element gi: "ENTRY"
		attributes: (list
			(list "HREF" (href-to (current-node))))
		(make element gi: "TITLE"
			(element-title-sosofo (current-node)))
		(process-children)))

(mode xmltoc
	(default (empty-sosofo))

	(element set (xml-toc-entry (current-node)
		(list (normalize "book"))))

	(element book (xml-toc-entry (current-node)
		(list (normalize "part")
			(normalize "preface")
			(normalize "chapter")
			(normalize "appendix")
			(normalize "reference"))))

	(element preface (xml-toc-entry (current-node)
		(list (normalize "sect1"))))

	(element part (xml-toc-entry (current-node)
		(list (normalize "preface")
			(normalize "chapter")
			(normalize "appendix")
			(normalize "reference"))))

	(element chapter (xml-toc-entry (current-node)
		(list (normalize "sect1"))))

	(element appendix (xml-toc-entry (current-node)
		(list (normalize "sect1"))))

	(element sect1 (xml-toc-entry (current-node)
		(list (normalize "sect2"))))

	(element sect2 (xml-toc-entry (current-node)
		(list (normalize "sect3"))))

	(element sect3 (xml-toc-entry (current-node) '()))

	(element reference (xml-toc-entry (current-node)
		(list (normalize "refentry"))))

	(element refentry (xml-toc-entry (current-node) '())))

</style-specification-body>
</style-specification>
<external-specification id="html" document="dbstyle">
</style-sheet>
