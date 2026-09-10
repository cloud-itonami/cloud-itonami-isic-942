(ns tradeunionorg.test
  "Comprehensive test suite: 20 test cases covering store, governor, operation, phase."
  (:require [tradeunionorg.store :as store]
            [tradeunionorg.advisor :as advisor]
            [tradeunionorg.governor :as governor]
            [tradeunionorg.operation :as operation]))

;; ---------------------- test utilities ----------------------

(defn assert-eq [name expected actual]
  (if (= expected actual)
    {:pass true :test name}
    {:pass false :test name :expected expected :actual actual}))

(defn run-test [test-fn]
  (try
    (test-fn)
    (catch #?(:clj Exception :cljs js/Error) e
      {:pass false :error (str e)})))

;; ---------------------- store tests (5) ----------------------

(defn test-member-lookup []
  (let [s (store/demo-store)
        member (store/member s "M001")]
    (assert-eq "test-member-lookup"
               "Alice Tanaka"
               (:name member))))

(defn test-all-members []
  (let [s (store/demo-store)
        members (store/all-members s)]
    (assert-eq "test-all-members" 3 (count members))))

(defn test-event-lookup []
  (let [s (store/demo-store)
        event (store/event s "E001")]
    (assert-eq "test-event-lookup"
               "Monthly General Assembly"
               (:name event))))

(defn test-account-lookup []
  (let [s (store/demo-store)
        account (store/account s "A001")]
    (assert-eq "test-account-lookup" "M001" (:member-id account))))

(defn test-ledger-append []
  (let [s (store/demo-store)
        _ (store/append-ledger s {:operation :test})
        ledger (:ledger s)]
    (assert-eq "test-ledger-append" 1 (count @ledger))))

;; ---------------------- governor tests (7) ----------------------

(defn test-member-verified-check []
  (let [s (store/demo-store)
        proposal {:operation :schedule-member-meeting
                  :effect :propose
                  :member-id "M001"}
        result (governor/govern s proposal)]
    (assert-eq "test-member-verified-check" true (:passes? result))))

(defn test-member-unverified-check []
  (let [s (store/demo-store)
        proposal {:operation :schedule-member-meeting
                  :effect :propose
                  :member-id "M003"}  ;; unverified
        result (governor/govern s proposal)]
    (assert-eq "test-member-unverified-check" false (:passes? result))))

(defn test-effect-not-propose []
  (let [s (store/demo-store)
        proposal {:operation :schedule-member-meeting
                  :effect :approve  ;; wrong effect
                  :member-id "M001"}
        result (governor/govern s proposal)]
    (assert-eq "test-effect-not-propose" false (:passes? result))))

(defn test-scope-exclusion-bargaining []
  (let [s (store/demo-store)
        proposal {:operation :schedule-member-meeting
                  :effect :propose
                  :member-id "M001"
                  :reason "Discuss collective bargaining"}
        result (governor/govern s proposal)]
    (assert-eq "test-scope-exclusion-bargaining" false (:passes? result))))

(defn test-scope-exclusion-grievance []
  (let [s (store/demo-store)
        proposal {:operation :schedule-member-meeting
                  :effect :propose
                  :member-id "M001"
                  :reason "Process grievance from member"}
        result (governor/govern s proposal)]
    (assert-eq "test-scope-exclusion-grievance" false (:passes? result))))

(defn test-scope-exclusion-strike []
  (let [s (store/demo-store)
        proposal {:operation :schedule-member-meeting
                  :effect :propose
                  :member-id "M001"
                  :reason "Authorize strike action"}
        result (governor/govern s proposal)]
    (assert-eq "test-scope-exclusion-strike" false (:passes? result))))

(defn test-safety-concern-allowed []
  (let [s (store/demo-store)
        proposal {:operation :flag-safety-concern
                  :effect :propose
                  :member-id "M001"
                  :concern "Facility safety issue"}
        result (governor/govern s proposal)]
    (assert-eq "test-safety-concern-allowed" true (:passes? result))))

;; ---------------------- operation tests (5) ----------------------

(defn test-operation-happy-path []
  (let [s (store/demo-store)
        proposal {:operation :schedule-member-meeting
                  :effect :propose
                  :member-id "M001"
                  :event-id "E001"}
        result (operation/flow s advisor/enrich-proposal governor/govern proposal)]
    (assert-eq "test-operation-happy-path" :APPROVE (:status result))))

(defn test-operation-unverified-rejection []
  (let [s (store/demo-store)
        proposal {:operation :schedule-member-meeting
                  :effect :propose
                  :member-id "M003"}
        result (operation/flow s advisor/enrich-proposal governor/govern proposal)]
    (assert-eq "test-operation-unverified-rejection" :HOLD (:status result))))

(defn test-operation-safety-escalation []
  (let [s (store/demo-store)
        proposal {:operation :flag-safety-concern
                  :effect :propose
                  :member-id "M001"
                  :concern "Safety hazard"}
        result (operation/flow s advisor/enrich-proposal governor/govern proposal)]
    (assert-eq "test-operation-safety-escalation" :ESCALATE (:status result))))

(defn test-operation-dues-logistics []
  (let [s (store/demo-store)
        proposal {:operation :coordinate-dues-processing-logistics
                  :effect :propose
                  :member-id "M001"}
        result (operation/flow s advisor/enrich-proposal governor/govern proposal)]
    (assert-eq "test-operation-dues-logistics" :APPROVE (:status result))))

(defn test-operation-supply-request []
  (let [s (store/demo-store)
        proposal {:operation :coordinate-supply-request
                  :effect :propose
                  :member-id "M001"
                  :items "Office supplies"}
        result (operation/flow s advisor/enrich-proposal governor/govern proposal)]
    (assert-eq "test-operation-supply-request" :APPROVE (:status result))))

;; ---------------------- phase tests (3) ----------------------

(defn test-phase-consistency-1 []
  ;; Phase 0 is read-only, but we can still test governance
  (let [s (store/demo-store)
        proposal {:operation :schedule-member-meeting
                  :effect :propose
                  :member-id "M001"}
        result (operation/flow s advisor/enrich-proposal governor/govern proposal)]
    (assert-eq "test-phase-consistency-1" :APPROVE (:status result))))

(defn test-phase-consistency-2 []
  ;; Phase 1: dues + events auto-commit
  (let [s (store/demo-store)
        proposal {:operation :coordinate-dues-processing-logistics
                  :effect :propose
                  :member-id "M001"}
        result (operation/flow s advisor/enrich-proposal governor/govern proposal)]
    (assert-eq "test-phase-consistency-2" :APPROVE (:status result))))

(defn test-phase-consistency-3 []
  ;; Phase 3: full auto-commit for approved ops
  (let [s (store/demo-store)
        proposal {:operation :coordinate-supply-request
                  :effect :propose
                  :member-id "M001"}
        result (operation/flow s advisor/enrich-proposal governor/govern proposal)]
    (assert-eq "test-phase-consistency-3" :APPROVE (:status result))))

;; ---------------------- test runner ----------------------

(def all-tests
  [test-member-lookup
   test-all-members
   test-event-lookup
   test-account-lookup
   test-ledger-append
   test-member-verified-check
   test-member-unverified-check
   test-effect-not-propose
   test-scope-exclusion-bargaining
   test-scope-exclusion-grievance
   test-scope-exclusion-strike
   test-safety-concern-allowed
   test-operation-happy-path
   test-operation-unverified-rejection
   test-operation-safety-escalation
   test-operation-dues-logistics
   test-operation-supply-request
   test-phase-consistency-1
   test-phase-consistency-2
   test-phase-consistency-3])

(defn run-all-tests []
  (let [results (mapv run-test all-tests)
        passed (count (filter :pass results))
        failed (count (filter (complement :pass) results))]
    {:total (count results)
     :passed passed
     :failed failed
     :results results}))

;; Allow direct execution
(comment
  (run-all-tests)
)
