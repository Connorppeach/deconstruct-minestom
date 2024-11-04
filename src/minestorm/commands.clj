(ns minestorm.commands
  (:require [minestorm.gui :as gui]
            [minestorm.constants :as consts]
            [minestorm.imanager :as iman]
            [minestorm.plots :as pworld]
            [minestorm.db :as db]
            [minestorm.steve :as steve])

  (:import [net.minestom.server MinecraftServer] ;minestorm
           [net.minestom.server.instance Instance InstanceContainer InstanceManager IChunkLoader]
           [net.minestom.server.entity Player]
           [net.minestom.server MinecraftServer]
           [net.minestom.server.coordinate Pos]
           [net.minestom.server.entity.attribute Attribute]
           
           [net.minestom.server.command.builder CommandExecutor Command CommandContext]
           [net.minestom.server.command CommandSender]))

(defn init
  [^InstanceManager iManager]
  (def cmanager (MinecraftServer/getCommandManager))
  
  (.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["size"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                   (let [sender ^Player  sender context ^CommandContext context]
                                                     (if (or (.equals "femboywifey" (.getUsername (.asPlayer sender))) (.equals "Openmindedness" (.getUsername (.asPlayer sender))))
                                                       (reset! consts/power (Integer/parseInt (nth (.split (.getInput context) " ") 1))))))))
               ^Command p))

  *(.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["spawnmob"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                   (let [sender ^Player  sender]
                                                     (steve/mk ^Instance (.getInstance sender) ^Pos (.getPosition sender) (str (nth (.split (.getInput context) " ") 1) ".bbmodel"))))))
               ^Command p))
  
  (.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["save"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                   (if (or (.equals "femboywifey" (.getUsername (.asPlayer sender))) (.equals "Openmindedness" (.getUsername (.asPlayer sender))))
                                                   (.saveChunksToStorage ^Instance (.getInstance (.asPlayer sender)))))))
               ^Command p))

  (.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["gui"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                   (.openInventory (.asPlayer sender) (gui/mainmenu #(.closeInventory %)))
                                                   )))
               ^Command p))
    (.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["spawn"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                   (.get (.teleport (.asPlayer sender) (Pos. 0.0 100.0 0.0 0.0 0.0)))
                                                   )))
               ^Command p))

  (.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["ban"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                   (if (or (.equals "femboywifey" (.getUsername (.asPlayer sender))) (.equals "Openmindedness" (.getUsername (.asPlayer sender))))
                                                     
                                                     (do (db/set-prop! (nth (.split (.getInput context) " ") 1) :banned true)
                                                         (.kick (.getOnlinePlayerByUsername (MinecraftServer/getConnectionManager) (nth (.split (.getInput context) " ") 1))
                                                                "BANNED(womp womp)"
                                                                )))
                                                   
                                                   )))
               ^Command p))
(.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["stop"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                   (if (or (.equals "femboywifey" (.getUsername (.asPlayer sender))) (.equals "Openmindedness" (.getUsername (.asPlayer sender))))

                                                     (MinecraftServer/stopCleanly))
                                                   
                                                   )))
               ^Command p))
  (.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["unban"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                   (if (or (.equals "femboywifey" (.getUsername (.asPlayer sender))) (.equals "Openmindedness" (.getUsername (.asPlayer sender))))

                                                     (db/set-prop! (nth (.split (.getInput context) " ") 1) :banned false))
                                                   
                                                   )))
               ^Command p))
  
  (.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["tp"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                   (if (or (.equals "femboywifey" (.getUsername (.asPlayer sender))) (.equals "Openmindedness" (.getUsername (.asPlayer sender))))
                                                     (.get (.teleport (.asPlayer sender) (.getPosition (.getOnlinePlayerByUsername (MinecraftServer/getConnectionManager) (nth (.split (.getInput context) " ") 1)))))
                                                     )
                                                   
                                                   )))
               ^Command p))
  (.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["psize"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                   (if (or (.equals "femboywifey" (.getUsername (.asPlayer sender))) (.equals "Openmindedness" (.getUsername (.asPlayer sender))))
                                                     (.setBaseValue (.getAttribute (.getOnlinePlayerByUsername (MinecraftServer/getConnectionManager) (nth (.split (.getInput context) " ") 1)) Attribute/GENERIC_SCALE) (nth (.split (.getInput context) " ") 1) (float (Integer/valueOf (nth (.split (.getInput context) " ") 2))))
                                                     )
                                                   
                                                   )))
               ^Command p))
  
  
  (.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["plot"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                   
                                                   (let [args (.split (.getInput context) " ")]
                                                       (cond
                                                         (= (count args) 1)
                                                         (let [instance ^Instance (or (iman/getInstance (.getUsername (.asPlayer sender))) (pworld/mkworld iManager (.getUsername (.asPlayer sender)) (.getUuid (.asPlayer sender))))]
                                                           (.setInstance (.asPlayer sender) instance)
                                                           (db/set-prop! (.getUsername (.asPlayer sender)) :plot (.toString (.getUuid (.asPlayer sender)))))
                                                         (= (count args) 2)
                                                         (let [playeruuid (db/get-prop (nth args 1) :plot)]
                                                           (let [instance ^Instance (or (iman/getInstance (nth args 1)) (pworld/mkworld iManager (nth args 1) (java.util.UUID/fromString playeruuid)))]
                                                           (.setInstance (.asPlayer sender) instance (Pos. 0.0 5.0 0.0 0.0 0.0))
                                                           
                                                           )
                                                         ))
                                                       ))
                                                     )
                                                   
                                                   )
               ^Command p))
  )
