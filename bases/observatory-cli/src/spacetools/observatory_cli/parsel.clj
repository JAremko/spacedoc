(ns spacetools.observatory-cli.parsel
  "Emacs lisp parser."
  (:require [clj-antlr.core :as antlr]
            [clojure.walk :as w]))


(def grammar
  "Elisp grammar."
  "grammar EmacsLisp ;

   root: any* EOF ;

   any: list | keyword | number | symbol | prefix | string | vector | char ;

   vector: '[' any* ']' ;

   list: '(' any* ')' ;

   prefix: quote | template | spread | hole ;

   quote: '#' ? '\\'' any ;

   template: '`' any ;

   spread: ',@' any ;

   hole: ',' any ;

   number: NUMB10 | NUMBX ;

   char: CHAR ;

   string: STRING ;

   keyword: KEYWORD ;

   symbol: IDENT ;

   comment: COMLINE ;

   CHAR: '?' ( ( '\\\\' ( 'C' | 'M' ) '-' ) | '\\\\' )? . ;

   STRING: '\"' ( '\\\\\\\\' | '\\\\\"' | . )*? '\"' ;

   NUMB10: [+-] ? ( ( D* '.' D+ ) | ( D+ '.' D* ) | D+ ) ;

   NUMBX: '#' ( 'b' | 'o' | 'x' | ( D+ 'r' ) ) [-+]? ( A | D )+ ;

   fragment
   D: '0'..'9' ;

   fragment
   A: 'a'..'z' ;

   KEYWORD: ':' IDENT ;

   IDENT: ( ( '\\\\' [\\\\[\\]() \\n\\t\\r\"',`] )+? |
            ( ~[[\\]() \\n\\t\\r\"',`] )+? )+ ;

   COMLINE: ';' ~[\\n\\r]* -> channel(HIDDEN) ;

   WS: [ \\n\\t\\r]+ -> channel(HIDDEN) ;")

(def elisp-str->parse-tree
  "Parses Emacs Lisp string into parse tree."
  (antlr/parser grammar))


(def text
  "Test text"
  ";; (1 2 3) foo
`,@foo #'baz
,@()
(1.0)
(defvar configuration-layer--refresh-package-timeout dotspacemacs-elpa-timeout
  \"Timeout in seconds to reach a package archive page.\")
,bar
:zzz
\"fff\"
     ((file-exists-p layer-dir)
      (configuration-layer/message
       (concat \"Cannot create configuration layer \\\"\\\", \"
               \"this layer already exists.\") name))
1011;; baz
   ;; Note:
(1+
;; bar
)
(+1.0 +2 .0 0.0.0 #24r5 #b0.0 #b111 '() 2+2 2 '2 +1.2b [])
              (let ((a [1 2 3])) a)
?b ?  ?? ?\\C-m ?\\\\  ( ?\\\\ )
'[1 2 3]
 ((equal event 32) ?')   ; space 
or()
\"\\\\\\\\\" a
\"'\"
bar'(foo) (quote 10)
\\\\\\\\\\[foo
;")

;; (elisp-str->parse-tree text)
