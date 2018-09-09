(ns spacedoc.data.node-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer :all]
            [spacedoc.data :as data]
            [spacedoc.data.node :refer :all]
            [clojure.spec.gen.alpha :as gen]))


(doall
 (for [v (vals (ns-publics 'spacedoc.data.node))
       :let [f-name (:name (meta v))]
       :when (function? (deref v))]
   (eval
    `(let [f-spec# (s/get-spec ~v)
           f-spec-args# (:args f-spec#)
           f-spec-ret# (:ret f-spec#)
           f-spec-desc# (some-> f-spec# s/form)]

       ;; All node constructors have specs?
       (deftest ~(symbol (str f-name "-has-spec"))
         (testing (str "Node constructor function \"" '~f-name "\" speced.")
           (is (s/spec? f-spec#)
               (format (str "Public function `%s` doesn't have spec\n"
                            "All public functions in the `spacedoc.data.node` "
                            " ns considered node constructors "
                            "and must be speced.\n")
                       '~f-name))
           (is (s/spec? f-spec-args#)
               (format "Function `%s` doesn't have :args spec.\n spec: \"%s\"\n"
                       '~f-name
                       f-spec-desc#))
           (is (and (s/spec? f-spec-ret#)
                    (not= 'clojure.core/any? (s/form f-spec-ret#)))
               (format (str "Function `%s` doesn't have :ret spec or "
                            "its spec is `any?`.\n spec: \"%s\"\n")
                       '~f-name
                       f-spec-desc#))))

       ;; [gentest] All node constructors produce valid values?
       (when (and f-spec-args# f-spec-ret#)
         (deftest ~(symbol (str f-name "-generates-valid-node"))
           (binding [s/*recursion-limit* 2]
             (let [ret-spec# (:ret f-spec#)
                   fail# (->> (s/exercise-fn ~v 10)
                              (filter #(->> %
                                            (second)
                                            (s/valid? ret-spec#)
                                            (false?)))
                              (first))]
               (is (nil? fail#)
                   (format (str "Function `%s` validation failed\n"
                                "With\n"
                                " arguments: %s\n"
                                " returned value: %s\n"
                                "Explanation:\n%s\n")
                           ~v
                           (vec (first fail#))
                           (second fail#)
                           (s/explain-str ret-spec# (second fail#))))))))))))
