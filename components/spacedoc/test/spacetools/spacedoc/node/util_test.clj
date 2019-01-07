(ns spacetools.spacedoc.node.util-test
  "All public function in `spacetools.spacedoc.node` ns are node constructors.
  So we simply select them and generate tests based on node specs."
  (:require [clojure.core.reducers :as r]
            [clojure.data :refer [diff]]
            [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [orchestra.core :refer [defn-spec]]
            [orchestra.spec.test :as st]
            [spacetools.spacedoc.config :as cfg]
            [spacetools.spacedoc.node :as n] ;; Ensure node spec generation.
            [spacetools.spacedoc.node.util :refer :all]
            [spacetools.test-util.interface :as tu]))


(defn-spec depth->headline :spacetools.spacedoc.node/headline
  "Make headline dummy of given DEPTH and WIDTH."
  [depth nat-int? width nat-int?]
  (letfn [(hl-tmpl [level]
            {:tag :headline
             :todo? false
             :value (format "headline of depth: [%s]" level)
             :children []})
          (btm-tmpl [width]
            (->> {:tag :headline :value "edge" :todo? true :children []}
                 (repeat)
                 (take width)
                 (vec)))]
    (if (or (<= depth 1) (< width 1))
      (first (btm-tmpl 1))
      ((fn rec [cur-d cur-node]
         (assoc cur-node
                :children
                (if (< cur-d depth)
                  (r/reduce (r/monoid conj vector)
                            (->> (hl-tmpl cur-d)
                                 (repeat)
                                 (take width)
                                 (r/map (partial rec (inc cur-d)))))
                  (btm-tmpl width))))
       2 {:tag :headline :todo? false :value "edge"}))))


(defn-spec hl-problems (s/nilable (s/coll-of map? :min-count 1))
  "Return true if X is a valid headline."
  [x any?]
  (:clojure.spec.alpha/problems
   (s/explain-data :spacetools.spacedoc.node/headline x)))


(st/instrument)


;; Those are pretty naive but fast tests just to make sure that stuff isn't
;; obviously broken. We use generative testing to catch non-trivial errors.
(deftest headline-utility-value-test
  (testing "headline? function"
    (testing "Headlines are headlines."
      (is (every? headline? [(-> "foo"
                                 (n/todo))
                             (->> "bar"
                                  (n/text)
                                  (n/paragraph)
                                  (n/section)
                                  (n/headline "baz"))
                             (-> (n/text "qux")
                                 (n/paragraph)
                                 (n/section)
                                 (n/description))])))
    (testing "Other nodes aren't headlines"
      (is (every? (complement headline?)
                  [(n/text "foo")
                   (n/src "bar" "baz")
                   (n/italic (n/text "qux"))]))))
  (testing "headline->depth function"
    (is (= (headline->depth (n/headline "foo" (n/todo "bar")))
           2))
    (is (= (headline->depth (n/todo "foo"))
           1)))
  (testing "clamp-headline-children function"
    (let [hl-2-lvl (n/todo "foo" (n/todo "bar"))
          hl-1-lvl (n/todo "foo")]
      (is (not= hl-2-lvl hl-1-lvl) "Sanity check")
      (is (= (clamp-headline-children 2 hl-2-lvl)
             hl-2-lvl))
      (is (= (clamp-headline-children 1 hl-2-lvl)
             hl-1-lvl))))
  (testing "mark-empty-as-todo function"
    (let [hl-2-lvl (depth->headline 2 1)
          hl-1-lvl-trimmed (assoc hl-2-lvl :children [])
          hl-1-lvl-todo (depth->headline 1 1)]
      (testing "Sanity"
        (is (not= hl-2-lvl hl-1-lvl-todo))
        (is (not= hl-2-lvl hl-1-lvl-trimmed))
        (is (not= hl-1-lvl-todo hl-1-lvl-trimmed)))
      (is (= (mark-empty-as-todo hl-1-lvl-trimmed)
             hl-1-lvl-todo))
      (is (= (mark-empty-as-todo hl-2-lvl)
             hl-2-lvl)))))


;; Make sure that all public functions in `spacetools.spacedoc.node.util`
;; name-space are fully speced.
(doall
 (for [v (vals (ns-publics 'spacetools.spacedoc.node.util))
       :let [f-name (str (:name (meta v)))]
       :when (function? (deref v))]
   (eval
    `(let [f-spec# (s/get-spec ~v)
           f-spec-args# (:args f-spec#)
           f-spec-ret# (:ret f-spec#)
           f-spec-desc# (some-> f-spec# s/form)]

       ;; All public functions properly speced?
       (deftest ~(symbol (str f-name "-has-spec"))
         (testing (str "Public function \"" ~v "\" properly speced.")
           (is (s/spec? f-spec#)
               (format (str "Public function \"%s`\" doesn't have spec.\n") ~v))
           (is (s/spec? f-spec-args#)
               (format "Function `%s` doesn't have :args spec.\n spec: \"%s\"\n"
                       ~v f-spec-desc#))
           (is (and (s/spec? f-spec-ret#)
                    (not= 'clojure.core/any? (s/form f-spec-ret#)))
               (format (str "Function `%s` doesn't have :ret spec or"
                            " its spec is `any?`.\n spec: \"%s\"\n")
                       ~v f-spec-desc#))))))))


(deftest headline-utility-gentest
  (defspec valid-headline-gentest
    {:num-tests (tu/samples 10)}
    (prop/for-all
     [headline (gen/no-shrink (s/gen :spacetools.spacedoc.node/headline))]
     (testing "Generate valid headline"
       (is (empty? (hl-problems headline))
           "We generated valid headline."))
     (testing "headline? function"
       (is (headline? headline)
           "Valid headline is headline."))
     (testing "todo-or-has-children? function"
       (is (todo-or-has-children? headline)
           "Valid headline should have children or b marked as todo."))
     (testing "headline->depth function"
       (is (>= (cfg/max-headline-depth) (headline->depth headline) 1)
           "Valid headline depth should be in acceptable range."))
     (testing "clamp-headline-children function"
       (let [[removed added same] (diff headline
                                        (clamp-headline-children
                                         (inc (cfg/max-headline-depth))
                                         headline))]
         (is (every? nil? [removed added])
             "Clamping children to level over upper bound should do nothing."))
       (is (nil? (hl-problems (clamp-headline-children
                               (cfg/max-headline-depth)
                               headline)))
           "headline clamped to max valid level should also be valid.")
       (is (->> headline (clamp-headline-children 1)
                (:children)
                (filter headline?)
                (empty?))
           "headline clamped to 1 level shouldn't have headline children.")
       (let [[removed added same] (diff headline
                                        (clamp-headline-children
                                         (headline->depth headline)
                                         headline))]
         (is (every? nil? [removed added])
             "headline clamped to its original level should be the same.")))
     (testing "mark-empty-as-todo function"
       (let [empty-headline (assoc headline :children [])]
         (is (:todo? (mark-empty-as-todo empty-headline))
             "Empty headline should be marked as todo.")))
     (testing "fmt-headline function"
       (let [empty-headline (assoc headline :children [])]
         (is (empty? (hl-problems (fmt-headline 1 empty-headline)))
             "Formatting empty headline should make it valid."))
       (let [[removed added same] (diff headline
                                        (fmt-headline
                                         (headline->depth headline)
                                         headline))]
         (is (every? nil? [removed added])
             "Formatting valid headline should do nothing."))))))
