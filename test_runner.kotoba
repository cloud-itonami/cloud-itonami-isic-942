(ns test-runner
  "nbb test runner for cloud-itonami-isic-942 trade union actor."
  (:require [tradeunionorg.test :as test]
            [tradeunionorg.sim :as sim]))

(defn -main [& args]
  (println "Running comprehensive test suite for cloud-itonami-isic-942...")
  (println "")

  (let [test-results (test/run-all-tests)
        {:keys [total passed failed results]} test-results]

    (println (str "Test Results: " passed "/" total " passed"))
    (println "")

    ;; Print individual results
    (doseq [result results]
      (if (:pass result)
        (println (str "✓ " (:test result)))
        (println (str "✗ " (:test result) " - expected " (:expected result) " got " (:actual result)))))

    (println "")
    (println (str "Total: " total " tests, " passed " passed, " failed " failed"))

    ;; Run simulation scenarios
    (println "")
    (println "Running 5 demo scenarios...")
    (let [scenarios (sim/run-scenarios)]
      (doseq [scen scenarios]
        (println (str "- " (:name scen) " -> " (:status (:result scen)))))
      (println ""))

    ;; Exit with proper code
    (if (= failed 0)
      (do (println "All tests passed!")
          (.exit js/process 0))
      (do (println (str failed " test(s) failed"))
          (.exit js/process 1)))))

(-main)
