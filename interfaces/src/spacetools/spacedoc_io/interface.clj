(ns spacetools.spacedoc-io.interface)


(defn *fp->sdn [path])
(defn *sdn-fps-in-dir [input-dir-path])
(defn *read-cfg-overrides [overrides-fp])
(defn *spit [path content])
(defn absolute [path])
(defn directory? [path])
(defn edn-file? [path])
(defn mkdir [path])
(defn rebase-path [old-base new-base path])
(defn sdn-file? [path])
(defn try-m->output [*output])
