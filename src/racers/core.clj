(ns racers.core
  (:gen-class)
  (:require [simple-svg :as svg]
            [clojure.string :as string]
            [cljts.geom :as geom]
            [cljts.relation :as relat]
            ))

(defn prn-ret [x] (prn x) x)

(defn racer
  ([x y dir] (racer x y dir 0.0 1.0))
  ([x y dir turn] (racer x y dir turn 1.0))
  ([x y dir turn speed] {:x x :y y :dir dir :path [{:x x :y y} {:x x :y y}] :turn turn :speed speed}))

(defn step [t racer]
  (into racer
        {:x (+ (:x racer) (* t (:speed racer) (Math/cos (:dir racer))))
         :y (+ (:y racer) (* t (:speed racer) (Math/sin (:dir racer))))
         :dir (+ (:dir racer) (* t (:turn racer)))}))

(defn racer-pos [racer] (select-keys racer [:x :y]))
(defn step-racer [time-step racer]
  (let [next-racer (step time-step racer)]
    (assoc next-racer :path (conj (:path racer) (racer-pos racer)))))
(defn run-racer [time-step racer] (iterate (partial step-racer time-step) racer))

(defn step-racer-no-cross [time-step paths racer]
  (letfn [(intersects [paths line] (some true? (map #(relat/crosses? % line) paths)))]
    (let [coord (geom/c (:x racer) (:y racer))]
      (print ".")(flush)
      (loop [next-racer (step-racer time-step racer)]
        ;;(println (select-keys next-racer [:x :y :speed]))
        (print ",")
        (if (intersects paths (geom/line-string [coord (geom/c (:x next-racer) (:y next-racer))]))
          (recur (step-racer time-step (into racer {:dir (- (:dir next-racer)
                                                            (* 4.0 time-step (:turn racer))) ;; 2.0 because step just puts it back
                                                    :speed (* (:speed next-racer) 0.99)})))
          next-racer)))))

;;(defn new-racer [racers]
;;  (let [pos (max-key #(* (+ (:x %) (:y %))
;;                         (+ (:x %) (:y %)))
;;                     (reduce (fn [acc r] (conj acc (:path r))) [] racers))]
;;    (into r
;;          {:path [(racer-pos r) (racer-pos r)]
;;           :y (+ (:x r) 0.1)
;;           :speed 1.0
;;           :dir (+ (:dir r) (:turn r))
;;           :deactivated false})))

(defn run-racers-no-cross [time-step racers]
  (iterate (fn [racers]
             (println (count racers));; racers)
             (let [paths (map (fn [r] (geom/line-string (map #(geom/c (:x %) (:y %)) (:path r))))
                              racers)

                   next-racers (map (fn [racer]
                                      (if (:deactivated racer)
                                        racer
                                        (let [next-racer (step-racer-no-cross time-step paths racer)]
                                          (if (< (:speed next-racer) 0.001)
                                            (into next-racer {:deactivated true})
                                            next-racer))))
                                    racers)]
               (if (< (count (filter #(not (:deactivated %)) next-racers)) 5)
                 (conj next-racers
                       (let [r (first (filter #(not (:deactivated %)) next-racers))
                             pos {:y ((if (pos? (:y r)) + -) (:y r) 0.1)
                                  :x ((if (pos? (:x r)) + -) (:x r) 0.1)}]
                         (into r
                               {:path [pos pos]
                                :x (:x pos)
                                :y (:y pos)
                                :speed 1.0
                                :dir (- (:dir r) (* 3.0 (:turn r)))
                                :deactivated false})))
                 next-racers)))
           racers))



;;----------------------------;;

(defn to-svg
  [filename lines]
  (println filename) (flush)
  (let [pts (reduce into lines)
        xs (map :x pts)
        ys (map :y pts)]
    (spit filename
          (svg/document (- (apply min xs) 10.0)
                        (- (apply min ys) 10.0)
                        (+ (- (apply max xs) (apply min xs)) 20.0)
                        (+ (- (apply max ys) (apply min ys)) 20.0)
                        (string/join "\n" (map #(svg/polyline % 0.2) lines))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [racer-run (run-racer 10.0 (racer 0.0 0.0 0.0 (/ Math/PI 8)))
        racers [(racer 0.0 0.00 (* (/ Math/PI 5) 0) (/ Math/PI 8) 1.1)
                (racer 0.0 0.01 (* (/ Math/PI 5) 1) (/ Math/PI 8) 1.2)
                (racer 0.0 0.02 (* (/ Math/PI 5) 2) (/ Math/PI 8) 1.3)
                (racer 0.0 0.03 (* (/ Math/PI 5) 3) (/ Math/PI 8) 1.5)
                (racer 0.0 0.04 (* (/ Math/PI 5) 4) (/ Math/PI 8) 1.7)
                ;;(racer 0.0 0.05 (* (/ Math/PI 8) 5) (/ Math/PI 8) 2.3)
                ;;(racer 0.0 0.06 (* (/ Math/PI 8) 6) (/ Math/PI 8) 2.7)
                ]
        iterations 100000
        frames 450]
    ;;(to-svg "test.svg" [(:path (nth racer-run 10))])
    ;(to-svg "tests.svg" (map :path (nth (run-racers-no-cross 0.75 racers) 100000)))
    (letfn [(frame-index [x] (Math/pow (* frames frames (/ x iterations)) 0.5))
            (write-frame? [x] (not (= (Math/floor (frame-index x))
                                      (Math/floor (frame-index (dec x))))))]
      (doseq [[racers iteration] (map list (run-racers-no-cross 0.75 racers) (range iterations))]
        (if (write-frame? iteration)
          (do
            (println "Frame " (int (frame-index iteration)) " at " iteration)
            (to-svg (format "racers%05d.svg" (int (frame-index iteration))) (map :path racers))))))
    ;; 1.0259^(30*15) = 99371
    ;; 1.0238^(30*15) = 40000
    ;; for 40K frames 1.0732^150 =~ 40003
    ;; for 100K frames 1.0797^150 =~ 100000
    ))
