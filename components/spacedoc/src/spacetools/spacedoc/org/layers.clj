(ns spacetools.spacedoc.org.layers
  "layers.org generator."
  (:require [clojure.core.reducers :as r]
            [clojure.spec.alpha :as s]
            [orchestra.core :refer [defn-spec]]
            [spacetools.spacedoc.config :as cfg]
            [spacetools.spacedoc.node :as n]
            [spacetools.spacedoc.node.val-spec :as vs]
            [spacetools.spacedoc.org.orgify :refer [sdn->org]]))


(defn-spec root->description (s/nilable :spacetools.spacedoc.node/headline)
  [node :spacetools.spacedoc.node/root]
  (->> node
       :children
       (filter (partial s/valid? :spacetools.spacedoc.node.meta/description))
       first))


(defn-spec describe :spacetools.spacedoc.node/headline
  [node :spacetools.spacedoc.node/root]
  (n/headline (:title node)
              (:children (root->description node)
                         (->> (str "If you read this,"
                                   " pleas add \"Description\" headline"
                                   " to the layer's README.org file.")
                              n/text
                              n/bold
                              n/paragraph
                              n/section))
              (-> (:source node)
                  n/section
                  n/paragraph
                  (n/link (n/text "link")))))


(defn-spec layers-sdn (s/nilable :spacetools.spacedoc.node/root)
  "Create layers.org SDN with QUARY from a seq of sdn DOCS."
  [quary :spacetools.spacedoc.config/layers-org-quary
   docs :spacetools.spacedoc.node/root]
  (when docs
    (apply n/root "Configuration layers" #{}
           (:children
            ((fn walk [ds node]
               (let [tag (if (map? node)
                           (ffirst node)
                           node)]
                 (when-let [f-ds (seq (filter #((:tags %) tag) ds))]
                   (if (string? node)
                     (apply n/headline tag (mapv #(describe %) f-ds))
                     (->> node
                          first
                          val
                          (map (partial walk f-ds))
                          (apply n/headline tag))))))
             docs {"layer" quary})))))


#_ (def documents
     [
      (n/root "asm" #{"layer" "general" "lang" "imperative"}  (n/todo "FOO"))

      (n/root "forth" #{"layer" "general" "imperative" "lang"}  (n/todo "BAR"))

      (n/root "agda" #{"layer" "pure" "lang"}  (n/todo "BAZ"))

      (n/root "c-c++" #{"layer" "general" "lang" "multi-paradigm"}  (n/todo "QUX"))

      (n/root "html" #{"layer" "markup"}  (n/todo "QUUX"))


      (n/root "markdown" #{"layer" "markup"}  (n/todo "QUUXY"))

      ])

#_ (tree->sdn (cfg/layers-org-quary) documents)

#_ (s/explain-str :spacetools.spacedoc.node/root (tree->sdn (cfg/layers-org-quary) documents))

#_ (spit "/tmp/test.org" (sdn->org (tree->sdn (cfg/layers-org-quary) documents)))
