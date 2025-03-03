(ns barf
  (:require [minestorm.core :as core]
            [minestorm.db :as db]
            [minestorm.explode :as expl])
  (:import [net.minestom.server MinecraftServer] ;minestorm
           [net.minestom.server.instance Instance]
           [net.minestom.server.entity Player]
           [net.minestom.server.event GlobalEventHandler]
           [net.minestom.server.event.player AsyncPlayerConfigurationEvent]
           [net.minestom.server.instance InstanceManager]
           [net.minestom.server.instance InstanceContainer]
           [net.minestom.server.instance.block Block]
           [net.minestom.server.coordinate Pos]
           [net.minestom.server.entity.attribute Attribute]
           [net.minestom.server.timer TaskSchedule]
                                        ;noise
           )
  )

(defn barftnt
  [^Player sender iters]

  (expl/summon-tnt ^Instance (.getInstance sender) ^Pos (.getPosition sender)
                   (let [pos (.direction (.getPosition sender))]
                     {:x (.x pos) :y (+ (.y pos) 0.5) :z (.z pos) :pitch (- 3 (rand-int 6)) :yaw (- 3 (rand-int 6))})
                   #(do
                      (db/set-prop! (.getUsername sender) :blocks-broken (+ (db/get-prop (.getUsername sender) :blocks-broken) %))
                      (db/set-prop! (.getUsername sender) :level (/ (Math/sqrt (db/get-prop (.getUsername sender) :blocks-broken)) 10))
                      (.setLevel sender (int (db/get-prop (.getUsername sender) :level)))
                      (.setExp sender (- (db/get-prop (.getUsername sender) :level) (int (db/get-prop (.getUsername sender) :level)))))
                   (db/get-prop (.getUsername sender) :power)
                   (db/get-prop (.getUsername sender) :dropstyle))
  (if (> iters 0)
    (.scheduleTask (.scheduler sender)
                     (reify Runnable
                       (run [this]
                         (barftnt sender (- iters 1))
                         )) (TaskSchedule/tick 1) (TaskSchedule/stop)))
  )

(db/set-prop! "hellandkeller" :power 4)
(barftnt (.getOnlinePlayerByUsername (MinecraftServer/getConnectionManager) "femboywifey") 500)


