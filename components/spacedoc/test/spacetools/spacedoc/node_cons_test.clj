(ns spacetools.spacedoc.node-cons-test
  "All public function in `spacetools.spacedoc.node` ns are node constructors.
  So the code simply select them and generate tests based on node specs."
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [orchestra.spec.test :as st]
            [spacetools.spacedoc.core :as sc]
            [spacetools.spacedoc.node :refer :all]
            [spacetools.test-util.interface :as tu]))


(st/instrument)

(deftest sanity-test
  (testing "Nodes are defined"
    (is (seq (sc/all-tags)))))

;; Grab all public functions in `spacetools.spacedoc.node`
;; and their specs. Then run it through generative testing.
(doall
 (for [v (vals (ns-publics 'spacetools.spacedoc.node))
       :let [f-name (str (:name (meta v)))]
       :when (function? (deref v))]
   (eval
    `(let [f-spec# (s/get-spec ~v)
           f-spec-args# (:args f-spec#)
           f-spec-ret# (:ret f-spec#)
           f-spec-desc# (some-> f-spec# s/form)]

       ;; All node constructors have specs?
       (deftest ~(symbol (str f-name "-has-spec"))
         (testing (str "Node constructor function \"" ~v "\" speced.")
           (is (s/spec? f-spec#)
               (format
                (str "Public function `%s` doesn't have spec\n"
                     "All public functions in the `spacetools.spacedoc.node`"
                     " ns considered node constructors "
                     "and must be speced.\n")
                ~v))
           (is (s/spec? f-spec-args#)
               (format "Function `%s` doesn't have :args spec.\n spec: \"%s\"\n"
                       ~v
                       f-spec-desc#))
           (is (and (s/spec? f-spec-ret#)
                    (not= 'clojure.core/any? (s/form f-spec-ret#)))
               (format (str "Function `%s` doesn't have :ret spec or "
                            "its spec is `any?`.\n spec: \"%s\"\n")
                       ~v
                       f-spec-desc#))))

       ;; [gentest] All node constructors produce valid values?
       (when (and f-spec-args# f-spec-ret#)
         (binding [s/*recursion-limit* 2]
           (defspec ~(with-meta (symbol (str f-name "-gen")) {:slow :true})
             {:num-tests ~(tu/samples 10)
              :reporter-fn (tu/make-f-spec-reper f-spec-ret# ~v ~f-name)}
             (testing "The function always returns valid result"
               (prop/for-all
                [args# (-> f-spec-args#
                           (s/gen)
                           (gen/no-shrink))]
                (s/valid? f-spec-ret# (apply ~v args#)))))))))))
