(ns do-alert.core)

(import '(java.util Calendar))
(import '(javax.swing JOptionPane))
(import '(java.awt Image MenuItem PopupMenu SystemTray Toolkit TrayIcon))
(import '(java.awt.event ActionEvent ActionListener))

;; utils -----------------------------------------------------------------------
;; store all timers.
(def timers-vector (java.util.Vector.))

(defn parse-int [s]
  (try (Integer. (re-find  #"\d+" s))
       (catch Exception e nil)))

(defn now []
  (System/currentTimeMillis))

(defn str-to-time [time-str]
  (let [hour-min (clojure.string/split time-str #":")
        calendar (Calendar/getInstance)
        hour (parse-int (first hour-min))
        min (parse-int (second hour-min))]
    (if (and (number? hour) (number? min))
      (do
        (doto calendar
          (.set Calendar/HOUR_OF_DAY hour)
          (.set Calendar/MINUTE min))
        (.. calendar getTime getTime))
      0)))

(defn count-interval [end-time]
  (-  end-time (now)))

(defn read-from-file [file-path]
  (line-seq (clojure.java.io/reader (clojure.java.io/file file-path))))

;; menu ------------------------------------------------------------------------
(def menu (PopupMenu.))

(defn load-image [path]
  (.getImage (Toolkit/getDefaultToolkit) (clojure.java.io/resource path)))

(defn create-icon [img tip auto-size]
  (let [icon (TrayIcon. img tip menu)]
    (.setImageAutoSize icon auto-size)
    icon))

;; item ------------------------------------------------------------------------
(defn create-item [label & [call-back]]
  (let [item (MenuItem. label)]
    (if (not= call-back nil)
      (.addActionListener item
                          (reify ActionListener
                            (actionPerformed [this evt]
                              (call-back)))))
    item))

(defn create-time-item [label interval call-back]
  (let [item (MenuItem. label)
        timer (javax.swing.Timer.
               interval
               (reify ActionListener
                 (actionPerformed [this evt]
                   (call-back)
                   (.. evt getSource stop))))]
    (println (str "interval: " interval))
    (.start timer)
    (.add timers-vector timer)
    item))

(defn add-item [item]
  (.add menu item))

(defn read-tasks []
  (doseq [line (read-from-file (str (System/getProperty "user.home") (java.io.File/separator) "tasks.txt"))]
    (let [time-str (first (clojure.string/split line #"\s+"))
          task-str (second (clojure.string/split line #"\s+"))
          interval (count-interval (str-to-time time-str))]
      (if (pos? interval)
        (add-item
         (create-time-item
          line
          interval
          #(do (println "call-back called!")
               (JOptionPane/showMessageDialog nil, (str "It's time to " task-str)))))))))

(defn remove-all-items []
  (.removeAll menu))

(defn stop-n-clear-timers []
  (doseq [timer timers-vector] (.stop timer))
  (.clear timers-vector))

(defn add-close []
  (add-item (create-item "close" #(System/exit 0))))

(defn add-refresh []
  (add-item
   (create-item "refresh" #(do (remove-all-items)
                               (stop-n-clear-timers)
                               (add-refresh)
                               (read-tasks)
                               (add-close)))))

;; main ------------------------------------------------------------------------
(defn init-tray []
  (let [tray (SystemTray/getSystemTray)]
    (.add tray (create-icon (load-image "clock-icon.png") "SystemTray Demo" true))))

(defn -main []
  (init-tray)
  (add-refresh)
  (read-tasks)
  (add-close))
