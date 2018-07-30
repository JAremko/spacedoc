#!/usr/bin/emacs --script
;;
;;; run.el -- Spacemacs documentation export runner -*- lexical-binding: t -*-
;;
;; Copyright (C) 2012-2018 Sylvain Benner & Contributors
;;
;; Author: Eugene "JAremko" Yaremenko <w3techplayground@gmail.com>
;; URL: https://github.com/syl20bnr/spacemacs
;; This file is not part of GNU Emacs.
;;
;; Note: see `sdnize-help-text' for usage.
;;
;;; License: GPLv3
;;; Commentary:
;;; Code:

(require 'json)
(require 'cl-lib)
(require 'subr-x)

(defconst sdnize-help-text
  (concat
   "Spacemacs documentation formatting tool\n"
   "=======================================\n"
   "First argument is the root directory (usually ~/.emacs.d/).\n"
   "It will be used to transform paths.\n"
   "The rest of arguments are \"*.org\" file paths or directories.\n"
   "Directories will be searched for *.org files.\n"
   "Files will be exported into \"export/target\" directory of the tool.\n"
   "Script can be called only with the first argument. In this case\n"
   "a default list of Spacemacs documentation files will be used.")
  "Help text for the script.")

(declare-function spacetools/find-org-files "shared.el" (paths))
(declare-function spacetools/get-cpu-count "shared.el" nil)
(declare-function spacetools/do-concurrently "shared.el" (files
                                                          w-count
                                                          w-path
                                                          sentinel
                                                          make-task))

(defconst sdnize-run-file-name
  (or load-file-name buffer-file-name)
  "Path to  run script of \"export\" tool.")

(defconst sdnize-run-file-dir
  (file-name-directory sdnize-run-file-name)
  "Path to \"export\" tool directory.")

(defconst sdnize-target-dir
  (concat sdnize-run-file-dir "target/")
  "Target directory for \"export\" tool.")

(load
 (expand-file-name
  "../lib/shared.el"
  sdnize-run-file-dir)
 nil t)
(defvar sdnize-root-dir ""
  "Root directory of the original documentation.")
(defvar sdnize-workers-fin 0
  "Number of Emacs instances that finished exporting.")
(defvar sdnize-stop-waiting nil
  "Used for blocking until all exporters have exited.")
(defvar sdnize-worker-count 0
  "How many workers(Emacs instances) should we use for exporting.")
(defvar sdnize-copy-queue '()
  "Queue of static dependencies to be copied to
the export dir.")
(defvar sdnize-default-exclude
  `("export/"
    "private/"
    "tests/"
    "elpa/"
    "layers/LAYERS.org")
  "List of Spacemacs directories and ORG files that normally
 shouldn't be exported.")

(defun sdnize/copy-file-to-target-dir (file-path)
  "Copy file at FILE-PATH into `sdnize-target-dir'.
ROOT-DIR is the documentation root directory. Empty FILE-PATH ignored."
  (unless (string-empty-p file-path)
    (let* ((op (file-relative-name file-path sdnize-root-dir))
           (np (expand-file-name op sdnize-target-dir))
           (np-dir (file-name-directory np)))
      (make-directory np-dir t)
      (message "Copying file %S into %S" file-path np)
      (copy-file file-path np t))))

(defun sdnize/sentinel (p e)
  "Sentinel for worker process."
  (condition-case err
      (let ((buff (process-buffer p)))
        (if (not (eq (process-status p) 'exit))
            (error "Process %s doesn't have status: exit" p)
          (sdnize/interpret-proc-output p buff)
          (kill-buffer buff))
        (if (string-match-p "finished" e)
            (progn
              (message "Process %s has finished.\n" p)
              (when (= (cl-incf sdnize-workers-fin)
                       sdnize-worker-count)
                (setq sdnize-stop-waiting t)))
          (error "Process %s was %s"
                 p e)
          (setq sdnize-stop-waiting t)))
    (error (setq sdnize-stop-waiting t)
           (error "%s" err))))

(defun sdnize/worker-msg-handler (resp)
  "Process payload received for a worker."
  (let ((type (alist-get 'type resp))
        ;; Unescape newlines inside payload.
        (text (replace-regexp-in-string
               "{{newline}}"
               "\n"
               (alist-get 'text resp))))
    (message
     "%s"
     (cond
      ((string= type "message")
       text)
      ((string= type "warning")
       (concat "\n=============== WARNING ===============\n"
               text
               "\n=======================================\n"))
      ((string= type "error")
       (concat "\n!!!!!!!!!!!!!!!! ERROR !!!!!!!!!!!!!!!!\n"
               text
               "\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"))
      ((string= type "export")
       (format
        (concat "File %S has static dependency %S\n"
                "=> it will be copied into the export directory")
        (alist-get 'source resp)
        (progn
          (push text sdnize-copy-queue)
          text)))
      (t
       (error
        "%s"
        (concat "\n?????????? UNKNOWN EVENT TYPE ????????????\n"
                (format "TYPE:\"%s\" TEXT: \"%s\"" type text)
                "\n?????????????????????????????????????????\n")))))))

(defun sdnize/interpret-proc-output (proc buff)
  "Parses process PROC BUFFER. Process P should be finished."
  (message "PROCESS: %S\n" proc)
  ;; We have one payload per line.
  (dolist (line (split-string (with-current-buffer buff (buffer-string)) "\n"))
    ;; Ignore junk.
    (unless (or (string= line "")
                (string-match-p "^Loading.*\\.\\.\\.$" line))
      ;; Parse payload
      (let ((resp (ignore-errors (json-read-from-string line))))
        (unless resp
          (error "Malformed response:%s" line))
        (sdnize/worker-msg-handler resp))))
  (while sdnize-copy-queue
    (sdnize/copy-file-to-target-dir
     (pop sdnize-copy-queue))))

(defun sdnize/build-default-list (root-dir)
  "Create default list of Spacemacs documentation files to export."
  (let ((exclude-re (regexp-opt (mapcar
                                 (apply-partially 'concat root-dir)
                                 sdnize-default-exclude))))
    (delete 0 (mapcar (lambda (path) (or (string-match-p exclude-re path) path))
                      (directory-files-recursively root-dir "\\.org$")))))

(defun sdnize/run (arg-list)
  "Main function for running as a script. ARG-LIST is an argument list.
See `sdnize-help-text' for description."
  (unless arg-list
    (error sdnize-help-text))
  (unless (file-directory-p (car arg-list))
    (error "The first argument must be a readable directory."))
  (setq sdnize-workers-fin 0
        sdnize-stop-waiting nil)
  (let* ((default-directory sdnize-run-file-dir)
         (w-path (progn (byte-compile-file "_worker.el")
                        (file-truename "_worker.elc")))
         (root-dir (file-truename (file-name-as-directory (pop arg-list))))
         (files (let ((default-directory root-dir))
                  (spacetools/find-org-files
                   (or arg-list
                       (sdnize/build-default-list root-dir)))))
         (f-length (length files))
         (w-count
          ;; FIXME: With 1-2  workers it gets extremely slow.
          (min (max 4 (spacetools/get-cpu-count)) f-length)))
    (if (= f-length 0)
        (progn (message "No files to export.")
               (kill-emacs 0))
      (setq sdnize-worker-count w-count
            sdnize-root-dir root-dir)
      (spacetools/do-concurrently
       files
       w-count
       w-path
       'sdnize/sentinel
       (lambda (f) (format "%S" `(sdnize/to-sdn ,root-dir ,sdnize-target-dir ',f))))
      (while (not sdnize-stop-waiting)
        (accept-process-output))
      (message "Done."))))

;; Script entry point.
(when (and load-file-name noninteractive)
  (sdnize/run argv))