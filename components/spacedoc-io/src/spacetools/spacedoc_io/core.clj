(ns spacetools.spacedoc-io.core
  "File-system I/O. Functions that can fail return `cats.monad.exception`."
  (:require [cats.core :as m]
            [cats.monad.exception :as exc]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [nio2.core :as nio]
            [orchestra.core :refer [defn-spec]]
            [spacetools.fs-io.interface
             :refer [filesystem exception-of? file-ref?] :as fio]
            [spacetools.spacedoc.interface :as sd :refer [valid-root?]]))


(defn-spec *fp->sdn (exception-of? map?)
  "Read and validate .SDN file."
  ([path file-ref?]
   (*fp->sdn :spacetools.spacedoc.node/root path))
  ([root-node-spec (s/or :spec s/spec? :spec-ref qualified-ident?)
    path file-ref?]
   (io! (exc/try-or-recover
         (with-open [input (->> path
                                fio/file-ref->path
                                nio/buffered-reader
                                io/reader
                                java.io.PushbackReader.)]
           (let [[obj fin] (repeatedly 2 (partial edn/read {:eof :fin} input))]
             (cond
               (not= :fin fin)
               (throw (Exception. ".SDN file should contain single root form."))
               (not= :root (:tag obj))
               (throw (Exception. "Non-root top level node in .SDN file."))
               ((complement s/valid?) root-node-spec obj)
               (throw (ex-info "Validation filed." (sd/explain-deepest obj)))
               :else obj)))
         (fn [^Exception err]
           (exc/failure
            (ex-info (.getMessage err) (merge {:file path} (ex-data err)))))))))


(defn-spec *read-cfg-overrides (exception-of? map?)
  "Read and validate configuration overrides from a PATH file."
  [path file-ref?]
  (io! (exc/try-or-recover
        (if (fio/edn-file? path)
          (with-open [input (->> path
                                 fio/file-ref->path
                                 nio/buffered-reader
                                 io/reader
                                 java.io.PushbackReader.)]
            (let [cfg-ovr (edn/read input)]
              (if (sd/valid-overrides? cfg-ovr)
                cfg-ovr
                (throw
                 (ex-info
                  "Invalid overrides"
                  {:explanation (s/explain-data
                                 :spacetools.spacedoc.config/overriding-configs
                                 cfg-ovr)})))))
          (ex-info "Not an edn-file" {:path (str path)}))
        (fn [^Exception err]
          (exc/failure (ex-info "Can't read configuration overrides file"
                                {:path path :error err}))))))


(defn-spec rm-file-prefix string?
  [path string?]
  (str/replace path #"^file:" ""))


(defn-spec add-file-prefix string?
  [path string?]
  (str "file:" path))


(defn-spec re-relativize file-ref?
  [path file-ref? old-root file-ref? other file-ref?]
  (fio/relativize path (fio/join (fio/parent old-root) other)))


;; TODO: Mb also change :spaceroot or how its called?
(defn-spec re-root-sdn valid-root?
  [root-dir file-ref? path file-ref? sdn valid-root?]
  (assoc sdn
         :source
         (-> root-dir
             (re-relativize path (rm-file-prefix path))
             (str/replace #"(?ix)\.sdn$" ".org"))
         :root-dir root-dir))


(defn-spec fix-relative-links valid-root?
  [root-dir file-ref? path file-ref? sdn valid-root?]
  ((fn inner [f-p sdn]
     (if (s/valid? :spacetools.spacedoc.node/link sdn)
       (condp = (:type sdn)
         :file (update sdn :path #(->> %
                                       rm-file-prefix
                                       (re-relativize root-dir f-p)
                                       add-file-prefix))
         :custom-id  (assoc sdn
                            :path (add-file-prefix (:source sdn))
                            :type :file)
         sdn)
       (update sdn :children (partial mapv (partial inner path)))))
   path sdn))


(defn-spec rebase-sdn valid-root?
  [old-root-dir file-ref? new-root-dir file-ref? sdn valid-root?]
  (->> sdn
       (re-root-sdn new-root-dir old-root-dir)
       (fix-relative-links new-root-dir old-root-dir)))
