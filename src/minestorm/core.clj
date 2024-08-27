(ns minestorm.core
  (:gen-class)
  (:import [net.minestom.server MinecraftServer] ;minestorm
           [net.minestom.server.instance Instance InstanceContainer InstanceManager IChunkLoader]
           [net.minestom.server.event GlobalEventHandler]
           [net.minestom.server.event.player AsyncPlayerConfigurationEvent]
           [net.minestom.server.instance.block Block Block$Setter]
           [net.minestom.server.coordinate Vec Pos Point]
                                        ; entitys
           [net.minestom.server.entity EntityType Entity Player EntityCreature]
           [net.minestom.server.entity.ai EntityAIGroupBuilder ]
           [net.minestom.server.entity.ai.goal  RandomLookAroundGoal RandomStrollGoal MeleeAttackGoal]
           [net.minestom.server.entity.ai.target LastEntityDamagerTarget ClosestEntityTarget]
           
           [net.minestom.server.instance.generator UnitModifier GenerationUnit]
           [net.minestom.server.event EventNode]
           [net.minestom.server.world.biome Biome Biome$Builder]
           [net.minestom.server.command.builder CommandExecutor Command CommandContext]
           [net.minestom.server.command CommandSender]
           [net.minestom.server.utils.time TimeUnit]
           [net.minestom.server.entity.metadata.display BlockDisplayMeta]
           [net.minestom.server.timer TaskSchedule]
           [net.minestom.server.item ItemStack Material]
                                        ;fastnoise
           [noise FastNoiseLite FastNoiseLite$NoiseType FastNoiseLite$FractalType]
           [net.kyori.adventure.resource ResourcePackRequest ResourcePackRequest$Builder ResourcePackInfo ResourcePackInfoLike]
           [net.minestom.server.instance.anvil AnvilLoader]
                                        ;raycast
           [dev.emortal.rayfast.casting.grid GridCast]
           [dev.emortal.rayfast.vector Vector3d]
                                        ;java 
           [java.util List]
           
           )
  )

(set! *warn-on-reflection* true)
(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (def noiseS (FastNoiseLite.))
  (def power2 4)
  (.SetNoiseType ^FastNoiseLite noiseS  FastNoiseLite$NoiseType/OpenSimplex2)
  (.SetFractalType ^FastNoiseLite noiseS  FastNoiseLite$FractalType/DomainWarpProgressive)
  (.SetSeed ^FastNoiseLite noiseS 1234)
  (.SetFrequency ^FastNoiseLite noiseS 0.01)
  (def noiseS2 (FastNoiseLite.))
  (.SetNoiseType ^FastNoiseLite noiseS  FastNoiseLite$NoiseType/OpenSimplex2)
  (.SetFractalType ^FastNoiseLite noiseS  FastNoiseLite$FractalType/DomainWarpProgressive)
  (.SetSeed ^FastNoiseLite noiseS 123)
  (.SetFrequency ^FastNoiseLite noiseS 0.01)
  
  (def server (MinecraftServer/init))
  (defn gen-tree
    [x y z unit start top bottom biome]
    (.fork ^GenerationUnit unit
           (reify
             java.util.function.Consumer
             (accept [this setter]
               (doseq [y2 (range -4 3)
                       x2 (range -2 3)
                       z2 (range -2 3)
                       :when (and (< (if (< z2 0) (- z2) z2) (- y2)) (< (if (< x2 0) (- x2) x2) (- y2)))]
                 (.setBlock ^Block$Setter setter (float (+ x x2)) (float (+ y y2 8)) (float (+ z z2)) top))
               (doseq [y (range y (+ y 5))]
                 (.setBlock ^Block$Setter setter (float x) (float y) (float z) bottom)
                 )
               )
             )))
  (defn gen-grass
    [x y z unit]
    (.fork ^GenerationUnit unit
           (reify
             java.util.function.Consumer
             (accept [this setter]
               (.setBlock ^Block$Setter setter (float x) (float (+ y 1)) (float z) Block/SHORT_GRASS)))))

  (def iManager (MinecraftServer/getInstanceManager))
  (def instance (.createInstanceContainer ^InstanceManager iManager))

  (defn deletefn
    [mentry iters]
    nil)
  (defn bouncefn
    [mentry iters]
    (.teleport  ^Entity (:entity mentry) (.add (.getPosition ^Entity (:entity mentry)) 0.0 (+ (float (:y (:velocity mentry))) 1) 0.0))

    (assoc-in mentry [:velocity :y] 1))
  (defn stockmove
    [mentry iters]
    (.setVelocity ^Entity (:entity mentry) (Vec. 0.0))
    (.teleport  ^Entity (:entity mentry) (.add (.getPosition ^Entity (:entity mentry)) (float (:x (:velocity mentry))) (float (:y (:velocity mentry))) (float (:z (:velocity mentry)))))
    (update-in mentry [:velocity :y] #(max -1 (- % 0.05))))
  (defn dloop
    [mmap speed iterations movfn colfn]
    (let [mvec (vec (remove nil? (for [mentry mmap] 
                                   (do 
                                     (if (not (.isAir (.getBlock (.getInstance  ^Entity (:entity mentry)) (.getPosition  ^Entity (:entity mentry)))))
                                       (or (colfn mentry iterations) (.remove ^Entity (:entity mentry)))
                                       (movfn mentry iterations))))))]
      (if (not (nil? (:entity (first mvec))))
        (.scheduleTask (.scheduler ^Entity (:entity (first mvec)))
                       (reify Runnable
                         (run [this]
                           (dloop mvec speed iterations movfn colfn)
                           )) (TaskSchedule/tick speed) (TaskSchedule/stop)))))

  (defn explodefn
    [mentry iters]
    (let [point (.getPosition ^Entity (:entity mentry))]
      (dloop (remove nil? (vec (flatten (for [x (range -1 1 (/ 4 (/ power2 0.8)))
                                              y (range -1 1 (/ 4 (/ power2 1)))
                                              z (range -1 1 (/ 4 (/ power2 0.8)))]
                                          (let [iterator (GridCast/createGridIterator (.x point) (.y point) (.z point) x y z 1.0 power2)]
                                            (loop [power power2 vlist []]
                                              (if (and (< 0 power) (.hasNext iterator))
                                                (let [n ^Vector3d (.next iterator)]
                                                  (let [old (.getBlock (.getInstance ^Entity (:entity mentry)) (float (.x n)) (float (.y n)) (float (.z n)))]
                                                    (recur (long (- power (+ (rand-int 3) (.explosionResistance (.registry old)))))
                                                           (conj vlist
                                                                 (if (not (.isAir old))
                                                                   (do (let [e ^Entity (proxy  [Entity]
                                                                                           [EntityType/BLOCK_DISPLAY])
                                                                             pos ^Pos (.getPosition ^Entity (:entity mentry))
                                                                             instance ^Instance (.getInstance ^Entity (:entity mentry))]
                                                                         (.setNoGravity  ^Entity e true)
                                                                         (.setPosRotInterpolationDuration ^BlockDisplayMeta (.getEntityMeta e) 2)
                                                                         (.setBlockState ^BlockDisplayMeta (.getEntityMeta e) old)
                                                                         (.setInstance  ^Entity e  ^Instance instance (Pos. (float (.x n)) (float (.y n)) (float (.z n)) (- (rand-int 360) 180) (- (rand-int 360) 180)))
                                                                         (.setBlock instance (float (.x n)) (float (.y n)) (float (.z n)) Block/AIR)
                                                                         (.scheduleTask (.scheduler instance)
                                                                                        (reify Runnable
                                                                                          (run [this]
                                                                                            (.setBlock instance (float (.x n)) (float (.y n)) (float (.z n)) old)

                                                                                            )) (TaskSchedule/tick (+ 40 (rand-int 10))) (TaskSchedule/stop))
                                                                         {:entity e :velocity {:x (/ (- (.x n) (.x pos)) 10) :y (/ (+ 5 (- (.y n) (.y pos))) 5) :z (/ (- (.z n) (.z pos)) 10)}}))))))) vlist))))))) 1 0 stockmove deletefn)) nil)
  
  (defn summon-tnt
    [instance pos d]

    (let [p ^Entity (proxy  [Entity]
                        [EntityType/BLOCK_DISPLAY])]
      (.setNoGravity  ^Entity p true)
      (.setBlockState ^BlockDisplayMeta (.getEntityMeta p) Block/TNT)
      (.setInstance  ^Entity p  ^Instance instance ^Pos pos)
      (.setPosRotInterpolationDuration ^BlockDisplayMeta (.getEntityMeta p) 4)
      (dloop [{:entity p :velocity d}] 1 0 stockmove explodefn)))
  
  (def gEventHandler (MinecraftServer/getGlobalEventHandler))
  (def cmanager (MinecraftServer/getCommandManager))
  (def b ^Biome$Builder (Biome/builder))
  (def mb (do 
            (.downfall ^Biome$Builder b 0.3)
            (.temperature ^Biome$Builder b 0.3)
            (.build ^Biome$Builder b)))

  (def mbiome (.register (MinecraftServer/getBiomeRegistry) "corruption" mb))
  
  (def noiseP (FastNoiseLite.))

  (.SetFractalType ^FastNoiseLite noiseP  FastNoiseLite$FractalType/DomainWarpProgressive)
  (.SetNoiseType ^FastNoiseLite noiseP  FastNoiseLite$NoiseType/OpenSimplex2)

  (.SetFrequency ^FastNoiseLite noiseP 0.005)

  (defn fillstock
    [mmap unit x z]
    (let [height2 (.GetNoise ^FastNoiseLite noiseS x z)
          interest (.GetNoise ^FastNoiseLite noiseS (* x 40) (* z 40))]
      (doseq [y (range -64  (+ 120 (- (* 10 height2) 2)))]
        (.setBiome ^UnitModifier (.modifier ^GenerationUnit unit) (int x) (int y) (int z) Biome/PLAINS))
      (doseq [y (range -64 (+ 100 (- (* 10 height2) 2)))]
        (cond
          (= (int y) -64) (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/BEDROCK)
          (= (int y) (int (+ 100  (- (* 10 height2) 2)))) (do
                                                            (cond
                                                              (> interest 0.95)
                                                              (gen-tree x y z unit ^Point (:start mmap) Block/OAK_LEAVES Block/OAK_LOG Biome/PLAINS)
                                                              (> interest 0.7)
                                                              (gen-grass x y z unit))
                                                            (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) (:upper mmap)))
          :else
          (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (long x) (long y) (long z) (:lower mmap))))))
  
  (defn fillcorruption
    [mmap unit x z]
    (let [height2  (.GetNoise ^FastNoiseLite noiseS x z)
          holes (java.lang.Math/abs (.GetNoise ^FastNoiseLite noiseS2 x z))
          interest (.GetNoise ^FastNoiseLite noiseS (* x 40) (* z 40))]
      (doseq [y (range -64 (+ 120 (- (* 10 height2) 2)))]
        (.setBiome ^UnitModifier (.modifier ^GenerationUnit unit) (int x) (int y) (int z) mbiome))

      (doseq [y (range -64 (+ 100  (- (* 10 height2) 2)))]
        (let [yrand (* (- (* y holes) 200) 0.8)]
          (cond
            (= (int y) -64) (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/BEDROCK)
            (and (> (* 105 (java.lang.Math/abs holes)) (java.lang.Math/abs yrand)) (> holes (* (:dist mmap) 0.9))) nil
            (and (> (* 108 (java.lang.Math/abs holes)) (java.lang.Math/abs yrand)) (> holes (* (:dist mmap) 0.8))) (if (> (java.lang.Math/abs (.GetNoise ^FastNoiseLite noiseS (* 10 x) (* 10 y) (* 10 z))) 0.1)
                                                                                                                     (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/OBSIDIAN))
            
            (and (> (* 111 (java.lang.Math/abs holes)) (java.lang.Math/abs yrand)) (> holes (* (:dist mmap) 0.7))) (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) Block/OBSIDIAN)
            
            (= (int y) (int (+ 100 (- (* 10 height2) 2))))
            (do
              (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) (:upper mmap))
              (cond
                (> interest 0.95)
                (gen-tree x y z unit ^Point (:start mmap) Block/OAK_LEAVES Block/OAK_LOG mbiome)
                
                (> interest 0.7)
                (gen-grass x y z unit)
                
                )
              (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (float x) (float y) (float z) (:upper mmap)))
            :else
            (.setBlock ^UnitModifier (.modifier ^GenerationUnit unit) (long x) (long y) (long z) (:lower mmap)))))))
  
  (def temptable
    [; ifn takes in x y z height2 height3 interest
     {:temp 2 ; marsh
      :humidity 2
      :ifn #(fillstock (assoc % :upper Block/GRASS_BLOCK :lower Block/DIRT) %2 %3 %4)}
     {:temp 5 ; corruption
      :humidity 5
      :ifn #(fillcorruption (assoc % :upper Block/GRASS_BLOCK :lower Block/DEEPSLATE) %2 %3 %4)}])

  (.setChunkLoader  ^InstanceContainer instance ^IChunkLoader (AnvilLoader. "worlds/test"))
  (.setChunkSupplier ^Instance instance
                     (reify
                       net.minestom.server.utils.chunk.ChunkSupplier
                       (createChunk [this instance chunkx chunky]
                         (net.minestom.server.instance.LightingChunk. instance chunkx chunky))))
  (.setGenerator ^Instance instance
                 (reify
                   net.minestom.server.instance.generator.Generator
                   (generate [this unit]
                     (let [start ^Point (.absoluteStart ^GenerationUnit unit)
                           size ^Point (.size ^GenerationUnit unit)]
                       
                       (doseq [x (range 0 (.blockX ^Point size))
                               z (range 0 (.blockZ ^Point size))]
                         (let [temperature (java.lang.Math/abs (* 10 (.GetNoise ^FastNoiseLite noiseP (/ (+ ^Point (.blockX ^Point start) x) 10) (/ (+ ^Point (.blockZ ^Point start) z) 10))))
                               humidity (java.lang.Math/abs (* 10 (.GetNoise ^FastNoiseLite noiseP (/ (+ ^Point (.blockX ^Point start) x) 10) (/ (+ ^Point (.blockZ ^Point start) z) 10))))]
                           (let [biome (loop [i 0.0 closest 1024.0 closestindex 0.0]
                                         (if (< i (count temptable))
                                           (let [x2 (nth temptable i)]
                                             (if (< (+ (java.lang.Math/pow (- temperature (:temp x2)) 2) (java.lang.Math/pow (- humidity (:humidity x2)) 2)) closest  )
                                               (recur (+ i 1) (+ (java.lang.Math/pow (- temperature (:temp x2)) 2) (java.lang.Math/pow (- humidity (:humidity x2)) 2)) i)
                                               (recur (+ i 1) closest closestindex)))
                                           {:biome (nth temptable closestindex) :dist closest}))]
                             ((:ifn (:biome biome)) {:temp temperature :humidity humidity :start start :size size :dist (:dist biome)} ^GenerationUnit unit (+ ^Point (.blockX start) x) (+ ^Point (.blockZ start) z)))))))))

  (.addListener ^GlobalEventHandler gEventHandler net.minestom.server.event.player.AsyncPlayerConfigurationEvent
                (reify
                  java.util.function.Consumer
                  (accept [this event]
                    (.setSpawningInstance ^net.minestom.server.event.player.AsyncPlayerConfigurationEvent event ^Instance instance)
                    (.setRespawnPoint (.getPlayer ^net.minestom.server.event.player.AsyncPlayerConfigurationEvent event) ^Point (Pos. 0.0 160.0 0.0)))))
  (.addListener ^GlobalEventHandler gEventHandler net.minestom.server.event.player.PlayerSpawnEvent
                (reify
                  java.util.function.Consumer
                  (accept [this event]
                    (.setGameMode ^Player (.getPlayer ^net.minestom.server.event.player.PlayerSpawnEvent event) net.minestom.server.entity.GameMode/CREATIVE)
                    (.sendResourcePacks ^Player (.getPlayer ^net.minestom.server.event.player.PlayerSpawnEvent event)
                                        (let [b ^ResourcePackRequest$Builder (ResourcePackRequest/resourcePackRequest)]
                                          (.required b true)
                                          (.replace b true)
                                          (.packs b ^ResourcePackInfoLike [(.get (.computeHashAndBuild (.uri (ResourcePackInfo/resourcePackInfo) (java.net.URI/create "https://download.mc-packs.net/pack/a76a6d61ec6a4e45355c3456d724f93c4e3ebce2.zip"))))])
                                          ^ResourcePackRequest (.build b))))))

  (.addListener ^GlobalEventHandler gEventHandler net.minestom.server.event.player.PlayerHandAnimationEvent
                (reify
                  java.util.function.Consumer
                  (accept [this event]
                    (let [sender ^Player (.getPlayer ^net.minestom.server.event.player.PlayerHandAnimationEvent event)]
                      (if (= Material/TNT (.material (.getItemInMainHand ^Player (.getPlayer ^net.minestom.server.event.player.PlayerHandAnimationEvent event) )))
                      (summon-tnt ^Instance (.getInstance sender) ^Pos (.getPosition sender)
                                  (let [pos (.direction (.getPosition sender))]
                                    {:x (.x pos) :y (+ (.y pos) 0.5) :z (.z pos)})))))))
  (.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["size"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                   (let [^Player sender ^CommandContext context]
                                                     (def power2 (Integer/parseInt (nth (.split (.getInput context) " ") 1)))))))
               ^Command p))

  (.register ^net.minestom.server.command.CommandManager cmanager ^Command
             (let [p ^Command (proxy
                                  [Command]
                                  ["save"])]
               (.setDefaultExecutor ^Command p (reify
                                                 CommandExecutor
                                                 (apply [this sender context]
                                                     (.saveChunksToStorage ^Instance instance))))
               ^Command p))

  
  (net.minestom.server.extras.velocity.VelocityProxy/enable "hXt2TN42ucml")
  (.start ^MinecraftServer server "0.0.0.0" 30066))

