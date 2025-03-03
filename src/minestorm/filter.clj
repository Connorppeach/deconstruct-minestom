(ns minestorm.filter
  (:require [minestorm.constants :as consts]
            [minestorm.db :as db]
            [clojure.core.reducers :as reducers]);
  (:import
   [net.minestom.server.entity Player]
   [net.minestom.server.event.player PlayerChatEvent]
   [net.kyori.adventure.text Component]
   [net.kyori.adventure.text.format TextColor]
   [java.util.zip GZIPOutputStream GZIPInputStream]
   [java.io ByteArrayInputStream BufferedReader ByteArrayOutputStream]))
(set! *warn-on-reflection* true)

(def filter (atom nil))
(def trainingset (atom nil))

(defn compress
  [str]
  (let [barray (ByteArrayOutputStream.)]
    (let [gzstream (GZIPOutputStream. barray)]
      (.write gzstream (.getBytes ^String str))
      (.flush gzstream)
      (.close gzstream)
      (float (count (.toByteArray barray))))))


(defn ncd [train+message messagec trainc] (/ (- train+message (min messagec trainc)) (max messagec trainc)))
(defn cdm [train+message messagec trainc]  (/ train+message (+ messagec trainc)))
(def dist ncd)

(defn mkfilter
  []
  (reset! trainingset (for [i  (.split ^String (slurp "profanity_en.csv") "\n")] (vec (.split ^String i ","))))
  (reset! filter (reify
                   java.util.function.Consumer
                   (accept [this event]
                     (let [event ^PlayerChatEvent event
                           message (apply str (dedupe (.getMessage event)))
                           p (.getPlayer event)
                           messagec (compress message)
                           bad (first (sort #(compare (second %) (second %2))
                                            (for [t @trainingset]
                                              (let [trainc (compress (nth t 0))
                                                    train+message (compress (str message " " (nth t 0)))
                                                    wholesentance (float (dist train+message messagec trainc))]
                                                [(nth t 0) (min wholesentance (apply min (for [word (seq (.split ^String message " "))]
                                                                                           (let [wordc (compress word)
                                                                                                 train+word (compress (str word " " (nth t 0)))]
                                                                                             
                                                                                             (float (dist train+word wordc trainc))))))
                                                 (nth t 4) (nth t 5) (nth t 6) (nth t 7)]))))
                           trust (db/get-prop (.getUsername p) :trust)]
                       (println (str (.getUsername p) " said " message " and got flagged for " bad " with a trust score of " trust))
                       (if (< (second bad) trust)
                         (do
                           (if (> 1.26 (second bad))
                               (db/set-prop! (.getUsername p) :trust (min (* (+ trust 0.01) (max (- (Float/parseFloat (nth bad 5)) 0.4) 1)) 0.161)))
                           (println (str (.getUsername p) " said " message " and got flagged for " bad))
                           (.sendMessage p (Component/text "potential profanity detected, your message was not sent" (TextColor/color 120 0 0)))
                           (.setCancelled event true))
                         (db/set-prop! (.getUsername p) :trust (max (- trust 0.001) 0.14)))
                       )))))




