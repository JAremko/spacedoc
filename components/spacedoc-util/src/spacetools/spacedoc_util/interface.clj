(ns spacetools.spacedoc-util.interface
  (:require [spacetools.spacedoc-util.core :as u]))

(def seps u/seps)
(def seps-right u/seps-right)
(def seps-left  u/seps-left)
(def link-type->prefix u/link-type->prefix)
(def link-types u/link-types)
(def toc-max-depth u/toc-max-depth)
(def toc-hl-val u/toc-hl-val)

(defn node->children-tag-s [node] (u/node->children-tag-s node))
(defn in-hl-level-range? [level] (u/in-hl-level-range? level))
(defn register-node! [tag spec-k] (u/register-node! tag spec-k))
(defn node->spec-k [node] (u/node->spec-k node))
(defn tag->spec-k [node-tag] (u/tag->spec-k node-tag))
(defn all-tags [] (u/all-tags))
(defn known-node? [tag] (u/known-node? tag))
(defn link->link-prefix [path] (u/link->link-prefix path))
(defn fmt-problem [node problem] (u/fmt-problem node problem))
(defn explain-deepest [node] (u/explain-deepest node))
(defn relation [parent] (u/relation parent))
(defn relations [parents] (u/relations parents))
(defn same-row-child-count? [rows] (u/same-row-child-count? rows))
(defn non-blank-string? [s] (u/non-blank-string? s))
(defn hl-val->gh-id-base [hl-value] (u/hl-val->gh-id-base hl-value))
(defn hl-val->path-id-frag [hl-value] (u/hl-val->path-id-frag hl-value))
(defn path-id? [val] (u/path-id? val))
(defn up-tags [spaceroot file root-node] (u/up-tags spaceroot file root-node))
(defn assoc-level-and-path-id
  ([node] (u/assoc-level-and-path-id node))
  ([parent-node node] (u/assoc-level-and-path-id parent-node node)))
(defn root-node? [node] (u/root-node? node))
(defn fmt-str [rep-map text] (u/fmt-str rep-map text))
(defn fmt-text [text] (u/fmt-text text))
(defn fmt-link [link-type link] (u/fmt-link link-type link))
(defn fmt-hl-val [hl-val] (u/fmt-hl-val hl-val))
(defn indent [indent-level s] (u/indent indent-level s))
