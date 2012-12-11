(ns do-alert.core
  (:use seesaw.core))

(defn -main [& args]
  (invoke-later
   (-> (frame :title "do alert",
              :content "Hello, Seesaw",
              :on-close :exit)
       pack!
       show!)))
