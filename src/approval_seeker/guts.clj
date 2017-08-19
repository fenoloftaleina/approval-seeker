(ns approval-seeker.guts
  (:require [clj-http.client :as client])
  (:use [opennlp.nlp]
        [hickory.core]
        [me.raynes.conch :refer [programs] :as sh]))

(def get-sentences (make-sentence-detector "en-sent.bin"))
(programs git)

(defn fetch-website [url]
  (:body (client/get url)))

(defn first-from-content [content]
  (-> content
      (first)
      (:content)))

(defn html-content [node]
  (first-from-content (:content node)))

(defn body-content [content]
  (->> content
       (filter #(= (:tag %) :body))
       (first-from-content)))

(defn list-of-strings [content]
  (cond
    (string? content) content
    (map? content) (list-of-strings (:content content))
    :else (map list-of-strings content)))

(defn cleanup [s]
  (clojure.string/replace s #"(\n\r|\n|\r)+" " "))

(defn text [content]
  (->> content
      (list-of-strings)
      (flatten)
      (clojure.string/join "")
      (cleanup)))

(defn tokenize-into-sentences [content]
  (-> content
      (parse)
      (as-hickory)
      (html-content)
      (body-content)
      (text)
      (get-sentences)))

(defn commit [sentence]
  (git "commit" "--allow-empty" "-m" sentence))

(defn commit-all [sentences]
  (doseq [sentence sentences]
    (commit sentence))
  (git "push"))

(defn seek-approval [url]
  (-> url
    (fetch-website)
    (tokenize-into-sentences)
    (reverse)
    (commit-all)))
