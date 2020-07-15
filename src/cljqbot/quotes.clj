(ns cljqbot.quotes
  (:require [clojure.string :as string]
            [clj-time.core :as t])
  (:import [clojure.lang IDeref]))


(defn cached
  "Returns an IDeref that holds a value for the given amount of time before
  recreating it by calling load-fn"
  [load-fn ttl-millis]
  (let [write-time (atom (Long/MIN_VALUE))                  ;; MIN_VALUE to force load on first deref
        cache (atom nil)]
    (reify IDeref
      (deref [_]
        (let [prev @write-time
              now (System/currentTimeMillis)]
          (if (and (> now (+ prev ttl-millis))
                   (compare-and-set! write-time prev now))
            (reset! cache (load-fn))
            @cache))))))


(defn ^:private fetch-quotes
  []
  (-> (slurp "https://github.com/Azel4231/clojure-quotes/raw/master/quotes.edn")
      (string/replace "#:clojure-quotes.core" "")
      read-string))


(def ^:private clj-quotes
  (cached fetch-quotes (-> 2 t/hours t/in-millis)))


(defn random-quote
  []
  (let [quotes @clj-quotes]
    (quotes (rand-int (count quotes)))))
