<!DOCTYPE style-sheet PUBLIC "-//James Clark//DTD DSSSL Style Sheet//EN" [
<!ENTITY dbstyle PUBLIC "-//Norman Walsh//DOCUMENT DocBook Print Stylesheet//EN"
CDATA DSSSL> ]>

<style-sheet>
<style-specification use="print">
<style-specification-body>

(define %two-side% #t)
(define %section-autolabel% #t)
(define %paper-type% "USletter")
(define %admon-graphics% #f)

(define %body-start-indent% 2pi)
(define %block-start-indent% 1pi)

;(define %default-quadding% 'justify)
;(define %hyphenation% #t)

(declare-characteristic preserve-sdata?
	"UNREGISTERED::James Clark//Characteristic::preserve-sdata?" #f)

(define %visual-acuity% "presbyopic")

(define %funcsynopsis-style% 'ansi)
(element void (literal "();"))

(element (listitem funcsynopsis funcprototype)
  (let ((paramdefs (select-elements (children (current-node))
				    (normalize "paramdef"))))
    (make sequence
	font-family-name: %mono-font-family%
        font-size: (* (inherited-font-size)
		%verbatim-size-factor%)
	(process-children)
      (if (equal? %funcsynopsis-style% 'kr)
	  (with-mode kr-funcsynopsis-mode
	    (process-node-list paramdefs))
	  (empty-sosofo)))))

(define %verbatim-size-factor% 0.85)

(element (listitem abstract) (process-children))

(element (ulink) (process-children))

</style-specification-body>
</style-specification>
<external-specification id="print" document="dbstyle">
</style-sheet>
