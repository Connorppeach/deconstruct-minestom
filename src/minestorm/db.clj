(ns minestorm.db
  (:require  [datalevin.core :as d]))
(set! *warn-on-reflection* true)

(def schema {:name {:db/valueType :db.type/string
                    :db/unique    :db.unique/identity}})

(def conn (atom nil))
(def propcache (atom nil))

(defn initdb
  []
  (reset! conn (d/get-conn "./data/db" schema))
  (d/datalog-index-cache-limit (d/db @conn) 1024)
  (reset!  propcache {})
  )

(defn username->dbid
  [username]
  (d/entid (d/db @conn) [:name username]))

(defn get-prop
  [username key]
  (if (and (contains? @propcache username) (contains? (get @propcache username) key))
    (let [val (get (get @propcache username) key)]
        val
        )
    (let [id (username->dbid username)]
      (if (nil? id)
        nil
        (do
          (let [val (get (d/pull (d/db @conn) [:db/id, key] id) key)]
          (swap! propcache assoc-in [username key] val)
          val))))))

(defn set-prop!
  [username key value]
  (swap! propcache assoc-in [username key] value)
  (d/transact! @conn
             [{:name username, :db/id -1 key value}])
  )
(defn add-prop-if-nil!
  [username key value]
  (if (nil? (get-prop username key))
    (set-prop! username key value))
  )





