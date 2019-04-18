(ns spacetools.spacedoc.config
  "Global configurations."
  (:require [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [orchestra.core :refer [defn-spec]]))


(def config-file-name
  "File name of configurations overrides file."
  "sdn_overrides.edn")


(def default-config
  "Default configurations."
  #::{:layers_org-valid-tags [{"chat" "Chats"}
                              {"checker" "Checkers"}
                              {"completion" "Completion"}
                              {"distribution" "Distributions"}
                              {"e-mail" "E-mail"}
                              {"emacs" "Emacs"}
                              {"framework" "Frameworks"}
                              {"fun" "Fun"}
                              {"i18n" "internationalization"}
                              {"markup" "Markup languages"}
                              {"misc" "Misc"}
                              {"os" "Operating systems"}
                              {"pairing" "Pair programming"}
                              {"lang" "Programming languages"}
                              {"extra" "Extra"}
                              {"dsl" "Domain-specific"}
                              {"lisp" "Lisp dialects"}
                              {"script" "Scripting"}
                              {"general" "General-purpose"}
                              {"Imperative" "Imperative"}
                              {"multi-paradigm" "Multi-paradigm"}
                              {"js" "JavaScript"}
                              {"pure" "Purely functional"}
                              {"versioning" "Source control"}
                              {"tagging" "Tags"}
                              {"theme" "Themes"}
                              {"tool" "Tools"}
                              {"vim" "Vim"}
                              {"web services" "Web services"}]
      :layers_org-structure [{"lang" ["pure"
                                      {"general" ["imperative" "multi-paradigm"]}]}
                             "markup"]
      :text-separators-rigth #{\space \! \' \( \tab \newline \, \. \‘ \: \; \{ \“ \\ \} \?}
      :text-separators-left #{\space \! \' \tab \) \newline \, \. \’ \: \; \{ \\ \” \} \?}
      :text-replacement-map {"\\r+" ""
                             "\\t" " "
                             "[ ]{2,}" " "
                             ;; Key-binding
                             "(?:(?i)(\\p{Blank}|\\p{Blank}\\p{Punct}+|^)(k){1}ey)(?:(?:(?i)[-_]*b)| B)(?:(?i)inding)((?i)s{0,1}(?:\\p{Blank}|\\p{Punct}+\\p{Blank}|\\p{Punct}+$|$))" "$1$2ey binding$3"
                             "((?i)k)ey bindingS" "$1ey bindings"}
      :link-custom-id-replacement-map {"(?i)([-]+|^|#)key(?:[_]*|-{2,})binding([s]{0,1})([-]+|$)" "$1key-binding$2$3"}
      :link-type->prefix {:file "file:"
                          :http "http://"
                          :https "https://"
                          :custom-id "#"
                          :ftp "ftp://"}
      :headline-max-depth 5
      :org-toc-max-depth 4
      :org-toc-template "Table of Contents                     :TOC_%s_gh:noexport:"
      :org-emphasis-tokens {:bold "*"
                            :italic "/"
                            :verbatim "="
                            :underline "_"
                            :kbd "~"  ;; Called code in the "classic" org.
                            :strike-through "+"}
      :org-block-indentation 2
      :org-table-indentation 0})


;; (s/def :layers_org/valid-tags (s/coll-of (s/map-of string? string?)
;;                                          :kind vector?))

;; (s/def :layers_org.structure )

;;   (s/def :layers_org/structure (s/coll-of
;;                                 (s/or :join :layers_org.structure/join
;;                                       :select :layers_org.structure/select)
;;                                 :kind vector?))


(s/def ::text-separators-rigth (s/coll-of char? :kind set?))

(s/def ::text-separators-left (s/coll-of char? :kind set?))

(s/def ::text-replacement-map (s/map-of string? string?))

(s/def ::link-custom-id-replacement-map (s/map-of string? string?))

(s/def ::link-type->prefix (s/map-of keyword? string?))

(s/def ::headline-max-depth nat-int?)

(s/def ::org-toc-max-depth nat-int?)

(s/def ::org-toc-template (s/and string? #(re-matches #".*%s.*" %)))

(s/def ::org-emphasis-tokens (s/map-of keyword? string?))

(s/def ::org-block-indentation nat-int?)

(s/def ::org-table-indentation nat-int?)

(s/def ::configs (s/keys :req [::text-separators-rigth
                               ::text-separators-left
                               ::text-replacement-map
                               ::link-custom-id-replacement-map
                               ::link-type->prefix
                               ::headline-max-depth
                               ::org-toc-max-depth
                               ::org-toc-template
                               ::org-emphasis-tokens
                               ::org-block-indentation
                               ::org-table-indentation]))

(s/def ::overriding-configs (s/keys :op [::text-separators-rigth
                                         ::text-separators-left
                                         ::text-replacement-map
                                         ::link-custom-id-replacement-map
                                         ::link-type->prefix
                                         ::headline-max-depth
                                         ::org-toc-max-depth
                                         ::org-toc-template
                                         ::org-emphasis-tokens
                                         ::org-block-indentation
                                         ::org-table-indentation]))


(def *configs
  "Configuration atom."
  (atom default-config))


(defn-spec valid-overrides? boolean?
  "Return true if CONFIGS is valid override configuration.
Same as `valid-configs?` but all elements of the CONFIGS map are optional."
  [configs any?]
  (s/valid? ::overriding-configs configs))


(defn-spec override-configs! ::configs
  "Apply OVERRIDES to the current  configuration atom."
  [overrides ::overriding-configs]
  (swap! *configs merge overrides))


(defn-spec regexp? boolean?
  "Return true if RE-PAT is a regex pattern."
  [re-pat any?]
  (instance? java.util.regex.Pattern re-pat))


;;;; General

(defn-spec seps-right ::text-separators-rigth
  "Return right separators."
  []
  (::text-separators-rigth @*configs))


(defn-spec seps-left ::text-separators-left
  "Return left separators."
  []
  (::text-separators-left @*configs))


(defn-spec text-rep-map (s/map-of regexp? string?)
  "Return text replacement map."
  []
  (reduce-kv (fn [m k v] (assoc m (re-pattern k) v))
             {}
             (::text-replacement-map @*configs)))


(defn-spec custom-id-link-rep-map (s/map-of regexp? string?)
  "Return custom-id links replacement map."
  []
  (reduce-kv (fn [m k v] (assoc m (re-pattern k) v))
             {}
             (::link-custom-id-replacement-map @*configs)))


(defn-spec link-type->prefix ::link-type->prefix
  "Given link type return corresponding prefix."
  []
  (::link-type->prefix @*configs))


(defn-spec link-types (s/coll-of keyword? :kind set?)
  "Return all types of links."
  []
  (-> (link-type->prefix) keys set))


(defn-spec max-headline-depth ::headline-max-depth
  "Return max depth(level) that headline can have."
  []
  (::headline-max-depth @*configs))


(defn-spec toc-max-depth ::org-toc-max-depth
  "Return depth after which TOC entries cut-off."
  []
  (::org-toc-max-depth @*configs))


(defn-spec toc-hl-val string?
  "Return standard headline for a TOC."
  []
  (format (::org-toc-template @*configs) (toc-max-depth)))


;;;; Org

(defn-spec emphasis-tokens ::org-emphasis-tokens
  "Return emphasis tokens of org-mode."
  []
  (::org-emphasis-tokens @*configs))


(defn-spec begin-end-indentation ::org-block-indentation
  "Return indentation of org-mode BEGIN-END blocks."
  []
  (::org-block-indentation @*configs))


(defn-spec table-indentation ::org-table-indentation
  "Return indentation of org-mode tables."
  []
  (::org-table-indentation @*configs))
