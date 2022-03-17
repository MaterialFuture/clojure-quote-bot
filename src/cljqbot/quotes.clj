(ns cljqbot.quotes
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [tick.core :as t])
  (:import [clojure.lang IDeref]))


(defonce requested (atom 0)) ; how many quotes were delivered

(def quote-url "https://gist.githubusercontent.com/MaterialFuture/a16f23ffc73cd0a0b00a4a5a8033e53b/raw/56bf3ea6adf7049dd04286d8affc98a2526ff851/quotes.edn")
;; Quotes are already parsed as needed


(defn cached
  "Returns an IDeref that holds a value for the given amount of time before
  recreating it by calling load-fn"
  [load-fn ttl-millis]
  (let [write-time (atom (Long/MIN_VALUE)) ;; MIN_VALUE to force load on first deref
        cache (atom nil)]
    (reify IDeref
      (deref [_]
        (let [prev @write-time
              now (System/currentTimeMillis)]
          (if (and (> now (+ prev ttl-millis))
                   (compare-and-set! write-time prev now))
            (reset! cache (load-fn))
            @cache))))))


(defn ^:private trim-strings
  [quotes]
  (walk/postwalk
    #(if (string? %) (string/trim %) %)
    quotes))


(defn ^:private fetch-quotes
  []
  (let [quotes (-> (slurp quote-url) ;pull from quote-url
                   read-string
                   trim-strings)]
    {:all       quotes
     :by-author (group-by #(string/lower-case (:quotee %)) quotes)})) ;;TODO: make use of this by adding the capability of requesting a quote by someone


(def ^:private clj-quotes
  (cached fetch-quotes (-> 24 (t/new-duration :hours) t/millis)))


(defn random-quote
  []
  (let [quotes (:all @clj-quotes)]
    (swap! requested inc)
    (quotes (rand-int (count quotes)))))


(comment

  (def quotes (:all (fetch-quotes)))

  )
