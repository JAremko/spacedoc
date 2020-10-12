(ns spacetools.observatory-cli.run
  (:gen-class)
  (:require [clojure.core.reducers :as r]
            [spacetools.observatory-cli.elisp.core :as el]
            [spacetools.observatory-cli.elisp.keybinding :as kb]))


(defn -main [dir target-f & args]
  (printf "Dumping legacy bindings from dir \"%s\" to \"%s\" file...\n"
          dir target-f)
  (->> dir
       clojure.java.io/file
       file-seq
       (keep (comp (partial re-matches #".*\.el$") str))
       (r/fold (r/monoid #(->> %2
                               slurp
                               el/read-str
                               kb/collect-legacy-bindings
                               (vector %2)
                               (conj %1))
                         vector))
       (remove (complement (comp seq second)))
       vec
       (spit target-f))
  (prn "Done!"))
