(ns tradeunionorg.sim
  "Demo simulation: 5 scenarios covering happy path, hard checks, escalation."
  (:require [tradeunionorg.store :as store]
            [tradeunionorg.advisor :as advisor]
            [tradeunionorg.governor :as governor]
            [tradeunionorg.operation :as operation]))

(defn scenario-1-happy-path
  "Schedule member meeting: verified member, proper operation, governance passes."
  [_store]
  (let [proposal {:operation :schedule-member-meeting
                  :effect :propose
                  :member-id "M001"
                  :event-id "E001"
                  :reason "Schedule general assembly"}]
    {:name "Happy path: Schedule member meeting"
     :proposal proposal}))

(defn scenario-2-unverified-member
  "Unverified member: governance hard check #1 should reject."
  [_store]
  (let [proposal {:operation :schedule-member-meeting
                  :effect :propose
                  :member-id "M003"  ;; unverified
                  :event-id "E001"
                  :reason "Attempt with unverified member"}]
    {:name "Hard check #1: Unverified member rejection"
     :proposal proposal}))

(defn scenario-3-wrong-effect
  "Wrong effect: governance hard check #2 should reject."
  [_store]
  (let [proposal {:operation :schedule-member-meeting
                  :effect :approve  ;; should be :propose
                  :member-id "M001"
                  :event-id "E001"
                  :reason "Wrong effect type"}]
    {:name "Hard check #2: Wrong effect rejection"
     :proposal proposal}))

(defn scenario-4-scope-exclusion
  "Scope exclusion: proposal touches collective bargaining, hard check #3 rejects."
  [_store]
  (let [proposal {:operation :schedule-member-meeting
                  :effect :propose
                  :member-id "M001"
                  :reason "Discuss collective bargaining positions for next negotiation"}]
    {:name "Hard check #3: Scope exclusion (collective bargaining)"
     :proposal proposal}))

(defn scenario-5-safety-concern-escalation
  "Safety concern: operation auto-escalates even if governance passes."
  [_store]
  (let [proposal {:operation :flag-safety-concern
                  :effect :propose
                  :member-id "M001"
                  :concern "Member reported unsafe conditions in facility"}]
    {:name "Escalation: Safety concern"
     :proposal proposal}))

(defn run-scenarios
  "Run all 5 demo scenarios and print results."
  []
  (let [store (store/demo-store)
        scenarios [scenario-1-happy-path
                   scenario-2-unverified-member
                   scenario-3-wrong-effect
                   scenario-4-scope-exclusion
                   scenario-5-safety-concern-escalation]
        results (mapv #(let [scen (% store)
                             proposal (:proposal scen)
                             result (operation/flow store advisor/enrich-proposal governor/govern proposal)]
                        (assoc scen :result result))
                      scenarios)]
    results))

(comment
  ;; Demo runner for development
  (run-scenarios)
)
