(ns code-maat.parsers.svn
  (:use [clojure.data.zip.xml :only (attr text xml-> xml1->)]) ; dep: see below
  (:require [incanter.core :as incanter]
            [clojure.xml :as xml]
            [clojure.zip :as zip]))

(defn zip->log-entries [zipped]
  (xml-> zipped :logentry))

(defn- make-extractor [logentry]
  (partial xml1-> logentry))

(defn- extract-modified-files [logentry]
  "Extracts all modified files from the given logentry."
  (let [paths (xml-> logentry :paths :path)
        files (filter #(= "file" (attr % :kind)) paths)
        modified-files (filter #(= "M" (attr % :action)) files)] ;TODO: extract C too and tag!
    (map text modified-files)))

(defn as-rows [svn-logentry]
  "Transforms the given svn logentry to a seq of rows containing
   the modification data for each entity."
  (let [extractor (make-extractor svn-logentry)
        entities (extract-modified-files svn-logentry)
        date (extractor :date text)
        author (extractor :author text)
        revision (extractor (attr :revision))]
    (for [e entities
          :let [row {:entity e :date date :author author :rev revision}]]
      row)))

(defn zip->modification-sets [zipped]
  "Transforms the given zipped svn log into an Incanter
   dataset of modification data."
  (incanter/to-dataset
   (flatten
    (map as-rows (zip->log-entries zipped)))))