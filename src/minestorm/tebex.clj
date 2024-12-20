(ns minestorm.tebex
  (:require [minestorm.constants :as consts]
            [minestorm.db :as db]
            [clj-http.client :as client]
            )
  (:import [net.minestom.server MinecraftServer] ;minestorm
           [net.minestom.server.instance Instance InstanceContainer InstanceManager IChunkLoader]
           [net.minestom.server.event GlobalEventHandler]
           [net.minestom.server.event.player AsyncPlayerConfigurationEvent]
           [net.minestom.server.instance.block Block]
           [net.minestom.server.coordinate Vec Pos Point]
           [net.minestom.server.instance.batch BatchOption AbsoluteBlockBatch]
                                        ; entitys
           [net.minestom.server.entity EntityType Entity Player EntityCreature]
           
           [net.minestom.server.utils.time TimeUnit]
           [net.minestom.server.entity.metadata.display BlockDisplayMeta]
           [net.minestom.server.timer TaskSchedule]
           [net.minestom.server.item ItemStack Material]
                                        ;fastnoise
           [net.kyori.adventure.resource ResourcePackRequest ResourcePackRequest$Builder ResourcePackInfo ResourcePackInfoLike]
           [net.minestom.server.instance.anvil AnvilLoader]
                                        ;raycast
           [dev.emortal.rayfast.casting.grid GridCast]
           [dev.emortal.rayfast.vector Vector3d]
           [net.worldseed.multipart ModelEngine]
           [net.minestom.server.timer TaskSchedule]
                                        ;java 
           ))
(def secret "ffe3ed14674f6450193e2a7ec5dcc21c09fb4e44")

(defn handleplayers
  []
  (let [request (:body (client/get "https://plugin.tebex.io/queue"
                                    {:headers {"X-Tebex-Secret" secret} :as :json}))
        players (:players
                 request)]
    (println request)
    
    
    (doseq [player players]
      (doseq [command (:commands (:body (client/get (str "https://plugin.tebex.io/queue/online-commands/" (:id player))
                                                    {:headers {"X-Tebex-Secret" secret} :as :json})))]
        (try
          (println command)
          (eval (read-string (.replace (:command command) "{username}" (str "\""(:name player) "\""))))
          (client/delete "https://plugin.tebex.io/queue" {:headers {"X-Tebex-Secret" secret}
                                                          :form-params {:ids [(:id command)]} :content-type :json
                                                          })
          (catch Exception e (println (str "caught exception while giving item: " (.getMessage e)))))
        )
      )
    (.scheduleTask (MinecraftServer/getSchedulerManager)
                   (reify Runnable
                     (run [this]
                       (handleplayers)
                       this
                       ))
                   (TaskSchedule/seconds (long (:next_check (:meta request))))
                   (TaskSchedule/park))
    nil)
  nil)
;(handleplayers)





;(db/set-prop! "femboywifey" :blocks-broken 0)



