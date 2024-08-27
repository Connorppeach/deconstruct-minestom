(defproject minestorm "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "LATEST"]
                 [net.minestom/minestom-snapshots "a521c4e7cd"] ; known working on a521c4e7cd
                 [com.github.EmortalMC/Rayfast "1.0.0"]
                 [ch.qos.logback/logback-core "1.5.7"]
                 ]
  :repositories [["https://jitpack.io" "https://jitpack.io"]]
  :main ^:skip-aot minestorm.core
  :target-path "target/%s"


  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :jar {:aot :all}
             :dev {:plugins [[lein-shell "0.5.0"]]}}
  :native-image {:name "minestorm"                 ;; name of output image, optional
                 :opts ["--verbose" "--initialize-at-build-time"
                        "-H:IncludeResources=.*"
                        "--gc=G1"
                        ;"--native-compiler-path=/opt/intel/oneapi/compiler/2024.1/bin/icx"
                        ;"--native-compiler-options=-O3"
                        "-O1"
                        "--enable-url-protocols=https"
                        "--pgo";-instrument"
                        "-march=compatibility"
                        "--initialize-at-run-time=net.minestom.server.item.component.PotDecorations"
                        "--initialize-at-run-time=net.minestom.server.entity.Entity"
                        "--initialize-at-run-time=net.minestom.server.entity.MetadataImpl"
                        "--initialize-at-run-time=net.minestom.server.particle.Particles"
                        "--initialize-at-run-time=net.minestom.server.particle.ParticleImpl"
                        "--initialize-at-run-time=net.minestom.server.listener.preplay.LoginListener"
                        "--initialize-at-run-time=net.minestom.server.registry.Registry"
                        "--trace-class-initialization=net.minestom.server.registry.Registry"
                        "--trace-class-initialization=net.minestom.server.MinecraftServer"
                        "--initialize-at-run-time=net.minestom.server.message.ChatType"
                        "--initialize-at-run-time=net.minestom.server.item.ItemStack"
                        "--initialize-at-run-time=net.minestom.server.instance.block.Blocks"
                        "--trace-class-initialization=net.minestom.server.item.ItemComponent,net.minestom.server.item.ItemStackImpl,net.minestom.server.instance.block.BlockImpl,net.minestom.server.instance.block.Blocks,net.minestom.server.item.ItemStack"
                        "--initialize-at-run-time=net.minestom.server.entity.EntityTypeImpl"
                        "--initialize-at-run-time=net.minestom.server.item.MaterialImpl"
                        "--initialize-at-run-time=net.minestom.server.item.Materials"
                        "--initialize-at-run-time=net.minestom.server.entity.EntityTypes"
                        "--trace-object-instantiation=net.minestom.server.entity.EntityTypeImpl"
                        "--initialize-at-run-time=net.minestom.server.item.ItemComponent"
                        "--initialize-at-run-time=net.minestom.server.instance.block.BlockImpl"
                        "--initialize-at-run-time=minestorm.core.main"
                        "--initialize-at-run-time=net.minestom.server.registry.DynamicRegistryImpl"
                        "--initialize-at-run-time=net.minestom.server.registry.DynamicRegistry"
                        "--initialize-at-run-time=net.minestom.server.listener.preplay.ConfigListener"
                        "--initialize-at-run-time=net.minestom.server.ServerProcessImpl"
                        "--initialize-at-run-time=net.minestom.server.MinecraftServer"
                        "--initialize-at-run-time=net.minestom.server.instance.InstanceManager"
                        "--initialize-at-run-time=net.minestom.server.listener.ChatMessageListener"
                        "--initialize-at-run-time=net.minestom.server.listener.PlayConfigListener"
                        "--initialize-at-run-time=net.minestom.server.entity.Player"
                        "--initialize-at-run-time=net.minestom.server.network.player.PlayerSocketConnection"
                        "--initialize-at-run-time=net.minestom.server.utils.ObjectPool"
                        "--initialize-at-run-time=net.minestom.server.adventure.audience.Audiences"
                        "--initialize-at-run-time=net.minestom.server.instance.DynamicChunk"
                        "--initialize-at-run-time=net.minestom.server.listener.BlockPlacementListener"
                        "--initialize-at-run-time=net.minestom.server.instance.block.predicate.BlockTypeFilter$Tag"
                        "--initialize-at-run-time=net.minestom.server.network.packet.server.common.TagsPacket"
                        "--trace-object-instantiation=java.lang.ref.Cleaner"
                        "--initialize-at-run-time=net.minestom.server.FeatureFlags"
                        "--initialize-at-run-time=net.minestom.server.FeatureFlagImpl"
                        "--initialize-at-run-time=net.minestom.server.potion.PotionEffects"
                        "--initialize-at-run-time=net.minestom.server.potion.PotionTypeImpl"
                        "--initialize-at-run-time=net.minestom.server.potion.PotionEffectImpl"
                        "--initialize-at-run-time=net.minestom.server.utils.entity.EntityUtils"
                        "--initialize-at-run-time=net.minestom.server.event.server.ClientPingServerEvent"
                        "--initialize-at-run-time=net.minestom.server.entity.attribute.AttributeImpl"
                        "--initialize-at-run-time=net.minestom.server.utils.time.TimeUnit"
                        "--initialize-at-run-time=net.minestom.server.sound.SoundEvents"
                        "--initialize-at-run-time=net.minestom.server.command.builder.arguments.Argument"
                        "--initialize-at-run-time=net.minestom.server.entity.attribute.Attributes"
                        "--initialize-at-run-time=net.minestom.server.potion.PotionTypes"
                        "--initialize-at-run-time=net.minestom.server.utils.time.Tick"
                        "--initialize-at-run-time=net.minestom.server.sound.BuiltinSoundEvent"
                        "--initialize-at-run-time=net.minestom.server.instance.InstanceContainer"
                        "--initialize-at-run-time=net.minestom.server.instance.anvil.AnvilLoader"]}  
  :plugins [[io.taylorwood/lein-native-image "0.3.1"]])
