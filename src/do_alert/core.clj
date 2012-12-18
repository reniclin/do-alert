(ns do-alert.core)

(import '(java.util Calendar))
(import '(javax.swing JOptionPane))
(import '(java.awt Image MenuItem PopupMenu SystemTray Toolkit TrayIcon))
(import '(java.awt.event ActionEvent ActionListener))

;; utils -----------------------------------------------------------------------
(defn parse-int [s]
  (Integer. (re-find  #"\d+" s)))

(defn now []
  (System/currentTimeMillis))

(defn str-to-time [time-str]
  (let [hour-min (clojure.string/split time-str #":")
        calendar (Calendar/getInstance)]
    (doto calendar
      (.set Calendar/HOUR_OF_DAY (parse-int (first hour-min)))
      (.set Calendar/MINUTE (parse-int (second hour-min))))
    (.. calendar getTime getTime)))

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
    item))

(defn add-item [item]
  (.add menu item))

(defn read-tasks []
  (doseq [line (read-from-file "/Users/reniclin/test.txt")]
    (let [time-str (first (clojure.string/split line #"\s+"))
          task-str (second (clojure.string/split line #"\s+"))
          interval (count-interval (str-to-time time-str))]
      (if (> interval 0)
        (add-item
         (create-time-item
          line
          interval
          #(do (println "call-back called!")
               (JOptionPane/showMessageDialog nil, (str "It's time to " task-str)))))))))

(defn add-close []
  (add-item (create-item "close" #((System/exit 0)))))

;; main ------------------------------------------------------------------------
(defn init-tray []
  (let [tray (SystemTray/getSystemTray)]
    (.add tray (create-icon (load-image "clock-icon.png") "SystemTray Demo" true))))

(defn -main []
  (init-tray)
  (read-tasks)
  (add-close))
